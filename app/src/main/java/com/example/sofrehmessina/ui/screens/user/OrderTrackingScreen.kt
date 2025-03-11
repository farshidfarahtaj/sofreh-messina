package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun OrderTrackingScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: OrderViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.id?.let { userId ->
            viewModel.loadOrders(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Orders") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No orders found",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders.sortedByDescending { it.createdAt }) { order ->
                    OrderTrackingCard(order = order)
                }
            }
        }
    }
}

@Composable
fun OrderTrackingCard(
    order: Order,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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
                    text = "Order #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        .format(order.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OrderStatusTracker(status = order.status)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Order Total:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$${String.format("%.2f", order.total)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.items.sumOf { it.quantity }} items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderStatusTracker(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    val statusList = OrderStatus.entries.toList()
    val currentIndex = statusList.indexOf(status)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        statusList.forEachIndexed { index, orderStatus ->
            OrderStatusDot(
                status = orderStatus,
                isCompleted = index <= currentIndex,
                isFirst = index == 0,
                isLast = index == statusList.lastIndex
            )
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OrderStatusDot(
    status: OrderStatus,
    isCompleted: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Gray
            ) {}
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = status.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
} 