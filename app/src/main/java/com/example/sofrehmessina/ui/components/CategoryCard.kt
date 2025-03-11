package com.example.sofrehmessina.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import com.example.sofrehmessina.util.FirestoreImageUtils
import com.example.sofrehmessina.util.ImageCacheManager
import com.example.sofrehmessina.util.LocaleHelper
import kotlinx.coroutines.launch

/**
 * A card component to display a category with its image and name
 * 
 * @param category The category to display
 * @param onClick Callback for when the card is clicked
 * @param modifier Optional modifier for the card
 * @param onFavoriteClick Optional callback for when the favorite button is clicked (if null, no favorite button is shown)
 * @param isFavorite Whether the category is marked as favorite
 * @param onError Optional callback for when an error occurs
 */
@Composable
fun CategoryCard(
    category: Category,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavoriteClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onError: ((Throwable) -> Unit)? = null
) {
    // Get current language for translation
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)
    
    // Use the translated name and description from the category based on current language
    val categoryName = category.getName(currentLang)
    val categoryDescription = category.getDescription(currentLang)
    
    // Get the ImageCacheManager via Hilt
    val sharedViewModel: SharedImageViewModel = hiltViewModel()
    val imageCacheManager = sharedViewModel.imageCacheManager
    
    // Prepare for image loading
    val coroutineScope = rememberCoroutineScope()
    var imageRequest by remember { mutableStateOf<coil.request.ImageRequest?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }
    
    // Generate a unique key that changes every time
    val uniqueKey = remember(category.id) { System.currentTimeMillis() }
    
    // Use multiple keys to ensure reloading when returning to the screen
    LaunchedEffect(uniqueKey, category.id) {
        Log.d("CategoryCard", "Loading image for category: ${category.id}, imageUrl: ${category.imageUrl}, key: $uniqueKey")
        
        // Always create a fresh image request when the effect runs
        coroutineScope.launch {
            try {
                imageRequest = null // Clear existing request
                imageLoadError = false // Reset error state
                
                // Force reload from network by setting forceReload to true
                imageRequest = imageCacheManager.createImageRequestFromPath(
                    path = category.imageUrl,
                    forceReload = true  // Force network reload
                )
            } catch (e: Exception) {
                Log.e("CategoryCard", "Error creating image request: ${e.message}", e)
                imageLoadError = true
                onError?.invoke(e)
            }
        }
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box {
                // Use our enhanced SubcomposeAsyncImage with fallbacks
                if (imageLoadError) {
                    // Show error placeholder immediately if we know the image failed to load
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // Use our enhanced SubcomposeAsyncImage with a key param to force reload
                    SubcomposeAsyncImage(
                        model = imageRequest ?: R.drawable.ic_image_placeholder,
                        contentDescription = categoryName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            Log.e("CategoryCard", "Failed to load image: ${category.imageUrl}")
                            imageLoadError = true
                            onError?.invoke(Exception("Failed to load image: ${category.imageUrl}"))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    )
                }
                
                // Add favorite button if callback is provided
                if (onFavoriteClick != null) {
                    FilledTonalIconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Category details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (categoryDescription.isNotEmpty()) {
                    Text(
                        text = categoryDescription,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} 