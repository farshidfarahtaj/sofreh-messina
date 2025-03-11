package com.example.sofrehmessina.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.sofrehmessina.data.model.Languages
import com.example.sofrehmessina.util.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URLEncoder

/**
 * Manages translations and language settings throughout the app
 */
@Singleton
class TranslationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Gets the current language code from preferences
     */
    fun getCurrentLanguage(): String {
        return LocaleHelper.getSelectedLanguageCode(context)
    }
    
    /**
     * Gets a fallback chain of languages to try,
     * starting with the current language and followed by fallbacks
     */
    fun getLanguageFallbackChain(): List<String> {
        val currentLanguage = getCurrentLanguage()
        val fallbackChain = mutableListOf(currentLanguage)
        
        // Always add English as a fallback if it's not the current language
        if (currentLanguage != Languages.ENGLISH) {
            fallbackChain.add(Languages.ENGLISH)
        }
        
        // Add Persian as another fallback if it's not already in the chain
        if (currentLanguage != Languages.PERSIAN && !fallbackChain.contains(Languages.PERSIAN)) {
            fallbackChain.add(Languages.PERSIAN)
        }
        
        // Add any remaining supported languages
        Languages.SUPPORTED_LANGUAGES.forEach { lang ->
            if (!fallbackChain.contains(lang)) {
                fallbackChain.add(lang)
            }
        }
        
        return fallbackChain
    }
    
    /**
     * Gets a localized value from a translations map,
     * falling back to other languages if needed
     */
    fun <T> getLocalizedValue(translations: Map<String, T>?): T? {
        if (translations.isNullOrEmpty()) return null
        
        // Try each language in the fallback chain
        for (langCode in getLanguageFallbackChain()) {
            translations[langCode]?.let { return it }
        }
        
        // If all else fails, just return the first available translation
        return translations.values.firstOrNull()
    }
    
    /**
     * Generate empty translation map for all supported languages
     */
    fun createEmptyTranslations(): Map<String, String> {
        return Languages.SUPPORTED_LANGUAGES.associateWith { "" }
    }
    
    /**
     * Generate a Google Translate URL to help with manual translation
     * 
     * @param text The text to translate
     * @param sourceLang The source language code
     * @param targetLang The target language code
     * @return A URL that can be opened in a browser to translate the text
     */
    fun getTranslateUrl(text: String, sourceLang: String, targetLang: String): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        return "https://translate.google.com/?sl=$sourceLang&tl=$targetLang&text=$encodedText&op=translate"
    }
    
    /**
     * Open Google Translate in a browser to translate the provided text
     * 
     * @param text The text to translate
     * @param sourceLang The source language code
     * @param targetLang The target language code
     */
    fun openTranslator(text: String, sourceLang: String, targetLang: String) {
        val url = getTranslateUrl(text, sourceLang, targetLang)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
} 