package com.example.sofrehmessina.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.data.model.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages auto-logout functionality across the app
 * This centralizes the logic for checking and performing auto-logout
 */
@Singleton
class AutoLogoutManager @Inject constructor(
    private val context: Context,
    private val firebaseRepository: FirebaseRepository
) {
    companion object {
        private const val TAG = "AutoLogoutManager"
        private const val PREFS_NAME = "AutoLogoutPrefs"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val KEY_AUTO_LOGOUT_ENABLED = "auto_logout_enabled"
        private const val KEY_AUTO_LOGOUT_TIME_MINUTES = "auto_logout_time_minutes"
        
        // Default auto-logout time in minutes
        private const val DEFAULT_AUTO_LOGOUT_TIME = 30
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Records when the app goes to background
     * Call this from onStop in activities
     */
    fun recordBackgroundTime() {
        prefs.edit()
            .putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis())
            .apply()
        
        Log.d(TAG, "Recorded background time: ${System.currentTimeMillis()}")
    }
    
    /**
     * Checks if auto-logout should be performed based on time in background
     * Call this from onResume in activities
     * 
     * @return true if auto-logout was performed, false otherwise
     */
    suspend fun checkAndPerformAutoLogoutIfNeeded(): Boolean {
        try {
            // Get the last background time
            val lastBackgroundTime = prefs.getLong(KEY_LAST_BACKGROUND_TIME, 0)
            if (lastBackgroundTime == 0L) {
                // No background time recorded yet
                return false
            }
            
            // Get auto-logout settings
            val settings = getAutoLogoutSettings()
            if (!settings.first) {
                // Auto-logout is disabled
                Log.d(TAG, "Auto-logout is disabled")
                return false
            }
            
            // Calculate time in background
            val currentTime = System.currentTimeMillis()
            val timeInBackgroundMinutes = (currentTime - lastBackgroundTime) / (1000 * 60)
            val autoLogoutTimeMinutes = settings.second
            
            Log.d(TAG, "Time in background: $timeInBackgroundMinutes minutes, Auto-logout after: $autoLogoutTimeMinutes minutes")
            
            // Check if auto-logout should be performed
            if (timeInBackgroundMinutes >= autoLogoutTimeMinutes) {
                Log.d(TAG, "Auto-logout time exceeded, performing logout")
                performAutoLogout()
                return true
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auto-logout: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Checks if auto-logout is enabled when the app is explicitly closed
     * Call this from onDestroy in activities when isFinishing is true
     */
    suspend fun checkAndPerformAutoLogoutOnClose() {
        try {
            // Get auto-logout settings
            val settings = getAutoLogoutSettings()
            if (!settings.first) {
                // Auto-logout is disabled
                Log.d(TAG, "Auto-logout is disabled on app close")
                return
            }
            
            Log.d(TAG, "Auto-logout is enabled on app close, performing logout")
            performAutoLogout()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auto-logout on close: ${e.message}", e)
        }
    }
    
    /**
     * Gets the auto-logout settings from Firestore
     * 
     * @return Pair of (isEnabled, timeInMinutes)
     */
    private suspend fun getAutoLogoutSettings(): Pair<Boolean, Int> {
        return try {
            // Try to get from Firestore first
            val settingsResult = firebaseRepository.getUserSettings()
            val autoLogoutEnabled = settingsResult.getOrNull()?.autoLogout ?: false
            val autoLogoutTimeMinutes = settingsResult.getOrNull()?.autoLogoutTimeMinutes ?: DEFAULT_AUTO_LOGOUT_TIME
            
            Log.d(TAG, "Fetched auto-logout settings from Firestore: enabled=$autoLogoutEnabled, time=$autoLogoutTimeMinutes")
            
            // Cache the settings for quick access
            prefs.edit()
                .putBoolean(KEY_AUTO_LOGOUT_ENABLED, autoLogoutEnabled)
                .putInt(KEY_AUTO_LOGOUT_TIME_MINUTES, autoLogoutTimeMinutes)
                .apply()
            
            // Safety check - if firestore is returning a null value for autoLogout
            // always treat it as false/disabled to prevent unexpected logouts
            val safeEnabled = autoLogoutEnabled && (settingsResult.getOrNull() != null)
            
            Pair(safeEnabled, autoLogoutTimeMinutes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auto-logout settings from Firestore: ${e.message}", e)
            
            // Fall back to cached settings
            val cachedEnabled = prefs.getBoolean(KEY_AUTO_LOGOUT_ENABLED, false) // Default to FALSE if not found
            val cachedTime = prefs.getInt(KEY_AUTO_LOGOUT_TIME_MINUTES, DEFAULT_AUTO_LOGOUT_TIME)
            
            Log.d(TAG, "Using cached auto-logout settings: enabled=$cachedEnabled, time=$cachedTime")
            
            Pair(cachedEnabled, cachedTime)
        }
    }
    
    /**
     * Performs the actual auto-logout
     */
    private suspend fun performAutoLogout() {
        withContext(Dispatchers.IO) {
            try {
                // Double-check that auto-logout is still enabled before performing the logout
                val settings = getAutoLogoutSettings()
                if (!settings.first) {
                    Log.d(TAG, "Auto-logout is now disabled, canceling logout operation")
                    return@withContext
                }
                
                Log.d(TAG, "Performing auto-logout")
                
                // Sign out the user
                firebaseRepository.signOut()
                
                // Clear user preferences
                val userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                userPrefs.edit().clear().apply()
                
                // Reset background time
                prefs.edit().remove(KEY_LAST_BACKGROUND_TIME).apply()
                
                Log.d(TAG, "Auto-logout completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during auto-logout: ${e.message}", e)
            }
        }
    }
    
    /**
     * Updates the auto-logout settings
     * Call this when the user changes the settings
     */
    suspend fun updateAutoLogoutSettings(enabled: Boolean, timeMinutes: Int = DEFAULT_AUTO_LOGOUT_TIME) {
        try {
            // Get current settings first
            val currentSettingsResult = firebaseRepository.getUserSettings()
            if (currentSettingsResult.isSuccess) {
                val currentSettings = currentSettingsResult.getOrNull()
                // Create a new settings object with updated auto logout values
                val updatedSettings = currentSettings?.copy(
                    autoLogout = enabled,
                    autoLogoutTimeMinutes = timeMinutes
                ) ?: Settings(
                    autoLogout = enabled,
                    autoLogoutTimeMinutes = timeMinutes
                )
                
                // Update the settings in Firestore
                firebaseRepository.updateUserSettings(updatedSettings)
            }
            
            // Update the cached settings
            prefs.edit()
                .putBoolean(KEY_AUTO_LOGOUT_ENABLED, enabled)
                .putInt(KEY_AUTO_LOGOUT_TIME_MINUTES, timeMinutes)
                .apply()
            
            Log.d(TAG, "Auto-logout settings updated - Enabled: $enabled, Time: $timeMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating auto-logout settings: ${e.message}", e)
        }
    }
    
    /**
     * Syncs auto-logout settings from server to local storage
     * Call this on app startup or when changes are made elsewhere
     */
    suspend fun syncAutoLogoutSettings() {
        try {
            // Try to get settings from Firestore
            val settingsResult = firebaseRepository.getUserSettings()
            
            if (settingsResult.isSuccess) {
                val settings = settingsResult.getOrNull()
                
                // Update local preferences with server settings
                if (settings != null) {
                    val enabled = settings.autoLogout
                    val timeMinutes = settings.autoLogoutTimeMinutes
                    
                    prefs.edit()
                        .putBoolean(KEY_AUTO_LOGOUT_ENABLED, enabled)
                        .putInt(KEY_AUTO_LOGOUT_TIME_MINUTES, timeMinutes)
                        .apply()
                    
                    Log.d(TAG, "Synced auto-logout settings from server: enabled=$enabled, time=$timeMinutes")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing auto-logout settings: ${e.message}", e)
            // If there's an error, we keep using the cached settings
        }
    }
} 