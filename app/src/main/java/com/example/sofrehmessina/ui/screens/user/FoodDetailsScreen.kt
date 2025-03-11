package com.example.sofrehmessina.ui.screens.user

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.components.QuantitySelector
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.components.AddToCartSuccessDialog
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import com.example.sofrehmessina.ui.viewmodel.LocalCartViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.foundation.background
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.floatingEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.pulseEffect
import com.example.sofrehmessina.ui.utils.UiAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.foundation.ScrollState
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailsScreen(
    foodId: String,
    categoryId: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToCategory: (String) -> Unit = {},
    viewModel: FoodViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    var quantity by remember { mutableIntStateOf(1) }
    var notes by remember { mutableStateOf("") }
    var showAddToCartDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Animation states
    val imageScale = remember { Animatable(0.8f) }
    val contentAlpha = remember { Animatable(0f) }
    
    // Add to cart button animation
    val addToCartInteractionSource = remember { MutableInteractionSource() }
    val isAddToCartPressed by addToCartInteractionSource.collectIsPressedAsState()
    val addToCartScale by animateFloatAsState(
        targetValue = if (isAddToCartPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "addToCartScale"
    )

    // Get the current language code and observe it for changes
    val context = LocalContext.current
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }
    
    // Update the current language whenever the composition is recomposed
    LaunchedEffect(Unit) {
        currentLanguage.value = LocaleHelper.getSelectedLanguageCode(context)
    }

    val selectedFood by viewModel.selectedFood.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(foodId) {
        viewModel.loadFoodDetails(foodId)
        viewModel.loadCategory(categoryId)
        
        // Start entrance animations
        launch {
            delay(100)
            imageScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        
        launch {
            delay(300)
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = EaseOutCirc
                )
            )
        }
    }

    // Log the food item's availability status whenever it changes
    LaunchedEffect(selectedFood) {
        selectedFood?.let { food ->
            Log.d("FoodDetailsScreen", "Food item loaded/updated: ${food.id} - ${food.getName(currentLanguage.value)}")
            Log.d("FoodDetailsScreen", "Availability status: ${food.foodAvailable}")
            Log.d("FoodDetailsScreen", "Button should be enabled: ${food.foodAvailable}")
        }
    }

    // Show the add to cart success dialog
    if (showAddToCartDialog && selectedFood != null) {
        AddToCartSuccessDialog(
            visible = true,
            foodName = selectedFood?.getName(currentLanguage.value) ?: "",
            quantity = quantity,
            onDismiss = { showAddToCartDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        selectedFood?.getName(currentLanguage.value) ?: stringResource(R.string.food_details),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
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
                actions = {
                    // Removed favorite icon, only keeping cart icon
                    IconButton(
                        onClick = onNavigateToCart
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = stringResource(R.string.cart),
                            modifier = Modifier.floatingEffect()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize()
                )
            } else if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = error.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.back))
                    }
                }
            } else if (selectedFood != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Food Image with animated scaling effect
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .graphicsLayer { 
                                scaleX = imageScale.value
                                scaleY = imageScale.value
                                alpha = imageScale.value
                            }
                    ) {
                        // Main food image
                        if (!selectedFood?.imageUrl.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = selectedFood?.imageUrl,
                                contentDescription = selectedFood?.getName(currentLanguage.value),
                        contentScale = ContentScale.Crop,
                        loading = {
                                    UiAnimations.ShimmerBox(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                    .shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            )
                            
                            // Gradient overlay for better readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.1f),
                                                Color.Black.copy(alpha = 0.4f)
                                            ),
                                            startY = 0f,
                                            endY = Float.POSITIVE_INFINITY
                                        )
                                    )
                            )
                            
                            // Discount percentage indicator in top left corner (if applicable)
                            if (selectedFood?.discountPercentage != null && selectedFood?.discountPercentage!! > 0) {
                                Surface(
                                    color = Color(0xFFE53935), // Red color
                                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .bounceEffect(enabled = true, bounceHeight = 2f)
                                ) {
                            Text(
                                        text = "-${selectedFood?.discountPercentage?.toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            
                            // Pricing and availability indicator overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .align(Alignment.BottomStart),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Price with animated glow effect
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    ),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.bounceEffect(enabled = true, bounceHeight = 3f)
                                ) {
                                    Text(
                                        text = currencyManager.formatPrice(selectedFood?.price ?: 0.0),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                
                                // Availability badge
                                if (selectedFood?.foodAvailable == true) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                        modifier = Modifier.bounceEffect(enabled = true, bounceHeight = 3f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                                text = stringResource(R.string.available),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Food details animated in
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .graphicsLayer {
                                alpha = contentAlpha.value
                                translationY = (1 - contentAlpha.value) * 50
                            }
                    ) {
                        // Category Chip Only (removed rating section)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Chip
                            selectedCategory?.let { category ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.clickable {
                                        // Navigate to category
                                        selectedFood?.categoryId?.let { categoryId ->
                                            onNavigateToCategory(categoryId)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = category.getName(currentLanguage.value),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        
                        // Title and Description
                        Text(
                            text = selectedFood?.getName(currentLanguage.value) ?: "",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = selectedFood?.getDescription(currentLanguage.value) ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        // Discount details box (if applicable) - before the quantity selector
                        if (selectedFood?.discountPercentage != null && selectedFood?.discountPercentage!! > 0) {
                                    Surface(
                                shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.discount_details),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Show the admin-defined discount message if available, otherwise fallback to generic message
                                        Text(
                                            text = if (selectedFood?.discountMessage?.isNotEmpty() == true) {
                                                selectedFood?.discountMessage ?: ""
                                            } else {
                                                stringResource(
                                                    R.string.buy_x_get_discount,
                                                    extractDiscountThreshold(selectedFood?.discountMessage ?: "")
                                                )
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Quantity selector with price display
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Calculate the current price based on quantity and discount
                                val hasDiscount = selectedFood?.discountPercentage != null && selectedFood?.discountPercentage!! > 0
                                
                                // Extract discount threshold from discount message or default to minimum quantity needed
                                val discountMessage = selectedFood?.discountMessage ?: ""
                                val discountThreshold = extractDiscountThreshold(discountMessage)
                                
                                val isDiscountApplied = hasDiscount && quantity >= discountThreshold
                                
                                val originalPrice = (selectedFood?.price ?: 0.0) * quantity
                                val discountedPrice = if (isDiscountApplied) {
                                    val discount = selectedFood?.discountPercentage ?: 0.0
                                    originalPrice * (1 - discount / 100)
                                } else {
                                    originalPrice
                                }
                                
                                // Header row with quantity and price
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.quantity),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    
                                    // Show original price with strikethrough if discount is applied
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (isDiscountApplied) {
                                            Text(
                                                text = currencyManager.formatPrice(originalPrice),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = currencyManager.formatPrice(discountedPrice),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = currencyManager.formatPrice(originalPrice),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                QuantitySelector(
                                    quantity = quantity,
                                    onQuantityChange = { quantity = it },
                                    maxQuantity = 20,
                                    minQuantity = 1,
                                    enabled = true,
                                    modifier = Modifier
                                )
                                
                                // If discount is available but not yet applied, show prompt
                                if (hasDiscount && !isDiscountApplied && discountThreshold > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val itemsNeeded = discountThreshold - quantity
                                    val promptText = if (itemsNeeded > 0) {
                                        if (selectedFood?.discountMessage?.isNotEmpty() == true) {
                                            "${stringResource(R.string.add_more_items_for_discounts)} (${itemsNeeded} ${if (itemsNeeded == 1) "item" else "items"} more for discount)"
                                        } else {
                                            "${stringResource(R.string.add_more_items_for_discounts)} (${itemsNeeded} ${if (itemsNeeded == 1) "item" else "items"} more for ${selectedFood?.discountPercentage?.toInt() ?: 0}% off)"
                                }
                            } else {
                                        stringResource(R.string.add_more_items_for_discounts)
                                    }
                                    
                                    Text(
                                        text = promptText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // Additional notes field styling
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.special_instructions),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                SpecialInstructionsField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    scrollState = scrollState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .autoScrollOnFocus(scrollState)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Space for the button
                }
                
                // Add to cart button - fixed at the bottom
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .graphicsLayer {
                            alpha = contentAlpha.value
                            translationY = (1 - contentAlpha.value) * 100
                            scaleX = addToCartScale
                            scaleY = addToCartScale
                        }
                        .zIndex(10f),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    // Calculate the final price for the button text
                    val hasDiscount = selectedFood?.discountPercentage != null && selectedFood?.discountPercentage!! > 0
                    
                    // Extract discount threshold from discount message or default to minimum quantity needed
                    val discountMessage = selectedFood?.discountMessage ?: ""
                    val discountThreshold = extractDiscountThreshold(discountMessage)
                    
                    val isDiscountApplied = hasDiscount && quantity >= discountThreshold
                    
                    val finalPrice = if (isDiscountApplied) {
                        val originalPrice = (selectedFood?.price ?: 0.0) * quantity
                        val discount = selectedFood?.discountPercentage ?: 0.0
                        originalPrice * (1 - discount / 100)
                    } else {
                        (selectedFood?.price ?: 0.0) * quantity
                    }
                    
                    Button(
                        onClick = {
                            // Add item to cart directly
                            selectedFood?.let { food ->
                                val cartItem = CartItem(food = food, quantity = quantity, notes = notes)
                                cartViewModel.addToCart(cartItem)
                                
                                // Show success message
                                showAddToCartDialog = true
                                
                                // Reset UI state
                                quantity = 1
                                notes = ""
                            }
                        },
                        enabled = selectedFood?.foodAvailable == true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFood?.foodAvailable == true) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedFood?.foodAvailable == true) 
                                MaterialTheme.colorScheme.onPrimary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        interactionSource = addToCartInteractionSource
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedFood?.foodAvailable == true) 
                                    "${stringResource(R.string.add_to_cart)} - ${currencyManager.formatPrice(finalPrice)}" 
                                else 
                                    stringResource(R.string.unavailable),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    maxQuantity: Int = 10,
    minQuantity: Int = 1,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Implementation of quantity selector
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { 
                if (quantity > minQuantity) {
                    onQuantityChange(quantity - 1)
                }
            },
            enabled = enabled && quantity > minQuantity
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrease quantity"
            )
        }
        
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        IconButton(
            onClick = { 
                if (quantity < maxQuantity) {
                    onQuantityChange(quantity + 1)
                }
            },
            enabled = enabled && quantity < maxQuantity
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase quantity"
            )
        }
    }
}

@Composable
fun SpecialInstructionsField(
    value: String,
    onValueChange: (String) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.special_instructions_hint)) },
        modifier = modifier
            .fillMaxWidth()
            .autoScrollOnFocus(scrollState),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

/**
 * Extracts the discount threshold from the discount message.
 * Looks for patterns like "Buy X" or "Order X" in the message.
 * Returns a default value of 3 if no threshold is found.
 */
private fun extractDiscountThreshold(discountMessage: String): Int {
    // Common patterns in discount messages
    val buyXPattern = Regex("(?i)buy\\s+(\\d+)")
    val orderXPattern = Regex("(?i)order\\s+(\\d+)")
    val getXPattern = Regex("(?i)get\\s+(\\d+)")
    val xItemsPattern = Regex("(?i)(\\d+)\\s+items")
    val xPiecesPattern = Regex("(?i)(\\d+)\\s+pieces")
    val numericPattern = Regex("\\d+")
    
    // Try to match each pattern
    buyXPattern.find(discountMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    orderXPattern.find(discountMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    getXPattern.find(discountMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    xItemsPattern.find(discountMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    xPiecesPattern.find(discountMessage)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
    
    // If no specific pattern matches, try to find any number in the message
    numericPattern.find(discountMessage)?.value?.toIntOrNull()?.let { return it }
    
    // Default fallback
    return 3
} 