package com.example.sofrehmessina.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.data.model.TimeRange
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.ui.screens.admin.DashboardMetric
import com.example.sofrehmessina.utils.CurrencyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val currencyManager: CurrencyManager
) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders
    
    private val _recentOrders = MutableStateFlow<List<Order>>(emptyList())
    val recentOrders: StateFlow<List<Order>> = _recentOrders
    
    private val _metrics = MutableStateFlow<List<DashboardMetric>>(emptyList())
    val metrics: StateFlow<List<DashboardMetric>> = _metrics

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    init {
        loadOrders()
        loadPendingOrders()
    }
    
    fun loadDashboardData() {
        loadOrders()
        loadPendingOrders()
        updateMetrics()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getOrders().collect { ordersList ->
                    _orders.value = filterOrdersByTimeRange(ordersList, _selectedTimeRange.value)
                    _recentOrders.value = ordersList.sortedByDescending { it.createdAt }.take(5)
                    updateMetrics()
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadPendingOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getPendingOrders().collect { _ ->
                    updateMetrics()
                }
            } catch (e: Exception) {
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    @Suppress("unused")
    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateOrderStatus(orderId, status)
                    .onSuccess {
                        loadOrders()
                        loadPendingOrders()
                    }
                    .onFailure { e ->
                        _error.value = e
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
        loadOrders()
    }

    private fun filterOrdersByTimeRange(orders: List<Order>, timeRange: TimeRange): List<Order> {
        val startDate = timeRange.toStartDate()
        return orders.filter { order ->
            order.createdAt.after(Date(startDate))
        }
    }
    
    private fun updateMetrics() {
        val totalRevenue = getTotalRevenue()
        val pendingCount = getOrderCountByStatus(OrderStatus.PENDING)
        val completedCount = getOrderCountByStatus(OrderStatus.DELIVERED)
        val cancelledCount = getOrderCountByStatus(OrderStatus.CANCELLED)
        
        _metrics.value = listOf(
            DashboardMetric(
                title = "Total Revenue",
                value = currencyManager.formatPrice(totalRevenue),
                icon = Icons.Default.Euro,
                color = Color(0xFF4CAF50)
            ),
            DashboardMetric(
                title = "Pending Orders",
                value = pendingCount.toString(),
                icon = Icons.Default.Pending,
                color = Color(0xFFFFA000)
            ),
            DashboardMetric(
                title = "Completed Orders",
                value = completedCount.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF2196F3)
            ),
            DashboardMetric(
                title = "Cancelled Orders",
                value = cancelledCount.toString(),
                icon = Icons.Default.Cancel,
                color = Color(0xFFF44336)
            )
        )
    }

    private fun getTotalRevenue(): Double {
        return _orders.value.sumOf { it.total }
    }

    private fun getOrderCountByStatus(status: OrderStatus): Int {
        return _orders.value.count { it.status == status }
    }

    fun clearError() {
        _error.value = null
    }
} 