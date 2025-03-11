package com.example.sofrehmessina.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling Firebase Storage operations
 */
@Singleton
class FirebaseStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "FirebaseStorageManager"
    private val storage = FirebaseStorage.getInstance()
    
    // Storage references for different image types
    private val foodImagesRef = storage.reference.child("foods")
    private val categoryImagesRef = storage.reference.child("categories")
    private val bannerImagesRef = storage.reference.child("banners")
    
    // Constants for URL processing
    private val FIREBASE_STORAGE_BASE_URL = "https://firebasestorage.googleapis.com/v0/b/"
    private val FIREBASE_STORAGE_ALT_URL = "https://storage.googleapis.com/"
    
    /**
     * Upload a food image to Firebase Storage
     *
     * @param uri URI of the image to upload
     * @param foodId ID of the food item
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadFoodImage(uri: Uri, foodId: String): String? {
        return uploadImage(uri, "${foodId}.jpg", foodImagesRef)
    }
    
    /**
     * Upload a category image to Firebase Storage
     *
     * @param uri URI of the image to upload
     * @param categoryId ID of the category
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadCategoryImage(uri: Uri, categoryId: String): String? {
        return uploadImage(uri, "${categoryId}.jpg", categoryImagesRef)
    }
    
    /**
     * Upload a banner image to Firebase Storage
     *
     * @param uri URI of the image to upload
     * @param bannerId ID of the banner
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadBannerImage(uri: Uri, bannerId: String): String? {
        return uploadImage(uri, "${bannerId}.jpg", bannerImagesRef)
    }
    
    /**
     * Upload an image to Firebase Storage
     *
     * @param uri URI of the image to upload
     * @param fileName Name to give the file in storage
     * @param storageRef Storage reference to upload to
     * @return URL of the uploaded image or null if upload failed
     */
    private suspend fun uploadImage(uri: Uri, fileName: String, storageRef: StorageReference): String? {
        return try {
            Log.d(TAG, "Starting image upload: $fileName to ${storageRef.path}")
            
            // Read and compress the image
            val compressedImageData = withContext(Dispatchers.IO) {
                compressImage(uri)
            }
            
            if (compressedImageData == null) {
                Log.e(TAG, "Failed to compress image: $fileName")
                return null
            }
            
            // Create a reference to the specific file
            val fileRef = storageRef.child(fileName)
            
            // Upload the file
            fileRef.putBytes(compressedImageData).await()
            
            // Get the download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Log.d(TAG, "Image uploaded successfully. URL: $downloadUrl")
            
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image $fileName: ${e.message}", e)
            null
        }
    }
    
    /**
     * Delete an image from Firebase Storage
     *
     * @param imageUrl URL of the image to delete
     * @return true if deletion was successful, false otherwise
     */
    suspend fun deleteImage(imageUrl: String): Boolean {
        return try {
            // Get the storage reference from the URL
            val storageRef = getStorageReferenceFromUrl(imageUrl)
            
            if (storageRef == null) {
                Log.e(TAG, "Could not get storage reference for URL: $imageUrl")
                return false
            }
            
            // Delete the file
            storageRef.delete().await()
            Log.d(TAG, "Image deleted successfully: $imageUrl")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image: ${e.message}", e)
            false
        }
    }
    
    /**
     * Converts a Firebase Storage URL to a StorageReference
     *
     * @param url The Firebase Storage URL
     * @return StorageReference for the URL, or null if conversion failed
     */
    private fun getStorageReferenceFromUrl(url: String): StorageReference? {
        if (url.isEmpty()) {
            Log.e(TAG, "Empty URL passed to getStorageReferenceFromUrl")
            return null
        }
        
        Log.d(TAG, "Attempting to convert URL to StorageReference: $url")
        
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
                        return null
                    }
                    
                    val bucket = parts[0]
                    val encodedPath = parts[1]
                    val path = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name())
                    
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
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating StorageReference from URL: ${e.message}", e)
            null
        }
    }
    
    /**
     * Compress an image from a Uri
     *
     * @param uri URI of the image to compress
     * @return Compressed image as a byte array, or null if compression failed
     */
    private suspend fun compressImage(uri: Uri): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                // Get the input stream from the URI
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null
                
                // Decode the bitmap
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (originalBitmap == null) {
                    Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                    return@withContext null
                }
                
                // Compress the bitmap
                val outputStream = ByteArrayOutputStream()
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                
                // Get the compressed byte array
                val compressedData = outputStream.toByteArray()
                
                // Cleanup
                outputStream.close()
                originalBitmap.recycle()
                
                compressedData
            } catch (e: Exception) {
                Log.e(TAG, "Error compressing image: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Get the default storage path for an image type
     *
     * @param type Type of image (food, category, banner)
     * @return Storage path for the image type
     */
    fun getDefaultStoragePath(type: ImageType, id: String): String {
        return when (type) {
            ImageType.FOOD -> "foods/${id}.jpg"
            ImageType.CATEGORY -> "categories/${id}.jpg"
            ImageType.BANNER -> "banners/${id}.jpg"
        }
    }
    
    /**
     * Extract ID from an image URL
     *
     * @param imageUrl The image URL
     * @param type Type of image (food, category, banner)
     * @return The extracted ID or null if extraction failed
     */
    fun extractIdFromImageUrl(imageUrl: String, type: ImageType): String? {
        val pathRegex = when (type) {
            ImageType.FOOD -> "foods/(.+?)\\.jpg".toRegex()
            ImageType.CATEGORY -> "categories/(.+?)\\.jpg".toRegex()
            ImageType.BANNER -> "banners/(.+?)\\.jpg".toRegex()
        }
        
        val matchResult = pathRegex.find(imageUrl)
        return matchResult?.groupValues?.get(1)
    }
}

/**
 * Enum representing different types of images
 */
enum class ImageType {
    FOOD,
    CATEGORY,
    BANNER
} 