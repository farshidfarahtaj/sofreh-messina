package com.example.sofrehmessina.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.data.model.BannerItem
import kotlinx.coroutines.delay
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.ImageLoader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage

/**
 * A slideshow component that automatically cycles through banner items
 * with a fade animation between slides
 */
@Composable
fun BannerSlideshow(
    banners: List<BannerItem>,
    onBannerClick: (BannerItem) -> Unit,
    modifier: Modifier = Modifier,
    autoSlideDuration: Long = 7000, // Changed from 3000ms to 7000ms (7 seconds)
) {
    if (banners.isEmpty()) return
    
    var currentPage by remember { mutableIntStateOf(0) }
    val bannerCount = banners.size
    
    // Auto-sliding effect
    LaunchedEffect(key1 = banners, key2 = currentPage) {
        if (bannerCount > 1) {
            delay(autoSlideDuration)
            currentPage = (currentPage + 1) % bannerCount
        }
    }
    
    // Preload all banner images when component is first created
    val context = LocalContext.current
    val imageLoader = remember { ImageLoader(context) }
    
    LaunchedEffect(banners) {
        // Preload all banner images
        banners.forEach { banner ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(banner.imageUrl)
                    .build()
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Main banner content
        banners.forEachIndexed { index, banner ->
            val visible = index == currentPage
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit = fadeOut(animationSpec = tween(durationMillis = 500))
            ) {
                Card(
                    onClick = { onBannerClick(banner) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Banner image - use SubcomposeAsyncImage for better state handling
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(banner.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            error = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        )
                        
                        // Gradient overlay for text visibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.7f)
                                        ),
                                        startY = 0f,
                                        endY = 400f
                                    )
                                )
                        )
                        
                        // Banner text
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (banner.subtitle.isNotEmpty()) {
                                Text(
                                    text = banner.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Only show indicator dots if there is more than one banner
        if (bannerCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(bannerCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
} 