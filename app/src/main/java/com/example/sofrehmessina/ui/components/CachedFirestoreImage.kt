package com.example.sofrehmessina.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.sofrehmessina.R
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import com.example.sofrehmessina.util.ImageCacheManager
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.job

private const val TAG = "CachedFirestoreImage"

/**
 * A composable that displays an image from Firebase Storage with caching
 * It handles loading states and errors
 *
 * @param storageRef The Firebase storage reference for the image
 * @param contentDescription Content description for accessibility
 * @param modifier Modifier for the image
 * @param contentScale How the image should scale inside the bounds
 * @param showLoading Whether to show a loading indicator while the image loads
 * @param bypassCache Whether to bypass the cache for this specific image
 * @param sharedViewModel The ViewModel that manages image caching
 * @param imageCacheManager The ImageCacheManager for handling image caching
 */
@Composable
fun CachedFirestoreImage(
    storageRef: StorageReference,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showLoading: Boolean = true,
    bypassCache: Boolean = ImageCacheManager.shouldBypassCache(storageRef.path),
    sharedViewModel: SharedImageViewModel = hiltViewModel(),
    imageCacheManager: ImageCacheManager? = null
) {
    var loadAttempted by remember { mutableStateOf(false) }
    var isCached by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var imageRequest by remember { mutableStateOf<ImageRequest?>(null) }
    
    // Error handler for image loading operations
    val errorHandler = remember {
        CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error in image loading: ${exception.message}")
            isLoading = false
        }
    }
    
    // Use a LaunchedEffect to ensure the image is cached and create the image request
    LaunchedEffect(storageRef.path) {
        // Check if we still need to load the image
        if (!loadAttempted) {
            loadAttempted = true
            isLoading = true
            
            try {
                // Try to load from cache first
                if (!bypassCache) {
                    // If we have direct access to the cache manager, check it first
                    if (imageCacheManager != null && !imageCacheManager.isImageCached(storageRef.path)) {
                        withContext(Dispatchers.IO + errorHandler) {
                            sharedViewModel.ensureImageCached(storageRef)
                        }
                        isCached = true
                    } else {
                        // Otherwise, let the ViewModel handle the caching logic
                        withContext(Dispatchers.IO + errorHandler) {
                            sharedViewModel.ensureImageCached(storageRef)
                        }
                        isCached = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring image cached: ${e.message}")
            }
            
            try {
                // Create the image request with proper error handling
                imageRequest = withContext(Dispatchers.IO + errorHandler) {
                    sharedViewModel.getImageRequest(storageRef, bypassCache)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating image request: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Display the image with SubcomposeAsyncImage for handling loading states
    if (imageRequest != null) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                if (showLoading && isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            error = {
                PlaceholderImage(contentDescription)
            }
        )
    } else {
        // Show a loading indicator while waiting for the image request
        if (showLoading) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            // Show placeholder if not showing loading
            PlaceholderImage(contentDescription)
        }
    }
}

/**
 * A composable for banner images that should always load from network
 * These are images that change frequently and should always use the latest version
 */
@Composable
fun BannerImage(
    storageRef: StorageReference,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    
    // Add a state to track whether loading failed
    var loadingFailed by remember { mutableStateOf(false) }
    
    // Unique key for each image with a timestamp to avoid stale cache issues
    val uniqueKey = remember(storageRef.path) { "${storageRef.path}?t=${System.currentTimeMillis()}" }
    
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(storageRef)
            .crossfade(true)
            .crossfade(500) // Longer crossfade for smoother transitions
            .memoryCachePolicy(CachePolicy.ENABLED) // Enable memory cache
            .diskCachePolicy(CachePolicy.ENABLED) // Enable disk cache for better performance
            .setParameter("key", uniqueKey) // Ensure unique loading for each render
            .listener(
                onStart = {
                    // Reset failure state on each load attempt
                    loadingFailed = false
                },
                onError = { _, _ ->
                    loadingFailed = true
                },
                onSuccess = { _, _ ->
                    loadingFailed = false
                }
            )
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp) // Larger for better visibility
                )
            }
        },
        error = {
            loadingFailed = true
            PlaceholderImage(contentDescription)
        }
    )
    
    // If loading failed, attempt immediate retry with LaunchedEffect
    if (loadingFailed) {
        LaunchedEffect(storageRef.path, loadingFailed) {
            // Add a small delay before retry
            kotlinx.coroutines.delay(500)
            // Force a new unique key to trigger reloading
            loadingFailed = false
        }
    }
}

/**
 * A simpler version of CachedFirestoreImage that uses AsyncImage
 * This is useful for images that don't need the additional loading states
 */
@Composable
fun SimpleCachedFirestoreImage(
    storageRef: StorageReference,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    bypassCache: Boolean = ImageCacheManager.shouldBypassCache(storageRef.path),
    sharedViewModel: SharedImageViewModel = hiltViewModel()
) {
    var imageRequest by remember { mutableStateOf<ImageRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Error handler for image loading operations
    val errorHandler = remember {
        CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error in image loading: ${exception.message}")
            isLoading = false
        }
    }
    
    // Create the image request
    LaunchedEffect(storageRef.path, bypassCache) {
        isLoading = true
        try {
            // Ensure the image is cached first if needed
            if (!bypassCache) {
                withContext(Dispatchers.IO + errorHandler) {
                    sharedViewModel.ensureImageCached(storageRef)
                }
            }
            
            // Then create the request
            imageRequest = withContext(Dispatchers.IO + errorHandler) {
                sharedViewModel.getImageRequest(storageRef, bypassCache)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing image: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    if (imageRequest != null) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
            error = painterResource(id = R.drawable.ic_image_placeholder),
            placeholder = painterResource(id = R.drawable.ic_image_placeholder)
        )
    } else {
        // Show a placeholder while waiting for the image request
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_image_placeholder),
                    contentDescription = contentDescription,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

/**
 * Placeholder for when an image fails to load
 */
@Composable
private fun PlaceholderImage(contentDescription: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_image_placeholder),
            contentDescription = contentDescription,
            tint = Color.Gray,
            modifier = Modifier.size(40.dp)
        )
    }
} 