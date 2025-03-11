package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.di.currencyManager
import com.example.sofrehmessina.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    
    // Function to refresh orders
    val refreshOrders = {
        if (userId.isNotEmpty()) {
            viewModel.loadOrders(userId)
        }
    }

    LaunchedEffect(userId) {
        refreshOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.order_history)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = refreshOrders,
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_orders_found),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = refreshOrders) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(orders.sortedByDescending { it.createdAt }) { order ->
                            OrderHistoryCard(
                                order = order,
                                onClick = { selectedOrder = order }
                            )
                        }
                    }
                }
            }
        }
    }
    
    selectedOrder?.let { order ->
        OrderDetailsDialog(
            order = order,
            onDismiss = { selectedOrder = null }
        )
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencyManager = remember { context.currencyManager() }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.order_number, order.id.take(8)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(order.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Order Items Summary
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (order.items.isNotEmpty()) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Use the food name if available, otherwise use the ID
                            val displayText = if (item.food.name.isNotEmpty()) {
                                stringResource(R.string.item_quantity, item.quantity, item.food.name)
                            } else {
                                stringResource(R.string.order_item_quantity, item.quantity, item.food.id.take(8))
                            }
                            
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currencyManager.formatPrice(item.food.price * item.quantity),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Fallback if items list is empty or not accessible
                    Text(
                        text = stringResource(R.string.order_items_count, order.items.size, currencyManager.formatPrice(order.total)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.order_total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currencyManager.formatPrice(order.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = when (status) {
            OrderStatus.PENDING -> MaterialTheme.colorScheme.errorContainer
            OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
            OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondaryContainer
            OrderStatus.READY -> MaterialTheme.colorScheme.tertiaryContainer
            OrderStatus.DELIVERED -> MaterialTheme.colorScheme.surfaceVariant
            OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
        }
    ) {
        Text(
            text = stringResource(
                when (status) {
                    OrderStatus.PENDING -> R.string.status_pending
                    OrderStatus.CONFIRMED -> R.string.status_confirmed
                    OrderStatus.PREPARING -> R.string.status_preparing
                    OrderStatus.READY -> R.string.status_ready
                    OrderStatus.DELIVERED -> R.string.status_delivered
                    OrderStatus.CANCELLED -> R.string.status_cancelled
                }
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = when (status) {
                OrderStatus.PENDING -> MaterialTheme.colorScheme.onErrorContainer
                OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimaryContainer
                OrderStatus.PREPARING -> MaterialTheme.colorScheme.onSecondaryContainer
                OrderStatus.READY -> MaterialTheme.colorScheme.onTertiaryContainer
                OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onSurfaceVariant
                OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onError
            }
        )
    }
}

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currencyManager = remember { context.currencyManager() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.order_details),
                    style = MaterialTheme.typography.headlineSmall
                )
                StatusChip(status = order.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Order ID
                Text(
                    text = stringResource(R.string.order_number, order.id.take(8)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Order Date
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(order.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Items - Invoice Details
                Text(
                    text = stringResource(R.string.items),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (order.items.isNotEmpty()) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Use the food name if available, otherwise use the ID
                            val displayText = if (item.food.name.isNotEmpty()) {
                                stringResource(R.string.item_quantity, item.quantity, item.food.name)
                            } else {
                                stringResource(R.string.order_item_quantity, item.quantity, item.food.id.take(8))
                            }
                            
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = currencyManager.formatPrice(item.food.price * item.quantity),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // Show notes if available
                        if (item.notes.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.item_note, item.notes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                            )
                        }
                    }
                } else {
                    // Fallback if items list is empty or not accessible
                    Text(
                        text = stringResource(R.string.order_items_count, order.items.size, currencyManager.formatPrice(order.total)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Order Total with discounts
                if (order.discounts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.subtotal),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = currencyManager.formatPrice(order.subtotal),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.End)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.discount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "-${currencyManager.formatPrice(order.subtotal - order.total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.End)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Final total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.order_total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currencyManager.formatPrice(order.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (order.couponCode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.coupon_applied, order.couponCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Delivery information
                Text(
                    text = stringResource(R.string.delivery_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.address),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = order.address,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.phone),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = order.phone,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (order.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.special_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
} 