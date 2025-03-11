package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.ui.components.AdminOrderStatusChip
import com.example.sofrehmessina.ui.viewmodel.AdminOrderViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun AdminOrdersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminOrderViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val orders by viewModel.filteredOrders.collectAsState()
        val selectedStatus by viewModel.statusFilter.collectAsState()
        var selectedOrder by remember { mutableStateOf<Order?>(null) }
        var showStatusDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.loadAllOrders()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Orders") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // Status Filter Chips
                OrderStatusFilter(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { viewModel.setStatusFilter(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders) { order ->
                        AdminOrderCard(
                            order = order,
                            currencyManager = currencyManager,
                            onClick = { onNavigateToOrderDetails(order.id) },
                            onStatusUpdate = {
                                selectedOrder = order
                                showStatusDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showStatusDialog && selectedOrder != null) {
            OrderStatusDialog(
                currentStatus = selectedOrder!!.status,
                onStatusSelected = { newStatus ->
                    viewModel.updateOrderStatus(selectedOrder!!.id, newStatus)
                    showStatusDialog = false
                    selectedOrder = null
                },
                onDismiss = {
                    showStatusDialog = false
                    selectedOrder = null
                }
            )
        }
    }
}

@Composable
fun OrderStatusFilter(
    selectedStatus: OrderStatus?,
    onStatusSelected: (OrderStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onStatusSelected(null) },
            label = { Text("All") }
        )
        OrderStatus.entries.forEach { status ->
            FilterChip(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                label = { Text(status.name) }
            )
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    currencyManager: CurrencyManager,
    onClick: () -> Unit,
    onStatusUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id.takeLast(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onStatusUpdate) {
                    Text(order.status.name)
                }
            }

            Text(
                text = "Items: ${order.items.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = currencyManager.formatPrice(order.total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun OrderStatusDialog(
    currentStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Order Status") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrderStatus.entries.forEach { status ->
                    if (status != currentStatus) {
                        TextButton(
                            onClick = { onStatusSelected(status) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminOrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit,
    onStatusUpdate: () -> Unit,
    dateFormat: SimpleDateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) },
    currencyManager: CurrencyManager = rememberCurrencyManager(),
    viewModel: AdminOrderViewModel = hiltViewModel()
) {
    // Load customer data when dialog is shown
    LaunchedEffect(order.userId) {
        viewModel.loadCustomerDataForOrdersScreen(order.userId)
    }

    // Get customer data state
    val customerData by viewModel.orderScreenCustomerData.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Order Details")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminOrderStatusChip(status = order.status)
                    TextButton(onClick = onStatusUpdate) {
                        Text("Update Status")
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Order #${order.id.takeLast(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Show customer name and phone if available
                    if (customerData[order.userId] != null) {
                        val customer = customerData[order.userId]
                        Text(
                            text = "Customer: ${customer?.name} ${customer?.familyName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Phone: ${customer?.phone}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Customer: ${order.userId}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        text = "Address: ${order.address}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column {
                    Text(
                        text = "Items",
                        style = MaterialTheme.typography.titleSmall,
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currencyManager.formatPrice(item.food.price),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.notes.isNotEmpty()) {
                            Text(
                                text = "Note: ${item.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencyManager.formatPrice(order.total),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (order.notes.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Special Instructions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Special Instructions: ${order.notes}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 