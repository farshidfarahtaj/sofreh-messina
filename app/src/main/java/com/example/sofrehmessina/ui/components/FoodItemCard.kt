package com.example.sofrehmessina.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import com.example.sofrehmessina.util.LocaleHelper
import com.example.sofrehmessina.util.FirestoreImageUtils
import coil.compose.SubcomposeAsyncImage
import com.example.sofrehmessina.ui.components.ExternalUrlImage

@Composable
fun FoodItemCard(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Get the current language
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)
    
    // Get translations
    val foodName = food.getName(currentLang)
    val foodDescription = food.getDescription(currentLang)
    
    // For debugging
    LaunchedEffect(Unit) {
        Log.d("FoodItemCard", "Rendering food with ID: ${food.id}, imageUrl: ${food.imageUrl}")
        Log.d("FoodItemCard", "Food availability status: ${food.foodAvailable}")
        Log.d("FoodItemCard", "Discount message: ${food.discountMessage}")
        Log.d("FoodItemCard", "Discount percentage: ${food.discountPercentage}")
        Log.d("FoodItemCard", "Discounted price: ${food.discountedPrice}")
    }
    
    // Check for actual or potential discounts
    // A discount is present if there's either a discounted price or a discount percentage
    val hasDiscount = (food.discountedPrice != null && food.discountedPrice < food.price) || 
                     (food.discountPercentage != null && food.discountPercentage > 0)
                     
    // Calculate potential savings for informational discounts
    val savingsAmount = if (food.discountedPrice != null) {
        food.price - food.discountedPrice
    } else if (food.discountPercentage != null) {
        food.price * (food.discountPercentage / 100.0)
    } else {
        0.0
    }
    
    // For informational discounts, calculate what the price would be
    val potentialDiscountedPrice = if (food.discountedPrice == null && food.discountPercentage != null) {
        food.price * (1 - food.discountPercentage / 100.0)
    } else {
        food.discountedPrice
    }
    
    // Animation for discount badge
    val infiniteTransition = rememberInfiniteTransition(label = "discountAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )
    
    // First appearance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = food.id) {
        isVisible = true
    }
    
    Column {
        // Remove the discount banner above the card
        // if (hasDiscount) {
        //     FoodItemOfferDisplay(
        //         food = food,
        //         modifier = Modifier.padding(bottom = 8.dp),
        //         currencyManager = currencyManager
        //     )
        // }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            Card(
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.height(120.dp)
                ) {
                    Box {
                        // Use the simple and effective approach from CategoryFoodItemCard
                        if (!food.imageUrl.isNullOrEmpty()) {
                            androidx.compose.foundation.Image(
                                painter = coil.compose.rememberAsyncImagePainter(
                                    model = food.imageUrl,
                                    error = coil.compose.rememberAsyncImagePainter(
                                        model = android.R.drawable.ic_menu_gallery
                                    )
                                ),
                                contentDescription = foodName,
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Show placeholder if no image URL
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
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
                        
                        // Enhanced discount badge with more visibility
                        if (hasDiscount) {
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopStart)
                                    .scale(scale)
                            ) {
                                Text(
                                    text = "-${food.discountPercentage?.toInt() ?: 0}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // Unavailable overlay with animation
                        if (!food.foodAvailable) {
                            // Background overlay with semi-transparency
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x80B71C1C)) // More muted dark red color with semi-transparency
                            )
                            
                            // Pulsing animation for the "Unavailable" badge
                            val pulseAnimation = rememberInfiniteTransition(label = "pulseAnimation")
                            val pulseScale by pulseAnimation.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.05f, // Make the animation even more subtle
                                animationSpec = infiniteRepeatable(
                                    animation = tween(700, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Surface(
                                    color = Color(0xFFB71C1C), // More muted dark red color
                                    shape = RoundedCornerShape(6.dp), // Slightly smaller corner radius
                                    modifier = Modifier
                                        .scale(pulseScale)
                                        .padding(4.dp) // Reduced padding even more
                                ) {
                                    Text(
                                        text = stringResource(R.string.out_of_stock),
                                        style = MaterialTheme.typography.labelSmall, // Reduced text size even more
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp) // Reduced padding even more
                                    )
                                }
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = foodName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Availability indicator
                            if (!food.foodAvailable) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = stringResource(R.string.out_of_stock),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        
                        if (foodDescription.isNotEmpty()) {
                            Text(
                                text = foodDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        
                        // Enhanced discount message with more visibility - MOVED TO THE TOP OUTSIDE THE CARD
                        /*
                        if (hasDiscount && !food.discountMessage.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = food.discountMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        */
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Price display with discount
                        if (hasDiscount) {
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                // Savings amount label
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "Save ${currencyManager.formatPrice(savingsAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Discounted price or potential discounted price
                                    potentialDiscountedPrice?.let { discPrice ->
                                        Text(
                                            text = currencyManager.formatPrice(discPrice),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Original price with strikethrough
                                    Text(
                                        text = currencyManager.formatPrice(food.price),
                                        style = MaterialTheme.typography.bodySmall,
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = currencyManager.formatPrice(food.price),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
} 