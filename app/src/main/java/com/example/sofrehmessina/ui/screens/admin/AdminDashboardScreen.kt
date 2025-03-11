package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.TimeRange
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.components.AppLogo
import com.example.sofrehmessina.ui.components.OrderStatusChip
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.viewmodel.AdminDashboardViewModel
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

data class DashboardMetric(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val metrics by viewModel.metrics.collectAsState()
        val recentOrders by viewModel.recentOrders.collectAsState()
        val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.loadDashboardData()
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
                        title = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppLogo(modifier = Modifier.size(40.dp))
                                Text("Admin Dashboard")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate(Screen.AdminOrders.route) }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Orders")
                            }
                            IconButton(onClick = { navController.navigate(Screen.AdminFood.route) }) {
                                Icon(Icons.Default.Restaurant, contentDescription = "Menu")
                            }
                            IconButton(onClick = { navController.navigate(Screen.AdminCategories.route) }) {
                                Icon(Icons.Default.Category, contentDescription = "Categories")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TimeRangeSelector(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { viewModel.updateTimeRange(it) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        MetricsGrid(metrics = metrics)
                    }

                    // Add admin navigation cards
                    item {
                        Text(
                            text = "Admin Tools",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavigationCard(
                                title = "Manage Categories",
                                icon = Icons.Default.Category,
                                onClick = { navController.navigate(Screen.AdminCategories.route) },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NavigationCard(
                                title = "Manage Food",
                                icon = Icons.Default.Restaurant,
                                onClick = { navController.navigate(Screen.AdminFood.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavigationCard(
                                title = "Manage Orders",
                                icon = Icons.AutoMirrored.Filled.List,
                                onClick = { navController.navigate(Screen.AdminOrders.route) },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NavigationCard(
                                title = "Manage Users",
                                icon = Icons.Default.People,
                                onClick = { navController.navigate(Screen.AdminUsers.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavigationCard(
                                title = "Manage Banners",
                                icon = Icons.Default.Image,
                                onClick = { navController.navigate(Screen.AdminBanners.route) },
                                modifier = Modifier.weight(1f)
                            )
                            
                            NavigationCard(
                                title = "Manage Discounts",
                                icon = Icons.Default.Percent,
                                onClick = { navController.navigate(Screen.AdminDiscounts.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                        
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavigationCard(
                                title = "Coupon Discount",
                                icon = Icons.Default.CardGiftcard,
                                onClick = { navController.navigate(Screen.AdminCouponManagement.route) },
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Empty space for alignment but with a proper card placeholder
                            Box(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Recent Orders",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(recentOrders) { order ->
                        OrderSummaryCard(
                            order = order,
                            onClick = { navController.navigate(Screen.AdminOrders.route) },
                            currencyManager = currencyManager
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.entries.forEach { range ->
                FilterChip(
                    selected = range == selectedRange,
                    onClick = { onRangeSelected(range) },
                    label = {
                        Text(
                            text = range.toDisplayName(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(
    metrics: List<DashboardMetric>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowMetrics.forEach { metric ->
                    MetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty space if the row has only one metric
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    metric: DashboardMetric,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = metric.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = metric.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderSummaryCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    
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
                OrderStatusChip(status = order.status)
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
            Text(
                text = dateFormat.format(order.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NavigationCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
} 