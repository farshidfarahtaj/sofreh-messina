package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sofrehmessina.ui.theme.DiscountDesignSystem
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager

/**
 * A component to display special offers in an attractive way
 */
@Composable
fun SpecialOfferDisplay(
    title: String,
    percentOff: Int,
    modifier: Modifier = Modifier,
    isItemSpecific: Boolean = true,
    quantity: Int = 0
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiscountDesignSystem.Colors.specialDiscountGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Percentage Badge at the top
                DiscountDesignSystem.PercentageDiscountBadge(
                    percentage = percentOff,
                    style = DiscountDesignSystem.BadgeStyle.CIRCLE,
                    animationType = DiscountDesignSystem.AnimationType.PULSE,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Title with special styling
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = DiscountDesignSystem.Colors.bundleBackground.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = title,
                        color = DiscountDesignSystem.Colors.discountTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                    )
                }
                
                // Discount message
                val discountMessage = if (quantity > 0) {
                    "Buy $quantity or more items and get $percentOff% off!"
                } else if (isItemSpecific) {
                    "Special discount: $percentOff% off this item only!"
                } else {
                    "$percentOff% off!"
                }
                
                // Using the theme system's method to show a discount message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DiscountDesignSystem.Colors.specialOfferBackground
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = discountMessage,
                            color = DiscountDesignSystem.Colors.discountPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Shimmer effect at the bottom
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = DiscountDesignSystem.Colors.discountPrimary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Limited Time Offer!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * A simplified version of SpecialOfferDisplay that shows directly on food items
 */
@Composable
fun FoodItemOfferDisplay(
    food: Food,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Check if there's an actual discount to display
    val hasDiscount = food.hasDiscount() || 
        (food.discountPercentage != null && food.discountPercentage > 0)
    
    if (!hasDiscount) return
    
    val discountPercent = food.discountPercentage?.toInt() ?: 0
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = DiscountDesignSystem.Colors.discountBadgeBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show discount badge
            DiscountDesignSystem.DiscountBadge(
                text = "-$discountPercent%",
                style = DiscountDesignSystem.BadgeStyle.TAG,
                color = DiscountDesignSystem.Colors.discountPrimary,
                animationType = DiscountDesignSystem.AnimationType.PULSE,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Show discount message if available
                if (!food.discountMessage.isNullOrBlank()) {
                    Text(
                        text = food.discountMessage,
                        color = DiscountDesignSystem.Colors.discountPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                // Show savings
                val originalPrice = food.price
                val discountedPrice = food.discountedPrice ?: (food.price * (1 - (food.discountPercentage ?: 0.0) / 100.0))
                val savings = originalPrice - discountedPrice
                
                if (savings > 0) {
                    Text(
                        text = "Save ${currencyManager.formatPrice(savings)}",
                        color = DiscountDesignSystem.Colors.discountSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
} 