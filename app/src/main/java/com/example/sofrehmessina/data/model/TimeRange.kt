package com.example.sofrehmessina.data.model

import java.util.Calendar

/**
 * Represents different time ranges for analytics and reporting
 */
enum class TimeRange {
    TODAY,
    WEEK,
    MONTH,
    LAST_7_DAYS,
    LAST_30_DAYS, 
    LAST_90_DAYS,
    THIS_YEAR,
    ALL_TIME;
    
    fun toDisplayName(): String {
        return when (this) {
            TODAY -> "Today"
            WEEK -> "This Week"
            MONTH -> "This Month"
            LAST_7_DAYS -> "Last 7 Days"
            LAST_30_DAYS -> "Last 30 Days"
            LAST_90_DAYS -> "Last 90 Days"
            THIS_YEAR -> "This Year"
            ALL_TIME -> "All Time"
        }
    }

    fun toStartDate(): Long {
        val calendar = Calendar.getInstance()
        return when (this) {
            TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            LAST_90_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            THIS_YEAR -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            ALL_TIME -> 0L
        }
    }
} 