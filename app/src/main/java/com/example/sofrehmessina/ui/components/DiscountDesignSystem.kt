package com.example.sofrehmessina.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager

/**
 * A design system for displaying discount information consistently throughout the app.
 * These components should only be shown when an actual discount is applied to an item.
 */
object DiscountDesignSystem {
    
    /**
     * Color palette for discount components
     */
    object Colors {
        val discountPrimary = Color(0xFFD32F2F)
        val discountSecondary = Color(0xFFE57373)
        val discountBackground = Color(0xFFFFF8E1)
        val timedDiscountBackground = Color(0xFFFFEBEE)
        val informationalBackground = Color(0xFFE3F2FD)
        val tagBackground = Color(0xFFFF5722)
        val tagText = Color.White
    }
    
    /**
     * Animation types for discount badges
     */
    enum class AnimationType {
        NONE,
        PULSE,
        ROTATE
    }
    
    /**
     * Badge styles for displaying discount percentages
     */
    enum class BadgeStyle {
        CIRCLE,
        TAG,
        RIBBON
    }
    
    /**
     * Displays a discount badge with percentage in the top-right corner of an item.
     * This should only be shown when a discount is actually applied.
     */
    @Composable
    fun DiscountBadge(discountPercentage: Double, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(bottomStart = 8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Colors.tagBackground,
                            Colors.discountSecondary
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-${discountPercentage.toInt()}%",
                color = Colors.tagText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
    
    /**
     * Enhanced version of the percentage discount badge with animation options
     */
    @Composable
    fun PercentageDiscountBadge(
        percentage: Int,
        style: BadgeStyle = BadgeStyle.TAG,
        animationType: AnimationType = AnimationType.NONE,
        modifier: Modifier = Modifier
    ) {
        // Animation properties
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (animationType == AnimationType.PULSE) 1.1f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = if (animationType == AnimationType.ROTATE) 3f else -3f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        val animModifier = when (animationType) {
            AnimationType.PULSE -> modifier.scale(scale)
            AnimationType.ROTATE -> modifier.rotate(rotation)
            else -> modifier
        }
        
        when (style) {
            BadgeStyle.CIRCLE -> {
                Box(
                    modifier = animModifier
                        .clip(CircleShape)
                        .background(Colors.tagBackground)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-$percentage%",
                        color = Colors.tagText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            BadgeStyle.TAG -> {
                Box(
                    modifier = animModifier
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Colors.tagBackground,
                                    Colors.discountSecondary
                                )
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-$percentage%",
                        color = Colors.tagText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            BadgeStyle.RIBBON -> {
                Box(
                    modifier = animModifier
                        .clip(RoundedCornerShape(bottomEnd = 8.dp))
                        .background(Colors.tagBackground)
                        .border(
                            width = 1.dp,
                            color = Colors.tagText.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-$percentage% OFF",
                        color = Colors.tagText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
    
    /**
     * Displays the original price with a strikethrough and the discounted price.
     * This component should only be shown when a discount is applied.
     */
    @Composable
    fun DiscountedPriceDisplay(
        originalPrice: Double,
        discountedPrice: Double,
        modifier: Modifier = Modifier,
        currencyManager: CurrencyManager = rememberCurrencyManager()
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencyManager.formatPrice(originalPrice),
                color = Color.Gray,
                fontSize = 14.sp,
                textDecoration = TextDecoration.LineThrough
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyManager.formatPrice(discountedPrice),
                color = Colors.discountPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
    
    /**
     * Displays the discount message with an appropriate icon.
     * This component should only be shown when a discount is applied.
     */
    @Composable
    fun DiscountMessageBanner(discountMessage: String, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Colors.discountBackground
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display appropriate icon based on the prefix in the message
                when {
                    discountMessage.startsWith("🔥") -> {
                        Text(
                            text = "🔥",
                            fontSize = 16.sp
                        )
                    }
                    discountMessage.startsWith("✨") -> {
                        Text(
                            text = "✨",
                            fontSize = 16.sp
                        )
                    }
                    discountMessage.startsWith("💡") -> {
                        Text(
                            text = "💡",
                            fontSize = 16.sp
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = "Discount",
                            tint = Colors.discountPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = discountMessage.replaceFirst(Regex("^[🔥✨💡] "), ""),
                    color = Color(0xFF5D4037),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    
    /**
     * Enhanced version of the discount message with customizable options
     */
    @Composable
    fun DiscountMessage(
        message: String,
        isImportant: Boolean = false,
        showIcon: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isImportant) Colors.timedDiscountBackground else Colors.discountBackground
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcon) {
                    when {
                        message.startsWith("🔥") -> {
                            Text(text = "🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        message.startsWith("✨") -> {
                            Text(text = "✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        message.startsWith("💡") -> {
                            Text(text = "💡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        isImportant -> {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Limited Time",
                                tint = Colors.discountPrimary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Discount",
                                tint = Colors.discountPrimary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = message.replaceFirst(Regex("^[🔥✨💡] "), ""),
                    color = if (isImportant) Colors.discountPrimary else Color(0xFF5D4037),
                    fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    
    /**
     * A comprehensive discount display that combines all discount information.
     * This component is only shown when a discount is applied to the food item.
     */
    @Composable
    fun FoodItemDiscountDisplay(food: Food, modifier: Modifier = Modifier) {
        val hasDiscount = food.discountedPrice != null && food.discountPercentage != null
        
        AnimatedVisibility(
            visible = hasDiscount,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = modifier) {
                if (hasDiscount && food.discountMessage != null) {
                    DiscountMessageBanner(food.discountMessage)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (hasDiscount && food.discountedPrice != null) {
                    DiscountedPriceDisplay(
                        originalPrice = food.price,
                        discountedPrice = food.discountedPrice
                    )
                }
            }
        }
    }
    
    /**
     * A display to show potential discounts that could apply if certain conditions are met.
     * This is used for informational purposes only when the discount is not yet applied.
     */
    @Composable
    fun PotentialDiscountInfo(food: Food, modifier: Modifier = Modifier) {
        // Only show informational discount messages (those starting with 💡)
        val hasInfoMessage = food.discountMessage?.startsWith("💡") == true
        
        AnimatedVisibility(
            visible = hasInfoMessage,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (hasInfoMessage && food.discountMessage != null) {
                Card(
                    modifier = modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Colors.informationalBackground
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 16.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = food.discountMessage.replaceFirst("💡 ", ""),
                            color = Color(0xFF0D47A1),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
} 