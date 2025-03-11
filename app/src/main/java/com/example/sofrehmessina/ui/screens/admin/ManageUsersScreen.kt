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
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.ui.components.AdminDrawer
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import com.example.sofrehmessina.navigation.Screen
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: UserViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for admin screens, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val users by viewModel.filteredUsers.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val error by viewModel.error.collectAsState()
        val currentUser by authViewModel.currentUser.collectAsState()
        
        var searchQuery by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf<UserRole?>(null) }
        var showFilterDialog by remember { mutableStateOf(false) }
        
        // Drawer state
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Load users when screen is first displayed
        LaunchedEffect(Unit) {
            // First try with the current cached user data
            if (currentUser?.role == UserRole.ADMIN) {
                viewModel.loadAllUsers()
            } else {
                // Get the current Firebase user ID
                val userId = authViewModel.getFirebaseAuth().currentUser?.uid
                if (userId != null) {
                    // Use our new method to force a direct Firestore check of admin status
                    authViewModel.forceAdminStatusCheck(userId) { isAdmin ->
                        if (isAdmin) {
                            // User is confirmed as admin, load the users
                            viewModel.loadAllUsers()
                        } else {
                            // User is definitely not an admin, show error
                            viewModel.setError("You don't have permission to view this page. Admin access required.")
                        }
                    }
                } else {
                    // No user is logged in
                    viewModel.setError("You must be logged in with admin privileges to view this page.")
                }
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
                        title = { Text("Manage Users") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter Users")
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
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            viewModel.searchUsers(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search users...") },
                        leadingIcon = { 
                            Icon(Icons.Default.Search, contentDescription = "Search") 
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    viewModel.searchUsers("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // User statistics card
                    UserStatistics(users)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Current filter chip if applied
                    if (selectedRole != null) {
                        FilterChip(
                            selected = true,
                            onClick = { selectedRole = null; viewModel.filterUsersByRole(null) },
                            label = { Text("Role: ${selectedRole.toString().lowercase().replaceFirstChar { it.uppercase() }}") },
                            modifier = Modifier.padding(horizontal = 16.dp),
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear filter") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Content
                    if (isLoading) {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else if (error != null) {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = "Error: $error",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else if (users.isEmpty()) {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = if (searchQuery.isEmpty() && selectedRole == null) 
                                    "No users found" 
                                else 
                                    "No users match the current filters",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        // Users list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(users) { user ->
                                UserCard(
                                    user = user,
                                    onClick = {
                                        navController.navigate(
                                            Screen.createRoute(Screen.AdminUserDetails.route, user.id)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Filter dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter Users") },
                text = {
                    Column {
                        Text("Filter by role:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        UserRole.values().forEach { role ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedRole = role
                                        viewModel.filterUsersByRole(role)
                                        showFilterDialog = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRole == role,
                                    onClick = { 
                                        selectedRole = role
                                        viewModel.filterUsersByRole(role)
                                        showFilterDialog = false
                                    }
                                )
                                Text(
                                    text = role.toString().lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        
                        // Option to clear filter
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedRole = null
                                    viewModel.filterUsersByRole(null)
                                    showFilterDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == null,
                                onClick = { 
                                    selectedRole = null
                                    viewModel.filterUsersByRole(null)
                                    showFilterDialog = false
                                }
                            )
                            Text(
                                text = "All Users",
                                modifier = Modifier.padding(start = 8.dp)
                            )
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
    }
}

@Composable
fun UserStatistics(users: List<User>) {
    val totalUsers = users.size
    val adminCount = users.count { it.role == UserRole.ADMIN }
    val regularUserCount = users.count { it.role == UserRole.USER }
    val guestCount = users.count { it.role == UserRole.GUEST }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "User Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    count = totalUsers,
                    label = "Total",
                    color = MaterialTheme.colorScheme.primary
                )
                
                StatItem(
                    count = adminCount,
                    label = "Admins",
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                StatItem(
                    count = regularUserCount,
                    label = "Users",
                    color = MaterialTheme.colorScheme.secondary
                )
                
                StatItem(
                    count = guestCount,
                    label = "Guests",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar or icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        when (user.role) {
                            UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                            UserRole.USER -> MaterialTheme.colorScheme.secondary
                            UserRole.GUEST -> MaterialTheme.colorScheme.error
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (user.role) {
                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                        UserRole.USER -> Icons.Default.Person
                        UserRole.GUEST -> Icons.Default.PersonOutline
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            // User info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "${user.name} ${user.familyName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Role: ${user.role.toString().lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (user.role) {
                        UserRole.ADMIN -> MaterialTheme.colorScheme.tertiary
                        UserRole.USER -> MaterialTheme.colorScheme.secondary
                        UserRole.GUEST -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            // Chevron icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 