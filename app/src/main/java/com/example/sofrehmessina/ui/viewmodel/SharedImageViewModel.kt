package com.example.sofrehmessina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import coil.size.Size
import com.example.sofrehmessina.util.ImageCacheManager
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * SharedImageViewModel handles the caching and management of images across the app.
 * It provides methods to:
 * 1. Ensure images are cached when needed
 * 2. Get image requests configured for caching
 * 3. Prefetch images in bulk
 * 4. Keep track of cached images to avoid duplicate loading
 */
@HiltViewModel
class SharedImageViewModel @Inject constructor(
    val imageCacheManager: ImageCacheManager
) : ViewModel() {

    companion object {
        private const val TAG = "SharedImageViewModel"
        private const val CACHE_TIMEOUT_MS = 8000L // 8 seconds timeout for caching operations
        private const val BATCH_SIZE = 10 // Number of images to prefetch in parallel
    }

    // Track which images are already being cached to avoid duplicate requests
    private val cachedImagePaths = ConcurrentHashMap<String, Boolean>()
    
    // Keep track of which images are being cached to prevent duplicate requests
    private val cachingInProgress = ConcurrentHashMap<String, Job>()
    
    // Track caching failures to avoid retrying too many times
    private val cachingFailures = ConcurrentHashMap<String, Int>()
    private val MAX_RETRY_ATTEMPTS = 2

    /**
     * Ensures an image is cached for quick loading
     * Prevents duplicate caching attempts for the same image
     * 
     * @param storageRef Firebase Storage reference to the image
     * @return true if the image was successfully cached (or already was), false otherwise
     */
    suspend fun ensureImageCached(storageRef: StorageReference): Boolean {
        val path = storageRef.path
        
        // Check if the image is already cached
        if (imageCacheManager.isImageCached(path)) {
            return true
        }
        
        // Check if we've exceeded retry attempts
        if ((cachingFailures[path] ?: 0) >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Skipping cache for $path: max retry attempts exceeded")
            return false
        }
        
        // Don't try to cache images that should bypass cache
        if (ImageCacheManager.shouldBypassCache(path)) {
            Log.d(TAG, "Skipping cache for bypass path: $path")
            return false
        }
        
        // Check if we're already caching this image - if so, just wait for it to complete
        cachingInProgress[path]?.let { job ->
            if (job.isActive) {
                Log.d(TAG, "Caching already in progress for $path, waiting for completion")
                return true
            }
        }
        
        // Actually cache the image with a timeout to prevent hanging
        return try {
            // Create a new job for this caching operation
            val cachingJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = withTimeoutOrNull(CACHE_TIMEOUT_MS) {
                        imageCacheManager.prefetchImage(storageRef)
                        true
                    } ?: false
                    
                    if (result) {
                        // Successfully cached
                        Log.d(TAG, "Successfully cached image: $path")
                        cachingFailures.remove(path)
                        cachedImagePaths[path] = true
                    } else {
                        // Timeout occurred
                        Log.w(TAG, "Timeout while caching image: $path")
                        val failures = (cachingFailures[path] ?: 0) + 1
                        cachingFailures[path] = failures
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.d(TAG, "Caching cancelled for $path")
                    } else {
                        Log.e(TAG, "Error caching image $path: ${e.message}")
                        val failures = (cachingFailures[path] ?: 0) + 1
                        cachingFailures[path] = failures
                    }
                } finally {
                    // Remove from in-progress map when done
                    cachingInProgress.remove(path)
                }
            }
            
            // Store the job
            cachingInProgress[path] = cachingJob
            
            // Return true to indicate we've started caching
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting cache job for $path: ${e.message}")
            false
        }
    }
    
    /**
     * Gets an ImageRequest for a storage reference, configured for optimal caching
     *
     * @param storageRef The Firebase Storage reference for the image
     * @param bypassCache Whether to bypass the cache for this image (useful for banners)
     * @return An ImageRequest configured for caching
     */
    suspend fun getImageRequest(
        storageRef: StorageReference,
        bypassCache: Boolean = ImageCacheManager.shouldBypassCache(storageRef.path)
    ): ImageRequest {
        // If not bypassing cache, try to ensure it's cached first
        if (!bypassCache && !imageCacheManager.isImageCached(storageRef.path)) {
            ensureImageCached(storageRef)
        }
        
        return imageCacheManager.createFirestoreImageRequest(
            storageRef = storageRef,
            size = Size.ORIGINAL,
            bypassCache = bypassCache
        )
    }
    
    /**
     * Creates an ImageRequest directly from a path string
     * This is a convenience method for UI components
     *
     * @param path The storage path or URL for the image
     * @return An ImageRequest configured for caching and fallbacks
     */
    suspend fun getImageRequestForPath(path: String): ImageRequest {
        return imageCacheManager.createImageRequestFromPath(path)
    }
    
    /**
     * Prefetches a list of images for improved performance
     * This is useful when loading a screen with multiple images
     *
     * @param imageRefs List of storage references to prefetch
     */
    suspend fun prefetchImages(imageRefs: List<StorageReference>) {
        if (imageRefs.isEmpty()) return
        
        Log.d(TAG, "Starting prefetch of ${imageRefs.size} images")
        
        // Filter out images that should bypass cache, are already cached, or have failed too many times
        val imagesToPrefetch = imageRefs.filter { ref ->
            val path = ref.path
            !ImageCacheManager.shouldBypassCache(path) && 
            !imageCacheManager.isImageCached(path) &&
            (cachingFailures[path] ?: 0) < MAX_RETRY_ATTEMPTS &&
            !cachingInProgress.containsKey(path)
        }
        
        if (imagesToPrefetch.isEmpty()) {
            Log.d(TAG, "No images need prefetching (all cached, bypassed, or failed)")
            return
        }
        
        Log.d(TAG, "Actually prefetching ${imagesToPrefetch.size} images")
        
        // Use supervisorScope so failure of one image doesn't cancel the others
        supervisorScope {
            // Process in batches to avoid overwhelming the device
            val batches = imagesToPrefetch.chunked(BATCH_SIZE)
            
            for (batch in batches) {
                Log.d(TAG, "Processing batch of ${batch.size} images")
                
                // Launch a job for each image in the batch
                batch.forEach { ref ->
                    val path = ref.path
                    
                    // Start caching the image if not already in progress
                    if (!cachingInProgress.containsKey(path)) {
                        ensureImageCached(ref)
                    }
                }
            }
        }
        
        Log.d(TAG, "Completed prefetch job initiation")
    }
    
    /**
     * Prefetches images based on path strings
     * Useful for prefetching images from categories and other models
     *
     * @param paths List of storage paths to prefetch
     */
    fun prefetchImagePaths(paths: List<String>) {
        if (paths.isEmpty()) return
        
        Log.d(TAG, "Starting prefetch of ${paths.size} images by path")
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Convert paths to storage references
                val storageRefs = paths.mapNotNull { path ->
                    if (path.isBlank()) return@mapNotNull null
                    if (path.startsWith("http")) return@mapNotNull null
                    
                    try {
                        com.google.firebase.storage.FirebaseStorage.getInstance().reference.child(path)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating storage reference for $path: ${e.message}")
                        null
                    }
                }
                
                prefetchImages(storageRefs)
            } catch (e: Exception) {
                Log.e(TAG, "Error during path-based prefetch: ${e.message}")
            }
        }
    }
    
    /**
     * Clears the cache of all images
     */
    fun clearImageCache() {
        Log.d(TAG, "Clearing image cache")
        
        // Cancel all in-progress caching jobs
        cachingInProgress.values.forEach { job ->
            job.cancel()
        }
        
        cachingInProgress.clear()
        cachingFailures.clear()
        cachedImagePaths.clear()
        imageCacheManager.clearCache()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clear in-progress tracking when ViewModel is cleared
        cachingInProgress.values.forEach { job ->
            job.cancel()
        }
        cachingInProgress.clear()
    }
} 