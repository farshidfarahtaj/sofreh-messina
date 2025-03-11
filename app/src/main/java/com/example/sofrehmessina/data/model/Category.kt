package com.example.sofrehmessina.data.model

/**
 * Data class representing a category with multilingual support
 */
data class Category(
    val id: String = "",
    val translations: Map<String, CategoryTranslation> = mapOf(),
    val imageUrl: String = "",
    // Legacy fields for backward compatibility
    val name: String = "",
    val description: String = ""
) {
    // Helper method to get the translation in the requested language
    // or fall back to another available language if not available
    fun getTranslation(langCode: String): CategoryTranslation {
        return translations[langCode] 
            ?: translations["en"]  // Fallback to English
            ?: translations["fa"]  // Fallback to Persian
            ?: translations.values.firstOrNull()  // Any available translation
            ?: CategoryTranslation(name, description)  // Legacy fallback
    }
    
    // Convenience property to get name in the given language
    fun getName(langCode: String): String = getTranslation(langCode).name
    
    // Convenience property to get description in the given language
    fun getDescription(langCode: String): String = getTranslation(langCode).description
}

/**
 * Data class representing language-specific content for a category
 */
data class CategoryTranslation(
    val name: String = "",
    val description: String = ""
) 