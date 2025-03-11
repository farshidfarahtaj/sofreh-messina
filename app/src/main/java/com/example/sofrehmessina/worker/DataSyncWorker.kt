package com.example.sofrehmessina.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.ImageCacheManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Worker that syncs data from Firebase to the local cache
 * This is used for periodic background syncing and ensuring persistent data across app restarts
 */
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseRepository: FirebaseRepository,
    private val firebaseAuth: FirebaseAuth,
    private val imageCacheManager: ImageCacheManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DataSyncWorker"
        
        // Create a separate persistent cache directory to ensure data survives app restarts
        private const val PERSISTENT_CACHE_DIR = "persistent_cache"
        private const val PERSISTENT_IMAGE_CACHE_DIR = "images"
        private const val SHADER_CACHE_DIR = "shader_cache"
        
        // List of critical image paths to always keep in persistent storage
        private val CRITICAL_IMAGE_PATHS = listOf<String>(
            // All critical image paths removed as they don't exist in Firebase Storage
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting data sync and persistent cache operation")
            
            // Add safety check for Firebase initialization
            if (!isFirebaseReady()) {
                Log.w(TAG, "Firebase not fully initialized yet. Skipping sync operation.")
                return@withContext Result.retry()
            }
            
            // Ensure persistent cache directories exist
            val persistentCacheRoot = ensurePersistentCacheDirectories()
            
            // Sync categories
            try {
                // Try to get categories from Firestore directly 
                val categories = getCategoriesFromFirestore()
                
                // Cache the categories and their images in persistent storage
                syncCategoriesToPersistentCache(categories, persistentCacheRoot)
                
                Log.d(TAG, "Successfully synced categories to persistent cache")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing categories", e)
                // Continue with other syncs even if this one fails
            }
            
            // Sync food items
            try {
                // Get food items and cache them
                syncFoodItemsToPersistentCache(persistentCacheRoot)
                
                Log.d(TAG, "Successfully synced food items to persistent cache")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing food items", e)
            }
            
            // Always sync critical files regardless of login status
            try {
                syncCriticalImagesToPersistentCache(persistentCacheRoot)
                Log.d(TAG, "Successfully synced critical images to persistent cache")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing critical images", e)
            }
            
            // Sync user settings if user is logged in
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val settingsResult = firebaseRepository.getUserSettings()
                    
                    // Cache the user settings in a persistent location
                    settingsResult.getOrNull()?.let { settings ->
                        saveSettingsToPersistentCache(settings, persistentCacheRoot)
                    }
                    
                    Log.d(TAG, "Successfully synced user settings to persistent cache")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user settings", e)
            }
            
            // Ensure shader cache is preserved
            try {
                preserveShaderCache(persistentCacheRoot)
                Log.d(TAG, "Successfully preserved shader cache")
            } catch (e: Exception) {
                Log.e(TAG, "Error preserving shader cache", e)
            }
            
            Log.d(TAG, "Data sync and persistent cache completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during data sync", e)
            // Retry on failure
            Result.retry()
        }
    }
    
    /**
     * Creates and ensures all necessary persistent cache directories exist
     */
    private fun ensurePersistentCacheDirectories(): File {
        // Use the app's files directory for truly persistent storage
        // This survives app uninstalls unless the user chooses to clear app data
        val persistentRoot = File(applicationContext.filesDir, PERSISTENT_CACHE_DIR)
        if (!persistentRoot.exists()) {
            persistentRoot.mkdirs()
        }
        
        // Create subdirectories for different types of cached data
        val imageDir = File(persistentRoot, PERSISTENT_IMAGE_CACHE_DIR)
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        
        val shaderDir = File(persistentRoot, SHADER_CACHE_DIR)
        if (!shaderDir.exists()) {
            shaderDir.mkdirs()
        }
        
        // Log the total and available space
        val totalSpace = persistentRoot.totalSpace / (1024 * 1024)
        val freeSpace = persistentRoot.freeSpace / (1024 * 1024)
        Log.d(TAG, "Persistent cache space: $freeSpace MB free of $totalSpace MB total")
        
        return persistentRoot
    }
    
    /**
     * Gets categories from Firestore
     */
    private suspend fun getCategoriesFromFirestore(): List<Map<String, Any>> {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("categories").get().await()
            
            return snapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    // Add the document ID to the map
                    data["id"] = doc.id
                    data
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories from Firestore", e)
            return emptyList()
        }
    }
    
    /**
     * Syncs categories and their images to persistent cache
     */
    private suspend fun syncCategoriesToPersistentCache(
        categories: List<Map<String, Any>>,
        persistentRoot: File
    ) {
        // Save the categories data as JSON
        val categoriesFile = File(persistentRoot, "categories.json")
        categoriesFile.writeText(categories.toString())
        
        // Extract image paths from categories and download them
        val imagePaths = categories.mapNotNull { category ->
            category["imageUrl"] as? String
        }
        
        // Download and save the images
        val imagesDir = File(persistentRoot, PERSISTENT_IMAGE_CACHE_DIR)
        val storage = FirebaseStorage.getInstance()
        
        for (path in imagePaths) {
            try {
                // Skip external URLs in Firebase Storage operations
                if (path.contains("drive.google.com") || path.startsWith("https://") || path.startsWith("http://")) {
                    Log.d(TAG, "Skipping external URL in FirebaseStorage: $path")
                    continue
                }
                
                val storageRef = storage.reference.child(path)
                // Let the ImageCacheManager handle caching
                imageCacheManager.prefetchImage(storageRef)
                
                // Also save to the persistent location
                val file = File(imagesDir, path.replace("/", "_"))
                storageRef.getFile(file).await()
                
                Log.d(TAG, "Saved category image to persistent storage: $path")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving category image: $path", e)
            }
        }
    }
    
    /**
     * Syncs food items to persistent cache
     */
    private suspend fun syncFoodItemsToPersistentCache(persistentRoot: File) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("food_items").get().await()
            
            // Save food items data
            val foodItemsFile = File(persistentRoot, "food_items.json")
            foodItemsFile.writeText(snapshot.documents.toString())
            
            // Extract and download images
            val imagePaths = snapshot.documents.mapNotNull { doc ->
                doc.getString("imageUrl")
            }
            
            // Download and save the images
            val imagesDir = File(persistentRoot, PERSISTENT_IMAGE_CACHE_DIR)
            val storage = FirebaseStorage.getInstance()
            
            for (path in imagePaths) {
                try {
                    // Skip external URLs in Firebase Storage operations
                    if (path.contains("drive.google.com") || path.startsWith("https://") || path.startsWith("http://")) {
                        Log.d(TAG, "Skipping external URL in FirebaseStorage: $path")
                        continue
                    }
                    
                    val storageRef = storage.reference.child(path)
                    // Let the ImageCacheManager handle caching
                    imageCacheManager.prefetchImage(storageRef)
                    
                    // Also save to the persistent location
                    val file = File(imagesDir, path.replace("/", "_"))
                    storageRef.getFile(file).await()
                    
                    Log.d(TAG, "Saved food item image to persistent storage: $path")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving food item image: $path", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing food items to persistent cache", e)
        }
    }
    
    /**
     * Syncs critical images to persistent cache
     */
    private suspend fun syncCriticalImagesToPersistentCache(persistentRoot: File) {
        val imagesDir = File(persistentRoot, PERSISTENT_IMAGE_CACHE_DIR)
        val storage = FirebaseStorage.getInstance()
        
        for (path in CRITICAL_IMAGE_PATHS) {
            try {
                // Check if this is a Google Drive URL
                if (path.contains("drive.google.com") || path.startsWith("https://") || path.startsWith("http://")) {
                    Log.d(TAG, "Skipping external URL in FirebaseStorage: $path")
                    // External URLs should be handled differently or skipped for Firebase Storage
                    continue
                }
                
                val storageRef = storage.reference.child(path)
                
                // Check if the file exists in Firebase Storage before trying to download it
                try {
                    // Get metadata to check if file exists (faster than downloading)
                    storageRef.metadata.await()
                    
                    // File exists, proceed with caching
                    // Let the ImageCacheManager handle caching
                    imageCacheManager.prefetchImage(storageRef)
                    
                    // Also save to the persistent location
                    val file = File(imagesDir, path.replace("/", "_"))
                    storageRef.getFile(file).await()
                    
                    Log.d(TAG, "Saved critical image to persistent storage: $path")
                } catch (e: Exception) {
                    // File doesn't exist or other error occurred
                    if (e.message?.contains("Object does not exist") == true || 
                        e.message?.contains("404") == true) {
                        Log.w(TAG, "Critical image not found in Firebase Storage: $path. Using fallback.")
                    } else {
                        // Some other error occurred
                        Log.e(TAG, "Error checking critical image: $path", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving critical image: $path", e)
            }
        }
    }
    
    /**
     * Saves user settings to persistent cache
     */
    private fun saveSettingsToPersistentCache(settings: Any, persistentRoot: File) {
        try {
            val settingsFile = File(persistentRoot, "user_settings.json")
            settingsFile.writeText(settings.toString())
            Log.d(TAG, "Saved user settings to persistent cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user settings to persistent cache", e)
        }
    }
    
    /**
     * Ensures shader cache is preserved across app restarts
     * This copies the shader cache from the app's cache directory to our persistent location
     */
    private fun preserveShaderCache(persistentRoot: File) {
        try {
            // Find the shader cache directory in the app's cache
            val shaderCacheSource = findShaderCacheDirectory(applicationContext.cacheDir)
            
            if (shaderCacheSource != null) {
                // Copy the shader cache to our persistent location
                val shaderCacheDest = File(persistentRoot, SHADER_CACHE_DIR)
                shaderCacheSource.copyRecursively(shaderCacheDest, overwrite = true)
                Log.d(TAG, "Successfully copied shader cache to persistent storage")
            } else {
                Log.w(TAG, "Shader cache directory not found in app cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preserving shader cache", e)
        }
    }
    
    /**
     * Find the shader cache directory in the given parent directory
     */
    private fun findShaderCacheDirectory(parent: File): File? {
        // Common names for shader cache directories
        val possibleNames = listOf(
            "com.android.opengl.shaders_cache",
            "shaders_cache",
            "gpu_cache",
            "sksl_cache",
            "shader_cache"
        )
        
        // Try to find a directory with one of these names
        for (name in possibleNames) {
            val dir = File(parent, name)
            if (dir.exists() && dir.isDirectory) {
                return dir
            }
        }
        
        // Recursively search subdirectories
        parent.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val result = findShaderCacheDirectory(file)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }

    /**
     * Check if Firebase is properly initialized and ready to use
     */
    private fun isFirebaseReady(): Boolean {
        try {
            // Check if Firebase Auth is initialized
            // Just verify Firebase Auth instance without storing the reference
            FirebaseAuth.getInstance()
            
            // Simple test operations to verify Firebase components are working
            FirebaseFirestore.getInstance().collection("test").document().id
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not fully initialized", e)
            return false
        }
    }
} 