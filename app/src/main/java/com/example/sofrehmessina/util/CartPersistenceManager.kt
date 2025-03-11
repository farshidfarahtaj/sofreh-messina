package com.example.sofrehmessina.util

import android.content.Context
import android.util.Log
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.data.model.FoodTranslation
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistence of cart items to ensure they are available when the app restarts
 */
@Singleton
class CartPersistenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CartPersistenceManager"
        private const val PREFS_NAME = "CartPreferences"
        private const val KEY_CART_ITEMS = "cart_items"
    }
    
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Saves the current cart items to SharedPreferences
     * @param cartItems The list of cart items to save
     */
    fun saveCart(cartItems: List<CartItem>) {
        try {
            if (cartItems.isEmpty()) {
                // If cart is empty, just clear the preferences
                prefs.edit().remove(KEY_CART_ITEMS).apply()
                Log.d(TAG, "Cart was empty, cleared preferences")
                return
            }
            
            val json = gson.toJson(cartItems)
            prefs.edit().putString(KEY_CART_ITEMS, json).apply()
            Log.d(TAG, "Saved ${cartItems.size} cart items to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cart to preferences: ${e.message}", e)
        }
    }
    
    /**
     * Loads cart items from SharedPreferences
     * @return The list of cart items or an empty list if none are saved
     */
    fun loadCart(): List<CartItem> {
        try {
            val json = prefs.getString(KEY_CART_ITEMS, null) ?: return emptyList()
            
            val type = object : TypeToken<List<CartItem>>() {}.type
            val cartItems: List<CartItem> = gson.fromJson(json, type)
            
            Log.d(TAG, "Loaded ${cartItems.size} cart items from preferences")
            return cartItems
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cart from preferences: ${e.message}", e)
            return emptyList()
        }
    }
    
    /**
     * Clears all saved cart items
     */
    fun clearSavedCart() {
        try {
            prefs.edit().remove(KEY_CART_ITEMS).apply()
            Log.d(TAG, "Cleared saved cart items")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing saved cart: ${e.message}", e)
        }
    }
} 