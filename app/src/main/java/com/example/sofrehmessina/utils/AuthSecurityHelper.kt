package com.example.sofrehmessina.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Helper class to enhance authentication security by detecting and preventing brute force attacks 
 * and implementing rate limiting for auth operations.
 */
object AuthSecurityHelper {
    
    private const val PREF_NAME = "auth_security_prefs"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_FIRST_FAILED_TIMESTAMP = "first_failed_timestamp"
    private const val KEY_LAST_AUTH_ATTEMPT = "last_auth_attempt"
    private const val KEY_LOCKED_UNTIL = "locked_until"
    private const val KEY_SUSPICIOUS_IPS = "suspicious_ips"
    
    // Thresholds for security measures
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCK_DURATION_MINUTES = 15L
    private const val RATE_LIMIT_DELAY_MS = 1000L // 1 second between auth attempts
    
    // Get shared preferences for storing security data
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if the current authentication attempt should be allowed or blocked
     * @return true if the attempt is allowed, false if it should be blocked
     */
    fun canAttemptAuth(context: Context): Boolean {
        val prefs = getPrefs(context)
        val currentTime = System.currentTimeMillis()
        
        // Check if account is locked
        val lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0)
        if (lockedUntil > currentTime) {
            val remainingTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(lockedUntil - currentTime) + 1
            Log.w("AuthSecurity", "Account is locked. Try again in $remainingTimeMinutes minutes")
            return false
        }
        
        // Apply rate limiting - check time since last attempt
        val lastAttempt = prefs.getLong(KEY_LAST_AUTH_ATTEMPT, 0)
        val timeSinceLastAttempt = currentTime - lastAttempt
        
        // Update last attempt timestamp
        prefs.edit {
            putLong(KEY_LAST_AUTH_ATTEMPT, currentTime)
        }
        
        return timeSinceLastAttempt >= RATE_LIMIT_DELAY_MS
    }
    
    /**
     * Record a failed authentication attempt and check if account should be locked
     * @return true if account is now locked, false otherwise
     */
    fun recordFailedAttempt(context: Context): Boolean {
        val prefs = getPrefs(context)
        val currentTime = System.currentTimeMillis()
        
        // Get current values
        val failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val firstFailedTime = if (failedAttempts == 1) currentTime 
                             else prefs.getLong(KEY_FIRST_FAILED_TIMESTAMP, currentTime)
        
        // Update stored values
        prefs.edit {
            putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
            putLong(KEY_FIRST_FAILED_TIMESTAMP, firstFailedTime)
        }
        
        // Check if we need to lock the account
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            val lockUntil = currentTime + TimeUnit.MINUTES.toMillis(LOCK_DURATION_MINUTES)
            prefs.edit {
                putLong(KEY_LOCKED_UNTIL, lockUntil)
            }
            Log.w("AuthSecurity", "Too many failed attempts. Account locked for $LOCK_DURATION_MINUTES minutes")
            return true
        }
        
        return false
    }
    
    /**
     * Record a successful authentication and reset the failed attempt counter
     */
    fun recordSuccessfulAuth(context: Context) {
        getPrefs(context).edit {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_FIRST_FAILED_TIMESTAMP, 0)
            putLong(KEY_LOCKED_UNTIL, 0)
        }
    }
    
    /**
     * Applies dynamic rate limiting based on network conditions and previous failures
     * Call this before authentication attempts for adaptive protection
     */
    suspend fun applyDynamicRateLimiting(context: Context) {
        val prefs = getPrefs(context)
        val failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        
        // Calculate delay based on failed attempts and network type
        var delayMs = RATE_LIMIT_DELAY_MS
        
        // Increase delay for multiple failures
        if (failedAttempts > 1) {
            // Exponential backoff: 1s, 2s, 4s, 8s, etc.
            delayMs *= (1 shl (failedAttempts - 1)).coerceAtMost(16)
        }
        
        // Further increase delay for suspicious conditions
        if (isUsingVpnOrProxy(context)) {
            delayMs = (delayMs * 1.5).toLong()
        }
        
        if (delayMs > 0) {
            delay(delayMs)
        }
    }
    
    /**
     * Check if device is using a VPN or proxy
     */
    private fun isUsingVpnOrProxy(context: Context): Boolean {
        try {
            val connectivityManager = 
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } else {
                @Suppress("DEPRECATION")
                return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("AuthSecurity", "Error checking VPN status: ${e.message}")
            return false
        }
    }
} 