package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.OrderStatusChip
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.OrderViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import com.example.sofrehmessina.ui.components.KeyboardAwareDialogContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("unused")
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    orderViewModel: OrderViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val recentOrders by orderViewModel.recentOrders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            orderViewModel.loadRecentOrders(userId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            KeyboardAwareLayout(
                modifier = modifier.padding(paddingValues),
                scrollState = scrollState
            ) {
                // Profile Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "${currentUser?.name} ${currentUser?.familyName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
                
                // Contact Information
                ProfileSection(
                    title = "Contact Information",
                    items = listOf(
                        "Phone" to (currentUser?.phone ?: "Not provided"),
                        "Address" to (currentUser?.address ?: "Not provided"),
                        "Postal Code" to (currentUser?.postalCode ?: "Not provided")
                    )
                )
                
                // Recent Orders
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Orders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            TextButton(
                                onClick = { navController.navigate("orders") }
                            ) {
                                Text("View All")
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "View All Orders"
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (recentOrders.isNotEmpty()) {
                            recentOrders.take(3).forEach { order: Order ->
                                RecentOrderItem(
                                    order = order,
                                    onClick = { navController.navigate("order_details/${order.id}") }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent orders",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                // Actions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { navController.navigate("settings") },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null
                            )
                        }
                        
                        HorizontalDivider()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { authViewModel.signOut() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    if (showEditProfileDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, familyName, phone, address, postalCode ->
                authViewModel.updateProfile(name, familyName, phone, address, postalCode)
                showEditProfileDialog = false
            }
        )
    }
}

@Composable
fun ProfileSection(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.width(100.dp)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (label != items.last().first) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun RecentOrderItem(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Order #${order.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(order.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${order.items.size} items • $${String.format(Locale.US, "%.2f", order.total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            OrderStatusChip(status = order.status)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Order Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (name: String, familyName: String, phone: String, address: String, postalCode: String) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var familyName by remember { mutableStateOf(user?.familyName ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var address by remember { mutableStateOf(user?.address ?: "") }
    var postalCode by remember { mutableStateOf(user?.postalCode ?: "") }
    
    // Create a scroll state for auto-scrolling behavior
    val dialogScrollState = rememberScrollState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            // Replace Column with KeyboardAwareDialogContent for better stability during typing
            KeyboardAwareDialogContent(
                scrollState = dialogScrollState
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoScrollOnFocus(dialogScrollState),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = familyName,
                    onValueChange = { familyName = it },
                    label = { Text("Family Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoScrollOnFocus(dialogScrollState),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoScrollOnFocus(dialogScrollState),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoScrollOnFocus(dialogScrollState),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { postalCode = it },
                    label = { Text("Postal Code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autoScrollOnFocus(dialogScrollState),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, familyName, phone, address, postalCode) },
                enabled = name.isNotBlank() && familyName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 