package com.example.sofrehmessina.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.sofrehmessina.util.NumberFormatter

/**
 * A custom Text component that ensures numeric values are always displayed in English format
 * regardless of the app's selected language.
 */
@Composable
fun EnglishNumericText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    // Process the text to ensure all numeric values are in English format
    val processedText = processNumericText(text)
    
    Text(
        text = processedText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Process text to ensure all numeric values are in English format.
 * This function preserves non-numeric text and only converts digits.
 */
private fun processNumericText(text: String): String {
    // Use a regex to find all numeric parts in the text
    val numericPattern = Regex("\\d+(\\.\\d+)?")
    
    return numericPattern.replace(text) { matchResult ->
        val numericPart = matchResult.value
        
        // Check if it's a decimal number
        if (numericPart.contains(".")) {
            try {
                val number = numericPart.toDouble()
                NumberFormatter.formatDouble(number)
            } catch (e: NumberFormatException) {
                numericPart // Return original if conversion fails
            }
        } else {
            try {
                val number = numericPart.toLong()
                NumberFormatter.formatLong(number)
            } catch (e: NumberFormatException) {
                numericPart // Return original if conversion fails
            }
        }
    }
}

/**
 * A convenience function for displaying price values in English format with currency symbol.
 */
@Composable
fun PriceText(
    price: Double,
    currencySymbol: String = "€",
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val formattedPrice = NumberFormatter.formatPrice(price, currencySymbol)
    
    Text(
        text = formattedPrice,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = style
    )
} 