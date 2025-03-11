package com.example.sofrehmessina.util

import android.content.Context
import android.util.Log
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.example.sofrehmessina.R
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Enhanced helper class for image loading that handles Firebase Storage errors gracefully
 * Note: Currently not used in production code, but kept for reference
 */
@Suppress("unused")
internal object ImageLoadingHelper {
    private const val TAG = "ImageLoadingHelper"
    private const val TIMEOUT_MS = 15000L // 15 seconds timeout
    private const val MAX_RETRY_ATTEMPTS = 3
    
    // Cache of failed image URLs to avoid repeatedly trying to load them
    private val failedImageUrls = mutableSetOf<String>()
    
    /**
     * Attempt to load an image from Firebase Storage with proper error handling
     * 
     * @param context Android context
     * @param imageUrl The URL of the image to load
     * @param cacheName A name for the cached file
     * @param retryAttempt Current retry attempt (starts at 0)
     * @return A File containing the downloaded image, or null if it couldn't be loaded
     */
    private suspend fun loadImageFromFirebase(
        context: Context, 
        imageUrl: String, 
        cacheName: String,
        retryAttempt: Int = 0
    ): File? = withContext(Dispatchers.IO) {
        if (imageUrl.isEmpty()) {
            Log.d(TAG, "Empty image URL provided")
            return@withContext null
        }
        
        // Skip already failed URLs
        if (failedImageUrls.contains(imageUrl)) {
            Log.d(TAG, "Skipping previously failed URL: $imageUrl")
            return@withContext null
        }
        
        // Check if this is an external URL like Google Drive
        if (FirestoreImageUtils.isExternalUrl(imageUrl)) {
            Log.d(TAG, "External URL detected: $imageUrl")
            return@withContext loadExternalImage(context, imageUrl, cacheName, retryAttempt)
        }
        
        // Try to get a storage reference from the URL
        val storageRef = FirestoreImageUtils.urlToStorageRef(imageUrl) ?: run {
            Log.e(TAG, "Could not convert URL to storage reference: $imageUrl")
            failedImageUrls.add(imageUrl)
            return@withContext null
        }
        
        try {
            // Create a local file for saving the image
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "img_${cacheName.hashCode()}.jpg")
            
            // Try to download with timeout
            withTimeout(TIMEOUT_MS) {
                storageRef.getFile(imageFile).await()
            }
            
            Log.d(TAG, "Successfully downloaded image to ${imageFile.path}")
            return@withContext imageFile
        } catch (e: StorageException) {
            logStorageException(e, imageUrl)
            
            // Try to retry a few times for certain errors
            if (shouldRetry(e) && retryAttempt < MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "Retrying image download. Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS")
                val delay = min(1000L * (retryAttempt + 1), 5000L) // Exponential backoff capped at 5 seconds
                kotlinx.coroutines.delay(delay)
                return@withContext loadImageFromFirebase(context, imageUrl, cacheName, retryAttempt + 1)
            }
            
            // Mark as failed after exhausting retries
            failedImageUrls.add(imageUrl)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image from Firebase Storage: $imageUrl", e)
            
            if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "Retrying image download. Attempt ${retryAttempt + 1}/$MAX_RETRY_ATTEMPTS")
                val delay = min(1000L * (retryAttempt + 1), 5000L) // Exponential backoff capped at 5 seconds
                kotlinx.coroutines.delay(delay)
                return@withContext loadImageFromFirebase(context, imageUrl, cacheName, retryAttempt + 1)
            }
            
            failedImageUrls.add(imageUrl)
            return@withContext null
        }
    }
    
    /**
     * Load an image from an external URL like Google Drive
     */
    private suspend fun loadExternalImage(
        context: Context,
        imageUrl: String,
        cacheName: String,
        retryAttempt: Int
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Get a clean version of the URL for external services
            val cleanUrl = FirestoreImageUtils.getCleanExternalUrl(imageUrl) ?: run {
                Log.e(TAG, "Could not get clean URL for external image: $imageUrl")
                return@withContext null
            }
            
            // Create a local file for saving the image
            val cacheDir = context.cacheDir
            val imageFile = File(cacheDir, "ext_${cleanUrl.hashCode()}.jpg")
            
            // If the file already exists in cache, just return it
            if (imageFile.exists() && imageFile.length() > 0) {
                return@withContext imageFile
            }
            
            // Download the file using HttpURLConnection
            val connection = java.net.URL(cleanUrl).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000 // 10s timeout
            connection.readTimeout = 15000 // 15s read timeout
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                // Download successful, save to file
                connection.inputStream.use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Successfully downloaded external image to ${imageFile.path}")
                return@withContext imageFile
            } else {
                Log.e(TAG, "HTTP error downloading external image: $responseCode, URL: $cleanUrl")
                
                if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                    val delay = min(1000L * (retryAttempt + 1), 5000L)
                    kotlinx.coroutines.delay(delay)
                    return@withContext loadExternalImage(context, imageUrl, cacheName, retryAttempt + 1)
                }
                
                failedImageUrls.add(imageUrl)
                return@withContext null
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO error downloading external image: $imageUrl", e)
            
            if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                val delay = min(1000L * (retryAttempt + 1), 5000L)
                kotlinx.coroutines.delay(delay)
                return@withContext loadExternalImage(context, imageUrl, cacheName, retryAttempt + 1)
            }
            
            failedImageUrls.add(imageUrl)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading external image: $imageUrl", e)
            failedImageUrls.add(imageUrl)
            return@withContext null
        }
    }
    
    /**
     * Log detailed information about a Firebase Storage exception
     */
    private fun logStorageException(e: StorageException, imageUrl: String) {
        val errorCode = e.errorCode
        val httpCode = e.httpResultCode
        
        when (errorCode) {
            StorageException.ERROR_OBJECT_NOT_FOUND -> {
                Log.e(TAG, "Image not found in Firebase Storage: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_BUCKET_NOT_FOUND -> {
                Log.e(TAG, "Storage bucket not found: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_PROJECT_NOT_FOUND -> {
                Log.e(TAG, "Firebase project not found: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_QUOTA_EXCEEDED -> {
                Log.e(TAG, "Storage quota exceeded: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_NOT_AUTHENTICATED -> {
                Log.e(TAG, "Not authenticated for Firebase Storage: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_NOT_AUTHORIZED -> {
                Log.e(TAG, "Not authorized for Firebase Storage: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> {
                Log.e(TAG, "Retry limit exceeded for Firebase Storage: $imageUrl, HTTP: $httpCode")
            }
            StorageException.ERROR_CANCELED -> {
                Log.d(TAG, "Storage operation canceled: $imageUrl, HTTP: $httpCode")
            }
            else -> {
                Log.e(TAG, "Unknown Storage error: code=$errorCode, http=$httpCode, URL: $imageUrl", e)
            }
        }
    }
    
    /**
     * Determine if we should retry a failed download based on the exception
     */
    private fun shouldRetry(e: StorageException): Boolean {
        val errorCode = e.errorCode
        val errorMessage = e.message?.lowercase() ?: ""
        
        // Specific error codes that should not be retried
        val terminalErrors = listOf(
            StorageException.ERROR_OBJECT_NOT_FOUND,
            StorageException.ERROR_BUCKET_NOT_FOUND,
            StorageException.ERROR_PROJECT_NOT_FOUND,
            StorageException.ERROR_INVALID_CHECKSUM,
            StorageException.ERROR_CANCELED
        )
        
        // Don't retry if we have a terminal error code
        if (terminalErrors.contains(errorCode)) {
            return false
        }
        
        // Retry for network-related errors or retry limit errors
        if (errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED || 
            errorMessage.contains("network") || 
            errorMessage.contains("timeout") ||
            errorMessage.contains("connect")) {
            return true
        }
        
        // For other errors, we'll try again by default
        return true
    }
    
    /**
     * Get placeholder resource ID based on the context
     */
    private fun getPlaceholderResourceId(isCategory: Boolean): Int {
        return if (isCategory) {
            R.drawable.ic_category_placeholder
        } else {
            R.drawable.ic_food_placeholder
        }
    }
    
    /**
     * Clear the cache of failed image URLs
     */
    private fun clearFailedImageCache() {
        failedImageUrls.clear()
        Log.d(TAG, "Cleared failed image URL cache")
    }
} 