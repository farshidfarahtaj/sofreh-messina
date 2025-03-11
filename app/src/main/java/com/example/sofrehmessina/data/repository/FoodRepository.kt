package com.example.sofrehmessina.data.repository

import android.util.Log
import com.example.sofrehmessina.data.model.FoodItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing food items in Firestore with multilingual support
 */
@Singleton
class FoodRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FoodRepository"
        private const val COLLECTION_FOODS = "foods"
    }
    
    private val _foods = MutableStateFlow<List<FoodItem>>(emptyList())
    val foods: Flow<List<FoodItem>> = _foods.asStateFlow()
    
    /**
     * Loads all food items from Firestore
     */
    suspend fun loadFoods() {
        try {
            val snapshot = db.collection(COLLECTION_FOODS).get().await()
            val foods = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
            _foods.value = foods
        } catch (e: Exception) {
            Log.e(TAG, "Error loading foods: ${e.message}")
        }
    }
    
    /**
     * Gets food items by category ID
     */
    suspend fun getFoodsByCategory(categoryId: String): List<FoodItem> {
        return try {
            val snapshot = db.collection(COLLECTION_FOODS)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("available", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foods by category: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Gets featured food items
     */
    suspend fun getFeaturedFoods(): List<FoodItem> {
        return try {
            val snapshot = db.collection(COLLECTION_FOODS)
                .whereEqualTo("featured", true)
                .whereEqualTo("available", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting featured foods: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Adds a new food item to Firestore
     */
    suspend fun addFood(food: FoodItem): Result<String> {
        return try {
            // Create a map to represent the food item
            val foodMap = mapOf(
                "translations" to food.translations,
                "price" to food.price,
                "discountPrice" to food.discountPrice,
                "categoryId" to food.categoryId,
                "imageUrl" to food.imageUrl,
                "featured" to food.featured,
                "available" to food.available,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            val docRef = db.collection(COLLECTION_FOODS).document()
            docRef.set(foodMap).await()
            
            // Refresh the food list
            loadFoods()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding food: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Updates an existing food item in Firestore
     */
    suspend fun updateFood(food: FoodItem): Result<Unit> {
        return try {
            // Create a map to represent the food item
            val foodMap = mapOf(
                "translations" to food.translations,
                "price" to food.price,
                "discountPrice" to food.discountPrice,
                "categoryId" to food.categoryId,
                "imageUrl" to food.imageUrl,
                "featured" to food.featured,
                "available" to food.available,
                "updatedAt" to System.currentTimeMillis()
            )
            
            db.collection(COLLECTION_FOODS).document(food.id).update(foodMap).await()
            
            // Refresh the food list
            loadFoods()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating food: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a food item from Firestore
     */
    suspend fun deleteFood(foodId: String): Result<Unit> {
        return try {
            db.collection(COLLECTION_FOODS).document(foodId).delete().await()
            
            // Refresh the food list
            loadFoods()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting food: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Gets a single food item by ID
     */
    suspend fun getFoodById(foodId: String): FoodItem? {
        return try {
            val doc = db.collection(COLLECTION_FOODS).document(foodId).get().await()
            doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting food by ID: ${e.message}")
            null
        }
    }
    
    /**
     * Searches for food items by name (across all languages)
     */
    suspend fun searchFoodsByName(query: String): List<FoodItem> {
        if (query.isBlank()) return emptyList()
        
        // Load all foods and filter in memory
        // This is a simple approach - for production, consider using Algolia or a similar search service
        return try {
            val allFoods = if (_foods.value.isEmpty()) {
                val snapshot = db.collection(COLLECTION_FOODS).get().await()
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(FoodItem::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                _foods.value
            }
            
            // Search across all language translations
            val lowercaseQuery = query.lowercase()
            allFoods.filter { food ->
                food.translations.values.any { translation ->
                    translation.name.lowercase().contains(lowercaseQuery) ||
                    translation.description.lowercase().contains(lowercaseQuery)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching foods: ${e.message}")
            emptyList()
        }
    }
} 