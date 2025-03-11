package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.DiscountDesignSystem
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.components.SofrehDrawer
import com.example.sofrehmessina.ui.components.BottomNavItem
import com.example.sofrehmessina.ui.components.DiscountCountdown
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import kotlinx.coroutines.delay
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.compose.ui.zIndex
import com.example.sofrehmessina.utils.rememberCurrencyManager
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.util.LocaleHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import com.example.sofrehmessina.R
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.floatingEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.pulseEffect
import com.example.sofrehmessina.ui.utils.UiAnimations
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope

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

/**
 * Gets an appropriate category label for the food item to display in discount details
 */
private fun Food.getCategoryLabel(languageCode: String): String? {
    // Don't use discount message since it's already shown separately
    // (This was causing the duplication)
    
    // Try to extract food type from the name (first word)
    val name = getName(languageCode)
    val firstWord = name.split(" ").firstOrNull()
    
    // If the first word is very short, use the entire name
    return if (firstWord?.length ?: 0 < 3) name else firstWord
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    foodViewModel: FoodViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val foodItems by foodViewModel.foodItems.collectAsState()
    val selectedCategory by categoryViewModel.selectedCategory.collectAsState()
    val isLoading by foodViewModel.isLoading.collectAsState()
    val error by foodViewModel.error.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Animation states
    val fadeInAnimation = remember { Animatable(0f) }
    val slideUpAnimation = remember { Animatable(50f) }
    val headerScale = remember { Animatable(0.9f) }
    
    // Get the current language code and observe it for changes
    val context = LocalContext.current
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }
    
    // For drawer navigation
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Update the current language whenever the composition is recomposed
    LaunchedEffect(Unit) {
        currentLanguage.value = LocaleHelper.getSelectedLanguageCode(context)
        
        // Start entrance animations
        launch {
            fadeInAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = EaseOutCirc)
            )
        }
        
        launch {
            slideUpAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(500, easing = EaseOutBack)
            )
        }
        
        launch {
            headerScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    
    LaunchedEffect(categoryId) {
        foodViewModel.loadFoodWithCategory(categoryId)
        categoryViewModel.loadCategory(categoryId)
    }
    
    // Safe access to selectedCategory
    val categoryName = remember(selectedCategory, currentLanguage.value) {
        selectedCategory?.getName(currentLanguage.value) ?: ""
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SofrehDrawer(
                currentUser = currentUser,
                navController = navController,
                onProfileClick = {
                    if (currentUser != null) {
                        navController.navigate(Screen.Profile.route)
                    } else {
                        navController.navigate(Screen.Login.route)
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
                onClose = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        },
        gesturesEnabled = true,
        modifier = Modifier.zIndex(1f)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = headerScale.value
                                        scaleY = headerScale.value
                                    }
                            ) 
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { 
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier.pulseEffect(true)
                        ) {
                            Icon(
                                Icons.Default.Menu, 
                                contentDescription = stringResource(R.string.menu)
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
                NavigationBar(
                    modifier = Modifier.shadow(8.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 16.dp
                ) {
                    BottomNavItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_home),
                        label = stringResource(R.string.home),
                        selected = false,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    BottomNavItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_restaurant),
                        label = stringResource(R.string.menu),
                        selected = true,
                        onClick = {
                            navController.navigate(Screen.Menu.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                    BottomNavItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_shopping_cart),
                        label = stringResource(R.string.cart),
                        selected = false,
                        onClick = {
                            navController.navigate(Screen.Cart.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> LoadingIndicator()
                    error != null -> {
                        ErrorDialog(
                            error = error.toString(),
                            onDismiss = { foodViewModel.clearError() }
                        )
                    }
                    foodItems.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = fadeInAnimation.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(bottom = 16.dp)
                                        .floatingEffect(),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                                
                                Text(
                                    text = stringResource(R.string.no_food_items),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.check_back_later),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.bounceEffect(enabled = true, bounceHeight = 3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.back_to_categories))
                                }
                            }
                        }
                    }
                    else -> {
                        // Category description if available
                        selectedCategory?.getDescription(currentLanguage.value)?.takeIf { it.isNotEmpty() }?.let { description ->
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Category header with description
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shadowElevation = 4.dp,
                                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .graphicsLayer {
                                            alpha = fadeInAnimation.value
                                            translationY = slideUpAnimation.value
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                
                                // Food items grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
                                ) {
                                    itemsIndexed(
                                        items = foodItems,
                                        key = { _, item -> item.id }
                                    ) { index, food ->
                                        // Calculate staggered animation delay
                                        val itemDelay = 50L + (index * 50L)
                                        
                                        CategoryFoodItemCard(
                                            food = food,
                                            onClick = {
                                                navController.navigate(
                                                    "${Screen.FoodDetails.route}/${food.id}/${categoryId}"
                                                )
                                            },
                                            currentLanguage = currentLanguage.value,
                                            itemDelay = itemDelay
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            // No description available, just show the grid
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = fadeInAnimation.value
                                    }
                            ) {
                                itemsIndexed(
                                    items = foodItems,
                                    key = { _, item -> item.id }
                                ) { index, food ->
                                    // Calculate staggered animation delay
                                    val itemDelay = 50L + (index * 50L)
                                    
                                    CategoryFoodItemCard(
                                        food = food,
                                        onClick = {
                                            navController.navigate(
                                                "${Screen.FoodDetails.route}/${food.id}/${categoryId}"
                                            )
                                        },
                                        currentLanguage = currentLanguage.value,
                                        itemDelay = itemDelay
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFoodItemCard(
    food: Food,
    onClick: () -> Unit,
    currentLanguage: String,
    modifier: Modifier = Modifier,
    itemDelay: Long = 0L,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    val foodName = food.getName(currentLanguage)
    
    // Animation states for card appearance
    var isVisible by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(itemDelay)
        isVisible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )
    
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hoverScale"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Card(
        onClick = {
            onClick()
            isHovered = !isHovered
        },
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale * hoverScale * if (isPressed) 0.95f else 1f
                scaleY = scale * hoverScale * if (isPressed) 0.95f else 1f
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
            focusedElevation = 4.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        interactionSource = interactionSource
    ) {
        Column {
            // Image section with overlays for discounts and availability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                // Food image
                Image(
                    painter = rememberAsyncImagePainter(
                        model = food.imageUrl,
                        error = rememberAsyncImagePainter(
                            model = android.R.drawable.ic_menu_gallery
                        )
                    ),
                    contentDescription = foodName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay for better text visibility at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                
                // Availability overlay if not available
                if (!food.foodAvailable) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.sold_out),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Discount badge with red background if applicable
                if (food.discountPercentage != null && food.discountPercentage > 0) {
                    Surface(
                        color = Color(0xFFE53935), // Red color as requested
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 12.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .bounceEffect(enabled = true, bounceHeight = 2f)
                    ) {
                        Text(
                            text = "-${food.discountPercentage.toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Price tag at the bottom right
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topStart = 12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .bounceEffect(enabled = isHovered, bounceHeight = 2f)
                ) {
                    if (food.discountPercentage != null && food.discountPercentage > 0) {
                        // Calculate discounted price if not provided
                        val discountedPrice = food.discountedPrice ?: 
                            (food.price * (1 - food.discountPercentage / 100.0))
                        
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Original price with strikethrough
                            Text(
                                text = currencyManager.formatPrice(food.price),
                                style = MaterialTheme.typography.labelSmall,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            
                            // Discounted price
                            Text(
                                text = currencyManager.formatPrice(discountedPrice),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        Text(
                            text = currencyManager.formatPrice(food.price),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Food name
            Text(
                text = foodName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            
            // Discount details box (if applicable)
            if (food.discountPercentage != null && food.discountPercentage > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.discount_details),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        // Show the discount message if available, otherwise show a generic message
                        val discountExplanation = if (!food.discountMessage.isNullOrBlank()) {
                            food.discountMessage
                        } else {
                            val threshold = extractDiscountThreshold(food.discountMessage ?: "")
                            if (threshold > 0) {
                                stringResource(
                                    R.string.buy_x_or_more_for_discount, 
                                    threshold.toString(), 
                                    food.discountPercentage.toInt().toString()
                                )
                            } else {
                                stringResource(
                                    R.string.limited_time_offer_discount, 
                                    food.discountPercentage.toInt().toString()
                                )
                            }
                        }
                        
                        // Show the discount explanation with a Light Bulb emoji as a bullet point
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = discountExplanation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Show time-limited offer notification if the discount has an end date
                        if (food.discountEndDate != null) {
                            Log.d("CategoryDetailsScreen", "Food ${food.id} has a discount with end date: ${food.discountEndDate}")
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⏳ ",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = stringResource(R.string.limited_time_offer),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Add the countdown timer
                            DiscountCountdown(endDate = food.discountEndDate)
                        }
                    }
                }
            }
            
            // Add bottom padding
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
} 