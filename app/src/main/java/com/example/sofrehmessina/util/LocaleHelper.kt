package com.example.sofrehmessina.util

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * Helper class for managing app localization
 */
class LocaleHelper {
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val SELECTED_LANGUAGE = "selected_language"
        
        // Language codes
        const val LANGUAGE_PERSIAN = "fa"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_ITALIAN = "it"
        
        // Default language
        const val DEFAULT_LANGUAGE = LANGUAGE_PERSIAN
        
        /**
         * Get the current locale from the context
         * Returns default locale if none is found
         */
        fun getLocale(context: Context): Locale {
            val configuration = context.resources.configuration
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = configuration.locales
                if (localeList.isEmpty) {
                    Locale(getSelectedLanguageCode(context))
                } else {
                    localeList.get(0) ?: Locale(getSelectedLanguageCode(context))
                }
            } else {
                @Suppress("DEPRECATION")
                configuration.locale
            }
        }
        
        /**
         * Get the currently selected language code from preferences
         */
        fun getSelectedLanguageCode(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(SELECTED_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        }
        
        /**
         * Save the selected language code to preferences
         */
        fun setSelectedLanguageCode(context: Context, languageCode: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(SELECTED_LANGUAGE, languageCode).apply()
        }
        
        /**
         * Update the locale for the given context
         */
        fun updateLocale(context: Context, languageCode: String): ContextWrapper {
            var newContext = context
            val resources = context.resources
            val configuration = Configuration(resources.configuration)
            val locale = Locale(languageCode)
            
            Locale.setDefault(locale)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
                newContext = context.createConfigurationContext(configuration)
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }
            
            return ContextWrapper(newContext)
        }
        
        /**
         * Apply the saved language preference to the given context
         */
        fun applyLanguage(context: Context): ContextWrapper {
            val languageCode = getSelectedLanguageCode(context)
            return updateLocale(context, languageCode)
        }
        
        /**
         * Check if the current locale is RTL
         * Only Persian language should use RTL layout
         */
        fun isRtl(context: Context): Boolean {
            val languageCode = getSelectedLanguageCode(context)
            return languageCode == LANGUAGE_PERSIAN
        }
        
        /**
         * Get the language display name for a given language code
         */
        fun getLanguageDisplayName(languageCode: String): String {
            val locale = Locale(languageCode)
            return locale.getDisplayLanguage(locale).capitalize()
        }
        
        /**
         * Capitalizes the first character of this string.
         */
        private fun String.capitalize(): String {
            return if (this.isNotEmpty()) {
                this[0].uppercaseChar() + this.substring(1)
            } else {
                this
            }
        }
    }
} 