package com.example.sofrehmessina.di

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import com.example.sofrehmessina.util.LocaleHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing app localization with dependency injection support
 */
@Singleton
class LanguageHelper @Inject constructor(private val context: Context) {
    
    /**
     * Update the locale for the given context
     */
    fun updateContextLocale(context: Context): ContextWrapper {
        val languageCode = LocaleHelper.getSelectedLanguageCode(context)
        return LocaleHelper.updateLocale(context, languageCode)
    }
    
    /**
     * Handle configuration changes by applying the stored language preference
     */
    fun onConfigurationChanged() {
        // This method is called when the app's configuration changes, like when the system language changes
        // We don't need to do anything here as the context will be recreated with attachBaseContext
    }
    
    /**
     * Apply the saved language preference to the current context
     */
    fun applyLanguage() {
        LocaleHelper.applyLanguage(context)
    }
    
    /**
     * Set a new language and apply it immediately
     */
    fun setLanguage(languageCode: String) {
        LocaleHelper.setSelectedLanguageCode(context, languageCode)
    }
    
    /**
     * Get the currently selected language code
     */
    fun getCurrentLanguage(): String {
        return LocaleHelper.getSelectedLanguageCode(context)
    }
    
    /**
     * Check if the current locale is RTL
     */
    fun isRtl(): Boolean {
        return LocaleHelper.isRtl(context)
    }
} 