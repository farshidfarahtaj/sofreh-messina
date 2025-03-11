package com.example.sofrehmessina.ui.screens.user

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Settings
import com.example.sofrehmessina.data.model.ThemeOption
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.util.AutoLogoutManager
import com.example.sofrehmessina.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.example.sofrehmessina.data.repository.FirebaseRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val autoLogoutManager: AutoLogoutManager,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    
    private val PREFS_NAME = "SofrehMessinaPrefs"
    private val THEME_KEY = "app_theme"
    private val EMAIL_NOTIFICATIONS_KEY = "email_notifications"
    private val ORDER_NOTIFICATIONS_KEY = "order_notifications"
    private val PROMO_NOTIFICATIONS_KEY = "promo_notifications"
    private val AUTO_LOGOUT_KEY = "auto_logout"
    private val AUTO_LOGOUT_TIME_KEY = "auto_logout_time"
    private val DISPLAY_NAME_KEY = "display_name"
    
    private val _settings = MutableStateFlow(
        Settings(
            displayName = "",
            theme = ThemeOption.SYSTEM,
            emailNotifications = true,
            orderNotifications = true,
            promotionalNotifications = false,
            autoLogout = false,
            autoLogoutTimeMinutes = 30
        )
    )
    
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    
    // Event flow for language changes
    private val _languageChangedEvent = MutableSharedFlow<String>()
    val languageChangedEvent: SharedFlow<String> = _languageChangedEvent
    
    // Event flow for theme changes
    private val _themeChangedEvent = MutableSharedFlow<ThemeOption>()
    val themeChangedEvent: SharedFlow<ThemeOption> = _themeChangedEvent

    // Event to signal that the activity should be recreated
    private val _activityRecreationNeeded = MutableSharedFlow<Unit>()
    val activityRecreationNeeded: SharedFlow<Unit> = _activityRecreationNeeded

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                
                // Load theme setting
                val themeName = prefs.getString(THEME_KEY, ThemeOption.SYSTEM.name)
                val theme = try {
                    ThemeOption.valueOf(themeName ?: ThemeOption.SYSTEM.name)
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Error parsing theme: $themeName", e)
                    ThemeOption.SYSTEM
                }
                
                // Load other settings from local storage
                val displayName = prefs.getString(DISPLAY_NAME_KEY, "") ?: ""
                val emailNotifications = prefs.getBoolean(EMAIL_NOTIFICATIONS_KEY, true)
                val orderNotifications = prefs.getBoolean(ORDER_NOTIFICATIONS_KEY, true)
                val promoNotifications = prefs.getBoolean(PROMO_NOTIFICATIONS_KEY, false)
                
                // For auto-logout, try to get the latest setting from server first
                var autoLogout: Boolean
                var autoLogoutTimeMinutes: Int
                try {
                    // Get the latest auto-logout settings from the server
                    val serverSettings = firebaseRepository.getUserSettings().getOrNull()
                    if (serverSettings != null) {
                        autoLogout = serverSettings.autoLogout
                        autoLogoutTimeMinutes = serverSettings.autoLogoutTimeMinutes
                        
                        // Update local preferences with server settings
                        prefs.edit()
                            .putBoolean(AUTO_LOGOUT_KEY, autoLogout)
                            .putInt(AUTO_LOGOUT_TIME_KEY, autoLogoutTimeMinutes)
                            .apply()
                            
                        Log.d("SettingsViewModel", "Updated local auto-logout settings from server: enabled=$autoLogout, time=$autoLogoutTimeMinutes")
                    } else {
                        // Fall back to local settings if server settings aren't available
                        autoLogout = prefs.getBoolean(AUTO_LOGOUT_KEY, false)
                        autoLogoutTimeMinutes = prefs.getInt(AUTO_LOGOUT_TIME_KEY, 30)
                        Log.d("SettingsViewModel", "Using local auto-logout settings: enabled=$autoLogout, time=$autoLogoutTimeMinutes")
                    }
                } catch (e: Exception) {
                    // Fall back to local settings if there's an error
                    autoLogout = prefs.getBoolean(AUTO_LOGOUT_KEY, false)
                    autoLogoutTimeMinutes = prefs.getInt(AUTO_LOGOUT_TIME_KEY, 30)
                    Log.e("SettingsViewModel", "Error getting auto-logout settings from server, using local: $autoLogout", e)
                }
                
                // Update the AutoLogoutManager with the latest settings
                autoLogoutManager.updateAutoLogoutSettings(autoLogout, autoLogoutTimeMinutes)
                
                Log.d("SettingsViewModel", "Loaded auto-logout from settings: enabled=$autoLogout, time=$autoLogoutTimeMinutes")
                
                _settings.update { currentSettings ->
                    currentSettings.copy(
                        displayName = displayName,
                        theme = theme,
                        emailNotifications = emailNotifications,
                        orderNotifications = orderNotifications,
                        promotionalNotifications = promoNotifications,
                        autoLogout = autoLogout,
                        autoLogoutTimeMinutes = autoLogoutTimeMinutes
                    )
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
                // In case of error, keep default values
            }
        }
    }
    
    /**
     * Initialize display name from user object if not already set
     */
    fun initializeDisplayNameFromUser(user: User?) {
        if (user == null) return
        
        viewModelScope.launch {
            val currentDisplayName = _settings.value.displayName
            
            // Only update if display name is empty or default
            if (currentDisplayName.isEmpty() || currentDisplayName == "User") {
                // Combine name and family name for the display name
                val newDisplayName = if (user.familyName.isNotEmpty()) {
                    "${user.name} ${user.familyName}"
                } else {
                    user.name
                }
                
                if (newDisplayName.isNotEmpty()) {
                    updateDisplayName(newDisplayName)
                }
            }
        }
    }
    
    fun updateDisplayName(name: String) {
        _settings.update { it.copy(displayName = name) }
        saveSettings(DISPLAY_NAME_KEY, name)
    }
    
    fun updateTheme(theme: ThemeOption) {
        Log.d("SettingsViewModel", "Updating theme to: $theme")
        
        // Update in-memory state
        _settings.update { it.copy(theme = theme) }
        
        // Force immediate write to preferences (use commit instead of apply)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(THEME_KEY, theme.name)
            // Use commit() instead of apply() to ensure it's saved before app restart
            val success = editor.commit()
            Log.d("SettingsViewModel", "Theme preference saved successfully: $success")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving theme preference", e)
            // Fallback to the old method if something goes wrong
            saveSettings(THEME_KEY, theme.name)
        }
        
        // Notify about theme change
        viewModelScope.launch {
            Log.d("SettingsViewModel", "Emitting theme change event: $theme")
            _themeChangedEvent.emit(theme)
            
            // Small delay to ensure theme change event is processed
            delay(100)
            
            // Request activity recreation
            Log.d("SettingsViewModel", "Requesting app restart for theme: $theme")
            _activityRecreationNeeded.emit(Unit)
        }
    }
    
    fun updateEmailNotifications(enabled: Boolean) {
        _settings.update { it.copy(emailNotifications = enabled) }
        saveSettings(EMAIL_NOTIFICATIONS_KEY, enabled)
    }
    
    fun updateOrderNotifications(enabled: Boolean) {
        _settings.update { it.copy(orderNotifications = enabled) }
        saveSettings(ORDER_NOTIFICATIONS_KEY, enabled)
    }
    
    fun updatePromotionalNotifications(enabled: Boolean) {
        _settings.update { it.copy(promotionalNotifications = enabled) }
        saveSettings(PROMO_NOTIFICATIONS_KEY, enabled)
    }
    
    fun updateAutoLogout(enabled: Boolean) {
        // Update local state
        _settings.update { it.copy(autoLogout = enabled) }
        
        // Save to local preferences
        saveSettings(AUTO_LOGOUT_KEY, enabled)
        
        // Update the AutoLogoutManager with the new setting
        viewModelScope.launch {
            try {
                // Use the current auto-logout time setting
                val timeMinutes = _settings.value.autoLogoutTimeMinutes
                
                // Update the auto-logout settings in the manager
                autoLogoutManager.updateAutoLogoutSettings(enabled, timeMinutes)
                
                Log.d("SettingsViewModel", "Auto-logout setting updated via manager: enabled=$enabled, time=$timeMinutes")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating auto-logout setting via manager", e)
            }
        }
    }
    
    fun updateAutoLogoutTime(minutes: Int) {
        // Update local state
        _settings.update { it.copy(autoLogoutTimeMinutes = minutes) }
        
        // Save to local preferences
        saveSettings(AUTO_LOGOUT_TIME_KEY, minutes)
        
        // Update the AutoLogoutManager with the new setting
        viewModelScope.launch {
            try {
                // Use the current auto-logout enabled setting
                val enabled = _settings.value.autoLogout
                
                // Update the auto-logout settings in the manager
                autoLogoutManager.updateAutoLogoutSettings(enabled, minutes)
                
                Log.d("SettingsViewModel", "Auto-logout time updated via manager: enabled=$enabled, time=$minutes")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating auto-logout time via manager", e)
            }
        }
    }
    
    private fun saveSettings(key: String, value: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(key, value).apply()
            Log.d("SettingsViewModel", "Saved setting $key: $value")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving setting $key", e)
        }
    }
    
    private fun saveSettings(key: String, value: Boolean) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(key, value).apply()
            Log.d("SettingsViewModel", "Saved setting $key: $value")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving setting $key", e)
        }
    }
    
    private fun saveSettings(key: String, value: Int) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(key, value).apply()
            Log.d("SettingsViewModel", "Saved setting $key: $value")
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Error saving setting $key", e)
        }
    }
    
    fun setLanguage(languageCode: String) {
        // Save the language code to shared preferences and update the app's locale
        viewModelScope.launch {
            // Save language to shared preferences
            LocaleHelper.setSelectedLanguageCode(context, languageCode)
            
            // Note: The actual app restart will be handled by the UI layer (SettingsScreen)
            // as restarting requires Activity-level operations
            
            // Optionally, we could trigger an event to notify UI that language has changed
            Log.d("SettingsViewModel", "Language set to: $languageCode")
        }
    }
    
    // Notify the app that the language has changed
    fun notifyLanguageChanged() {
        viewModelScope.launch {
            val currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
            _languageChangedEvent.emit(currentLanguage)
            Log.d("SettingsViewModel", "Language change event emitted: $currentLanguage")
        }
    }
} 