package com.example.sofrehmessina.util

import android.content.Context
import android.util.Log

import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.sofrehmessina.R
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.withTimeout

/**
 * ImageCacheManager is responsible for efficiently managing image caching for Firestore Storage images.
 * It provides mechanisms for:
 * 1. Local disk and memory caching of images
 * 2. Automatic detection of image updates in Firestore
 * 3. Versioning to handle image updates
 * 4. Fallback to generic placeholders when remote images fail to load
 * 5. Using persistent storage to survive app restarts
 */
@OptIn(ExperimentalCoilApi::class)
@Singleton
class ImageCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ImageCacheManager"
        const val CACHE_SIZE_BYTES = 150L * 1024L * 1024L // Increased to 150MB disk cache
        private const val MEMORY_CACHE_SIZE = 0.25 // 25% of app memory
        private const val PREFETCH_TIMEOUT_MS = 15_000L // 15 seconds timeout for prefetching
        
        // Persistent cache directories (must match DataSyncWorker)
        private const val PERSISTENT_CACHE_DIR = "persistent_cache"
        private const val PERSISTENT_IMAGE_CACHE_DIR = "images"
        
        // Paths that should bypass cache (always loaded from network)
        // Banners now use regular caching to fix loading issues with multiple banners
        private val BYPASS_CACHE_PATHS = listOf("promotions/")
        
        /**
         * Checks if the given path should bypass cache
         */
        fun shouldBypassCache(path: String): Boolean {
            // Special handling for banners - use cache but with shorter TTL
            if (path.startsWith("banners/")) {
                return false  // No longer bypass cache for banners
            }
            return BYPASS_CACHE_PATHS.any { path.startsWith(it) }
        }
        
        /**
         * Determines the best fallback resource based on the path type
         */
        fun getFallbackResourceForPath(path: String): Int {
            return when {
                path.startsWith("categories/") -> R.drawable.ic_category_placeholder
                path.startsWith("foods/") -> R.drawable.ic_food_placeholder
                path.startsWith("user/") -> R.drawable.ic_person
                path.startsWith("logo/") -> R.drawable.logo
                else -> R.drawable.ic_image_placeholder
            }
        }
    }

    // Cache of download URLs to avoid redundant Firebase calls
    private val downloadUrlCache = ConcurrentHashMap<String, String>()
    
    // Indicator of which images are already cached to disk
    private val cachedImagesPaths = ConcurrentHashMap<String, Boolean>()
    
    // Track failed URLs to avoid repeated attempts
    private val failedImagePaths = ConcurrentHashMap<String, Int>()
    private val maxRetryAttempts = 2
    
    // Get reference to the persistent cache directory
    private val persistentCacheRoot = File(context.filesDir, PERSISTENT_CACHE_DIR)
    private val persistentImageCache = File(persistentCacheRoot, PERSISTENT_IMAGE_CACHE_DIR)

    // Create a custom image loader with configured caching
    private val imageLoader = ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(File(context.cacheDir, "image_cache"))
                .maxSizeBytes(CACHE_SIZE_BYTES)
                .build()
        }
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(MEMORY_CACHE_SIZE)
                .build()
        }
        .respectCacheHeaders(false) // Don't respect cache headers for Firestore images
        .crossfade(true) // Enable crossfade for smoother image loading
        .build()
    
    init {
        // Initialize by restoring from persistent cache
        restoreFromPersistentCache()
    }
    
    /**
     * Restore images from the persistent cache to the regular cache
     * This ensures images are available immediately after app restart
     */
    private fun restoreFromPersistentCache() {
        try {
            if (!persistentImageCache.exists() || !persistentImageCache.isDirectory) {
                Log.d(TAG, "No persistent image cache found to restore from")
                return
            }
            
            val persistentFiles = persistentImageCache.listFiles() ?: return
            Log.d(TAG, "Found ${persistentFiles.size} files in persistent cache to restore")
            
            // Kick off a coroutine to copy files
            CoroutineScope(Dispatchers.IO).launch {
                persistentFiles.forEach { file ->
                    try {
                        // Convert filename back to path
                        val path = file.name.replace("_", "/")
                        
                        // Mark as cached
                        cachedImagesPaths[path] = true
                        
                        // Copy to disk cache
                        val cacheDest = File(context.cacheDir, "image_cache/$path")
                        if (!cacheDest.exists()) {
                            cacheDest.parentFile?.mkdirs()
                            file.copyTo(cacheDest, overwrite = true)
                        }
                        
                        Log.d(TAG, "Restored image from persistent cache: $path")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring file from persistent cache: ${file.name}", e)
                    }
                }
                
                Log.d(TAG, "Completed restoring images from persistent cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during restoration from persistent cache", e)
        }
    }
    
    /**
     * Gets a file from the persistent cache if available
     */
    private fun getFromPersistentCache(path: String): File? {
        try {
            // Convert path to filename
            val filename = path.replace("/", "_")
            val file = File(persistentImageCache, filename)
            
            return if (file.exists() && file.length() > 0) {
                file
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file from persistent cache: $path", e)
            return null
        }
    }

    /**
     * Creates an image request that efficiently caches Firestore images with versioning support
     * and generic fallbacks for missing remote images
     *
     * @param storageRef The Firebase storage reference for the image
     * @param size The desired size of the image (or null for original size)
     * @param bypassCache Whether to bypass the cache for this specific image
     * @return ImageRequest configured for optimal caching
     */
    suspend fun createFirestoreImageRequest(
        storageRef: StorageReference,
        size: Size = Size.ORIGINAL,
        bypassCache: Boolean = shouldBypassCache(storageRef.path)
    ): ImageRequest {
        val path = storageRef.path
        
        // Check for previously failed images - use appropriate fallback
        if (failedImagePaths.getOrDefault(path, 0) >= maxRetryAttempts) {
            Log.d(TAG, "Using generic fallback for failed image: $path")
            return ImageRequest.Builder(context)
                .data(getFallbackResourceForPath(path))
                .size(size)
                .crossfade(true)
                .build()
        }
        
        // Check if image is available in the persistent cache
        if (!bypassCache) {
            val persistentFile = getFromPersistentCache(path)
            if (persistentFile != null) {
                Log.d(TAG, "Using image from persistent cache: $path")
                return ImageRequest.Builder(context)
                    .data(persistentFile)
                    .size(size)
                    .crossfade(true)
                    .error(getFallbackResourceForPath(path))
                    .build()
            }
        }
        
        // Get the image URL - use cached URL if possible to avoid Firebase calls
        val imageUrl = getImageUrl(storageRef, bypassCache)
        
        // If URL retrieval failed, return a request with an appropriate fallback
        if (imageUrl.isEmpty()) {
            val fallbackResId = getFallbackResourceForPath(path)
            Log.d(TAG, "Using fallback image for $path: resource ID $fallbackResId")
            return ImageRequest.Builder(context)
                .data(fallbackResId)
                .size(size)
                .crossfade(true)
                .build()
        }
        
        // Create appropriate cache policies - always enable disk cache for better persistence
        val memoryCachePolicy = if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED
        val diskCachePolicy = if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED
        
        // Mark this path as cached for future reference
        if (!bypassCache) {
            cachedImagesPaths[path] = true
        }
        
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .size(size)
            .memoryCachePolicy(memoryCachePolicy)
            .diskCachePolicy(diskCachePolicy) // Always enable disk cache for persistent storage
            .crossfade(true) // Add crossfade for smoother transitions
            .error(getFallbackResourceForPath(path)) // Add fallback for loading errors
            .build()
    }

    /**
     * Gets the Firestore image URL, using cached URLs when possible
     * 
     * @param storageRef The Firebase storage reference
     * @param bypassCache Whether to bypass the cache for this image
     * @return URL string for the image
     */
    private suspend fun getImageUrl(
        storageRef: StorageReference,
        bypassCache: Boolean
    ): String {
        val path = storageRef.path
        
        // If we've failed multiple times with this path, return empty to avoid wasting resources
        if (failedImagePaths.getOrDefault(path, 0) >= maxRetryAttempts) {
            Log.w(TAG, "Skipping URL fetch for repeatedly failed path: $path")
            return ""
        }
        
        // If we have a cached URL and not bypassing cache, use it
        if (!bypassCache && downloadUrlCache.containsKey(path)) {
            return downloadUrlCache[path] ?: ""
        }
        
        // Otherwise fetch the URL - remove any version query params that might prevent caching
        return withContext(Dispatchers.IO) {
            try {
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                // Strip any version or cache-busting query parameters that might prevent caching
                val cleanUrl = downloadUrl.split("?").firstOrNull() ?: downloadUrl
                
                // Cache the clean URL if not bypassing cache
                if (!bypassCache) {
                    downloadUrlCache[path] = cleanUrl
                }
                
                // Clear any previous failures
                failedImagePaths.remove(path)
                
                cleanUrl
            } catch (e: Exception) {
                // Improved error handling with more detailed logging
                when (e) {
                    is com.google.firebase.storage.StorageException -> {
                        Log.e(TAG, "StorageException for $path: ${e.message}, code: ${e.errorCode}", e)
                        // Check for specific error codes
                        if (e.errorCode == -13010) { // Object does not exist
                            Log.w(TAG, "Image does not exist in Firebase Storage: $path")
                        }
                    }
                    else -> {
                        Log.e(TAG, "Error getting download URL for $path: ${e.message}", e)
                    }
                }
                
                // Track failures to avoid repeated attempts
                val failures = failedImagePaths.getOrDefault(path, 0) + 1
                failedImagePaths[path] = failures
                
                // Return an empty string on error
                ""
            }
        }
    }

    /**
     * Creates an image request directly from a path string
     * Handles both Firebase Storage paths and local fallbacks
     *
     * @param path The path string (usually from a Category.imageUrl field)
     * @param size The desired size of the image
     * @param forceReload Whether to force reloading from the source rather than using cache
     * @return ImageRequest configured appropriately
     */
    suspend fun createImageRequestFromPath(
        path: String,
        size: Size = Size.ORIGINAL,
        forceReload: Boolean = false
    ): ImageRequest {
        // If the path is empty or invalid, use a placeholder
        if (path.isBlank()) {
            return ImageRequest.Builder(context)
                .data(R.drawable.ic_image_placeholder)
                .size(size)
                .crossfade(true)
                .build()
        }
        
        // If the path starts with "http", it's already a direct URL
        if (path.startsWith("http")) {
            // Add a timestamp query parameter to force cache busting
            val cacheBustedUrl = if (forceReload) {
                val separator = if (path.contains("?")) "&" else "?"
                "$path${separator}t=${System.currentTimeMillis()}"
            } else {
                path
            }
            
            return ImageRequest.Builder(context)
                .data(cacheBustedUrl)
                .size(size)
                .crossfade(true)
                .error(R.drawable.ic_image_placeholder)
                // If forceReload is true, disable both memory and disk cache
                .memoryCachePolicy(if (forceReload) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .diskCachePolicy(if (forceReload) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .build()
        }
        
        try {
            // For Firebase Storage paths, add timestamp as a "tag" to the image request
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child(path)
            
            // Check if we should bypass normal caching rules
            val bypassCache = shouldBypassCache(path) || forceReload
            
            // Use the main method which handles fallbacks
            return createFirestoreImageRequest(storageRef, size, bypassCache)
        } catch (e: Exception) {
            // If any exception occurs during the process, return a fallback image
            Log.e(TAG, "Error creating image request for path $path: ${e.message}", e)
            return ImageRequest.Builder(context)
                .data(getFallbackResourceForPath(path))
                .size(size)
                .crossfade(true)
                .build()
        }
    }

    /**
     * Utility function to prefetch and cache an image from Firestore
     *
     * @param storageRef The Firebase storage reference for the image
     */
    suspend fun prefetchImage(storageRef: StorageReference) {
        val path = storageRef.path
        
        try {
            // Skip if path looks like an external URL (prevent storing URLs in Firebase Storage)
            if (path.startsWith("/http") || path.startsWith("http") || path.contains("drive.google.com")) {
                Log.w(TAG, "Skipping prefetch for external URL path: $path")
                return
            }
            
            // Don't prefetch images that should bypass cache
            if (shouldBypassCache(path)) {
                Log.d(TAG, "Skipping prefetch for bypass cache path: $path")
                return
            }
            
            // Skip if we've had repeated failures
            if (failedImagePaths.getOrDefault(path, 0) >= maxRetryAttempts) {
                Log.w(TAG, "Skipping prefetch for repeatedly failed path: $path")
                return
            }
            
            // Create the image request with caching enabled
            val request = createFirestoreImageRequest(storageRef)
            
            // Execute the request with a timeout to prevent hanging
            withContext(Dispatchers.IO) {
                withTimeout(PREFETCH_TIMEOUT_MS) {
                    val result = imageLoader.execute(request)
                    
                    // Check if the image was successfully cached
                    if (result.drawable != null) {
                        Log.d(TAG, "Successfully prefetched and cached image: $path")
                        // Mark as cached
                        cachedImagesPaths[path] = true
                        // Clear any previous failures
                        failedImagePaths.remove(path)
                        
                        // Also save to persistent cache
                        saveToPersistentCache(storageRef)
                    } else {
                        Log.w(TAG, "Image prefetched but may not be cached: $path")
                        
                        // Track as a failure
                        val failures = failedImagePaths.getOrDefault(path, 0) + 1
                        failedImagePaths[path] = failures
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error prefetching image $path: ${e.message}", e)
            
            // Track as a failure
            val failures = failedImagePaths.getOrDefault(path, 0) + 1
            failedImagePaths[path] = failures
            
            // Don't rethrow - let the app continue even if prefetching fails
        }
    }
    
    /**
     * Saves an image to the persistent cache
     */
    private suspend fun saveToPersistentCache(storageRef: StorageReference) {
        val path = storageRef.path
        try {
            // Skip external URLs
            if (path.startsWith("/http") || path.startsWith("http") || path.contains("drive.google.com")) {
                Log.w(TAG, "Skipping persistent cache for external URL path: $path")
                return
            }
            
            // Ensure the persistent cache directory exists
            if (!persistentImageCache.exists()) {
                persistentImageCache.mkdirs()
            }
            
            // Convert path to valid filename
            val filename = path.replace("/", "_")
            val persistentFile = File(persistentImageCache, filename)
            
            // Skip if already saved
            if (persistentFile.exists() && persistentFile.length() > 0) {
                return
            }
            
            // Download the file directly to the persistent cache
            storageRef.getFile(persistentFile).await()
            
            Log.d(TAG, "Successfully saved image to persistent cache: $path")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to persistent cache: $path", e)
        }
    }

    /**
     * Prefetches a list of images from Firebase Storage
     *
     * @param storageRefs List of Firebase storage references to prefetch
     */
    suspend fun prefetchImages(storageRefs: List<StorageReference>) {
        // Filter out images that should bypass cache or have had repeated failures
        val cachableRefs = storageRefs.filter { 
            !shouldBypassCache(it.path) && 
            failedImagePaths.getOrDefault(it.path, 0) < maxRetryAttempts
        }
        
        Log.d(TAG, "Prefetching ${cachableRefs.size} images (skipped ${storageRefs.size - cachableRefs.size} uncachable images)")
        
        // Prefetch each image individually with error handling
        cachableRefs.forEach { 
            try {
                prefetchImage(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch image ${it.path}: ${e.message}")
                // Continue with other images even if one fails
            }
        }
    }

    /**
     * Check if an image is already in cache
     * 
     * @param path The storage path of the image
     * @return True if the image is in the cache, false otherwise
     */
    fun isImageCached(path: String): Boolean {
        // First check our internal tracking
        if (cachedImagesPaths.containsKey(path)) {
            return true
        }
        
        // Then check if we have a cached URL for it
        if (downloadUrlCache.containsKey(path)) {
            return true
        }
        
        // Otherwise assume it's not cached
        return false
    }

    /**
     * Provides the custom image loader
     */
    @Suppress("unused")
    fun getImageLoader(): ImageLoader = imageLoader

    /**
     * Clears all cached images
     */
    fun clearCache() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        downloadUrlCache.clear()
        cachedImagesPaths.clear()
        failedImagePaths.clear()
    }
    
    /**
     * Ensures the cache directories exist and are writable
     */
    fun ensureCacheDirectoriesExist() {
        val cacheDir = File(context.cacheDir, "image_cache")
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            Log.d(TAG, "Created image cache directory: $created")
        } else {
            Log.d(TAG, "Image cache directory already exists")
        }
        
        // Check if the directory is writable
        if (!cacheDir.canWrite()) {
            Log.e(TAG, "Cache directory is not writable: ${cacheDir.absolutePath}")
        } else {
            Log.d(TAG, "Cache directory is writable: ${cacheDir.absolutePath}")
        }
        
        // Log the total and available space
        val totalSpace = cacheDir.totalSpace / (1024 * 1024)
        val freeSpace = cacheDir.freeSpace / (1024 * 1024)
        Log.d(TAG, "Cache directory space: $freeSpace MB free of $totalSpace MB total")
        
        // Log the content of the cache directory for debugging
        Log.d(TAG, "Cache directory contains ${cacheDir.listFiles()?.size ?: 0} files")
    }
    
    /**
     * Cleans up old cache files that exceed the specified age
     * 
     * @param maxAgeMillis Maximum age of cache files in milliseconds before they are deleted
     */
    fun cleanupOldCacheFiles(maxAgeMillis: Long) {
        val currentTimeMillis = System.currentTimeMillis()
        val cacheDir = File(context.cacheDir, "image_cache")
        
        if (!cacheDir.exists() || !cacheDir.isDirectory) {
            Log.d(TAG, "Cache directory does not exist, nothing to clean up")
            return
        }
        
        try {
            var deletedCount = 0
            var deletedBytes = 0L
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTimeMillis - file.lastModified()
                    if (fileAge > maxAgeMillis) {
                        val fileSize = file.length()
                        if (file.delete()) {
                            deletedCount++
                            deletedBytes += fileSize
                            Log.d(TAG, "Deleted old cache file: ${file.name}, age: ${fileAge / (1000 * 60 * 60)} hours")
                        }
                    }
                }
            }
            
            // Also clean up persistent cache using the same rule
            if (persistentImageCache.exists() && persistentImageCache.isDirectory) {
                persistentImageCache.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val fileAge = currentTimeMillis - file.lastModified()
                        if (fileAge > maxAgeMillis) {
                            if (file.delete()) {
                                deletedCount++
                                Log.d(TAG, "Deleted old persistent cache file: ${file.name}")
                            }
                        }
                    }
                }
            }
            
            val deletedMB = deletedBytes / (1024 * 1024)
            Log.d(TAG, "Cache cleanup completed: deleted $deletedCount files (${deletedMB}MB)")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old cache files: ${e.message}", e)
        }
    }

    /**
     * Clears any cached data for a specific image path
     * Useful for forcing a reload after an image has been updated
     *
     * @param path The storage path of the image to clear from cache
     */
    fun clearCacheForPath(path: String) {
        // Remove from URL cache
        downloadUrlCache.remove(path)
        // Remove from tracked paths
        cachedImagesPaths.remove(path)
        // Reset failure count
        failedImagePaths.remove(path)
        
        Log.d(TAG, "Cleared cache data for path: $path")
    }

    /**
     * Prepares the cache manager for a screen revisit by clearing any stale
     * cache entries and ensuring fresh image loading
     */
    fun prepareForScreenRevisit() {
        Log.d(TAG, "Preparing cache manager for screen revisit - forcing refresh of categories")
        
        // Clear ALL URL caches for categories to force fresh loading
        val categoryKeys = downloadUrlCache.keys.filter { it.startsWith("categories/") }.toList()
        categoryKeys.forEach { path ->
            Log.d(TAG, "Clearing cache for category: $path")
            downloadUrlCache.remove(path)
            cachedImagesPaths.remove(path)
        }
        
        // Clear any images that have recorded failures
        failedImagePaths.keys.forEach { path ->
            downloadUrlCache.remove(path)
            cachedImagesPaths.remove(path)
        }
        failedImagePaths.clear()
        
        // Clear memory cache in Coil
        imageLoader.memoryCache?.clear()
        
        Log.d(TAG, "Cache prepared for screen revisit - cleared ${categoryKeys.size} category entries")
    }
    
    /**
     * Clears the memory cache of image loader
     * This forces fresh loading of images
     */
    fun clearMemoryCache() {
        Log.d(TAG, "Clearing memory cache")
        imageLoader.memoryCache?.clear()
        
        // Also clear disk cache for images that should be force-reloaded
        try {
            val cacheDir = File(context.cacheDir, "image_cache")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                // Only clear category images from disk cache
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name == "categories") {
                        file.listFiles()?.forEach { categoryFile ->
                            if (categoryFile.delete()) {
                                Log.d(TAG, "Deleted category image from disk cache: ${categoryFile.path}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing disk cache: ${e.message}", e)
        }
    }
    
    /**
     * Performs a complete reset of all caches for a clean start
     * This is an extreme measure when normal cache clearing isn't working
     */
    fun forceCompleteReset() {
        Log.d(TAG, "FORCING COMPLETE CACHE RESET")
        
        // Clear all in-memory caches
        downloadUrlCache.clear()
        cachedImagesPaths.clear()
        failedImagePaths.clear()
        
        // Clear Coil caches
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
        
        // Try to invalidate image loader
        try {
            // Shut down the image loader to clear any in-progress operations
            imageLoader.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down image loader: ${e.message}", e)
        }
        
        // Delete all disk cache files for categories
        try {
            val cacheDir = File(context.cacheDir, "image_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name == "categories") {
                        Log.d(TAG, "Deleting category cache directory: ${file.path}")
                        file.deleteRecursively()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cache files: ${e.message}", e)
        }
        
        Log.d(TAG, "Complete cache reset finished")
    }

    /**
     * Special method to completely reset the image caching system for the main screen
     * This should be used when returning to the main screen from another screen
     * and images aren't loading properly
     */
    fun resetForMainScreen() {
        Log.d(TAG, "RESETTING CACHE SYSTEM FOR MAIN SCREEN")
        
        // Clear EVERYTHING first
        downloadUrlCache.clear()
        cachedImagesPaths.clear()
        failedImagePaths.clear()
        
        // Clear Coil's memory and disk caches
        imageLoader.memoryCache?.clear()
        
        try {
            // Force garbage collection to release any stuck resources
            System.gc()
            
            // Clear ALL cache directories for categories
            val cacheDir = File(context.cacheDir, "image_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name == "categories") {
                        Log.d(TAG, "Deleting category cache directory: ${file.path}")
                        file.deleteRecursively()
                    }
                }
            }
            
            // Also clear persistent cache
            if (persistentImageCache.exists() && persistentImageCache.isDirectory) {
                Log.d(TAG, "Clearing persistent image cache for categories")
                persistentImageCache.listFiles()?.forEach { file ->
                    val path = file.name.replace("_", "/")
                    if (path.startsWith("categories")) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during mainScreen reset: ${e.message}", e)
        }
        
        Log.d(TAG, "Main screen cache reset complete")
    }

    /**
     * Special method to handle banner images with appropriate caching
     * Banners need special treatment - they should cache but refresh more often
     */
    fun handleBannerImageRequest(
        storageRef: StorageReference,
        forceFresh: Boolean = false
    ): ImageRequest {
        val path = storageRef.path
        
        // For banner images, we want to use cache but with a more aggressive refresh strategy
        return ImageRequest.Builder(context)
            .data(storageRef)
            .diskCachePolicy(if (forceFresh) CachePolicy.DISABLED else CachePolicy.ENABLED) // Control disk cache based on forceFresh
            .memoryCachePolicy(if (forceFresh) CachePolicy.DISABLED else CachePolicy.ENABLED) // Control memory cache based on forceFresh
            .crossfade(true)
            .placeholderMemoryCacheKey(path) // Use the path as a memory cache key
            .setParameter("refresh", if (forceFresh) System.currentTimeMillis() else null) // Add timestamp only when forcing refresh
            .error(R.drawable.ic_image_placeholder)
            .build()
    }
}