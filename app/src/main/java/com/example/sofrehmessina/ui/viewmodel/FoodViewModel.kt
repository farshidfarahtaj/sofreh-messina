package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Discount
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import java.util.Date
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import com.example.sofrehmessina.utils.MemoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import com.example.sofrehmessina.util.FirebaseStorageManager
import java.util.UUID

/**
 * Extension function to safely launch a coroutine and handle cancellation exceptions properly
 */
private fun CoroutineScope.safeLaunch(
    logTag: String = "FoodViewModel",
    block: suspend CoroutineScope.() -> Unit
) = launch {
    try {
        block()
    } catch (e: Exception) {
        when (e) {
            is CancellationException -> {
                // Just let it propagate
                Log.d(logTag, "Operation was cancelled normally")
                throw e
            }
            else -> {
                // Log other exceptions
                Log.e(logTag, "Error in coroutine: ${e.message}", e)
            }
        }
    }
}

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val storageManager: FirebaseStorageManager
) : ViewModel() {
    private val _foodItems = MutableStateFlow<List<Food>>(emptyList())
    val foodItems: StateFlow<List<Food>> = _foodItems

    private val _selectedFood = MutableStateFlow<Food?>(null)
    val selectedFood: StateFlow<Food?> = _selectedFood

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error
    
    // Store active discounts for the current category
    private val _activeDiscounts = MutableStateFlow<List<Discount>>(emptyList())
    val activeDiscounts: StateFlow<List<Discount>> = _activeDiscounts
    
    // Store the original food items before discount application
    private val _originalFoodItems = MutableStateFlow<List<Food>>(emptyList())
    
    private val _cartItems = MutableStateFlow<Map<String, Int>>(emptyMap())
    
    // Image upload status
    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading
    
    private val _imageUploadError = MutableStateFlow<String?>(null)
    val imageUploadError: StateFlow<String?> = _imageUploadError
    
    init {
        // Preload all discounts when the ViewModel is created
        viewModelScope.safeLaunch("FoodViewModel") {
            try {
                val allDiscountsResult = repository.getAllActiveDiscounts()
                if (allDiscountsResult.isSuccess) {
                    val allDiscounts = allDiscountsResult.getOrDefault(emptyList())
                    Log.d("FoodViewModel", "Preloaded ${allDiscounts.size} active discounts on init")
                    
                    // Store all discounts for future reference
                    _activeDiscounts.value = allDiscounts
                }
            } catch (e: Exception) {
                // Handle cancellation exceptions gracefully
                if (e is CancellationException) {
                    Log.d("FoodViewModel", "Discount preloading cancelled due to navigation or lifecycle event")
                    throw e
                } else {
                    Log.e("FoodViewModel", "Error preloading discounts: ${e.message}", e)
                }
            }
        }
        
        // Observe the shared cart quantities with improved error handling and performance
        viewModelScope.safeLaunch("FoodViewModel") {
            try {
                CartViewModel.cartQuantitiesFlow
                    .collect { cartQuantities ->
                        try {
                            val startTime = System.currentTimeMillis()
                            
                            // Skip processing if there's no change in the quantities
                            if (_cartItems.value == cartQuantities) {
                                return@collect
                            }
                            
                            Log.d("FoodViewModel", "Received updated cart quantities: ${cartQuantities.size} items")
                            _cartItems.value = cartQuantities
                            
                            // Only re-apply discounts if we're displaying a category and have items
                            val categoryId = _selectedCategory.value?.id
                            if (!categoryId.isNullOrEmpty() && _originalFoodItems.value.isNotEmpty()) {
                                withContext(Dispatchers.Default) {
                                    try {
                                        val discountsStartTime = System.currentTimeMillis()
                                        
                                        // Apply discounts to the original items with the updated cart quantities
                                        val discountedItems = applyDiscountsToFoodItems(_originalFoodItems.value, _activeDiscounts.value)
                                        
                                        withContext(Dispatchers.Main) {
                                            _foodItems.value = discountedItems
                                        }
                                        
                                        val discountProcessingTime = System.currentTimeMillis() - discountsStartTime
                                        Log.d("FoodViewModel", "Re-applied discounts in ${discountProcessingTime}ms after cart quantities changed")
                                    } catch (e: Exception) {
                                        if (e is CancellationException) {
                                            Log.d("FoodViewModel", "Discount application cancelled due to navigation or lifecycle event")
                                            throw e
                                        } else {
                                            Log.e("FoodViewModel", "Error applying discounts after cart update: ${e.message}", e)
                                        }
                                    }
                                }
                            }
                            
                            val totalProcessingTime = System.currentTimeMillis() - startTime
                            if (totalProcessingTime > 500) {
                                Log.w("FoodViewModel", "Cart quantity processing took ${totalProcessingTime}ms, which may cause UI lag")
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                Log.d("FoodViewModel", "Cart quantity processing cancelled")
                                throw e
                            } else {
                                Log.e("FoodViewModel", "Error processing cart quantity update: ${e.message}", e)
                            }
                        }
                    }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("FoodViewModel", "Cart quantities flow collection cancelled")
                    throw e
                } else {
                    Log.e("FoodViewModel", "Error collecting cart quantities: ${e.message}", e)
                }
            }
        }
    }

    fun loadFoodItems(categoryId: String? = null) {
        viewModelScope.safeLaunch {
            _isLoading.value = true
            
            Log.d("FoodViewModel", "Loading food items for category: ${categoryId ?: "all"}")
            
            // First, fetch all REGULAR active discounts (excluding coupon discounts)
            val allDiscountsResult = repository.getAllRegularActiveDiscounts()
            if (allDiscountsResult.isSuccess) {
                val allDiscounts = allDiscountsResult.getOrDefault(emptyList())
                
                // Filter for discounts that apply to this category (if specified) or are global
                val relevantDiscounts = if (categoryId != null) {
                    allDiscounts.filter { discount ->
                        discount.categoryId.isEmpty() || discount.categoryId == categoryId ||
                        (!discount.specificFoodIds.isNullOrEmpty() && discount.categoryId == categoryId)
                    }
                } else {
                    allDiscounts
                }
                
                _activeDiscounts.value = relevantDiscounts
                Log.d("FoodViewModel", "Found ${relevantDiscounts.size} relevant regular discounts (no coupon discounts)")
                
                // Use collectLatest to ensure we always have the most recent data
                repository.getFoodItems(categoryId)
                    .catch { e ->
                        _isLoading.value = false
                        if (e is CancellationException) {
                            throw e
                        } else {
                            _error.value = e
                            Log.e("FoodViewModel", "Error loading food items: ${e.message}", e)
                        }
                    }
                    .collectLatest { foodItems ->
                        _isLoading.value = false
                        
                        if (foodItems.isEmpty()) {
                            Log.d("FoodViewModel", "No food items found for category: ${categoryId ?: "all"}")
                            _foodItems.value = emptyList()
                            _originalFoodItems.value = emptyList()
                            return@collectLatest
                        }
                        
                        // Store the original items without discounts
                        _originalFoodItems.value = foodItems
                        
                        // Apply discounts to the items
                        val discountedItems = applyDiscountsToFoodItems(foodItems, relevantDiscounts)
                        _foodItems.value = discountedItems
                        
                        Log.d("FoodViewModel", "Loaded ${discountedItems.size} food items for category: ${categoryId ?: "all"}")
                    }
            } else {
                // If we failed to get discounts, still show the food items without discounts
                repository.getFoodItems(categoryId)
                    .catch { e ->
                        _isLoading.value = false
                        if (e is CancellationException) {
                            throw e
                        } else {
                            _error.value = e
                            Log.e("FoodViewModel", "Error loading food items: ${e.message}", e)
                        }
                    }
                    .collectLatest { foodItems ->
                        _isLoading.value = false
                        _foodItems.value = foodItems
                        _originalFoodItems.value = foodItems
                        Log.d("FoodViewModel", "Loaded ${foodItems.size} food items (without discounts)")
                    }
            }
        }
    }

    fun loadFoodDetails(foodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("FoodViewModel", "Loading food details for item: $foodId")
                
                // First, fetch all REGULAR active discounts (excluding coupon discounts)
                val allDiscountsResult = try {
                    repository.getAllRegularActiveDiscounts()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Properly handle cancellation by letting it propagate up
                    Log.d("FoodViewModel", "Discount loading was cancelled during navigation")
                    _isLoading.value = false
                    throw e
                } catch (e: Exception) {
                    Log.e("FoodViewModel", "Error fetching discounts: ${e.message}", e)
                    Result.failure(e)
                }
                
                val activeDiscounts = if (allDiscountsResult.isSuccess) {
                    allDiscountsResult.getOrDefault(emptyList())
                } else {
                    Log.e("FoodViewModel", "Failed to load discounts: ${allDiscountsResult.exceptionOrNull()?.message}")
                    emptyList()
                }
                
                // Then fetch the food item details
                repository.getFoodItem(foodId)
                    .onSuccess { food ->
                        Log.d("FoodViewModel", "======= FOOD ITEM LOADED =======")
                        Log.d("FoodViewModel", "Successfully loaded food item: ${food.name} (ID: ${food.id})")
                        Log.d("FoodViewModel", "Food availability status: ${food.foodAvailable}")
                        Log.d("FoodViewModel", "Raw food object: $food")
                        Log.d("FoodViewModel", "=================================")
                        
                        try {
                            // Get current cart quantity for this item
                            val currentCartQuantity = _cartItems.value[food.id] ?: 0
                            
                            // First, check for item-specific discounts
                            val itemSpecificDiscounts = activeDiscounts.filter { discount ->
                                discount.active && 
                                !discount.specificFoodIds.isNullOrEmpty() && 
                                discount.specificFoodIds.contains(food.id) &&
                                isDiscountTimeValid(discount)
                            }
                            
                            // Then check for category discounts
                            val categoryDiscounts = activeDiscounts.filter { discount ->
                                discount.active && 
                                discount.specificFoodIds.isNullOrEmpty() &&
                                (discount.categoryId.isEmpty() || discount.categoryId == food.categoryId) &&
                                isDiscountTimeValid(discount)
                            }
                            
                            // Split by regular vs tiered discounts
                            val regularDiscounts = (itemSpecificDiscounts + categoryDiscounts)
                                .filter { it.minQuantity == 0 }
                            
                            val tieredDiscounts = (itemSpecificDiscounts + categoryDiscounts)
                                .filter { it.minQuantity > 0 }
                            
                            // Applicable tiered discounts (based on current cart quantity)
                            val applicableTieredDiscounts = tieredDiscounts.filter { 
                                currentCartQuantity >= it.minQuantity 
                            }
                            
                            Log.d("FoodViewModel", "Found ${regularDiscounts.size} regular discounts and " +
                                "${tieredDiscounts.size} tiered discounts (${applicableTieredDiscounts.size} applicable)")
                            
                            // CASE 1: We have applicable regular discounts
                            if (regularDiscounts.isNotEmpty()) {
                                // Find best regular discount
                                val bestRegularDiscount = regularDiscounts.maxByOrNull { it.percentOff }
                                
                                // CASE 1A: We also have applicable tiered discounts - choose the best one
                                if (applicableTieredDiscounts.isNotEmpty()) {
                                    // Find best tiered discount
                                    val bestTieredDiscount = applicableTieredDiscounts.maxByOrNull { it.percentOff }
                                    
                                    // Apply the better of the two
                                    if (bestTieredDiscount != null && bestRegularDiscount != null) {
                                        if (bestTieredDiscount.percentOff > bestRegularDiscount.percentOff) {
                                            // Tiered is better
                                            val isItemSpecific = !bestTieredDiscount.specificFoodIds.isNullOrEmpty()
                                            _selectedFood.value = applyDiscountToFood(food, bestTieredDiscount, true, isItemSpecific)
                                        } else {
                                            // Regular is better
                                            val isItemSpecific = !bestRegularDiscount.specificFoodIds.isNullOrEmpty()
                                            _selectedFood.value = applyDiscountToFood(food, bestRegularDiscount, false, isItemSpecific)
                                        }
                                    } else if (bestTieredDiscount != null) {
                                        // Only tiered available
                                        val isItemSpecific = !bestTieredDiscount.specificFoodIds.isNullOrEmpty()
                                        _selectedFood.value = applyDiscountToFood(food, bestTieredDiscount, true, isItemSpecific)
                                    } else if (bestRegularDiscount != null) {
                                        // Only regular available
                                        val isItemSpecific = !bestRegularDiscount.specificFoodIds.isNullOrEmpty()
                                        _selectedFood.value = applyDiscountToFood(food, bestRegularDiscount, false, isItemSpecific)
                                    }
                                }
                                // CASE 1B: Just regular discounts - apply the best one
                                else if (bestRegularDiscount != null) {
                                    val isItemSpecific = !bestRegularDiscount.specificFoodIds.isNullOrEmpty() 
                                    _selectedFood.value = applyDiscountToFood(food, bestRegularDiscount, false, isItemSpecific)
                                }
                            }
                            // CASE 2: We only have tiered discounts
                            else if (tieredDiscounts.isNotEmpty()) {
                                // CASE 2A: We have applicable tiered discounts based on current cart quantity
                                if (applicableTieredDiscounts.isNotEmpty()) {
                                    // Find best applicable tiered discount
                                    val bestTieredDiscount = applicableTieredDiscounts.maxByOrNull { it.percentOff }
                                    
                                    if (bestTieredDiscount != null) {
                                        val isItemSpecific = !bestTieredDiscount.specificFoodIds.isNullOrEmpty()
                                        _selectedFood.value = applyDiscountToFood(food, bestTieredDiscount, true, isItemSpecific)
                                    }
                                }
                                // CASE 2B: We have tiered discounts but none are applicable yet
                                // Show the best potential discount as informational
                                else {
                                    // Find the best potential tiered discount
                                    val bestPotentialDiscount = tieredDiscounts.maxByOrNull { it.percentOff }
                                    
                                    if (bestPotentialDiscount != null) {
                                        val isItemSpecific = !bestPotentialDiscount.specificFoodIds.isNullOrEmpty()
                                        // Apply as informational only - the applyDiscountToFood method will handle this
                                        _selectedFood.value = applyDiscountToFood(food, bestPotentialDiscount, true, isItemSpecific)
                                    }
                                }
                            }
                            // CASE 3: No applicable discounts, but we have potential tiered discounts that could apply
                            else if (tieredDiscounts.isNotEmpty()) {
                                // Show informational message about best potential tiered discount
                                val bestPotentialDiscount = tieredDiscounts.maxByOrNull { it.percentOff }
                                if (bestPotentialDiscount != null) {
                                    Log.d("FoodViewModel", "Found potential tiered discount for ${food.name} requiring ${bestPotentialDiscount.minQuantity} items")
                                    
                                    // Create a copy of the food with just the informational message, not the actual discount
                                    val isItemSpecific = !bestPotentialDiscount.specificFoodIds.isNullOrEmpty()
                                    val informationalMessage = createTieredDiscountMessage(bestPotentialDiscount, isItemSpecific, isInformationalOnly = true)
                                    
                                    // Calculate what the discounted price would be
                                    val potentialDiscountedPrice = food.price * (1 - (bestPotentialDiscount.percentOff / 100.0))
                                    
                                    // Don't actually apply discount price, just show the message what would happen
                                    _selectedFood.value = food.copy(
                                        discountMessage = informationalMessage,
                                        discountPercentage = bestPotentialDiscount.percentOff,
                                        // Include potential discounted price for UI display purposes only
                                        // This won't affect cart price calculations since we handle that separately
                                        discountedPrice = potentialDiscountedPrice,
                                        foodAvailable = food.foodAvailable  // Explicitly preserve availability
                                    )
                                } else {
                                    Log.d("FoodViewModel", "No potential discounts to apply. Food foodAvailable: ${food.foodAvailable}")
                                    _selectedFood.value = food
                                }
                            }
                            // CASE 4: No discounts at all
                            else {
                                Log.d("FoodViewModel", "No discounts at all for food item. Availability status: ${food.foodAvailable}")
                                _selectedFood.value = food
                            }
                        } catch (e: Exception) {
                            Log.e("FoodViewModel", "Error processing food discounts: ${e.message}", e)
                        } finally {
                            _isLoading.value = false
                        }
                    }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) {
                            Log.d("FoodViewModel", "Food loading cancelled due to navigation")
                            throw e
                        } else {
                            Log.e("FoodViewModel", "Error loading food item: ${e.message}", e)
                            _error.value = e
                            _isLoading.value = false
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Just rethrow cancellation exceptions
                Log.d("FoodViewModel", "Food details loading cancelled")
                _isLoading.value = false
                throw e
            } catch (e: Exception) {
                Log.e("FoodViewModel", "Exception loading food details: ${e.message}", e)
                _error.value = e
                _isLoading.value = false
            }
        }
    }

    fun loadFoodWithCategory(categoryId: String) {
        viewModelScope.launch {
            // Reset current state
            _isLoading.value = true
            _activeDiscounts.value = emptyList() // Clear previous discounts
            _originalFoodItems.value = emptyList() // Clear previous items
            _foodItems.value = emptyList() // Clear displayed items
            
            Log.d("FoodViewModel", "Loading category $categoryId with fresh state")
            
            try {
                // First, fetch all REGULAR active discounts (excluding coupon discounts)
                val allDiscountsResult = repository.getAllRegularActiveDiscounts()
                if (allDiscountsResult.isSuccess) {
                    val allDiscounts = allDiscountsResult.getOrDefault(emptyList())
                    
                    // Filter for discounts that apply to this category or are global discounts
                    // Also include discounts with specific food IDs in this category
                    val relevantDiscounts = allDiscounts.filter { discount ->
                        discount.categoryId.isEmpty() || 
                        discount.categoryId == categoryId ||
                        (!discount.specificFoodIds.isNullOrEmpty() && discount.categoryId == categoryId)
                    }
                    
                    _activeDiscounts.value = relevantDiscounts
                    
                    if (relevantDiscounts.isNotEmpty()) {
                        Log.d("FoodViewModel", "Found ${relevantDiscounts.size} regular discounts relevant to category $categoryId (no coupon discounts)")
                        relevantDiscounts.forEach { discount ->
                            val scope = if (discount.specificFoodIds.isNullOrEmpty()) 
                                          "all items" 
                                       else 
                                          "${discount.specificFoodIds.size} specific items"
                            Log.d("FoodViewModel", "Discount: ${discount.name}, scope: $scope, percentOff: ${discount.percentOff}%")
                        }
                    } else {
                        Log.d("FoodViewModel", "No regular discounts found for category $categoryId")
                    }
                } else {
                    Log.e("FoodViewModel", "Failed to load discounts: ${allDiscountsResult.exceptionOrNull()?.message}")
                }
                
                // Load category
                repository.getCategory(categoryId)
                    .onSuccess { category ->
                        _selectedCategory.value = category
                    }
                    .onFailure { e ->
                        Log.e("FoodViewModel", "Failed to load category: ${e.message}")
                    }
                
                // Then fetch food items
                repository.getFoodItems(categoryId)
                    .catch { e -> 
                        if (e.message?.contains("was cancelled") == true || 
                            e.message?.contains("Flow was aborted") == true) {
                            Log.d("FoodViewModel", "Food items flow collection cancelled for category $categoryId")
                        } else {
                            _error.value = e
                            Log.e("FoodViewModel", "Error loading category food items: ${e.message}", e)
                        }
                        _isLoading.value = false
                    }
                    .collect { items ->
                        _originalFoodItems.value = items
                        
                        // Apply discounts - always apply discounts to ensure they're visible immediately
                        val discountedItems = applyDiscountsToFoodItems(items, _activeDiscounts.value)
                        
                        // Log discount application results
                        var discountedCount = 0
                        var informationalCount = 0
                        discountedItems.forEach { food ->
                            if (food.discountedPrice != null) {
                                discountedCount++
                                Log.d("FoodViewModel", "Applied discount to ${food.name}: " +
                                    "original price: ${food.price}, " +
                                    "discounted price: ${food.discountedPrice}, " +
                                    "discount: ${food.discountPercentage}%, " +
                                    "message: ${food.discountMessage}")
                            } else if (food.discountMessage != null) {
                                informationalCount++
                                Log.d("FoodViewModel", "Added informational discount to ${food.name}: " +
                                    "message: ${food.discountMessage}")
                            }
                        }
                        
                        _foodItems.value = discountedItems
                        Log.d("FoodViewModel", "Applied discounts to $discountedCount items and added " +
                            "informational messages to $informationalCount items out of ${items.size} " +
                            "food items in category $categoryId")
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                if (e.message?.contains("was cancelled") == true || 
                    e.message?.contains("Flow was aborted") == true) {
                    Log.d("FoodViewModel", "Loading category $categoryId was cancelled")
                } else {
                    _error.value = e
                    Log.e("FoodViewModel", "Error loading category food items: ${e.message}", e)
                }
                _isLoading.value = false
            }
        }
    }

    fun loadCategory(categoryId: String) {
        viewModelScope.safeLaunch("FoodViewModel") {
            _isLoading.value = true
            try {
                repository.getCategory(categoryId)
                    .onSuccess { category ->
                        _selectedCategory.value = category
                        Log.d("FoodViewModel", "Successfully loaded category: ${category.name}")
                    }
                    .onFailure { e ->
                        _error.value = e
                        Log.e("FoodViewModel", "Failed to load category: ${e.message}", e)
                    }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("FoodViewModel", "Category loading cancelled due to navigation")
                    throw e
                } else {
                    _error.value = e
                    Log.e("FoodViewModel", "Error loading category: ${e.message}", e)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addFoodItem(food: Food) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.addFoodItem(food)
                    .onSuccess {
                        // No need to explicitly reload - the real-time listener will handle it
                        Log.d("FoodViewModel", "Food item added successfully: ${food.name}")
                    }
                    .onFailure { e ->
                        Log.e("FoodViewModel", "Error adding food item: ${e.message}", e)
                        _error.value = e
                    }
            } catch (e: Exception) {
                Log.e("FoodViewModel", "Exception adding food item: ${e.message}", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFoodItem(food: Food) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateFoodItem(food)
                    .onSuccess {
                        // No need to explicitly reload - the real-time listener will handle it
                        Log.d("FoodViewModel", "Food item updated successfully: ${food.name}")
                    }
                    .onFailure { e ->
                        Log.e("FoodViewModel", "Error updating food item: ${e.message}", e)
                        _error.value = e
                    }
            } catch (e: Exception) {
                Log.e("FoodViewModel", "Exception updating food item: ${e.message}", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFoodItem(foodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteFoodItem(foodId)
                    .onSuccess {
                        // No need to explicitly reload - the real-time listener will handle it
                        Log.d("FoodViewModel", "Food item deleted successfully: $foodId")
                    }
                    .onFailure { e ->
                        Log.e("FoodViewModel", "Error deleting food item: ${e.message}", e)
                        _error.value = e
                    }
            } catch (e: Exception) {
                Log.e("FoodViewModel", "Exception deleting food item: ${e.message}", e)
                _error.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllFoodItems() {
        loadFoodItems()
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Apply discounts to food items
     * @param foodItems The original food items
     * @param discounts The available discounts for the category
     * @return The food items with discounted prices
     */
    private fun applyDiscountsToFoodItems(foodItems: List<Food>, discounts: List<Discount>): List<Food> {
        // Skip processing if there are no discounts or no food items
        if (discounts.isEmpty() || foodItems.isEmpty()) {
            Log.d("FoodViewModel", "No discounts or food items to process")
            return foodItems
        }
        
        // Get the current cart quantities for tiered discount calculations
        val cartQuantities = _cartItems.value
        
        // First, separate item-specific discounts from category-wide discounts
        val itemSpecificDiscounts = discounts.filter { !it.specificFoodIds.isNullOrEmpty() }
        val categoryWideDiscounts = discounts.filter { it.specificFoodIds.isNullOrEmpty() }
        
        Log.d("FoodViewModel", "Processing ${itemSpecificDiscounts.size} item-specific discounts and ${categoryWideDiscounts.size} category-wide discounts")
        
        // Process each food item
        return foodItems.map { food ->
            try {
                // Skip processing for unavailable items
                if (!food.foodAvailable) {
                    Log.d("FoodViewModel", "Food availability status: ${food.foodAvailable}")
                    return@map food
                }
                
                // First, check if there are any item-specific discounts for this food
                val specificDiscounts = itemSpecificDiscounts.filter { 
                    it.specificFoodIds?.contains(food.id) == true 
                }
                
                // If we have item-specific discounts, use only those
                if (specificDiscounts.isNotEmpty()) {
                    Log.d("FoodViewModel", "Found ${specificDiscounts.size} item-specific discounts for food ID: ${food.id}")
                    
                    // Find the best item-specific discount
                    var bestDiscount: Discount? = null
                    var bestDiscountedPrice = food.price
                    var bestDiscountPercentage = 0.0
                    
                    for (discount in specificDiscounts) {
                        // Calculate the discounted price based on the discount type
                        val (discountedPrice, percentOff) = calculateDiscountedPrice(
                            food = food,
                            discount = discount,
                            cartQuantity = cartQuantities[food.id] ?: 0
                        )
                        
                        // If this discount gives a better price, use it
                        if (discountedPrice < bestDiscountedPrice) {
                            bestDiscountedPrice = discountedPrice
                            bestDiscount = discount
                            bestDiscountPercentage = percentOff
                        }
                    }
                    
                    // Apply the best item-specific discount if found
                    if (bestDiscount != null && bestDiscountedPrice < food.price) {
                        // Create a copy of the food item with the discount applied
                        val discountedFood = food.copy(
                            discountedPrice = bestDiscountedPrice,
                            discountPercentage = bestDiscountPercentage,
                            discountEndDate = bestDiscount.endDate,
                            discountMessage = bestDiscount.name,
                            foodAvailable = food.foodAvailable  // Explicitly preserve availability
                        )
                        
                        Log.d("FoodViewModel", "Applied item-specific discount to food ID: ${food.id}, discount: ${bestDiscount.id}, percentage: $bestDiscountPercentage%")
                        return@map discountedFood
                    }
                    
                    // If no applicable discount yet but there are tiered discounts, show them as potential
                    val tieredDiscounts = specificDiscounts.filter { it.minQuantity > 0 }
                    if (tieredDiscounts.isNotEmpty()) {
                        // Find the best potential tiered discount
                        val bestPotentialDiscount = tieredDiscounts.maxByOrNull { it.percentOff }
                        
                        if (bestPotentialDiscount != null) {
                            // Calculate what the price would be if the discount were applied
                            val potentialDiscountedPrice = food.price * (1 - (bestPotentialDiscount.percentOff / 100.0))
                            
                            // Create an informational message
                            val informationalMessage = "💡 Add ${bestPotentialDiscount.minQuantity} to cart for ${bestPotentialDiscount.percentOff.toInt()}% off!"
                            
                            // Return food with potential discount info
                            return@map food.copy(
                                discountPercentage = bestPotentialDiscount.percentOff,
                                discountMessage = informationalMessage,
                                discountedPrice = potentialDiscountedPrice,  // Show potential price
                                foodAvailable = food.foodAvailable
                            )
                        }
                    }
                }
                
                // If no item-specific discounts, check for category-wide discounts
                val applicableCategoryDiscounts = categoryWideDiscounts.filter { discount ->
                    // Check if the discount applies to this food item's category
                    discount.categoryId == food.categoryId || discount.categoryId.isEmpty()
                }
                
                if (applicableCategoryDiscounts.isNotEmpty()) {
                    // Find the best category discount
                    var bestDiscount: Discount? = null
                    var bestDiscountedPrice = food.price
                    var bestDiscountPercentage = 0.0
                    
                    for (discount in applicableCategoryDiscounts) {
                        // Calculate the discounted price based on the discount type
                        val (discountedPrice, percentOff) = calculateDiscountedPrice(
                            food = food,
                            discount = discount,
                            cartQuantity = cartQuantities[food.id] ?: 0
                        )
                        
                        // If this discount gives a better price, use it
                        if (discountedPrice < bestDiscountedPrice) {
                            bestDiscountedPrice = discountedPrice
                            bestDiscount = discount
                            bestDiscountPercentage = percentOff
                        }
                    }
                    
                    // Apply the best category discount if found
                    if (bestDiscount != null && bestDiscountedPrice < food.price) {
                        // Create a copy of the food item with the discount applied
                        val discountedFood = food.copy(
                            discountedPrice = bestDiscountedPrice,
                            discountPercentage = bestDiscountPercentage,
                            discountEndDate = bestDiscount.endDate,
                            discountMessage = bestDiscount.name,
                            foodAvailable = food.foodAvailable  // Explicitly preserve availability
                        )
                        
                        Log.d("FoodViewModel", "Applied category-wide discount to food ID: ${food.id}, discount: ${bestDiscount.id}, percentage: $bestDiscountPercentage%")
                        return@map discountedFood
                    }
                    
                    // If no applicable discount yet but there are tiered discounts, show them as potential
                    val tieredDiscounts = applicableCategoryDiscounts.filter { it.minQuantity > 0 }
                    if (tieredDiscounts.isNotEmpty()) {
                        // Find the best potential tiered discount
                        val bestPotentialDiscount = tieredDiscounts.maxByOrNull { it.percentOff }
                        
                        if (bestPotentialDiscount != null) {
                            // Calculate what the price would be if the discount were applied
                            val potentialDiscountedPrice = food.price * (1 - (bestPotentialDiscount.percentOff / 100.0))
                            
                            // Create an informational message
                            val informationalMessage = "💡 Add ${bestPotentialDiscount.minQuantity} to cart for ${bestPotentialDiscount.percentOff.toInt()}% off!"
                            
                            // Return food with potential discount info
                            return@map food.copy(
                                discountPercentage = bestPotentialDiscount.percentOff,
                                discountMessage = informationalMessage,
                                discountedPrice = potentialDiscountedPrice,  // Show potential price
                                foodAvailable = food.foodAvailable
                            )
                        }
                    }
                }
                
                // No applicable discounts for this item
                Log.d("FoodViewModel", "No applicable discounts for food ID: ${food.id}")
                return@map food
            } catch (e: Exception) {
                Log.e("FoodViewModel", "Error applying discounts to food item: ${e.message}", e)
                return@map food
            }
        }
    }

    /**
     * Helper method to create a discount message based on discount type
     */
    private fun createTieredDiscountMessage(
        discount: Discount,
        isItemSpecific: Boolean,
        isInformationalOnly: Boolean = false
    ): String {
        val baseMessage = when {
            discount.minQuantity > 0 && isItemSpecific -> {
                "Special item offer: Buy ${discount.minQuantity}+ for ${discount.percentOff.toInt()}% off"
            }
            discount.minQuantity > 0 -> {
                "Buy ${discount.minQuantity}+ for ${discount.percentOff.toInt()}% off"
            }
            isItemSpecific -> {
                "Special item offer: ${discount.percentOff.toInt()}% off"
            }
            discount.endDate != null -> {
                "Limited time offer: ${discount.percentOff.toInt()}% off"
            }
            else -> {
                "${discount.percentOff.toInt()}% off"
            }
        }
        
        // Add appropriate icon based on whether this is informational or applied
        return if (isInformationalOnly) {
            "💡 $baseMessage" // Use a different icon for informational-only messages
        } else if (discount.minQuantity > 0) {
            // For applied tiered discounts
            "🔥 $baseMessage"
        } else {
            // For regular applied discounts
            "✨ $baseMessage"
        }
    }

    /**
     * Helper method to check if a discount is currently valid based on time constraints
     */
    private fun isDiscountTimeValid(discount: Discount): Boolean {
        val now = Date()
        return (discount.startDate == null || discount.startDate <= now) &&
               (discount.endDate == null || discount.endDate >= now)
    }
    
    /**
     * Helper method to apply discount to a food item
     */
    private fun applyDiscountToFood(
        food: Food, 
        discount: Discount, 
        isTiered: Boolean,
        isItemSpecific: Boolean = false
    ): Food {
        val discountedPrice = food.price * (1 - (discount.percentOff / 100.0))
        
        // For tiered discounts, check if the current cart quantity meets the requirement
        val currentCartQuantity = CartViewModel.cartQuantitiesFlow.value[food.id] ?: 0
        val meetsQuantityRequirement = !isTiered || currentCartQuantity >= discount.minQuantity
        
        // Use the helper method to create the discount message
        val isInformationalOnly = isTiered && !meetsQuantityRequirement
        val enhancedMessage = createTieredDiscountMessage(discount, isItemSpecific, isInformationalOnly)
        
        Log.d("FoodViewModel", "Applied ${if (isItemSpecific) "item-specific " else ""}${if (isTiered) "tiered" else "regular"} discount to ${food.name}: " +
            "original: ${food.price}, discounted: $discountedPrice, message: $enhancedMessage, " +
            "cart quantity: $currentCartQuantity, meets requirement: $meetsQuantityRequirement")
        
        // Only track analytics if the discount is actually applied (not just informational)
        if (!isInformationalOnly) {
            trackDiscountApplication(food, discount)
        }
            
        return food.copy(
            // Only set the discounted price if the quantity requirement is met
            discountedPrice = if (meetsQuantityRequirement) discountedPrice else null,
            discountPercentage = discount.percentOff,
            discountEndDate = discount.endDate,
            discountMessage = enhancedMessage,
            foodAvailable = food.foodAvailable  // Explicitly preserve the availability status
        )
    }

    // Helper method to track discount application for analytics
    private fun trackDiscountApplication(
        food: Food,
        discount: Discount
    ) {
        viewModelScope.launch {
            try {
                // Log the event
                Log.d("DiscountAnalytics", "Tracked discount application: ${discount.name} (${discount.percentOff}%) applied to ${food.name}")
                
                // Attempt to save the analytics event to Firebase
                try {
                    repository.trackDiscountUsage(discount.id, food.id, food.categoryId)
                        .onFailure { e ->
                            Log.e("DiscountAnalytics", "Failed to save discount analytics data: ${e.message}", e)
                        }
                } catch (e: Exception) {
                    Log.e("DiscountAnalytics", "Error saving discount analytics: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.e("DiscountAnalytics", "Error tracking discount application: ${e.message}", e)
            }
        }
    }
    
    // Call this method when the language changes to refresh the food data
    fun refreshFoodForLanguageChange() {
        viewModelScope.safeLaunch("FoodViewModel") {
            try {
                // If we have a selected food item, reload it
                _selectedFood.value?.let { currentFood ->
                    loadFoodDetails(currentFood.id)
                }
                
                // If we have a selected category, reload its food items
                _selectedCategory.value?.let { currentCategory ->
                    loadFoodWithCategory(currentCategory.id)
                }
                // Otherwise, reload all food items
                ?: run {
                    loadFoodItems()
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("FoodViewModel", "Food refresh cancelled due to navigation")
                    throw e
                } else {
                    Log.e("FoodViewModel", "Error refreshing food data: ${e.message}", e)
                    _error.value = e
                }
            }
        }
    }

    // Call this method when the language changes to refresh the food items data
    fun refreshFoodItemsForLanguageChange() {
        viewModelScope.safeLaunch("FoodViewModel") {
            try {
                // First, refresh the food items in the repository
                repository.refreshFoodItems()
                
                // Then reload the food items to update the UI
                val categoryId = _selectedCategory.value?.id
                repository.getFoodItems(categoryId).collect { foodItems ->
                    // Store the original items without discounts
                    _originalFoodItems.value = foodItems
                    
                    // Apply discounts to the items
                    val discountedItems = applyDiscountsToFoodItems(foodItems, _activeDiscounts.value)
                    _foodItems.value = discountedItems
                    
                    // If we have a selected food item, refresh it too
                    _selectedFood.value?.let { currentFood ->
                        val updatedFood = foodItems.find { it.id == currentFood.id }
                        if (updatedFood != null) {
                            _selectedFood.value = updatedFood
                        }
                    }
                    
                    Log.d("FoodViewModel", "Food items refreshed for language change: ${foodItems.size} items")
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d("FoodViewModel", "Food items refresh cancelled")
                    throw e
                } else {
                    Log.e("FoodViewModel", "Error refreshing food items for language change: ${e.message}", e)
                    _error.value = e
                }
            }
        }
    }

    private fun handleFirebaseException(exception: Throwable) {
        if (exception.message?.contains("FAILED_PRECONDITION") == true && 
            exception.message?.contains("requires an index") == true) {
            // Extract the URL from the error message
            val indexUrl = exception.message?.let {
                val startIndex = it.indexOf("https://")
                val endIndex = it.indexOf("\n", startIndex).takeIf { it != -1 } ?: it.length
                if (startIndex != -1) it.substring(startIndex, endIndex) else null
            }
            
            _error.value = Exception(
                "This query requires a Firestore index. Please contact the developer and provide this URL:\n$indexUrl"
            )
        } else if (exception.message?.contains("PERMISSION_DENIED") == true) {
            _error.value = Exception("You don't have permission to access this data.")
        } else {
            _error.value = exception
        }
    }

    /**
     * Toggle the availability of a food item
     * This ensures both foodAvailable and available fields are updated consistently
     */
    fun toggleFoodAvailability(foodId: String, isAvailable: Boolean) {
        viewModelScope.safeLaunch {
            _isLoading.value = true
            try {
                // First get the current food item
                val result = repository.getFoodItem(foodId)
                if (result.isSuccess) {
                    val food = result.getOrThrow()
                    Log.d("FoodViewModel", "Current availability: ${food.foodAvailable}, setting to: $isAvailable")
                    
                    // Use the withAvailability helper method to ensure both fields are updated
                    val updatedFood = food.withAvailability(isAvailable)
                    
                    // Update the food item in Firestore
                    repository.updateFoodItem(updatedFood)
                    Log.d("FoodViewModel", "Successfully toggled food availability to: $isAvailable")
                } else {
                    _error.value = result.exceptionOrNull() ?: Exception("Unknown error toggling food availability")
                }
            } catch (e: Exception) {
                _error.value = e
                Log.e("FoodViewModel", "Error toggling food availability: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calculate the discounted price for a food item based on a discount
     * @return Pair of (discounted price, discount percentage applied)
     */
    private fun calculateDiscountedPrice(
        food: Food,
        discount: Discount,
        cartQuantity: Int
    ): Pair<Double, Double> {
        // If this is a tiered discount but we don't have enough quantity, no discount
        if (discount.minQuantity > 0 && cartQuantity < discount.minQuantity) {
            return Pair(food.price, 0.0)
        }
        
        // Calculate the discount - simple percentage discount
        val percentOff = discount.percentOff
        val discountedPrice = food.price * (1 - (percentOff / 100.0))
        
        return Pair(discountedPrice, percentOff)
    }

    /**
     * Upload a food image to Firebase Storage
     * 
     * @param uri URI of the image to upload
     * @param foodId ID of the food item
     * @return URL of the uploaded image or null if upload failed
     */
    suspend fun uploadFoodImage(uri: Uri, foodId: String): String? {
        _isImageUploading.value = true
        _imageUploadError.value = null
        
        return try {
            val imageUrl = storageManager.uploadFoodImage(uri, foodId)
            
            if (imageUrl == null) {
                _imageUploadError.value = "Failed to upload image"
                null
            } else {
                // Update the food item with the new image URL
                val foodResult = repository.getFoodItem(foodId)
                if (foodResult.isSuccess) {
                    val food = foodResult.getOrThrow()
                    val updatedFood = food.copy(imageUrl = imageUrl)
                    repository.updateFoodItem(updatedFood)
                    
                    // Update the local state
                    _selectedFood.value = updatedFood
                    _foodItems.value = _foodItems.value.map { 
                        if (it.id == foodId) updatedFood else it 
                    }
                }
                
                imageUrl
            }
        } catch (e: Exception) {
            Log.e("FoodViewModel", "Error uploading food image: ${e.message}", e)
            _imageUploadError.value = e.message ?: "Unknown error occurred"
            null
        } finally {
            _isImageUploading.value = false
        }
    }
    
    /**
     * Add a food item with image upload
     * 
     * @param food Food item to add
     * @param imageUri Optional URI of the image to upload
     */
    fun addFoodWithImage(food: Food, imageUri: Uri?) {
        viewModelScope.safeLaunch {
            _isLoading.value = true
            _error.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                // Generate a new ID for the food item if it doesn't already have one
                val foodId = food.id.ifEmpty { UUID.randomUUID().toString() }
                val newFood = food.copy(id = foodId)
                
                // First add the food item without an image
                val result = repository.addFoodItem(newFood)
                
                if (result.isSuccess) {
                    // If there's an image URI, upload it
                    if (imageUri != null) {
                        val imageUrl = uploadFoodImage(imageUri, foodId)
                        if (imageUrl != null) {
                            // Update the food item with the image URL
                            val updatedFood = newFood.copy(imageUrl = imageUrl)
                            repository.updateFoodItem(updatedFood)
                            
                            // Update local state
                            _foodItems.value = _foodItems.value + updatedFood
                        } else {
                            // Still add the food, but without an image
                            _foodItems.value = _foodItems.value + newFood
                        }
                    } else {
                        // Add the food without an image
                        _foodItems.value = _foodItems.value + newFood
                    }
                    
                    // Refresh the food items
                    loadFoodWithCategory(newFood.categoryId)
                } else {
                    _error.value = result.exceptionOrNull() ?: Exception("Failed to add food item")
                }
            } catch (e: Exception) {
                _error.value = e
                Log.e("FoodViewModel", "Error adding food with image: ${e.message}", e)
            } finally {
                _isLoading.value = false
                _isImageUploading.value = false
            }
        }
    }
    
    /**
     * Update a food item with image upload
     * 
     * @param food Food item to update
     * @param imageUri Optional URI of the image to upload
     */
    fun updateFoodWithImage(food: Food, imageUri: Uri?) {
        viewModelScope.safeLaunch {
            _isLoading.value = true
            _error.value = null
            _isImageUploading.value = imageUri != null
            
            try {
                var updatedFood = food
                
                // If there's an image URI, upload it
                if (imageUri != null) {
                    val imageUrl = uploadFoodImage(imageUri, food.id)
                    if (imageUrl != null) {
                        updatedFood = food.copy(imageUrl = imageUrl)
                    }
                }
                
                // Update the food item in Firestore
                val result = repository.updateFoodItem(updatedFood)
                
                if (result.isSuccess) {
                    // Update local state
                    _foodItems.value = _foodItems.value.map { 
                        if (it.id == food.id) updatedFood else it 
                    }
                    
                    if (_selectedFood.value?.id == food.id) {
                        _selectedFood.value = updatedFood
                    }
                } else {
                    _error.value = result.exceptionOrNull() ?: Exception("Failed to update food item")
                }
            } catch (e: Exception) {
                _error.value = e
                Log.e("FoodViewModel", "Error updating food with image: ${e.message}", e)
            } finally {
                _isLoading.value = false
                _isImageUploading.value = false
            }
        }
    }
    
    /**
     * Clear image upload error
     */
    fun clearImageUploadError() {
        _imageUploadError.value = null
    }
}

sealed class FoodState {
    @Suppress("unused")
    data object Initial : FoodState()
    data object Loading : FoodState()
    @Suppress("unused")
    data object Success : FoodState()
    data class Error(val message: String) : FoodState()
} 