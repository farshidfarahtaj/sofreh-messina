package com.example.sofrehmessina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.data.model.Discount
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.util.CartPersistenceManager
import com.example.sofrehmessina.utils.safeLaunch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.security.SecureRandom
import kotlinx.coroutines.withContext
import com.example.sofrehmessina.utils.MemoryUtils
import java.util.Date
import kotlinx.coroutines.flow.combine
import com.google.firebase.auth.FirebaseAuth

/**
 * Extension function to safely launch a coroutine and handle cancellation exceptions properly
 */
private fun CoroutineScope.safeLaunch(
    logTag: String = "CartViewModel",
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
class CartViewModel @Inject constructor(
    @Suppress("unused") private val firebaseRepository: FirebaseRepository,
    private val cartPersistenceManager: CartPersistenceManager,
    private val authRepository: FirebaseAuth
) : ViewModel() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Calculate total with discounts applied
    val totalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { item -> 
            // Use discounted price if available, otherwise use regular price
            val effectivePrice = item.food.discountedPrice ?: item.food.price
            effectivePrice * item.quantity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    // Calculate original total before discounts
    val originalTotalAmount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { item -> item.food.price * item.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    // Calculate total savings from discounts
    val totalSavings: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { item -> 
            val regularPrice = item.food.price
            val discountedPrice = item.food.discountedPrice ?: item.food.price
            (regularPrice - discountedPrice) * item.quantity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cartItems.map { items ->
        items.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    // Add coupon related state variables
    private val _couponCode = MutableStateFlow<String?>(null)
    val couponCode: StateFlow<String?> = _couponCode
    
    private val _appliedCouponDiscount = MutableStateFlow<Discount?>(null)
    val appliedCouponDiscount: StateFlow<Discount?> = _appliedCouponDiscount
    
    private val _couponError = MutableStateFlow<String?>(null)
    val couponError: StateFlow<String?> = _couponError
    
    private val _isCouponValidating = MutableStateFlow(false)
    val isCouponValidating: StateFlow<Boolean> = _isCouponValidating
    
    // Add a state for checkout-only coupon savings
    private val _couponSavings = MutableStateFlow(0.0)
    val couponSavings: StateFlow<Double> = _couponSavings
    
    // Add a state for total amount with coupon (to be used only at checkout)
    private val _totalWithCoupon = MutableStateFlow(0.0)
    val totalWithCoupon: StateFlow<Double> = _totalWithCoupon

    // Calculate subtotal (sum of all item prices * quantities)
    val subtotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { item -> item.food.price * item.quantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Calculate discount (difference between regular prices and discounted prices)
    val discount: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { item -> 
            val regularPrice = item.food.price * item.quantity
            val effectivePrice = (item.food.discountedPrice ?: item.food.price) * item.quantity
            regularPrice - effectivePrice
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Calculate total (subtotal - discount)
    val total: StateFlow<Double> = combine(subtotal, discount) { sub, disc ->
        sub - disc
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    init {
        // Load saved cart items when ViewModel is created
        loadSavedCart()
        
        // Set up a listener to save cart items whenever they change
        viewModelScope.safeLaunch {
            _cartItems.collect { items ->
                // Save cart items to persistence
                cartPersistenceManager.saveCart(items)
            }
        }
        
        // Set up a listener to update totalWithCoupon whenever total or couponDiscount changes
        viewModelScope.safeLaunch {
            combine(total, _appliedCouponDiscount) { currentTotal, couponDiscount ->
                Pair(currentTotal, couponDiscount)
            }.collect { (currentTotal, couponDiscount) ->
                calculateTotalWithCoupon(currentTotal, couponDiscount)
            }
        }
    }
    
    /**
     * Loads the saved cart from persistent storage
     */
    private fun loadSavedCart() {
        viewModelScope.safeLaunch {
            try {
                val savedCart = cartPersistenceManager.loadCart()
                if (savedCart.isNotEmpty()) {
                    Log.d("CartViewModel", "Loaded ${savedCart.size} items from persistent storage")
                    _cartItems.value = savedCart
                    
                    // Update shared cart quantities
                    updateSharedCartQuantities()
                    
                    // Refresh discounts after loading cart
                    refreshDiscounts()
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error loading saved cart: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calculate the total with coupon applied (for checkout only)
     * This doesn't modify any item prices, only the final total at checkout
     */
    private fun calculateTotalWithCoupon(total: Double, couponDiscount: Discount?) {
        if (couponDiscount == null) {
            // No coupon applied, but we still need to set the totalWithCoupon
            // to the total (which already has item discounts applied)
            _couponSavings.value = 0.0
            _totalWithCoupon.value = total
            
            Log.d("CartViewModel", "No coupon applied. Setting checkout total to: $total")
            return
        }
        
        // Calculate how much the coupon saves
        val savingsAmount = total * (couponDiscount.percentOff / 100.0)
        _couponSavings.value = savingsAmount
        
        // Calculate the total with coupon applied
        _totalWithCoupon.value = total - savingsAmount
        
        Log.d("CartViewModel", "Coupon calculation (CHECKOUT ONLY): Total: $total, Discount: ${couponDiscount.percentOff}%, " +
                               "Savings: $savingsAmount, Final total: ${_totalWithCoupon.value}")
    }
    
    /**
     * Updates the shared cart quantities that can be observed by other ViewModels
     * Simplified version without memory checks
     */
    private fun updateSharedCartQuantities() {
        viewModelScope.safeLaunch("CartViewModel") {
            withContext(Dispatchers.Default) {
                try {
                    val startTime = System.currentTimeMillis()
                    
                    // Create a map of food ID to quantity
                    val cartQuantities = _cartItems.value.associate { it.food.id to it.quantity }
                    
                    // Update the shared cart quantities
                    cartQuantitiesFlow.value = cartQuantities
                    
                    val duration = System.currentTimeMillis() - startTime
                    Log.d("CartViewModel", "Updated shared cart quantities in ${duration}ms: ${cartQuantities.size} items")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("CartViewModel", "Error updating shared cart quantities: ${e.message}", e)
                }
            }
        }
    }

    fun addToCart(cartItem: CartItem) {
        Log.d("CartViewModel", "Starting to add to cart: ${cartItem.food.name}, qty: ${cartItem.quantity}")
        
        // Use safeLaunch to prevent blocking the main thread
        viewModelScope.safeLaunch("CartViewModel") {
            try {
                val startTime = System.currentTimeMillis()
                Log.d("CartViewModel", "Current cart size: ${_cartItems.value.size}")
                
                val existingItemIndex = _cartItems.value.indexOfFirst { it.food.id == cartItem.food.id }
                
                if (existingItemIndex != -1) {
                    // Update existing item quantity
                    val updatedItems = _cartItems.value.toMutableList()
                    val existingItem = updatedItems[existingItemIndex]
                    updatedItems[existingItemIndex] = existingItem.copy(
                        quantity = existingItem.quantity + cartItem.quantity,
                        notes = cartItem.notes.ifEmpty { existingItem.notes }
                    )
                    _cartItems.value = updatedItems
                    Log.d("CartViewModel", "Updated existing item, new qty: ${updatedItems[existingItemIndex].quantity}")
                } else {
                    // Add new item
                    _cartItems.value = _cartItems.value + cartItem
                    Log.d("CartViewModel", "Added new item to cart")
                }
                
                Log.d("CartViewModel", "New cart size: ${_cartItems.value.size}")
                
                // Update shared cart quantities
                updateSharedCartQuantities()
                
                // Explicitly save cart to persistence
                cartPersistenceManager.saveCart(_cartItems.value)
                
                // Performance metric
                val updateTime = System.currentTimeMillis() - startTime
                Log.d("CartViewModel", "Cart update took $updateTime ms")
                
                // Refresh discounts in a separate coroutine to avoid blocking UI
                refreshDiscounts()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("CartViewModel", "Error adding to cart: ${e.message}", e)
            }
        }
    }

    fun removeFromCart(foodId: String) {
        viewModelScope.safeLaunch {
            try {
                // Filter out the item with the given food ID
                val updatedItems = _cartItems.value.filter { it.food.id != foodId }
                _cartItems.value = updatedItems
                
                // Update shared cart quantities
                updateSharedCartQuantities()
                
                // Refresh discounts
                refreshDiscounts()
                
                Log.d("CartViewModel", "Removed item $foodId from cart")
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error removing from cart: ${e.message}", e)
            }
        }
    }

    /**
     * Force refresh the cart to ensure all discounts are properly applied
     * This should be called after any changes to the cart that might affect discounts
     */
    fun forceRefreshCart() {
        Log.d("CartViewModel", "Force refreshing cart to ensure discounts are properly applied")
        
        viewModelScope.safeLaunch {
            try {
                // First refresh discounts
                refreshDiscounts()
                
                // Make sure the shared cart quantities are updated
                updateSharedCartQuantities()
                
                // Load from persistence to ensure data is fully synchronized
                val savedCart = cartPersistenceManager.loadCart()
                if (savedCart.isNotEmpty()) {
                    Log.d("CartViewModel", "Reloaded ${savedCart.size} items from persistent storage")
                    _cartItems.value = savedCart
                }
                
                // Create a temporary copy of the cart items and update to ensure UI recomposition
                val tempItems = _cartItems.value.toList()
                _cartItems.value = tempItems
                
                Log.d("CartViewModel", "Cart force refresh completed with ${tempItems.size} items")
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error during force refresh: ${e.message}", e)
            }
        }
    }
    
    /**
     * Update the quantity of an item in the cart
     */
    fun updateQuantity(foodId: String, newQuantity: Int) {
        viewModelScope.safeLaunch {
            try {
                if (newQuantity <= 0) {
                    // If quantity is zero or negative, remove the item
                    removeFromCart(foodId)
                    return@safeLaunch
                }
                
                // Find the item and update its quantity
                val updatedItems = _cartItems.value.toMutableList()
                val index = updatedItems.indexOfFirst { it.food.id == foodId }
                
                if (index != -1) {
                    updatedItems[index] = updatedItems[index].copy(quantity = newQuantity)
                    _cartItems.value = updatedItems
                    
                    // Update shared cart quantities
                    updateSharedCartQuantities()
                    
                    // Refresh discounts
                    refreshDiscounts()
                    
                    Log.d("CartViewModel", "Updated quantity for item $foodId to $newQuantity")
                }
            } catch (e: Exception) {
                Log.e("CartViewModel", "Error updating quantity: ${e.message}", e)
            }
        }
    }

    fun secureClearCartOnLogout() {
        Log.d("CartViewModel", "secureClearCartOnLogout called. Current cart has ${_cartItems.value.size} items")
        
        // Clear the cart in memory
        _cartItems.value = emptyList()
        
        // Update shared cart quantities
        updateSharedCartQuantities()
        
        // Clear persistent storage
        cartPersistenceManager.clearSavedCart()
        
        Log.d("CartViewModel", "Cart data cleared due to logout/guest")
    }

    fun clearCart() {
        Log.d("CartViewModel", "Clearing cart")
        
        // Clear the cart in memory
        _cartItems.value = emptyList()
        
        // Update shared cart quantities
        updateSharedCartQuantities()
        
        // Clear persistent storage
        cartPersistenceManager.clearSavedCart()
        
        // Reset coupon
        _couponCode.value = null
        _appliedCouponDiscount.value = null
        _couponError.value = null
        
        // Force a refresh to ensure UI updates properly
        forceRefreshCart()
    }

    @Suppress("unused")
    private fun calculateSubtotal(): Double {
        return _cartItems.value.sumOf { it.food.price * it.quantity }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateCartItemNotes(food: Food, note: String) {
        val existingItemIndex = _cartItems.value.indexOfFirst { it.food.id == food.id }
        Log.d("CartViewModel", "Updating cart item note: ${food.name}, index: $existingItemIndex")
        
        if (existingItemIndex != -1) {
            val updatedItems = _cartItems.value.toMutableList()
            val existingItem = updatedItems[existingItemIndex]
            updatedItems[existingItemIndex] = existingItem.copy(notes = note)
            _cartItems.value = updatedItems
            Log.d("CartViewModel", "Updated notes for ${food.name}")
        }
    }
    
    /**
     * Refresh all discounts in the cart
     * This will fetch active discounts and apply them to cart items
     */
    fun refreshDiscounts() {
        viewModelScope.safeLaunch("CartViewModel") {
            // Get all REGULAR active discounts (excluding coupon discounts)
            val discountsResult = firebaseRepository.getAllRegularActiveDiscounts()
            if (discountsResult.isSuccess) {
                val regularDiscounts = discountsResult.getOrDefault(emptyList())
                
                Log.d("CartViewModel", "Found ${regularDiscounts.size} active regular discounts (no coupon discounts)")
                
                // Log details of all available discounts for debugging
                regularDiscounts.forEachIndexed { index, discount ->
                    Log.d("CartViewModel", "Discount #${index + 1}: " +
                          "Percentage: ${discount.percentOff}%, " +
                          "Min Quantity: ${discount.minQuantity}, " +
                          "Category: ${discount.categoryId}, " +
                          "Specific Foods: ${discount.specificFoodIds?.size ?: 0}")
                }
                
                // First, create a clean copy of cart items with all discount information removed
                // This ensures we don't have any stale coupon discounts affecting the display
                val cleanCartItems = _cartItems.value.map { cartItem ->
                    val cleanFood = cartItem.food.copy(
                        discountedPrice = null,
                        discountPercentage = null,
                        discountEndDate = null,
                        discountMessage = null
                    )
                    cartItem.copy(food = cleanFood)
                }
                
                // Then apply regular discounts to clean items
                val updatedCartItems = if (regularDiscounts.isNotEmpty()) {
                    cleanCartItems.map { cartItem ->
                        // First check if there are any product-specific discounts
                        val specificFoodDiscounts = regularDiscounts.filter { discount ->
                            discount.specificFoodIds?.contains(cartItem.food.id) == true
                        }
                        
                        // If there are product-specific discounts, ONLY consider those
                        // Otherwise, check for category discounts
                        val applicableDiscounts = if (specificFoodDiscounts.isNotEmpty()) {
                            Log.d("CartViewModel", "Found ${specificFoodDiscounts.size} product-specific discounts for ${cartItem.food.name}")
                            specificFoodDiscounts
                        } else {
                            // No product-specific discounts, check for category discounts
                            // BUT ONLY if they are true category-wide discounts (not targeted)
                            regularDiscounts.filter { discount ->
                                // This discount applies to this category
                                val categoryMatches = discount.categoryId.isNotEmpty() && discount.categoryId == cartItem.food.categoryId
                                
                                // Check if this is a true category-wide discount:
                                // - It doesn't have specificFoodIds (applies to all items in category)
                                // - OR it explicitly includes this product in specificFoodIds
                                val specificFoodIds = discount.specificFoodIds
                                val isTrueCategoryWideDiscount = specificFoodIds == null || 
                                                               specificFoodIds.isEmpty() ||
                                                               specificFoodIds.contains(cartItem.food.id)
                                
                                categoryMatches && isTrueCategoryWideDiscount
                            }.also { 
                                if (it.isNotEmpty()) {
                                    Log.d("CartViewModel", "No product-specific discounts for ${cartItem.food.name}, using ${it.size} category-wide discounts")
                                } else {
                                    Log.d("CartViewModel", "No applicable discounts for ${cartItem.food.name} - product should remain at original price")
                                }
                            }
                        }
                        
                        // If there are applicable discounts, find the best one
                        if (applicableDiscounts.isNotEmpty()) {
                            // Sort discounts by percentOff in descending order to prioritize larger discounts
                            val sortedDiscounts = applicableDiscounts.sortedByDescending { it.percentOff }
                            Log.d("CartViewModel", "Found ${sortedDiscounts.size} applicable discounts for ${cartItem.food.name}, sorted by discount percentage")
                            
                            // Find best applicable discount
                            var bestDiscount: Discount? = null
                            var bestDiscountedPrice = cartItem.food.price
                            
                            for (discount in sortedDiscounts) {
                                // Check if we meet quantity requirements for tiered discounts
                                val meetsQuantityRequirement = if (discount.minQuantity > 0) {
                                    // Calculate total quantity for this item in the cart
                                    val cartQuantity = _cartItems.value
                                        .filter { it.food.id == cartItem.food.id }
                                        .sumOf { it.quantity }
                                    
                                    cartQuantity >= discount.minQuantity
                                } else {
                                    true // No quantity requirement
                                }
                                
                                if (meetsQuantityRequirement) {
                                    // Apply the discount
                                    val discountedPrice = cartItem.food.price * (1 - (discount.percentOff / 100.0))
                                    
                                    // If this discount results in a better price, use it
                                    if (discountedPrice < bestDiscountedPrice) {
                                        bestDiscountedPrice = discountedPrice
                                        bestDiscount = discount
                                        Log.d("CartViewModel", "Better discount found for ${cartItem.food.name}: ${discount.percentOff}%, final price: $discountedPrice")
                                    }
                                } else {
                                    Log.d("CartViewModel", "Discount with ${discount.percentOff}% off not applied to ${cartItem.food.name} - quantity requirement not met (need ${discount.minQuantity})")
                                }
                            }
                            
                            // Apply the best discount if found
                            if (bestDiscount != null) {
                                // Calculate total quantity for this item in the cart
                                val cartQuantity = _cartItems.value
                                    .filter { it.food.id == cartItem.food.id }
                                    .sumOf { it.quantity }
                                
                                // We've already verified quantity requirements earlier, but recheck for safety
                                val meetsQuantityRequirement = bestDiscount.minQuantity <= 0 || cartQuantity >= bestDiscount.minQuantity
                                
                                if (meetsQuantityRequirement) {
                                    // Apply the discount
                                    val discountedPrice = cartItem.food.price * (1 - (bestDiscount.percentOff / 100.0))
                                    
                                    // Create discount message with context about the discount type
                                    val bestDiscountSpecificFoodIds = bestDiscount.specificFoodIds
                                    val discountType = if (bestDiscountSpecificFoodIds != null && 
                                                         bestDiscountSpecificFoodIds.contains(cartItem.food.id)) {
                                        "product-specific"
                                    } else if (bestDiscount.categoryId.isNotEmpty() && 
                                              (bestDiscountSpecificFoodIds == null || bestDiscountSpecificFoodIds.isEmpty())) {
                                        "category-wide" // Applies to all products in category
                                    } else {
                                        "category" // Some other type of category discount
                                    }
                                    
                                    val discountMessage = when {
                                        bestDiscount.minQuantity > 0 -> {
                                            "Buy ${bestDiscount.minQuantity}+ for ${bestDiscount.percentOff.toInt()}% off"
                                        }
                                        else -> {
                                            "${bestDiscount.percentOff.toInt()}% off"
                                        }
                                    }
                                    
                                    // Update the food item with discount information
                                    val updatedFood = cartItem.food.copy(
                                        discountedPrice = discountedPrice,
                                        discountPercentage = bestDiscount.percentOff,
                                        discountEndDate = bestDiscount.endDate,
                                        discountMessage = discountMessage
                                    )
                                    
                                    Log.d("CartViewModel", "Applied $discountType discount to ${cartItem.food.name}: " +
                                        "Original price: ${cartItem.food.price}, Discounted price: $discountedPrice, " +
                                        "Discount percentage: ${bestDiscount.percentOff}%, Savings: ${cartItem.food.price - discountedPrice}")
                                    
                                    // Return updated cart item
                                    cartItem.copy(food = updatedFood)
                                } else {
                                    // Shouldn't happen since we checked earlier, but just in case
                                    Log.d("CartViewModel", "Skipped discount application for ${cartItem.food.name} as quantity requirement not met")
                                    cartItem
                                }
                            } else {
                                // No applicable discount after quantity requirements, use clean item with no discount
                                Log.d("CartViewModel", "No discount applied to ${cartItem.food.name} - no applicable product-specific or category discounts")
                                cartItem
                            }
                        } else {
                            // No applicable discounts, use clean item with no discount
                            Log.d("CartViewModel", "No discount applied to ${cartItem.food.name} - no applicable product-specific or category discounts")
                            cartItem
                        }
                    }
                } else {
                    // No regular discounts, just use the clean items
                    cleanCartItems
                }
                
                // Update cart items with refreshed discount information
                _cartItems.value = updatedCartItems
                Log.d("CartViewModel", "Updated ${updatedCartItems.size} cart items with discount information")
                
                // Make sure to update the totalWithCoupon value with the latest total after refreshing discounts
                val currentTotal = total.value
                val currentCouponDiscount = _appliedCouponDiscount.value
                calculateTotalWithCoupon(currentTotal, currentCouponDiscount)
            }
        }
    }
    
    /**
     * Apply a coupon code
     */
    fun applyCouponCode(code: String) {
        if (code.isBlank()) {
            _couponError.value = "Please enter a valid coupon code"
            return
        }
        
        _isCouponValidating.value = true
        _couponError.value = null
        
        viewModelScope.safeLaunch {
            try {
                val result = firebaseRepository.validateCouponCode(code)
                
                if (result.isSuccess) {
                    val couponDiscount = result.getOrNull()
                    
                    // Verify that the returned discount is a valid coupon with matching code
                    if (couponDiscount != null && !couponDiscount.couponCode.isNullOrEmpty() && 
                        couponDiscount.couponCode.equals(code, ignoreCase = true)) {
                        _couponCode.value = code
                        _appliedCouponDiscount.value = couponDiscount
                        
                        // The coupon discount should only apply at checkout, not to individual items
                        // Force a refresh of items to ensure no coupon discounts are shown on items
                        refreshDiscounts()
                        
                        // Make sure to explicitly recalculate the total with coupon
                        calculateTotalWithCoupon(total.value, couponDiscount)
                        
                        Log.d("CartViewModel", "Applied coupon code: $code with discount: ${couponDiscount.percentOff}% (CHECKOUT ONLY)")
                    } else {
                        _couponError.value = "Invalid or expired coupon code"
                        _couponCode.value = null
                        _appliedCouponDiscount.value = null
                        _couponSavings.value = 0.0
                        _totalWithCoupon.value = totalAmount.value
                    }
                } else {
                    _couponError.value = "Failed to validate coupon: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                    _couponCode.value = null
                    _appliedCouponDiscount.value = null
                    _couponSavings.value = 0.0
                    _totalWithCoupon.value = totalAmount.value
                }
            } catch (e: Exception) {
                _couponError.value = "Error: ${e.message}"
                _couponCode.value = null
                _appliedCouponDiscount.value = null
                _couponSavings.value = 0.0
                _totalWithCoupon.value = totalAmount.value
            } finally {
                _isCouponValidating.value = false
            }
        }
    }
    
    /**
     * Remove applied coupon code
     */
    fun removeCouponCode() {
        _couponCode.value = null
        _appliedCouponDiscount.value = null
        _couponError.value = null
        _couponSavings.value = 0.0
        
        // Ensure the totalWithCoupon is reset to the regular total
        _totalWithCoupon.value = total.value
        
        // Force a refresh of discounts to make sure no coupon discounts remain on items
        refreshDiscounts()
        
        Log.d("CartViewModel", "Removed coupon code and reset all coupon-related values")
    }
    
    /**
     * Checks if the current user is a guest (not authenticated)
     * @return true if the user is a guest, false if logged in
     */
    fun isUserGuest(): Boolean {
        return authRepository.currentUser == null
    }
    
    companion object {
        // Shared StateFlow for cart quantities that can be observed by other ViewModels
        // Using lazy initialization to ensure thread safety
        val cartQuantitiesFlow: MutableStateFlow<Map<String, Int>> by lazy { 
            val flow = MutableStateFlow<Map<String, Int>>(emptyMap())
            Log.d("CartViewModel", "Created new cartQuantitiesFlow instance")
            flow
        }
        
        // Static scope to ensure the flow isn't garbage collected
        private val staticScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        
        // Used to log initialization in a static context
        init {
            Log.d("CartViewModel", "CartViewModel companion object initialized")
            
            // Ensure the flow remains active by collecting it in a static scope
            staticScope.launch {
                try {
                    cartQuantitiesFlow.collect { quantities ->
                        if (quantities.isNotEmpty()) {
                            Log.d("CartViewModel", "Cart quantities updated: ${quantities.size} items")
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.d("CartViewModel", "Static cart flow collection cancelled")
                    } else {
                        Log.e("CartViewModel", "Error in static cart flow: ${e.message}")
                    }
                }
            }
        }
    }
} 