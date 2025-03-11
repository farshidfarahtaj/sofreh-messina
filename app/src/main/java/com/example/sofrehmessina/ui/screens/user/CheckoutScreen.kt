package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sofrehmessina.R
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.LocalCartViewModel
import com.example.sofrehmessina.ui.viewmodel.OrderViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.sofrehmessina.data.model.Discount
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.focus.FocusState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.floatingEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.pulseEffect
import com.example.sofrehmessina.ui.utils.UiAnimations

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Suppress("unused")
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onCheckoutComplete: () -> Unit,
    orderViewModel: OrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Get the shared CartViewModel from CompositionLocal
    val cartViewModel = LocalCartViewModel.current
    
    var deliveryAddress by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var orderPlacementInitiated by remember { mutableStateOf(false) }
    var couponCode by remember { mutableStateOf("") }

    // Animation states
    val fadeInAnimation = remember { Animatable(0f) }
    val slideUpAnimation = remember { Animatable(50f) }
    val checkoutButtonScale = remember { Animatable(0.95f) }
    
    val cartItems by cartViewModel.cartItems.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Coupon related states
    val appliedCouponCode by cartViewModel.couponCode.collectAsState()
    val appliedCouponDiscount by cartViewModel.appliedCouponDiscount.collectAsState()
    val couponError by cartViewModel.couponError.collectAsState()
    val isCouponValidating by cartViewModel.isCouponValidating.collectAsState()
    
    // Create a scroll state to share with the keyboard layout and input fields
    val scrollState = rememberScrollState()
    
    // Create a coroutine scope for focus handling
    val coroutineScope = rememberCoroutineScope()
    
    // Start entrance animations
    LaunchedEffect(Unit) {
        launch {
            fadeInAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = EaseOutCirc)
            )
        }
        
        launch {
            slideUpAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(500, easing = EaseOutQuart)
            )
        }
        
        launch {
            delay(300)
            checkoutButtonScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        
        // Force refresh cart data when the checkout screen is displayed
        cartViewModel.forceRefreshCart()
    }
    
    // Calculate totals
    val originalTotal by cartViewModel.originalTotalAmount.collectAsState()
    val totalSavings by cartViewModel.totalSavings.collectAsState()
    val couponSavings by cartViewModel.couponSavings.collectAsState()
    val totalWithCoupon by cartViewModel.totalWithCoupon.collectAsState()

    val isLoading by orderViewModel.isLoading.collectAsState()
    val error by orderViewModel.error.collectAsState()
    
    // Animated total price
    val animatedTotal = remember { Animatable(totalWithCoupon.toFloat()) }
    LaunchedEffect(totalWithCoupon) {
        animatedTotal.animateTo(
            targetValue = totalWithCoupon.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    // Monitor error state
    LaunchedEffect(error) {
        if (error != null) {
            showErrorDialog = true
            errorMessage = error.toString()
        }
    }

    // Monitor loading state for order placement
    LaunchedEffect(isLoading) {
        if (orderPlacementInitiated && !isLoading && error == null) {
            // Order placement completed successfully
            showSuccessDialog = true
            orderPlacementInitiated = false  // Reset the flag
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            deliveryAddress = user.address
        }
    }

    // Get the current language
    val context = LocalContext.current
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }
    
    // Update the current language whenever the composition is recomposed
    LaunchedEffect(Unit) {
        currentLanguage.value = LocaleHelper.getSelectedLanguageCode(context)
    }

    // Show success dialog with animation
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSuccessDialog = false
                // Clear cart and navigate to the order confirmation screen
                cartViewModel.clearCart()
                onCheckoutComplete()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .bounceEffect(enabled = true, bounceHeight = 5f),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.order_placed),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.order_success_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showSuccessDialog = false
                        // Clear cart and navigate to the order confirmation screen
                        cartViewModel.clearCart()
                        onCheckoutComplete()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceEffect(enabled = true, bounceHeight = 2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.view_orders))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
    }

    // Show error dialog with animation
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.error),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showErrorDialog = false 
                        orderViewModel.clearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
    }

    // Confirmation dialog with animation
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.confirm_order),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.confirm_order_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.total_amount),
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Text(
                                text = currencyManager.formatPrice(totalWithCoupon),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        // Mark order placement as initiated to track when it completes
                        orderPlacementInitiated = true
                        
                        // Place the order
                        orderViewModel.placeOrder(
                            cartItems = cartItems,
                            deliveryAddress = deliveryAddress,
                            specialInstructions = specialInstructions,
                            couponCode = appliedCouponCode,
                            totalAmount = totalWithCoupon
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceEffect(enabled = true, bounceHeight = 2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.place_order))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.checkout),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pulseEffect(true)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        bottomBar = {
            // BOTTOM BUTTON SECTION - Fixed in the scaffold bottomBar for guaranteed visibility
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { 
                        alpha = fadeInAnimation.value
                        translationY = slideUpAnimation.value
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Add a headline for the checkout section
                    Text(
                        text = stringResource(R.string.complete_your_order),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // First show the Confirm Checkout with Discounts button when discounts are applied
                    if (totalSavings > 0 || couponSavings > 0) {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            ),
                            enabled = deliveryAddress.isNotEmpty() && cartItems.isNotEmpty()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.checkout_with_discounts, currencyManager.formatPrice(totalWithCoupon)),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show a divider to separate the buttons
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Regular Place Order button with distinct styling when discounts are applied
                        OutlinedButton(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = deliveryAddress.isNotEmpty() && cartItems.isNotEmpty(),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = stringResource(R.string.order_without_discounts, currencyManager.formatPrice(originalTotal)),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Single Place Order button when no discounts
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = deliveryAddress.isNotEmpty() && cartItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.place_order, currencyManager.formatPrice(originalTotal)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Add hint about potential discounts
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.discount_info),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.add_more_items_for_discounts),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    // Add explanatory text if button is disabled
                    if (deliveryAddress.isEmpty() || cartItems.isEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (deliveryAddress.isEmpty()) 
                                stringResource(R.string.please_enter_delivery_address)
                            else if (cartItems.isEmpty()) 
                                stringResource(R.string.your_cart_empty)
                            else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Use KeyboardAwareLayout with improved stability
        KeyboardAwareLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            scrollState = scrollState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Order Summary
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.order_summary),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // List all cart items with enhanced discount information
                    cartItems.forEach { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.food.getName(currentLanguage.value)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                if (item.food.discountedPrice != null && item.food.discountedPrice < item.food.price) {
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = currencyManager.formatPrice(item.food.discountedPrice * item.quantity),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Text(
                                            text = currencyManager.formatPrice(item.food.price * item.quantity),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDecoration = TextDecoration.LineThrough,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        // Show per-item savings
                                        val itemSavings = (item.food.price - item.food.discountedPrice) * item.quantity
                                        Text(
                                            text = stringResource(R.string.save_discount, currencyManager.formatPrice(itemSavings)),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = currencyManager.formatPrice(item.food.price * item.quantity),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            
                            if (item.notes.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.note, item.notes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            
                            // Display discount reason if available
                            item.food.discountMessage?.let { message ->
                                if (message.isNotEmpty() && item.food.discountedPrice != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = stringResource(R.string.discount_info),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Coupon Code Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.have_a_coupon),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (appliedCouponCode != null) {
                                // Show applied coupon
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Applied: ${appliedCouponCode}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        appliedCouponDiscount?.let { discount: Discount ->
                                            Text(
                                                text = "${discount.percentOff.toInt()}% off",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { cartViewModel.removeCouponCode() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text("Remove")
                                    }
                                }
                            } else {
                                // Show coupon input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = couponCode,
                                        onValueChange = { couponCode = it.trim() },
                                        label = { Text(stringResource(R.string.enter_coupon_code)) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .autoScrollOnFocus(scrollState),
                                        singleLine = true,
                                        isError = couponError != null
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Button(
                                        onClick = { cartViewModel.applyCouponCode(couponCode) },
                                        enabled = couponCode.isNotEmpty() && !isCouponValidating,
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        if (isCouponValidating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(stringResource(R.string.apply))
                                        }
                                    }
                                }
                                
                                // Show error if any
                                couponError?.let { error ->
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Enhanced summary section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.subtotal),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = currencyManager.formatPrice(originalTotal),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            // Only show discount section if there are discounts or a coupon is applied
                            if (totalSavings > 0 || couponSavings > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Discount summary headline
                                Text(
                                    text = stringResource(R.string.discount_summary),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                // Item discounts row - only show if there are regular discounts
                                if (totalSavings > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.item_discounts),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "-${currencyManager.formatPrice(totalSavings)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                // Coupon discount row - only show if a coupon is applied
                                if (appliedCouponDiscount != null && couponSavings > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Coupon: $appliedCouponCode",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "-${currencyManager.formatPrice(couponSavings)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Total discount row with background highlight
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.total_savings),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "-${currencyManager.formatPrice(totalSavings + couponSavings)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Final total with enhanced styling
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.total),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = currencyManager.formatPrice(totalWithCoupon),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            
                            // Add "potential discount" message when no discounts are currently applied
                            if (totalSavings <= 0 && couponSavings <= 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = stringResource(R.string.discount_info),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.add_more_items_for_discounts),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Add payment method info
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.payment_method_cash_on_delivery),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Delivery Details
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delivery_details),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text(stringResource(R.string.delivery_address)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            // Use a custom modifier to ensure this field always scrolls high enough
                            .onFocusEvent { focusState: FocusState ->
                                if (focusState.isFocused) {
                                    // When this field is focused, aggressively scroll to it
                                    coroutineScope.launch {
                                        // Start with a basic scroll to bring the field into view
                                        scrollState.animateScrollTo(scrollState.value + 300)
                                        delay(50)
                                        // Then scroll more to position the field well above the keyboard
                                        scrollState.animateScrollTo(scrollState.value + 100)
                                    }
                                }
                            }
                            .autoScrollOnFocus(scrollState),
                        minLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text(stringResource(R.string.special_instructions)) },
                        placeholder = { Text(stringResource(R.string.any_special_delivery_instructions)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .autoScrollOnFocus(scrollState),
                        minLines = 3
                    )
                    
                    // Add a small spacer to improve visibility
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Add just enough space for the bottom bar
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
} 