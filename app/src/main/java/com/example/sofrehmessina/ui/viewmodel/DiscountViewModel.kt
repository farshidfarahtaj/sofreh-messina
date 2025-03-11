package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Discount
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class DiscountViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    // UI States
    private val _discounts = MutableStateFlow<List<Discount>>(emptyList())
    val discounts: StateFlow<List<Discount>> = _discounts.asStateFlow()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadDiscounts()
        loadCategories()
    }
    
    fun loadDiscounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                repository.getDiscounts().collectLatest { discountList ->
                    _discounts.value = discountList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load discounts: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getCategories().collectLatest { categoryList ->
                    _categories.value = categoryList
                }
            } catch (e: Exception) {
                _error.value = "Failed to load categories: ${e.message}"
            }
        }
    }
    
    fun createDiscount(
        name: String,
        description: String,
        categoryId: String,
        minQuantity: Int,
        percentOff: Double,
        active: Boolean = true,
        startDate: Date? = null,
        endDate: Date? = null,
        specificFoodIds: List<String>? = null,
        couponCode: String? = null,
        isCustomerSpecific: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val now = Date()
                val discount = Discount(
                    name = name,
                    description = description,
                    categoryId = categoryId,
                    minQuantity = minQuantity,
                    percentOff = percentOff,
                    active = active,
                    startDate = startDate,
                    endDate = endDate,
                    specificFoodIds = specificFoodIds,
                    createdAt = now,
                    updatedAt = now,
                    couponCode = couponCode,
                    isCustomerSpecific = isCustomerSpecific
                )
                
                val result = repository.createDiscount(discount)
                
                if (result.isSuccess) {
                    loadDiscounts()
                } else {
                    _error.value = "Failed to create discount: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error creating discount: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun updateDiscount(
        id: String,
        name: String,
        description: String,
        categoryId: String,
        minQuantity: Int,
        percentOff: Double,
        active: Boolean = true,
        startDate: Date? = null,
        endDate: Date? = null,
        specificFoodIds: List<String>? = null,
        couponCode: String? = null,
        isCustomerSpecific: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val discount = Discount(
                    id = id,
                    name = name,
                    description = description,
                    categoryId = categoryId,
                    minQuantity = minQuantity,
                    percentOff = percentOff,
                    active = active,
                    startDate = startDate,
                    endDate = endDate,
                    specificFoodIds = specificFoodIds,
                    updatedAt = Date(),
                    couponCode = couponCode,
                    isCustomerSpecific = isCustomerSpecific
                )
                
                val result = repository.updateDiscount(discount)
                
                if (result.isSuccess) {
                    loadDiscounts()
                } else {
                    _error.value = "Failed to update discount: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error updating discount: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun deleteDiscount(discountId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = repository.deleteDiscount(discountId)
                if (result.isSuccess) {
                    _discounts.value = _discounts.value.filter { it.id != discountId }
                } else {
                    _error.value = "Failed to delete discount: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete discount: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleDiscountStatus(discount: Discount) {
        val updatedDiscount = discount.copy(active = !discount.active, updatedAt = Date())
        updateDiscount(updatedDiscount)
    }
    
    fun updateDiscount(discount: Discount) {
        updateDiscount(
            id = discount.id,
            name = discount.name,
            description = discount.description,
            categoryId = discount.categoryId,
            minQuantity = discount.minQuantity,
            percentOff = discount.percentOff,
            active = discount.active,
            startDate = discount.startDate,
            endDate = discount.endDate,
            specificFoodIds = discount.specificFoodIds,
            couponCode = discount.couponCode,
            isCustomerSpecific = discount.isCustomerSpecific
        )
    }
    
    fun getCategoryName(categoryId: String): String {
        return _categories.value.find { it.id == categoryId }?.name ?: "Unknown Category"
    }
    
    fun clearError() {
        _error.value = null
    }
} 