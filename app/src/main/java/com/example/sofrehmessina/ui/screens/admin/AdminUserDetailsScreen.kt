package com.example.sofrehmessina.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailsScreen(
    userId: String,
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val currentUser by authViewModel.currentUser.collectAsState()
        var selectedUser by remember { mutableStateOf<User?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        // Dialog states
        var showRoleDialog by remember { mutableStateOf(false) }
        var showResetPasswordDialog by remember { mutableStateOf(false) }
        var showDisableAccountDialog by remember { mutableStateOf(false) }
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        
        // Fetch user data
        LaunchedEffect(userId) {
            viewModel.loadUser(userId)
        }
        
        // Observe user data
        LaunchedEffect(Unit) {
            viewModel.selectedUser.collect { user ->
                selectedUser = user
                isLoading = false
            }
        }
        
        // Observe error
        LaunchedEffect(Unit) {
            viewModel.error.collect { errorMessage ->
                error = errorMessage
                isLoading = false
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
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (error != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadUser(userId) }) {
                                Text("Retry")
                            }
                        }
                    } else if (selectedUser == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "User not found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    } else {
                        // User details content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // User Profile Header
                            UserProfileHeader(user = selectedUser!!)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Contact Information
                            UserContactInfo(user = selectedUser!!)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Account Management
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
                                    
                                    // Change user role button
                                    OutlinedButton(
                                        onClick = { showRoleDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SupervisorAccount,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text("Change User Role")
                                    }
                                    
                                    // Quick promote to admin button (only for non-admin users)
                                    if (selectedUser != null && selectedUser!!.role != UserRole.ADMIN) {
                                        val buttonScope = rememberCoroutineScope()
                                        var isPromoting by remember { mutableStateOf(false) }
                                        
                                        Button(
                                            onClick = {
                                                isPromoting = true
                                                buttonScope.launch {
                                                    viewModel.promoteToAdmin(selectedUser!!.id).onSuccess {
                                                        selectedUser = selectedUser!!.copy(role = UserRole.ADMIN)
                                                        viewModel.loadUser(selectedUser!!.id)
                                                        isPromoting = false
                                                    }.onFailure {
                                                        isPromoting = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            enabled = !isPromoting
                                        ) {
                                            if (isPromoting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .padding(end = 8.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.AdminPanelSettings,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                            Text("Promote to Admin")
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Reset Password Button
                                    Button(
                                        onClick = { showResetPasswordDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = "Reset Password",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset Password")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Disable Account Button
                                    Button(
                                        onClick = { showDisableAccountDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Order History
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
                                        text = "User Activity",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Placeholder for order history (will be implemented later)
                                    Text(
                                        text = "Order History will be displayed here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Button(
                                        onClick = { 
                                            // TODO: Load user orders
                                        },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("View Orders")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Role Dialog
        if (showRoleDialog && selectedUser != null) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = { Text("Change User Role") },
                text = {
                    Column {
                        Text("Current Role: ${selectedUser!!.role}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select new role:")
                        
                        UserRole.values().forEach { role ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUser!!.role == role,
                                    onClick = {
                                        viewModel.updateUserRole(selectedUser!!.id, role)
                                        showRoleDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(role.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRoleDialog = false }) {
                        Text("Cancel")
                    }
                },
                dismissButton = {}
            )
        }
        
        // Reset Password Dialog
        if (showResetPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showResetPasswordDialog = false },
                title = { Text("Reset User Password") },
                text = {
                    Text("This will send a password reset email to the user. Continue?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.sendPasswordResetEmail(selectedUser!!.email)
                            showResetPasswordDialog = false
                        }
                    ) {
                        Text("Send Reset Email")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Disable Account Dialog
        if (showDisableAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDisableAccountDialog = false },
                title = { Text("Disable User Account") },
                text = {
                    Text("This will disable the user's account. This action cannot be undone. Continue?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.disableUserAccount(selectedUser!!.id)
                            showDisableAccountDialog = false
                        }
                    ) {
                        Text("Disable Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisableAccountDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun UserProfileHeader(user: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // User avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (user.profilePictureUrl.isNotEmpty()) {
                // Image implementation would go here
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // User name
        Text(
            text = if (user.name.isNotEmpty() || user.familyName.isNotEmpty()) 
                "${user.name} ${user.familyName}" 
            else 
                "Anonymous User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // User role
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when (user.role) {
                        UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                        UserRole.USER -> MaterialTheme.colorScheme.secondary
                        UserRole.GUEST -> MaterialTheme.colorScheme.error
                    }.copy(alpha = 0.2f)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = user.role.name,
                style = MaterialTheme.typography.labelLarge,
                color = when (user.role) {
                    UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                    UserRole.USER -> MaterialTheme.colorScheme.secondary
                    UserRole.GUEST -> MaterialTheme.colorScheme.error
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // User ID
        Text(
            text = "ID: ${user.id}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserContactInfo(user: User) {
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
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email
            ContactItem(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email.ifEmpty { "Not provided" }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Phone
            ContactItem(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = user.phone.ifEmpty { "Not provided" }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Address
            ContactItem(
                icon = Icons.Default.Home,
                label = "Address",
                value = user.address.ifEmpty { "Not provided" }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Postal Code
            ContactItem(
                icon = Icons.Default.LocationOn,
                label = "Postal Code",
                value = user.postalCode.ifEmpty { "Not provided" }
            )
        }
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
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