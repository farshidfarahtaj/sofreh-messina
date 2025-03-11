package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sofrehmessina.utils.CurrencyManager

/**
 * A reusable composable for displaying prices with Euro currency.
 */
@Composable
fun PriceDisplay(
    price: Double,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold,
    showSymbol: Boolean = true
) {
    Text(
        text = currencyManager.formatPriceCustom(price, showSymbol),
        style = textStyle,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

/**
 * A price display with both original and discounted prices
 */
@Composable
fun DiscountPriceDisplay(
    originalPrice: Double,
    discountedPrice: Double,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Discounted price (primary)
        PriceDisplay(
            price = discountedPrice,
            currencyManager = currencyManager,
            textStyle = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        // Original price (strikethrough)
        Text(
            text = currencyManager.formatPrice(originalPrice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 14.sp,
            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
        )
    }
}

/**
 * A price display for total amounts (like in cart or checkout)
 */
@Composable
fun TotalPriceDisplay(
    total: Double,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    textSize: Int = 18
) {
    Text(
        text = currencyManager.formatPrice(total),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = textSize.sp,
        modifier = modifier
    )
} 