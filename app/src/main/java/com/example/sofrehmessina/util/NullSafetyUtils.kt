package com.example.sofrehmessina.util

import java.util.Date

/**
 * Utility functions for safely handling null values throughout the application
 */
object NullSafetyUtils {
    /**
     * Ensures a String is never null
     * @param value The potentially null string
     * @return A non-null string (empty string if the input was null)
     */
    fun safeString(value: String?): String {
        return value ?: ""
    }
    
    /**
     * Safely converts a nullable String to a Double
     * @param value The potentially null string
     * @param default The default value to use if the string is null or not a valid double
     * @return A double value
     */
    fun safeStringToDouble(value: String?, default: Double = 0.0): Double {
        return try {
            value?.toDoubleOrNull() ?: default
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * Safely converts a nullable String to an Int
     * @param value The potentially null string
     * @param default The default value to use if the string is null or not a valid integer
     * @return An integer value
     */
    fun safeStringToInt(value: String?, default: Int = 0): Int {
        return try {
            value?.toIntOrNull() ?: default
        } catch (e: Exception) {
            default
        }
    }
    
    /**
     * Safely handles date comparisons, handling null values
     * @param date1 First date, may be null
     * @param date2 Second date, may be null
     * @return true if date1 is before date2, or if appropriate handling for null values
     */
    fun isDateBefore(date1: Date?, date2: Date?): Boolean {
        return when {
            date1 == null || date2 == null -> true  // Consider null dates as valid case
            else -> date1.before(date2)
        }
    }
    
    /**
     * Creates a safe copy of a map with non-null string keys and values
     * @param map The original map that might contain null values
     * @return A new map with no null keys or values
     */
    fun safeStringMap(map: Map<String?, String?>?): Map<String, String> {
        if (map == null) return emptyMap()
        
        return map.entries
            .filter { it.key != null }
            .associate { (key, value) ->
                safeString(key) to safeString(value)
            }
    }
} 