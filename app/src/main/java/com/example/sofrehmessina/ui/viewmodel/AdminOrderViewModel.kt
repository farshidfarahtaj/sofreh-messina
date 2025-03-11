package com.example.sofrehmessina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminOrderViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders

    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    private val _statusFilter = MutableStateFlow<OrderStatus?>(null)
    val statusFilter: StateFlow<OrderStatus?> = _statusFilter

    private val _customerData = MutableStateFlow<User?>(null)
    val customerData: StateFlow<User?> = _customerData

    private val _orderScreenCustomerData = MutableStateFlow<Map<String, User?>>(emptyMap())
    val orderScreenCustomerData: StateFlow<Map<String, User?>> = _orderScreenCustomerData

    init {
        loadAllOrders()
    }

    fun loadAllOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getOrders().collect { orders ->
                    _allOrders.value = orders
                    applyFilters()
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOrderDetails(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First try to find in memory
                val cachedOrder = _allOrders.value.find { it.id == orderId }
                if (cachedOrder != null) {
                    _selectedOrder.value = cachedOrder
                    loadCustomerData(cachedOrder.userId)
                } else {
                    // If not found in memory, fetch directly from repository
                    repository.getOrder(orderId)
                        .onSuccess { order ->
                            _selectedOrder.value = order
                            loadCustomerData(order.userId)
                        }
                        .onFailure { e ->
                            _error.value = e
                        }
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadCustomerData(userId: String) {
        viewModelScope.launch {
            try {
                repository.getUserData(userId)
                    .onSuccess { user ->
                        _customerData.value = user
                    }
                    .onFailure { e ->
                        Log.e("AdminOrderViewModel", "Error loading customer data: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e("AdminOrderViewModel", "Exception loading customer data: ${e.message}")
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateOrderStatus(orderId, status)
                    .onSuccess {
                        loadAllOrders()
                        _selectedOrder.value?.let { order ->
                            if (order.id == orderId) {
                                _selectedOrder.value = order.copy(status = status)
                            }
                        }
                    }
                    .onFailure { e ->
                        _error.value = e
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setStatusFilter(status: OrderStatus?) {
        _statusFilter.value = status
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = _allOrders.value.filter { order ->
            (_statusFilter.value == null || order.status == _statusFilter.value)
        }
        _filteredOrders.value = filtered
    }

    fun clearSelectedOrder() {
        _selectedOrder.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun loadCustomerDataForOrdersScreen(userId: String) {
        viewModelScope.launch {
            try {
                if (_orderScreenCustomerData.value.containsKey(userId)) {
                    return@launch
                }
                
                repository.getUserData(userId)
                    .onSuccess { user ->
                        val currentMap = _orderScreenCustomerData.value.toMutableMap()
                        currentMap[userId] = user
                        _orderScreenCustomerData.value = currentMap
                    }
                    .onFailure { e ->
                        Log.e("AdminOrderViewModel", "Error loading customer data for orders screen: ${e.message}")
                        val currentMap = _orderScreenCustomerData.value.toMutableMap()
                        currentMap[userId] = null
                        _orderScreenCustomerData.value = currentMap
                    }
            } catch (e: Exception) {
                Log.e("AdminOrderViewModel", "Exception loading customer data for orders screen: ${e.message}")
            }
        }
    }
} 