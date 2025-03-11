package com.example.sofrehmessina.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()
    
    private val _categories = MutableStateFlow<Map<String, Category>>(emptyMap())
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // Store discounts information
    private val _discounts = MutableStateFlow<Map<String, Double>>(emptyMap())
    val discounts: StateFlow<Map<String, Double>> = _discounts.asStateFlow()
    
    // Track total before and after discounts
    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()
    
    private val _discountTotal = MutableStateFlow(0.0)
    val discountTotal: StateFlow<Double> = _discountTotal.asStateFlow()
    
    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { categories ->
                _categories.value = categories.associateBy { it.id }
            }
        }
    }
    
    fun addToCart(food: Food, quantity: Int = 1) {
        val currentCart = _cartItems.value.toMutableList()
        val existingItem = currentCart.find { it.food.id == food.id }
        
        if (existingItem != null) {
            // Update existing item
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            // Add new item
            currentCart.add(CartItem(food = food, quantity = quantity))
        }
        
        _cartItems.value = currentCart
        calculateTotals()
    }
    
    fun removeFromCart(foodId: String) {
        _cartItems.value = _cartItems.value.filter { it.food.id != foodId }
        calculateTotals()
    }
    
    fun updateQuantity(foodId: String, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(foodId)
            return
        }
        
        val currentCart = _cartItems.value.toMutableList()
        val existingItem = currentCart.find { it.food.id == foodId }
        
        if (existingItem != null) {
            val index = currentCart.indexOf(existingItem)
            currentCart[index] = existingItem.copy(quantity = quantity)
            _cartItems.value = currentCart
            calculateTotals()
        }
    }
    
    fun clearCart() {
        _cartItems.value = emptyList()
        _discounts.value = emptyMap()
        _subtotal.value = 0.0
        _discountTotal.value = 0.0
        _total.value = 0.0
    }
    
    fun calculateTotals() {
        viewModelScope.launch {
            val items = _cartItems.value
            
            // Calculate subtotal (before discounts)
            val subtotal = items.sumOf { it.food.price * it.quantity }
            _subtotal.value = subtotal
            
            // Calculate discounts
            try {
                val discountsResult = repository.calculateDiscounts(items)
                if (discountsResult.isSuccess) {
                    val discounts = discountsResult.getOrNull() ?: emptyMap()
                    _discounts.value = discounts
                    
                    // Sum up all discounts
                    val totalDiscount = discounts.values.sum()
                    _discountTotal.value = totalDiscount
                    
                    // Calculate final total
                    _total.value = subtotal - totalDiscount
                } else {
                    // If discount calculation fails, just use subtotal as total
                    _discounts.value = emptyMap()
                    _discountTotal.value = 0.0
                    _total.value = subtotal
                    
                    Log.e("CartViewModel", "Error calculating discounts: ${discountsResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                // If discount calculation fails, just use subtotal as total
                _discounts.value = emptyMap()
                _discountTotal.value = 0.0
                _total.value = subtotal
                
                Log.e("CartViewModel", "Error calculating discounts: ${e.message}")
            }
        }
    }
    
    fun getCategoryName(categoryId: String): String {
        return _categories.value[categoryId]?.name ?: "Unknown Category"
    }
    
    fun checkout(userId: String, address: String, phone: String, paymentMethod: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Always recalculate discounts before checkout
                calculateTotals()
                
                val items = _cartItems.value
                if (items.isEmpty()) {
                    _message.value = "Cart is empty"
                    _isLoading.value = false
                    return@launch
                }
                
                // Create the order
                val now = Date()
                val order = Order(
                    userId = userId,
                    items = items,
                    status = OrderStatus.PENDING,
                    subtotal = _subtotal.value,
                    discounts = _discounts.value,
                    total = _total.value,
                    address = address,
                    phone = phone,
                    paymentMethod = paymentMethod,
                    createdAt = now,
                    updatedAt = now
                )
                
                val result = repository.createOrder(order)
                if (result.isSuccess) {
                    _message.value = "Order placed successfully!"
                    clearCart()
                } else {
                    _message.value = "Failed to place order: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _message.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
} 