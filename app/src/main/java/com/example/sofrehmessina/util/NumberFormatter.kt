package com.example.sofrehmessina.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility class for formatting numbers consistently in English format
 * regardless of the app's selected language
 */
class NumberFormatter {
    companion object {
        // Always use English locale for number formatting
        private val ENGLISH_LOCALE = Locale.ENGLISH
        
        /**
         * Format an integer to a string using English digits
         */
        fun formatInteger(number: Int): String {
            val formatter = NumberFormat.getInstance(ENGLISH_LOCALE)
            return formatter.format(number)
        }
        
        /**
         * Format a long to a string using English digits
         */
        fun formatLong(number: Long): String {
            val formatter = NumberFormat.getInstance(ENGLISH_LOCALE)
            return formatter.format(number)
        }
        
        /**
         * Format a double to a string using English digits with specified decimal places
         */
        fun formatDouble(number: Double, decimalPlaces: Int = 2): String {
            val pattern = buildString {
                append("#,##0")
                if (decimalPlaces > 0) {
                    append(".")
                    repeat(decimalPlaces) { append("0") }
                }
            }
            
            val formatter = DecimalFormat(pattern)
            formatter.isDecimalSeparatorAlwaysShown = decimalPlaces > 0
            return formatter.format(number)
        }
        
        /**
         * Format a price with currency symbol
         */
        fun formatPrice(price: Double, currencySymbol: String = "€"): String {
            return "$currencySymbol ${formatDouble(price, 2)}"
        }
        
        /**
         * Format a percentage
         */
        fun formatPercentage(value: Double): String {
            return "${formatDouble(value * 100, 1)}%"
        }
    }
} 