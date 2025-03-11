package com.example.sofrehmessina.data.model

/**
 * Data class representing a food item with multilingual support
 */
data class FoodItem(
    val id: String = "",
    val translations: Map<String, FoodTranslation> = mapOf(),
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val categoryId: String = "",
    val imageUrl: String = "",
    val featured: Boolean = false,
    val available: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Helper method to get the translation in the requested language
    // or fall back to another available language if not available
    fun getTranslation(langCode: String): FoodTranslation {
        return translations[langCode] 
            ?: translations["en"]  // Fallback to English
            ?: translations["fa"]  // Fallback to Persian
            ?: translations.values.firstOrNull()  // Any available translation
            ?: FoodTranslation()  // Empty translation as last resort
    }
    
    // Convenience property to get name in the current app language
    fun getName(langCode: String): String = getTranslation(langCode).name
    
    // Convenience property to get description in the current app language
    fun getDescription(langCode: String): String = getTranslation(langCode).description
}

/**
 * Data class representing language-specific content for a food item
 */
data class FoodTranslation(
    val name: String = "",
    val description: String = "",
    val ingredients: List<String> = listOf()
)

/**
 * Supported languages in the application
 */
object Languages {
    const val PERSIAN = "fa" 
    const val ENGLISH = "en"
    const val ITALIAN = "it"
    
    val SUPPORTED_LANGUAGES = listOf(PERSIAN, ENGLISH, ITALIAN)
} 