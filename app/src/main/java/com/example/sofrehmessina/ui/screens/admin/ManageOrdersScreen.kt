package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.components.AdminOrderStatusChip
import com.example.sofrehmessina.ui.viewmodel.AdminOrderViewModel
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.navigation.Screen
import kotlinx.coroutines.launch
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrdersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminOrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        var selectedStatus by remember { mutableStateOf<OrderStatus?>(null) }
        var showFilterDialog by remember { mutableStateOf(false) }
        var showStatusDialog by remember { mutableStateOf<String?>(null) }

        val orders by viewModel.filteredOrders.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        // Function to refresh orders
        val refreshOrders = {
            viewModel.loadAllOrders()
        }

        LaunchedEffect(Unit) {
            refreshOrders()
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AdminDrawer(
                    currentUser = currentUser,
                    navController = navController,
                    onLogout = { authViewModel.signOut() },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Manage Orders") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                                        contentDescription = "Refresh Orders"
                                    )
                                }
                            }
                            
                            // Filter button
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter Orders")
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
                        // Order Statistics
                        OrderStatistics(orders = orders)

                        // Orders List
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
                                        text = "No orders found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = refreshOrders) {
                                        Text("Refresh")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = orders.sortedByDescending { it.createdAt },
                                    key = { it.id }
                                ) { order ->
                                    ManageOrderCard(
                                        order = order,
                                        onStatusUpdate = { orderId ->
                                            showStatusDialog = orderId
                                        },
                                        onClick = {
                                            navController.navigate(Screen.createRoute(Screen.AdminOrderDetails.route, order.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Filter Dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter Orders") },
                text = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedStatus == null,
                                onClick = {
                                    selectedStatus = null
                                    viewModel.setStatusFilter(null)
                                    showFilterDialog = false
                                }
                            )
                            Text("All Orders")
                        }
                        
                        OrderStatus.entries.forEach { status ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedStatus == status,
                                    onClick = {
                                        selectedStatus = status
                                        viewModel.setStatusFilter(status)
                                        showFilterDialog = false
                                    }
                                )
                                Text(status.toDisplayName())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Status Update Dialog
        showStatusDialog?.let { orderId ->
            AlertDialog(
                onDismissRequest = { showStatusDialog = null },
                title = { Text("Update Order Status") },
                text = {
                    Column {
                        OrderStatus.entries.forEach { status ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = orders.find { it.id == orderId }?.status == status,
                                    onClick = {
                                        viewModel.updateOrderStatus(orderId, status)
                                        showStatusDialog = null
                                    }
                                )
                                Text(status.toDisplayName())
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatusDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun OrderStatistics(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val pendingCount = orders.count { it.status == OrderStatus.PENDING }
    val confirmedCount = orders.count { it.status == OrderStatus.CONFIRMED }
    val preparingCount = orders.count { it.status == OrderStatus.PREPARING }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Today's Orders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Pending",
                    count = pendingCount,
                    color = MaterialTheme.colorScheme.error
                )
                StatisticItem(
                    label = "Confirmed",
                    count = confirmedCount,
                    color = MaterialTheme.colorScheme.primary
                )
                StatisticItem(
                    label = "Preparing",
                    count = preparingCount,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ManageOrderCard(
    order: Order,
    onStatusUpdate: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
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
                AdminOrderStatusChip(
                    status = order.status,
                    modifier = Modifier.clickable { onStatusUpdate(order.id) }
                )
            }

            Text(
                text = "Items: ${order.items.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencyManager.formatPrice(order.total),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = order.userId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 