package com.example.sofrehmessina.data.model

import java.util.Date

/**
 * Data class representing a food item with multilingual support
 */
data class Food(
    val id: String = "",
    val translations: Map<String, FoodTranslation> = mapOf(),
    val price: Double = 0.0,
    val imageUrl: String = "",
    val categoryId: String = "",
    
    // Primary availability field - this is the source of truth
    val foodAvailable: Boolean = true, // Default to true
    
    // Optional discount-related fields
    val discountedPrice: Double? = null,
    val discountPercentage: Double? = null,
    val discountEndDate: Date? = null,
    val discountMessage: String? = null,
    
    // Legacy fields for backward compatibility
    val name: String = "",
    val description: String = "",
    
    // Legacy availability field - keep for backward compatibility
    val available: Boolean = true
) {
    // Computed property for UI/display - NOT stored in Firestore directly
    // Do NOT name this "isAvailable" to avoid conflicting getters with FireStore
    fun isItemAvailable(): Boolean {
        return foodAvailable
    }
    
    // Helper method to get the translation in the requested language
    // or fall back to another available language if not available
    fun getTranslation(langCode: String): FoodTranslation {
        return translations[langCode] 
            ?: translations["en"]  // Fallback to English
            ?: translations["fa"]  // Fallback to Persian
            ?: translations.values.firstOrNull()  // Any available translation
            ?: FoodTranslation(name, description)  // Legacy fallback
    }
    
    // Convenience property to get name in the given language
    fun getName(langCode: String): String = getTranslation(langCode).name
    
    // Convenience property to get description in the given language
    fun getDescription(langCode: String): String = getTranslation(langCode).description
    
    /**
     * Create a copy of this food item but with availability explicitly set
     * This ensures both fields are updated
     */
    fun withAvailability(available: Boolean): Food {
        return this.copy(foodAvailable = available, available = available)
    }
    
    /**
     * Get the effective price (discounted if available, otherwise regular)
     */
    fun getEffectivePrice(): Double {
        return discountedPrice ?: price
    }
    
    /**
     * Check if this food item has any kind of discount
     */
    fun hasDiscount(): Boolean {
        return (discountedPrice != null && discountedPrice < price) || 
               (discountPercentage != null && discountPercentage > 0)
    }
    
    /**
     * Get a debug representation of this food item
     */
    fun toDebugString(): String {
        return "Food(id=$id, name=$name, available=$available, foodAvailable=$foodAvailable, price=$price, " +
               "discountedPrice=$discountedPrice, discountPercentage=$discountPercentage)"
    }
}

// FoodTranslation class has been moved to FoodItem.kt 