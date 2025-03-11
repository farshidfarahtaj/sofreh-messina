package com.example.sofrehmessina.ui.screens.admin

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.ui.components.OrderStatusChip
import com.example.sofrehmessina.ui.viewmodel.AdminOrderViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "AdminOrderDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailsScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminOrderViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        var showConfirmDialog by remember { mutableStateOf(false) }
        var showDeclineDialog by remember { mutableStateOf(false) }
        
        // Debug log
        Log.d(TAG, "Loading order details for ID: $orderId")
        
        LaunchedEffect(orderId) {
            Log.d(TAG, "LaunchedEffect triggered, loading order with ID: $orderId")
            viewModel.loadOrderDetails(orderId)
        }
        
        val orderDetails by viewModel.selectedOrder.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        val customerData by viewModel.customerData.collectAsState()
        
        // Debug logs
        LaunchedEffect(orderDetails) {
            Log.d(TAG, "Order details updated: ${orderDetails != null}")
            orderDetails?.let {
                Log.d(TAG, "Order loaded: #${it.id}, status: ${it.status}, items: ${it.items.size}")
            }
        }
        
        LaunchedEffect(error) {
            error?.let {
                Log.e(TAG, "Error loading order: ${it.message}", it)
            }
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Order Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (error != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error loading order",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${error?.message ?: "Unknown error"}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadOrderDetails(orderId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Try Again")
                        }
                    }
                } else if (orderDetails != null) {
                    val order = orderDetails!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Order ID and Date
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Order #${order.id.takeLast(6).uppercase()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Placed on ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(order.createdAt)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Status Chip
                                val (statusColor, statusIcon) = when (order.status) {
                                    OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiary to Icons.Default.AccessTime
                                    OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primary to Icons.Default.CheckCircle
                                    OrderStatus.PREPARING -> MaterialTheme.colorScheme.secondary to Icons.Default.Restaurant
                                    OrderStatus.READY -> MaterialTheme.colorScheme.secondary to Icons.Default.LocalShipping
                                    OrderStatus.DELIVERED -> MaterialTheme.colorScheme.primary to Icons.Default.DoneAll
                                    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to Icons.Default.Cancel
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = null,
                                        tint = statusColor
                                    )
                                    Text(
                                        text = order.status.toDisplayName(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Order Items
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Order Items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                order.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.quantity}x ${item.food.name}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = currencyManager.formatPrice(item.food.price * item.quantity),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    if (item.notes.isNotEmpty()) {
                                        Text(
                                            text = "Note: ${item.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                                        )
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currencyManager.formatPrice(order.total),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Customer Details
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Delivery Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Display customer name if available
                                if (customerData != null) {
                                    Text(
                                        text = "Customer: ${customerData?.name} ${customerData?.familyName}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Phone: ${customerData?.phone}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                } else {
                                    Text(
                                        text = "Customer: ${order.userId}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Address: ${order.address}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                if (order.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Special Instructions:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = order.notes,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        
                        // Only show action buttons if the order is pending
                        if (order.status == OrderStatus.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDeclineDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Decline")
                                }
                                
                                Button(
                                    onClick = { showConfirmDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Order not found",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
            }
        }
        
        // Confirm Order Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Order") },
                text = { Text("Are you sure you want to confirm this order?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
                            showConfirmDialog = false
                        }
                    ) {
                        Text("Yes, Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Decline Order Dialog
        if (showDeclineDialog) {
            AlertDialog(
                onDismissRequest = { showDeclineDialog = false },
                title = { Text("Decline Order") },
                text = { Text("Are you sure you want to decline this order? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED)
                            showDeclineDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Yes, Decline")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeclineDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 