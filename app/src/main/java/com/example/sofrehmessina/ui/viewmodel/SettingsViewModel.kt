package com.example.sofrehmessina.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Settings
import com.example.sofrehmessina.data.model.ThemeOption
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This is the old version of SettingsViewModel 
 * Kept for reference but now replaced by the version in ui.screens.user
 */
@HiltViewModel
class OldSettingsViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    @Suppress("unused")
    private val _settingsState = MutableStateFlow<SettingsState>(SettingsState.Initial)
    @Suppress("unused")
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private val _language = MutableStateFlow(LocaleHelper.DEFAULT_LANGUAGE)
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            _settingsState.value = SettingsState.Loading
            try {
                repository.getUserSettings()
                    .onSuccess { userSettings ->
                        _settings.value = userSettings
                        _settingsState.value = SettingsState.Success(userSettings)
                    }
                    .onFailure { e ->
                        _error.value = e
                        _settingsState.value = SettingsState.Error(e)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateUserSettings(settings)
                    .onSuccess {
                        _settings.value = settings
                        _settingsState.value = SettingsState.Success(settings)
                    }
                    .onFailure { e ->
                        _error.value = e
                        _settingsState.value = SettingsState.Error(e)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTheme(theme: ThemeOption) {
        val updatedSettings = _settings.value.copy(
            theme = theme
        )
        updateSettings(updatedSettings)
    }

    fun toggleNotifications() {
        val updatedSettings = _settings.value.copy(
            orderNotifications = !_settings.value.orderNotifications
        )
        updateSettings(updatedSettings)
    }

    fun toggleEmailNotifications() {
        val updatedSettings = _settings.value.copy(
            emailNotifications = !_settings.value.emailNotifications
        )
        updateSettings(updatedSettings)
    }

    fun setLanguage(languageCode: String) {
        _language.value = languageCode
        // The actual language change is handled by LocaleHelper
    }

    // Method to sync the language setting with LocaleHelper
    fun syncLanguageWithLocaleHelper(context: Context) {
        val languageCode = LocaleHelper.getSelectedLanguageCode(context)
        if (_language.value != languageCode) {
            _language.value = languageCode
        }
    }

    fun clearAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearUserData()
                    .onSuccess {
                        _settings.value = Settings() // Reset to defaults
                        _settingsState.value = SettingsState.Success(_settings.value)
                    }
                    .onFailure { e ->
                        _error.value = e
                        _settingsState.value = SettingsState.Error(e)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
        if (_settingsState.value is SettingsState.Error) {
            _settingsState.value = SettingsState.Initial
        }
    }

    sealed class SettingsState {
        data object Initial : SettingsState()
        data object Loading : SettingsState()
        data class Success(val settings: Settings) : SettingsState()
        data class Error(val error: Throwable) : SettingsState()
    }
} 