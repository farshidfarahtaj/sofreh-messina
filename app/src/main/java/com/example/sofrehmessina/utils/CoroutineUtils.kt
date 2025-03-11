package com.example.sofrehmessina.utils

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Extension function for CoroutineScope that properly handles cancellation exceptions.
 * 
 * This function will:
 * 1. Log regular exceptions as errors
 * 2. Properly propagate CancellationExceptions without logging them as errors
 * 3. Provide a clean way to handle coroutine cancellation due to navigation
 * 
 * Usage:
 * ```
 * viewModelScope.safeLaunch("MyTag") {
 *     // Your coroutine code here
 * }
 * ```
 */
fun CoroutineScope.safeLaunch(
    tag: String,
    block: suspend CoroutineScope.() -> Unit
) = launch {
    try {
        block()
    } catch (e: Exception) {
        when (e) {
            is CancellationException -> {
                // Just log at debug level and let it propagate
                Log.d(tag, "Operation was cancelled normally: ${e.message}")
                throw e
            }
            else -> {
                // Log other exceptions
                Log.e(tag, "Error in coroutine: ${e.message}", e)
            }
        }
    }
}

/**
 * Safely executes the given block, catching and handling any exceptions except CancellationException.
 * This is useful for flow collection blocks where we want to handle errors gracefully but still
 * allow cancellation to propagate.
 * 
 * Usage:
 * ```
 * safeExecute("MyTag") {
 *     // Your code that might throw exceptions
 * }
 * ```
 */
suspend fun <T> safeExecute(tag: String, block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        when (e) {
            is CancellationException -> {
                Log.d(tag, "Operation was cancelled normally: ${e.message}")
                throw e
            }
            else -> {
                Log.e(tag, "Error executing block: ${e.message}", e)
                null
            }
        }
    }
} 