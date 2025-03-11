package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.OrderStatus
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.UserManagementState
import com.example.sofrehmessina.ui.viewmodel.UserViewModel
import com.example.sofrehmessina.utils.CurrencyManager
import com.example.sofrehmessina.utils.rememberCurrencyManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED")
fun UserDetailsScreen(
    userId: String,
    navController: NavController,
    onNavigateBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val selectedUser by viewModel.selectedUser.collectAsState()
        val userOrders by viewModel.userOrders.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val managementState by viewModel.managementState.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        var showRoleDialog by remember { mutableStateOf(false) }
        var showPasswordResetDialog by remember { mutableStateOf(false) }
        var showDisableAccountDialog by remember { mutableStateOf(false) }
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        // Fetch user data
        LaunchedEffect(userId) {
            viewModel.loadUser(userId)
        }
        
        // Show success or error snackbar for management actions
        val snackbarHostState = remember { SnackbarHostState() }
        
        LaunchedEffect(managementState) {
            when (managementState) {
                is UserManagementState.Success -> {
                    snackbarHostState.showSnackbar("Operation completed successfully")
                    viewModel.clearManagementState()
                }
                is UserManagementState.Error -> {
                    snackbarHostState.showSnackbar((managementState as UserManagementState.Error).message)
                    viewModel.clearManagementState()
                }
                else -> { /* No action needed */ }
            }
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
                        title = { Text("User Details") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState)
                }
            ) { paddingValues ->
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (selectedUser == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("User not found")
                    }
                } else {
                    // User details content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // User Profile Card
                            UserProfileCard(
                                user = selectedUser!!,
                                onEditRole = { showRoleDialog = true }
                            )
                        }
                        
                        item {
                            // Account Management Card
                            AccountManagementCard(
                                user = selectedUser!!,
                                onResetPassword = { showPasswordResetDialog = true },
                                onDisableAccount = { showDisableAccountDialog = true }
                            )
                        }
                        
                        item {
                            // User Activity Card
                            UserActivityCard(
                                user = selectedUser!!,
                                orders = userOrders,
                                currencyManager = currencyManager
                            )
                        }
                        
                        if (userOrders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Order History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            items(userOrders) { order ->
                                OrderHistoryItem(order = order, currencyManager = currencyManager)
                            }
                        }
                    }
                    
                    // Role Selection Dialog
                    if (showRoleDialog) {
                        RoleSelectionDialog(
                            currentRole = selectedUser!!.role,
                            onDismiss = { showRoleDialog = false },
                            onRoleSelected = { newRole ->
                                viewModel.updateUserRole(selectedUser!!.id, newRole)
                                showRoleDialog = false
                            }
                        )
                    }
                    
                    // Password Reset Dialog
                    if (showPasswordResetDialog) {
                        AlertDialog(
                            onDismissRequest = { showPasswordResetDialog = false },
                            title = { Text("Reset Password") },
                            text = {
                                Text("Send a password reset email to ${selectedUser!!.email}?")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.sendPasswordResetEmail(selectedUser!!.email)
                                        showPasswordResetDialog = false
                                    }
                                ) {
                                    Text("Send")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showPasswordResetDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    
                    // Disable Account Dialog
                    if (showDisableAccountDialog) {
                        AlertDialog(
                            onDismissRequest = { showDisableAccountDialog = false },
                            title = { Text("Disable Account") },
                            text = {
                                Text("Are you sure you want to disable this user account?")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.disableUserAccount(selectedUser!!.id)
                                        showDisableAccountDialog = false
                                    }
                                ) {
                                    Text("Disable")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDisableAccountDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileCard(
    user: User,
    onEditRole: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (user.profilePictureUrl.isNotEmpty()) {
                    // If there's a profile picture, display it
                    // Image implementation would go here
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // User Name
            Text(
                text = if (user.name.isNotEmpty() || user.familyName.isNotEmpty()) 
                    "${user.name} ${user.familyName}" 
                else 
                    "Anonymous User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // User Role with Edit Button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (user.role) {
                                UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                                UserRole.USER -> MaterialTheme.colorScheme.secondary
                                UserRole.GUEST -> MaterialTheme.colorScheme.error
                            }.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = user.role.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (user.role) {
                            UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                            UserRole.USER -> MaterialTheme.colorScheme.secondary
                            UserRole.GUEST -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                IconButton(onClick = onEditRole) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Role",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // User Details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                if (user.email.isNotEmpty()) {
                    DetailItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user.email
                    )
                }
                
                if (user.phone.isNotEmpty()) {
                    DetailItem(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = user.phone
                    )
                }
                
                if (user.address.isNotEmpty()) {
                    DetailItem(
                        icon = Icons.Default.LocationOn,
                        label = "Address",
                        value = user.address
                    )
                }
                
                if (user.postalCode.isNotEmpty()) {
                    DetailItem(
                        icon = Icons.Default.Code,
                        label = "Postal Code",
                        value = user.postalCode
                    )
                }
            }
        }
    }
}

@Composable
fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun AccountManagementCard(
    @Suppress("UNUSED_PARAMETER") user: User,
    onResetPassword: () -> Unit,
    onDisableAccount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Account Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reset Password Button
            OutlinedButton(
                onClick = onResetPassword,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "Reset Password",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Password")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Disable Account Button
            OutlinedButton(
                onClick = onDisableAccount,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Disable Account",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disable Account")
            }
        }
    }
}

@Composable
fun UserActivityCard(
    @Suppress("UNUSED_PARAMETER") user: User,
    orders: List<Order>,
    currencyManager: CurrencyManager = rememberCurrencyManager()
) {
    val totalOrders = orders.size
    val completedOrders = orders.count { it.status == OrderStatus.DELIVERED }
    val totalSpent = orders.sumOf { it.total }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Activity Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityStat(
                    value = totalOrders.toString(),
                    label = "Total Orders"
                )
                
                ActivityStat(
                    value = completedOrders.toString(),
                    label = "Completed"
                )
                
                ActivityStat(
                    value = currencyManager.formatPrice(totalSpent),
                    label = "Total Spent"
                )
            }
        }
    }
}

@Composable
fun ActivityStat(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun OrderHistoryItem(order: Order, currencyManager: CurrencyManager) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Order #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = dateFormat.format(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (order.status) {
                            OrderStatus.PENDING -> MaterialTheme.colorScheme.primary
                            OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.secondary
                            OrderStatus.PREPARING -> MaterialTheme.colorScheme.tertiary
                            OrderStatus.READY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                            OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        }.copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = order.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (order.status) {
                        OrderStatus.PENDING -> MaterialTheme.colorScheme.primary
                        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.secondary
                        OrderStatus.PREPARING -> MaterialTheme.colorScheme.tertiary
                        OrderStatus.READY -> MaterialTheme.colorScheme.tertiary
                        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary
                        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = currencyManager.formatPrice(order.total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RoleSelectionDialog(
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change User Role") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select a new role for this user:")
                
                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (role == currentRole) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onRoleSelected(role) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = role == currentRole,
                            onClick = { onRoleSelected(role) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = role.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = when (role) {
                                    UserRole.ADMIN -> "Full access to admin panel and all features"
                                    UserRole.USER -> "Regular user with standard permissions"
                                    UserRole.GUEST -> "Limited access, browsing only"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
} 