package com.example.sofrehmessina.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A design system for discount-related UI elements
 */
object DiscountDesignSystem {
    
    // Discount color palette
    object Colors {
        // Primary discount color - used for most discount indicators
        val discountPrimary = Color(0xFFE91E63) // Pink
        
        // Secondary discount color - used for special or limited-time offers
        val discountSecondary = Color(0xFFFF9800) // Orange
        
        // Tertiary discount color - used for bundle deals or tiered discounts
        val discountTertiary = Color(0xFF4CAF50) // Green
        
        // Background colors for discount badges
        val discountBadgeBackground = Color(0xFFFFEBEE) // Light pink
        val specialOfferBackground = Color(0xFFFFF3E0) // Light orange
        val bundleBackground = Color(0xFFE8F5E9) // Light green
        
        // Text colors for discount elements
        val discountText = Color(0xFFFFFFFF) // White text on colored backgrounds
        val discountTextDark = Color(0xFF212121) // Dark text for light backgrounds
        
        // Gradient for special discount highlights
        val specialDiscountGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF9800),
                Color(0xFFE91E63)
            )
        )
    }
    
    // Discount badge styles
    enum class BadgeStyle {
        CIRCLE,      // Circular badge, good for percentage numbers
        RIBBON,      // Ribbon style, good for "SALE" or short text
        TAG,         // Tag style with pointed edge, good for prices
        PILL         // Pill shape, good for longer text
    }
    
    // Discount animation types
    enum class AnimationType {
        PULSE,       // Pulsing animation that draws attention
        BOUNCE,      // Bouncing animation for playful emphasis
        ROTATE,      // Rotating animation for special offers
        SHIMMER      // Shimmer effect for premium discounts
    }
    
    /**
     * Creates a discount badge with the specified style and optional animation
     */
    @Composable
    fun DiscountBadge(
        text: String,
        style: BadgeStyle = BadgeStyle.CIRCLE,
        color: Color = Colors.discountPrimary,
        animationType: AnimationType? = null,
        modifier: Modifier = Modifier
    ) {
        val animatedModifier = when (animationType) {
            AnimationType.PULSE -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                modifier.scale(scale)
            }
            AnimationType.BOUNCE -> {
                val infiniteTransition = rememberInfiniteTransition(label = "bounce")
                val offset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offset"
                )
                modifier.offset(y = offset.dp)
            }
            AnimationType.ROTATE -> {
                val infiniteTransition = rememberInfiniteTransition(label = "rotate")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -5f,
                    targetValue = 5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rotation"
                )
                modifier.rotate(rotation)
            }
            else -> modifier
        }
        
        when (style) {
            BadgeStyle.CIRCLE -> CircleBadge(text, color, animatedModifier)
            BadgeStyle.RIBBON -> RibbonBadge(text, color, animatedModifier)
            BadgeStyle.TAG -> TagBadge(text, color, animatedModifier)
            BadgeStyle.PILL -> PillBadge(text, color, animatedModifier)
        }
    }
    
    @Composable
    private fun CircleBadge(text: String, color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Colors.discountText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
    
    @Composable
    private fun RibbonBadge(text: String, color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .height(24.dp)
                .clip(RoundedCornerShape(0.dp, 12.dp, 12.dp, 0.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Colors.discountText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
    
    @Composable
    private fun TagBadge(text: String, color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Colors.discountText,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = text,
                    color = Colors.discountText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
    
    @Composable
    private fun PillBadge(text: String, color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Colors.discountText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
    
    /**
     * Creates a percentage discount badge
     */
    @Composable
    fun PercentageDiscountBadge(
        percentage: Int,
        style: BadgeStyle = BadgeStyle.CIRCLE,
        animationType: AnimationType? = null,
        modifier: Modifier = Modifier
    ) {
        val text = "$percentage%"
        DiscountBadge(
            text = text,
            style = style,
            color = when {
                percentage >= 50 -> Colors.discountTertiary
                percentage >= 25 -> Colors.discountSecondary
                else -> Colors.discountPrimary
            },
            animationType = animationType,
            modifier = modifier
        )
    }
    
    /**
     * Creates a discount message with optional animation
     */
    @Composable
    fun DiscountMessage(
        message: String,
        isImportant: Boolean = false,
        showIcon: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        var visible by remember { mutableStateOf(false) }
        
        LaunchedEffect(message) {
            visible = true
        }
        
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            Surface(
                modifier = modifier,
                color = if (isImportant) Colors.specialOfferBackground else Colors.discountBadgeBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showIcon) {
                        Icon(
                            imageVector = Icons.Default.Percent,
                            contentDescription = null,
                            tint = if (isImportant) Colors.discountSecondary else Colors.discountPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = message,
                        color = if (isImportant) Colors.discountSecondary else Colors.discountPrimary,
                        fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    
    /**
     * Creates a shimmer effect for discount highlights
     */
    @Composable
    fun ShimmerDiscount(
        content: @Composable () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val shimmerColors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0.3f)
        )
        
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "translate"
        )
        
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset(
                x = translateAnimation.value - 1000f,
                y = 0f
            ),
            end = androidx.compose.ui.geometry.Offset(
                x = translateAnimation.value,
                y = 1000f
            )
        )
        
        Surface(
            modifier = modifier,
            color = Colors.discountPrimary
        ) {
            Box(
                modifier = Modifier
                    .background(brush)
                    .fillMaxSize()
            ) {
                content()
            }
        }
    }
} 