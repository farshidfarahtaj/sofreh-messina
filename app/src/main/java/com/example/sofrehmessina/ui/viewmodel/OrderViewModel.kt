package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.data.model.OrderItem
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _recentOrders = MutableStateFlow<List<Order>>(emptyList())
    val recentOrders: StateFlow<List<Order>> = _recentOrders

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun placeOrder(
        cartItems: List<CartItem>,
        totalAmount: Double,
        deliveryAddress: String,
        specialInstructions: String,
        couponCode: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current user ID from repository
                val userId = repository.getCurrentUserId()
                    ?: throw Exception("User not logged in")
                
                // Get user data for the order
                val userResult = repository.getUserData(userId)
                
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    
                    // Calculate original subtotal (without discounts)
                    val originalSubtotal = cartItems.sumOf { it.food.price * it.quantity }
                    
                    // Calculate discounts by category
                    val discountsByCategory = mutableMapOf<String, Double>()
                    cartItems.forEach { item ->
                        if (item.food.discountedPrice != null) {
                            val categoryId = item.food.categoryId
                            val itemDiscount = (item.food.price - item.food.discountedPrice) * item.quantity
                            discountsByCategory[categoryId] = (discountsByCategory[categoryId] ?: 0.0) + itemDiscount
                        }
                    }
                    
                    // Create the Order object
                    val order = Order(
                        userId = userId,
                        items = cartItems,
                        subtotal = originalSubtotal,
                        total = totalAmount, // Use the discounted total passed from checkout
                        discounts = discountsByCategory, // Include discount breakdown by category
                        address = deliveryAddress,
                        phone = user.phone,
                        paymentMethod = "Cash", // Assuming cash payment method
                        notes = specialInstructions,
                        status = OrderStatus.PENDING,
                        couponCode = couponCode // Include the coupon code if provided
                    )
                    
                    // Save the order to Firebase
                    val result = repository.createOrder(order)
                    
                    result.onSuccess { createdOrder ->
                        // Add the created order to the orders list
                        val updatedOrders = _orders.value.toMutableList()
                        updatedOrders.add(createdOrder)
                        _orders.value = updatedOrders
                        
                        // Immediately refresh the user's orders to ensure the new order appears in the list
                        loadUserOrders(userId)
                    }.onFailure { e ->
                        _error.value = "Failed to create order: ${e.message}"
                    }
                } else {
                    _error.value = "Failed to get user data: ${userResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error placing order: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOrders(userId: String) {
        loadUserOrders(userId)
    }

    private fun loadUserOrders(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getUserOrders(userId)
                    .onSuccess { orders ->
                        _orders.value = orders
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getOrders().collect { orders ->
                    _orders.value = orders
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateOrderStatus(orderId, status)
                    .onSuccess { _ ->
                        val updatedOrders = _orders.value.map { order ->
                            if (order.id == orderId) order.copy(status = status) else order
                        }
                        _orders.value = updatedOrders
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getOrderById(orderId: String): Order? {
        return _orders.value.find { it.id == orderId }
    }

    fun getOrdersByStatus(status: OrderStatus): List<Order> {
        return _orders.value.filter { it.status == status }
    }

    fun clearError() {
        _error.value = null
    }

    fun loadRecentOrders(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getUserOrders(userId)
                    .onSuccess { orders ->
                        _recentOrders.value = orders.sortedByDescending { it.createdAt }
                    }
                    .onFailure { e ->
                        _error.value = e.message
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }
} 