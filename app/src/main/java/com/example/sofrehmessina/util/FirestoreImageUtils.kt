package com.example.sofrehmessina.util

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.sofrehmessina.R

/**
 * Utility functions to handle Firestore image URLs and references
 */
object FirestoreImageUtils {
    private const val TAG = "FirestoreImageUtils"
    private const val FIREBASE_STORAGE_BASE_URL = "https://firebasestorage.googleapis.com/v0/b/"
    private const val FIREBASE_STORAGE_ALT_URL = "https://storage.googleapis.com/"
    
    // Google Drive URL patterns
    private const val GOOGLE_DRIVE_URL = "https://drive.google.com"
    private val EXTERNAL_URL_PATTERNS = listOf(
        "https://drive.google.com",
        "https://www.dropbox.com",
        "https://i.imgur.com"
    )
    
    // Tracking failed URLs to avoid repeated attempts
    private val failedUrls = mutableSetOf<String>()
    
    /**
     * Returns a placeholder resource ID based on the context
     * 
     * @param isCategory Whether the placeholder is for a category (true) or a food item (false)
     * @return Resource ID for the appropriate placeholder image
     */
    fun getPlaceholderResourceId(isCategory: Boolean): Int {
        return if (isCategory) {
            R.drawable.ic_category_placeholder
        } else {
            R.drawable.ic_food_placeholder
        }
    }
    
    /**
     * Checks if a URL is an external URL (not Firebase Storage)
     * This includes Google Drive, Dropbox, Imgur, etc.
     * 
     * @param url The URL to check
     * @return True if the URL is an external URL, false otherwise
     */
    fun isExternalUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        
        return EXTERNAL_URL_PATTERNS.any { pattern ->
            url.startsWith(pattern)
        }
    }
    
    /**
     * Checks if a URL is specifically a Google Drive URL
     * 
     * @param url The URL to check
     * @return True if the URL is a Google Drive URL, false otherwise
     */
    fun isGoogleDriveUrl(url: String): Boolean {
        return url.startsWith(GOOGLE_DRIVE_URL)
    }
    
    /**
     * Gets a clean version of an external URL for caching purposes
     * Removes query parameters that might prevent caching
     * 
     * @param url The URL to clean
     * @return A clean version of the URL suitable for caching, or null if URL is known to fail
     */
    fun getCleanExternalUrl(url: String): String? {
        // Check if this URL has previously failed
        if (failedUrls.contains(url)) {
            Log.w(TAG, "Skipping previously failed URL: $url")
            return null
        }
        
        // For Google Drive URLs, keep the ID parameter which is needed
        if (isGoogleDriveUrl(url)) {
            // Extract just the ID from Google Drive URL which is the essential part
            val idPattern = "id=([\\w-]+)".toRegex()
            val matchResult = idPattern.find(url)
            
            return if (matchResult != null) {
                val id = matchResult.groupValues[1]
                // Return a consistent URL format for caching
                "https://drive.google.com/uc?export=download&id=$id"
            } else {
                // Alternate pattern for direct Google Drive URLs
                val altPattern = "/d/([\\w-]+)".toRegex()
                val altMatch = altPattern.find(url)
                
                if (altMatch != null) {
                    val id = altMatch.groupValues[1]
                    "https://drive.google.com/uc?export=download&id=$id"
                } else {
                    // If we can't extract the ID, mark as failed and return null
                    failedUrls.add(url)
                    Log.e(TAG, "Failed to extract ID from Google Drive URL: $url")
                    null
                }
            }
        }
        
        // For other external URLs, strip query parameters
        return url.split("?").firstOrNull() ?: url
    }
    
    /**
     * Mark a URL as failed to avoid repeated attempts to load it
     * 
     * @param url The URL that failed to load
     */
    fun markUrlAsFailed(url: String) {
        failedUrls.add(url)
        Log.d(TAG, "Marked URL as failed: $url. Total failed URLs: ${failedUrls.size}")
    }
    
    /**
     * Check if a URL is known to have failed previously
     * 
     * @param url The URL to check
     * @return True if the URL has failed previously, false otherwise
     */
    fun isFailedUrl(url: String): Boolean {
        return failedUrls.contains(url)
    }
    
    /**
     * Clear the list of failed URLs (useful if you want to retry all URLs)
     */
    fun clearFailedUrls() {
        failedUrls.clear()
        Log.d(TAG, "Cleared failed URLs list")
    }
    
    /**
     * Converts a Firebase Storage URL to a Storage Reference.
     * This is useful when you have a URL stored in your database but need
     * a StorageReference for caching purposes.
     *
     * @param url The Firebase Storage URL
     * @return StorageReference or null if the URL is not a valid Firebase Storage URL
     */
    fun urlToStorageRef(url: String): StorageReference? {
        if (url.isEmpty()) {
            Log.d(TAG, "Empty URL provided")
            return null
        }
        
        // Check if this URL has previously failed
        if (failedUrls.contains(url)) {
            Log.w(TAG, "Skipping previously failed URL: $url")
            return null
        }
        
        Log.d(TAG, "Attempting to convert URL to StorageReference: $url")
        
        // Check if it's an external URL first to avoid processing
        if (isExternalUrl(url)) {
            Log.d(TAG, "URL is an external URL (e.g., Google Drive), cannot convert to StorageReference: $url")
            return null
        }
        
        return try {
            when {
                url.startsWith(FIREBASE_STORAGE_BASE_URL) -> {
                    Log.d(TAG, "URL is a Firebase Storage URL")
                    // Extract the path from the URL
                    val urlWithoutParams = url.split("?").first()
                    
                    // Format: https://firebasestorage.googleapis.com/v0/b/[BUCKET]/o/[PATH]
                    val parts = urlWithoutParams.removePrefix(FIREBASE_STORAGE_BASE_URL).split("/o/")
                    if (parts.size != 2) {
                        Log.e(TAG, "Invalid Firebase Storage URL format: $url")
                        failedUrls.add(url)
                        return null
                    }
                    
                    val bucket = parts[0]
                    val encodedPath = parts[1]
                    val path = decodePath(encodedPath)
                    
                    // Create the storage reference
                    val storage = FirebaseStorage.getInstance("gs://$bucket")
                    val ref = storage.reference.child(path)
                    Log.d(TAG, "Created StorageReference with bucket: $bucket, path: $path")
                    ref
                }
                
                url.startsWith(FIREBASE_STORAGE_ALT_URL) -> {
                    Log.d(TAG, "URL is an alternate Firebase Storage URL")
                    // Format: https://storage.googleapis.com/[BUCKET]/[PATH]
                    val urlWithoutParams = url.split("?").first()
                    val path = urlWithoutParams.removePrefix(FIREBASE_STORAGE_ALT_URL)
                    
                    // First segment is the bucket, the rest is the path
                    val segments = path.split("/", limit = 2)
                    if (segments.size != 2) {
                        Log.e(TAG, "Invalid alternate Firebase Storage URL format: $url")
                        failedUrls.add(url)
                        return null
                    }
                    
                    val bucket = segments[0]
                    val objectPath = segments[1]
                    
                    // Create the storage reference
                    val storage = FirebaseStorage.getInstance("gs://$bucket")
                    val ref = storage.reference.child(objectPath)
                    Log.d(TAG, "Created StorageReference with bucket: $bucket, path: $objectPath")
                    ref
                }
                
                url.startsWith("gs://") -> {
                    Log.d(TAG, "URL is a gs:// URL")
                    // Format: gs://[BUCKET]/[PATH]
                    val gsPath = url.removePrefix("gs://")
                    val segments = gsPath.split("/", limit = 2)
                    if (segments.size != 2) {
                        Log.e(TAG, "Invalid gs:// URL format: $url")
                        failedUrls.add(url)
                        return null
                    }
                    
                    val bucket = segments[0]
                    val objectPath = segments[1]
                    
                    // Create the storage reference
                    val storage = FirebaseStorage.getInstance("gs://$bucket")
                    val ref = storage.reference.child(objectPath)
                    Log.d(TAG, "Created StorageReference with bucket: $bucket, path: $objectPath")
                    ref
                }
                
                // Handle direct paths to images in the default bucket
                !url.contains("://") && !url.startsWith("http") -> {
                    Log.d(TAG, "URL appears to be a direct path: $url")
                    // Assume it's a direct path to an image in the default bucket
                    val storage = FirebaseStorage.getInstance()
                    val ref = storage.reference.child(url)
                    Log.d(TAG, "Created StorageReference with default bucket and path: $url")
                    ref
                }
                
                else -> {
                    Log.e(TAG, "Unknown URL format, cannot convert to StorageReference: $url")
                    failedUrls.add(url)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating StorageReference from URL: ${e.message}", e)
            failedUrls.add(url)
            null
        }
    }
    
    /**
     * Decode URL-encoded path from Firebase Storage URL
     */
    private fun decodePath(encodedPath: String): String {
        return encodedPath.replace("%2F", "/")
                         .replace("%20", " ")
                         .replace("%25", "%")
                         .replace("%3A", ":")
                         .replace("%3F", "?")
                         .replace("%3D", "=")
                         .replace("%26", "&")
    }
} 