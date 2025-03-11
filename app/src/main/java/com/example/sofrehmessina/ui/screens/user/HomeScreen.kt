package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.navigation.Screen
import androidx.navigation.NavController
import com.example.sofrehmessina.R
import com.example.sofrehmessina.ui.components.AppLogo
import com.example.sofrehmessina.ui.components.BannerSlideshow
import com.example.sofrehmessina.ui.components.CachedFirestoreImage
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.components.SofrehDrawer
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.BannerViewModel
import com.example.sofrehmessina.ui.viewmodel.CategoryViewModel
import com.example.sofrehmessina.util.FirestoreImageUtils
import com.example.sofrehmessina.util.LocaleHelper
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.sofrehmessina.ui.utils.AnimationUtils.bounceEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.floatingEffect
import com.example.sofrehmessina.ui.utils.AnimationUtils.pulseEffect
import com.example.sofrehmessina.ui.utils.UiAnimations
import com.example.sofrehmessina.ui.viewmodel.SharedImageViewModel
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import java.util.*
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    categoryViewModel: CategoryViewModel = hiltViewModel(),
    bannerViewModel: BannerViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val currentUser by authViewModel.currentUser.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading by categoryViewModel.isLoading.collectAsState()
    val error by categoryViewModel.error.collectAsState()
    val banners by bannerViewModel.banners.collectAsState()
    val cartItemCount by cartViewModel.cartItemCount.collectAsState()
    
    // Animation states
    var animatedAppBarHeight by remember { mutableStateOf(64.dp) }
    val density = LocalDensity.current
    
    // Get current language to force recomposition when language changes
    val context = LocalContext.current
    val currentLanguage = LocaleHelper.getSelectedLanguageCode(context)
    
    // Load categories when screen launches
    LaunchedEffect(currentLanguage) {
        Log.d("HomeScreen", "Loading categories with language: $currentLanguage")
        categoryViewModel.loadCategories()
        
        // Use fixed app bar height for consistency across devices
        animatedAppBarHeight = 76.dp  // Slightly increased fixed height for better vertical spacing
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
                // Fixed top bar with better styling
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp,
                    modifier = Modifier.height(animatedAppBarHeight)
                ) {
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    // Calculate adaptive sizes based on screen width
                    val logoSize = if (screenWidth.value < 360f) 36.dp else 44.dp
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        // Navigation menu icon - placed at start
                        IconButton(
                            onClick = { 
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .pulseEffect(true)
                        ) {
                            Icon(
                                Icons.Default.Menu, 
                                contentDescription = stringResource(R.string.menu),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Logo and title - moved to corner (after nav icon)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 56.dp) // Adjust spacing to position after the menu icon
                        ) {
                            // Logo with fixed size
                            AppLogo(
                                modifier = Modifier
                                    .size(logoSize)
                                    .clip(CircleShape)
                                    .shadow(4.dp, CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Title with adjusted sizes and spacing
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // First line - Sofreh
                                Text(
                                    stringResource(R.string.app_title_first_line),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (screenWidth.value < 360f) 16.sp else 20.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // Second line - Persian Foods
                                Text(
                                    stringResource(R.string.app_title_second_line),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (screenWidth.value < 360f) 12.sp else 14.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        
                        // Action icons - placed at end
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                        ) {
                            // Cart Icon with badge
                            var cartPulsing by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(2000)
                                cartPulsing = true
                                delay(4000)
                                cartPulsing = false
                            }
                            
                            Box(contentAlignment = Alignment.Center) {
                                IconButton(
                                    onClick = { navController.navigate(Screen.Cart.route) },
                                    modifier = Modifier.pulseEffect(cartPulsing || cartItemCount > 0)
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart, 
                                        contentDescription = stringResource(R.string.cart),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Show cart badge if count > 0
                                if (cartItemCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-6).dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                            .bounceEffect(enabled = true, bounceHeight = 2f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                                            color = MaterialTheme.colorScheme.onError,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            // Chat/Contact Icon
                            IconButton(
                                onClick = { 
                                    val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sofrehmessina"))
                                    context.startActivity(telegramIntent)
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = stringResource(R.string.contact_us),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.floatingEffect()
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (error != null) {
                    ErrorDialog(
                        error = error.toString(),
                        onDismiss = { categoryViewModel.clearError() }
                    )
                } else {
                    // Create staggered animation for content
                    val animatedItems = categories.mapIndexed { _, _ -> 
                        remember { Animatable(0f) } 
                    }
                    
                    LaunchedEffect(categories) {
                        animatedItems.forEachIndexed { index, animatable ->
                            launch {
                                delay(100L * index)
                                animatable.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(300, easing = EaseOutBack)
                                )
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Animated Banner Slideshow with enhanced effects
                        AnimatedVisibility(
                            visible = banners.isNotEmpty(),
                            enter = fadeIn() + slideInVertically { with(density) { -40.dp.roundToPx() } },
                            exit = fadeOut() + slideOutVertically { with(density) { -40.dp.roundToPx() } }
                        ) {
                            BannerSlideshow(
                                banners = banners,
                                onBannerClick = { banner ->
                                    Log.d("HomeScreen", "Banner clicked: ${banner.title}")
                                    if (banner.actionUrl.isNotEmpty()) {
                                        val category = categories.find { it.id == banner.actionUrl }
                                        if (category != null) {
                                            navController.navigate("category_details/${category.id}")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shadow(8.dp, RoundedCornerShape(12.dp))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Food Categories Header
                        Text(
                            text = stringResource(R.string.food_categories),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = categories,
                                key = { "${it.id}_${currentLanguage}" }
                            ) { category ->
                                val index = categories.indexOf(category)
                                val animatedValue = animatedItems.getOrNull(index)?.value ?: 1f
                                
                                HomeCategoryCard(
                                    category = category,
                                    onClick = {
                                        navController.navigate("category_details/${category.id}")
                                    },
                                    animatedValue = animatedValue
                                )
                            }
                        }
                        
                        // Add some spacing at the bottom
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCategoryCard(
    category: Category,
    onClick: () -> Unit,
    animatedValue: Float = 1f,
    modifier: Modifier = Modifier
) {
    // Get current language for translation
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguageCode(context)
    
    // Use the translated name from the category based on current language
    val categoryName = category.getName(currentLang)
    
    // Interactive animations
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Remember if this card has been hovered to trigger animation
    var hasBeenViewed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(Random().nextInt(2000).toLong())
        hasBeenViewed = true
    }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Apply entrance animation
                alpha = animatedValue
                scaleX = 0.8f + (0.2f * animatedValue)
                scaleY = 0.8f + (0.2f * animatedValue)
                
                // Apply press animation
                if (isPressed) {
                    scaleX *= 0.95f
                    scaleY *= 0.95f
                }
            }
            .bounceEffect(hasBeenViewed, 5f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            focusedElevation = 6.dp
        ),
        border = BorderStroke(
            width = 1.dp, 
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                )
            )
        ),
        interactionSource = interactionSource
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (!category.imageUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = category.imageUrl,
                            error = rememberAsyncImagePainter(
                                model = android.R.drawable.ic_menu_gallery
                            )
                        ),
                        contentDescription = categoryName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Add a gradient overlay at the bottom for better text visibility
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
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
                }
            }
            
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
} 