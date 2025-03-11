package com.example.sofrehmessina.util

import android.content.Context
import android.widget.Toast
import java.util.Date

/**
 * Extension functions for common operations throughout the app
 */

/**
 * Ensures a String is never null
 * @return A non-null string (empty string if this was null)
 */
fun String?.orEmpty(): String = this ?: ""

/**
 * Safely converts a nullable String to an Int
 * @param default The default value to use if the string is null or not a valid integer
 * @return An integer value
 */
fun String?.toIntSafely(default: Int = 0): Int {
    return try {
        this?.toIntOrNull() ?: default
    } catch (e: Exception) {
        default
    }
}

/**
 * Safely converts a nullable String to a Double
 * @param default The default value to use if the string is null or not a valid double
 * @return A double value
 */
fun String?.toDoubleSafely(default: Double = 0.0): Double {
    return try {
        this?.toDoubleOrNull() ?: default
    } catch (e: Exception) {
        default
    }
}

/**
 * Safely checks if this date is before another date, handling null values
 * @param other The other date to compare with
 * @return true if this date is before the other date, or appropriate handling for null values
 */
fun Date?.isSafelyBefore(other: Date?): Boolean {
    return when {
        this == null || other == null -> true  // Consider null dates as valid case
        else -> this.before(other)
    }
}

/**
 * Show a short toast message
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Shows a long toast message
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Creates a safe copy of a map with non-null string keys and values
 * @return A new map with no null keys or values
 */
fun Map<String?, String?>?.toSafeStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    
    return entries
        .filter { it.key != null }
        .associate { (key, value) ->
            key.orEmpty() to value.orEmpty()
        }
} 