package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.CartItem
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.util.LocaleHelper
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    cartViewModel: CartViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    var showClearCartDialog by remember { mutableStateOf(false) }
    
    // Get the current language
    val context = LocalContext.current
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }
    
    // Update the current language whenever the composition is recomposed
    LaunchedEffect(Unit) {
        currentLanguage.value = LocaleHelper.getSelectedLanguageCode(context)
    }

    // Calculate cart totals
    val subtotal = cartItems.sumOf { it.food.price * it.quantity }
    val finalTotal = cartItems.sumOf { 
        val itemPrice = if (it.food.discountPercentage != null && it.food.discountPercentage > 0) {
            val discountedPrice = it.food.discountedPrice ?: 
                (it.food.price * (1 - it.food.discountPercentage / 100.0))
            discountedPrice
        } else {
            it.food.price
        }
        itemPrice * it.quantity 
    }
    val totalDiscount = subtotal - finalTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.your_cart)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { showClearCartDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_cart)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (cartItems.isEmpty()) {
                // Empty cart message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.cart_empty),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.add_items_to_cart),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Cart items list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(cartItems) { cartItem ->
                            CartItemCard(
                                cartItem = cartItem,
                                onIncreaseQuantity = { cartViewModel.updateQuantity(cartItem.food.id, cartItem.quantity + 1) },
                                onDecreaseQuantity = { cartViewModel.updateQuantity(cartItem.food.id, cartItem.quantity - 1) },
                                onRemove = { cartViewModel.removeFromCart(cartItem.food.id) },
                                currencyManager = currencyManager
                            )
                        }
                    }
                    
                    // Cart summary and checkout button
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Savings row
                            if (totalDiscount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.savings),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        text = "-${currencyManager.formatPrice(totalDiscount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                            
                            // Subtotal row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.subtotal),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = currencyManager.formatPrice(subtotal),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Discount row
                            if (totalDiscount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.discount),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "-${currencyManager.formatPrice(totalDiscount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            
                            // Total row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.total),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currencyManager.formatPrice(finalTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDiscount > 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Force refresh the cart before navigating to checkout
                                    cartViewModel.forceRefreshCart()
                                    onNavigateToCheckout()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = cartItems.isNotEmpty()
                            ) {
                                Text(
                                    text = stringResource(R.string.proceed_to_checkout),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            
                            // Add login button for guests if needed
                            if (cartViewModel.isUserGuest()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onNavigateToLogin,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.sign_in_or_register),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.login_to_checkout),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearCartDialog) {
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            title = { Text(stringResource(R.string.clear_cart)) },
            text = { Text(stringResource(R.string.clear_cart_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        cartViewModel.clearCart()
                        showClearCartDialog = false
                    }
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun CartItemCard(
    cartItem: CartItem,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Get the current language
    val context = LocalContext.current
    val currentLanguage = remember { mutableStateOf(LocaleHelper.getSelectedLanguageCode(context)) }
    
    // Update the current language whenever the composition is recomposed
    LaunchedEffect(Unit) {
        currentLanguage.value = LocaleHelper.getSelectedLanguageCode(context)
    }

    // Calculate the discounted price if applicable
    val hasDiscount = cartItem.food.discountPercentage != null && cartItem.food.discountPercentage > 0
    val discountedPrice = if (hasDiscount) {
        cartItem.food.discountedPrice ?: (cartItem.food.price * (1 - cartItem.food.discountPercentage!! / 100.0))
    } else {
        cartItem.food.price
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food image with discount badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (!cartItem.food.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = cartItem.food.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Discount badge
                if (hasDiscount) {
                    Surface(
                        color = Color(0xFFE53935), // Red color
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${cartItem.food.discountPercentage?.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Item name and price
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = cartItem.food.getName(currentLanguage.value),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Price display
                        if (hasDiscount) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Original price with strikethrough
                                Text(
                                    text = currencyManager.formatPrice(cartItem.food.price),
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                // Discounted price
                                Text(
                                    text = currencyManager.formatPrice(discountedPrice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                text = currencyManager.formatPrice(cartItem.food.price),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Remove button
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Quantity controls and price
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (cartItem.quantity > 1) onDecreaseQuantity() },
                            enabled = cartItem.quantity > 1,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Text(
                            text = cartItem.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = onIncreaseQuantity,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Total price for this item
                    Text(
                        text = if (hasDiscount) {
                            currencyManager.formatPrice(discountedPrice * cartItem.quantity)
                        } else {
                            currencyManager.formatPrice(cartItem.food.price * cartItem.quantity)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDiscount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Notes if any
                if (!cartItem.notes.isNullOrEmpty()) {
                    Text(
                        text = "Note: ${cartItem.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
