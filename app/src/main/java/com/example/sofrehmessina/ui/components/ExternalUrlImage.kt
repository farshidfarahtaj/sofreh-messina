package com.example.sofrehmessina.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.example.sofrehmessina.R
import com.example.sofrehmessina.util.FirestoreImageUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ExternalUrlImage"

/**
 * A composable that displays an image from an external URL (like Google Drive)
 * with proper caching. It handles loading states and errors.
 *
 * @param url The external URL to the image (Google Drive, Dropbox, etc.)
 * @param contentDescription Content description for accessibility
 * @param modifier Modifier for the image
 * @param contentScale How the image should scale inside the bounds
 * @param isCategory Whether this image is for a category (true) or food item (false)
 */
@Composable
fun ExternalUrlImage(
    url: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isCategory: Boolean = false
) {
    val context = LocalContext.current
    
    // Check if URL is already known to fail
    if (FirestoreImageUtils.isFailedUrl(url)) {
        PlaceholderImage(
            contentDescription = contentDescription,
            modifier = modifier,
            isCategory = isCategory
        )
        return
    }
    
    // Clean the URL to ensure proper caching
    val cleanUrl = remember(url) {
        FirestoreImageUtils.getCleanExternalUrl(url)
    }
    
    // If URL couldn't be cleaned or is invalid, show placeholder
    if (cleanUrl == null) {
        PlaceholderImage(
            contentDescription = contentDescription,
            modifier = modifier,
            isCategory = isCategory
        )
        return
    }
    
    // Create a coroutine exception handler for error handling
    val errorHandler = remember {
        CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error loading external image: ${exception.message}")
            // Mark URL as failed for future reference
            FirestoreImageUtils.markUrlAsFailed(url)
        }
    }
    
    // Create a properly configured image request for caching
    val imageRequest = remember(cleanUrl) {
        ImageRequest.Builder(context)
            .data(cleanUrl)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .crossfade(300)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .error(FirestoreImageUtils.getPlaceholderResourceId(isCategory))
            .placeholder(R.drawable.ic_image_placeholder)
            .listener(
                onError = { _, result ->
                    Log.e(TAG, "Failed to load image: $cleanUrl Error: ${result.throwable}")
                    FirestoreImageUtils.markUrlAsFailed(url)
                }
            )
            .build()
    }
    
    // Log the image loading attempt and prefetch the image
    LaunchedEffect(cleanUrl) {
        try {
            // Use the error handler with withContext to catch any errors during logging or prefetch
            withContext(Dispatchers.IO + errorHandler) {
                Log.d(TAG, "Loading external image with URL: $cleanUrl (original: $url)")
                
                // Optional: Prefetch the image to trigger caching
                val imageLoader = coil.ImageLoader.Builder(context)
                    .diskCache {
                        DiskCache.Builder()
                            .directory(context.cacheDir.resolve("external_image_cache"))
                            .maxSizeBytes(100 * 1024 * 1024) // 100MB
                            .build()
                    }
                    .build()
                
                // Execute a prefetch request to populate the cache
                imageLoader.enqueue(imageRequest)
            }
        } catch (e: Exception) {
            // This catch block isn't strictly necessary with the errorHandler
            // but adding it for extra safety
            Log.e(TAG, "Error during image prefetch: ${e.message}")
            FirestoreImageUtils.markUrlAsFailed(url)
        }
    }
    
    // Display the image with loading and error states
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        error = {
            Log.e(TAG, "Failed to load external image: $url")
            // Mark the URL as failed for future reference
            FirestoreImageUtils.markUrlAsFailed(url)
            
            PlaceholderImage(
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                isCategory = isCategory
            )
        }
    )
}

/**
 * A placeholder image to show when an image fails to load
 */
@Composable
fun PlaceholderImage(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    isCategory: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            painter = painterResource(id = FirestoreImageUtils.getPlaceholderResourceId(isCategory)),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Creates a Coil image loader configuration optimized for external URLs
 */
@Composable
fun createExternalUrlImageLoader() {
    val context = LocalContext.current
    
    // Configure Coil for caching external URLs
    coil.ImageLoader.Builder(context)
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("external_image_cache"))
                .maxSizeBytes(100 * 1024 * 1024) // 100MB
                .build()
        }
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .respectCacheHeaders(false) // Don't respect HTTP cache headers
        .crossfade(true)
        .build()
}

/**
 * A simpler version of ExternalUrlImage with minimal loading states
 */
@Composable
fun SimpleExternalUrlImage(
    url: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    isCategory: Boolean = false
) {
    val context = LocalContext.current
    
    // Check if URL is already known to fail
    if (FirestoreImageUtils.isFailedUrl(url)) {
        PlaceholderImage(
            contentDescription = contentDescription,
            modifier = modifier,
            isCategory = isCategory
        )
        return
    }
    
    val cleanUrl = FirestoreImageUtils.getCleanExternalUrl(url)
    
    // If URL couldn't be cleaned or is invalid, show placeholder
    if (cleanUrl == null) {
        PlaceholderImage(
            contentDescription = contentDescription,
            modifier = modifier,
            isCategory = isCategory
        )
        return
    }
    
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(cleanUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(FirestoreImageUtils.getPlaceholderResourceId(isCategory))
            .listener(
                onError = { _, result ->
                    Log.e(TAG, "Failed to load image: $cleanUrl Error: ${result.throwable}")
                    FirestoreImageUtils.markUrlAsFailed(url)
                }
            )
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        error = {
            PlaceholderImage(
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                isCategory = isCategory
            )
        }
    )
} 