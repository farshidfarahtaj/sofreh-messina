package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.Food
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.components.FoodItemCard
import com.example.sofrehmessina.ui.components.DiscountCountdown
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import com.example.sofrehmessina.ui.viewmodel.FoodViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.util.LocaleHelper
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.sofrehmessina.ui.components.DiscountDesignSystem
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.floatingEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.pulseEffect
import com.example.sofrehmessina.ui.utils.UiAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    foodViewModel: FoodViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val categories by categoryViewModel.categories.collectAsState()
    val foodItems by foodViewModel.foodItems.collectAsState()
    val isLoadingCategories by categoryViewModel.isLoading.collectAsState()
    val isLoadingFood by foodViewModel.isLoading.collectAsState()
    val categoryError by categoryViewModel.error.collectAsState()
    val foodError by foodViewModel.error.collectAsState()
    val cartItemCount by cartViewModel.cartItemCount.collectAsState()
    
    // Animation states
    val fadeInAnimation = remember { Animatable(0f) }
    val slideUpAnimation = remember { Animatable(100f) }
    
    // Get the current language code and observe it for changes
    val context = LocalContext.current
    val currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
    
    // Use rememberUpdatedState to track screen visits
    val screenVisitCount = remember { mutableStateOf(0) }
    
    // Animate initial appearance
    LaunchedEffect(Unit) {
        screenVisitCount.value++
        Log.d("MenuScreen", "Screen visit count: ${screenVisitCount.value}")
        
        launch {
            fadeInAnimation.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = EaseOutCirc)
            )
        }
        launch {
            slideUpAnimation.animateTo(
                targetValue = 0f,
                animationSpec = tween(600, easing = EaseOutQuart)
            )
        }
    }
    
    // Load data when screen is first shown or when language changes
    LaunchedEffect(currentLanguage) {
        // Simple approach: just load the data when language changes
        Log.d("MenuScreen", "Loading menu data for language: $currentLanguage")
        
        // Load fresh category and food data
        categoryViewModel.loadCategories()
        foodViewModel.loadFoodItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.menu_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.pulseEffect(true)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("cart") },
                        modifier = Modifier.pulseEffect(cartItemCount > 0)
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge { 
                                        Text(
                                            cartItemCount.toString(),
                                            modifier = Modifier.bounceEffect(enabled = true, bounceHeight = 1f)
                                        ) 
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart, 
                                contentDescription = stringResource(R.string.cart)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoadingCategories || isLoadingFood) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize()
                )
            } else if (categoryError != null) {
                ErrorDialog(
                    error = categoryError.toString(),
                    onDismiss = { categoryViewModel.clearError() }
                )
            } else if (foodError != null) {
                ErrorDialog(
                    error = foodError.toString(),
                    onDismiss = { foodViewModel.clearError() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            alpha = fadeInAnimation.value
                            translationY = slideUpAnimation.value
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.categories),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        CategoriesRow(
                            categories = categories,
                            onCategoryClick = { categoryId ->
                                navController.navigate("category_details/$categoryId")
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    item {
                        Text(
                            text = stringResource(R.string.all_food_items),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    val groupedFoodItems = foodItems.groupBy { it.categoryId }
                    
                    groupedFoodItems.forEach { (categoryId, foods) ->
                        // Find the category name by ID
                        val category = categories.find { it.id == categoryId }
                        // Get category name outside of the remember block
                        val fallbackName = "Uncategorized"
                        val categoryName = category?.getName(currentLanguage) ?: fallbackName
                        
                        item {
                            // Category Header
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        
                        itemsIndexed(
                            items = foods,
                            key = { _, item -> item.id }
                        ) { index, food ->
                            val itemAppeared = remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                delay(100 + (index * 50).toLong())
                                itemAppeared.value = true
                            }
                            
                            val animatedScale by animateFloatAsState(
                                targetValue = if (itemAppeared.value) 1f else 0.8f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "scale"
                            )
                            
                            val animatedAlpha by animateFloatAsState(
                                targetValue = if (itemAppeared.value) 1f else 0f,
                                animationSpec = tween(300),
                                label = "alpha"
                            )
                            
                            AnimatedFoodItem(
                                food = food,
                                onClick = {
                                    navController.navigate("food_details/${food.id}/${food.categoryId}")
                                },
                                animatedScale = animatedScale,
                                animatedAlpha = animatedAlpha,
                                currentLanguage = currentLanguage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesRow(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocaleHelper.getSelectedLanguageCode(LocalContext.current)
    val scrollState = rememberScrollState()
    
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(bottom = 8.dp)
        ) {
            categories.forEachIndexed { index, category ->
                val animatedAppearance = remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    delay(100 + (index * 100).toLong())
                    animatedAppearance.value = true
                }
                
                val scale by animateFloatAsState(
                    targetValue = if (animatedAppearance.value) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "categoryScale"
                )
                
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                
                Surface(
                    onClick = { onCategoryClick(category.id) },
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .graphicsLayer { 
                            scaleX = scale * if (isPressed) 0.95f else 1f
                            scaleY = scale * if (isPressed) 0.95f else 1f
                            alpha = scale
                        }
                        .bounceEffect(enabled = animatedAppearance.value, bounceHeight = 3f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shadowElevation = 4.dp,
                    interactionSource = interactionSource
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category icon or mini image
                        if (!category.imageUrl.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = category.imageUrl,
                                    error = rememberAsyncImagePainter(
                                        model = android.R.drawable.ic_menu_gallery
                                    )
                                ),
                                contentDescription = category.getName(currentLanguage),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Text(
                            text = category.getName(currentLanguage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedFoodItem(
    food: Food,
    onClick: () -> Unit,
    animatedScale: Float,
    animatedAlpha: Float,
    currentLanguage: String
) {
    val foodName = food.getName(currentLanguage)
    val description = food.getDescription(currentLanguage)
    
    // Hover animation with bounce
    var isHovered by remember { mutableStateOf(false) }
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hoverScale"
    )
    
    Card(
        onClick = {
            onClick()
            isHovered = !isHovered
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale * hoverScale
                scaleY = animatedScale * hoverScale
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food image with fancy border
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(80.dp)
                    .pulseEffect(isHovered)
            ) {
                Box {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = food.imageUrl,
                            error = rememberAsyncImagePainter(
                                model = android.R.drawable.ic_menu_gallery
                            )
                        ),
                        contentDescription = foodName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Availability indicator
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
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    // Discount badge if applicable
                    if (food.discountPercentage != null && food.discountPercentage > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .bounceEffect(enabled = true, bounceHeight = 2f)
                        ) {
                            Text(
                                text = "-${food.discountPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Food details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price with discount if applicable
                    if (food.discountedPrice != null && food.discountedPrice < food.price) {
                        Column {
                            Text(
                                text = "€${String.format("%.2f", food.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            
                            Text(
                                text = "€${String.format("%.2f", food.discountedPrice)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            
                            // Display discount explanation
                            if (food.discountMessage != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = food.discountMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                // Show limited time indicator if the discount has an end date
                                if (food.discountEndDate != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "⏳ ",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = stringResource(R.string.limited_time_offer),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Add the countdown timer from components
                                    DiscountCountdown(endDate = food.discountEndDate)
                                }
                            } else if (food.discountPercentage != null) {
                                // Fallback generic discount message
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "Save ${food.discountPercentage.toInt()}% for limited time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                // Show limited time indicator if the discount has an end date
                                if (food.discountEndDate != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "⏳ ",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = stringResource(R.string.limited_time_offer),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Add the countdown timer from components
                                    DiscountCountdown(endDate = food.discountEndDate)
                                }
                            }
                        }
                    } else {
                        Column {
                            Text(
                                text = "€${String.format("%.2f", food.price)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Even when not showing a discounted price, check if there's a discount message to display
                            if (food.discountMessage != null && food.discountPercentage != null && food.discountPercentage > 0) {
                                Text(
                                    text = food.discountMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                
                                // Show time limit if applicable
                                if (food.discountEndDate != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "⏳ ",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = stringResource(R.string.limited_time_offer),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Add the countdown timer from components
                                    DiscountCountdown(endDate = food.discountEndDate)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 