package com.example.sofrehmessina.ui.screens.user

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.R
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.ErrorDialog
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.LoadingIndicator
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.ChangePasswordState
import com.example.sofrehmessina.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.verticalScroll

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val profileUser by profileViewModel.user.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val error by profileViewModel.error.collectAsState()
    val changePasswordState by authViewModel.changePasswordState.collectAsState()
    
    // State to track if initialization has been done
    var initialLoadAttempted by remember { mutableStateOf(false) }
    val user = profileUser ?: currentUser
    
    // Create state variables with default values
    var isEditing by remember { mutableStateOf(false) }
    
    // Create these variables only when we actually have user data
    // to prevent unnecessary UI updates during loading
    var name by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") } 
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var emailNotifications by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    
    // Create a scroll state for auto-scrolling behavior
    val scrollState = rememberScrollState()
    
    // Dialog states
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    
    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(changePasswordState) {
        when (changePasswordState) {
            is ChangePasswordState.Success -> {
                snackbarHostState.showSnackbar("Password reset email sent successfully")
            }
            is ChangePasswordState.Error -> {
                val errorState = changePasswordState as ChangePasswordState.Error
                snackbarHostState.showSnackbar(errorState.message)
            }
            else -> {}
        }
    }

    // Load user profile only once, with better error handling and timeout protection
    LaunchedEffect(Unit) {
        if (!initialLoadAttempted) {
            initialLoadAttempted = true
            
            try {
                currentUser?.id?.let { userId ->
                    if (userId.isNotEmpty()) {
                        // Set a short timeout to detect if loading is taking too long
                        val timeoutJob = launch {
                            delay(3000) // 3 seconds timeout
                            if (isLoading) {
                                snackbarHostState.showSnackbar(
                                    message = "Loading profile data...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                        
                        // Load user profile in the background
                        profileViewModel.loadUserProfile(userId)
                        
                        // Once loading completes (success or error), cancel the timeout
                        timeoutJob.cancel()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error loading user profile", e)
            }
        }
    }

    // Only update form fields when user data changes and is not null
    LaunchedEffect(user) {
        if (user != null) {
            name = user.name
            familyName = user.familyName
            phoneNumber = user.phone
            address = user.address
            postalCode = user.postalCode
            emailNotifications = user.emailNotifications
            pushNotifications = user.pushNotifications
        }
    }

    // Show error messages
    LaunchedEffect(error) {
        error?.let { currentError ->
            Log.e("ProfileScreen", "Profile error: ${currentError.message}", currentError)
            snackbarHostState.showSnackbar(
                message = "Error loading profile: ${currentError.message ?: "Unknown error"}",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Basic scaffold with top bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isLoading && user != null) {
                        if (isEditing) {
                            IconButton(onClick = {
                                user.let { currentUser ->
                                    val updatedUser = currentUser.copy(
                                        name = name,
                                        familyName = familyName,
                                        phone = phoneNumber,
                                        address = address,
                                        postalCode = postalCode,
                                        emailNotifications = emailNotifications,
                                        pushNotifications = pushNotifications
                                    )
                                    profileViewModel.updateUserProfile(updatedUser)
                                }
                                isEditing = false
                            }) {
                                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_profile))
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Show loading indicator in the center
                LoadingIndicator()
            } else if (error != null) {
                // Show error state with retry button
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Could not load profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            currentUser?.id?.let { userId ->
                                profileViewModel.clearError()
                                profileViewModel.loadUserProfile(userId)
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                // Only show the form when the profile data is loaded
                if (user != null) {
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    KeyboardAwareLayout(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(surfaceColor),
                        scrollState = scrollState
                    ) {
                        ProfileHeader(user)

                        if (isEditing) {
                            // Edit Profile Fields
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.personal_information),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text(stringResource(R.string.name)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .autoScrollOnFocus(scrollState)
                                    )

                                    OutlinedTextField(
                                        value = familyName,
                                        onValueChange = { familyName = it },
                                        label = { Text(stringResource(R.string.family_name)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .autoScrollOnFocus(scrollState)
                                    )

                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text(stringResource(R.string.phone_number)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .autoScrollOnFocus(scrollState)
                                    )

                                    OutlinedTextField(
                                        value = address,
                                        onValueChange = { address = it },
                                        label = { Text(stringResource(R.string.address)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .autoScrollOnFocus(scrollState)
                                    )

                                    OutlinedTextField(
                                        value = postalCode,
                                        onValueChange = { postalCode = it },
                                        label = { Text(stringResource(R.string.postal_code)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .autoScrollOnFocus(scrollState)
                                    )
                                    
                                    Text(
                                        text = stringResource(R.string.notification_preferences),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = emailNotifications,
                                            onCheckedChange = { emailNotifications = it }
                                        )
                                        Text(
                                            text = stringResource(R.string.receive_email_notifications),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = pushNotifications,
                                            onCheckedChange = { pushNotifications = it }
                                        )
                                        Text(
                                            text = stringResource(R.string.receive_push_notifications),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        } else {
                            // Display Profile Fields in Cards
                            
                            // Personal Information Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.personal_information),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    ProfileField(
                                        icon = Icons.Default.Person,
                                        label = stringResource(R.string.name),
                                        value = "${user.name} ${user.familyName}"
                                    )

                                    ProfileField(
                                        icon = Icons.Default.Phone,
                                        label = stringResource(R.string.phone),
                                        value = user.phone
                                    )

                                    ProfileField(
                                        icon = Icons.Default.LocationOn,
                                        label = stringResource(R.string.address),
                                        value = user.address
                                    )

                                    ProfileField(
                                        icon = Icons.Default.Mail,
                                        label = stringResource(R.string.postal_code),
                                        value = user.postalCode
                                    )

                                    ProfileField(
                                        icon = Icons.Default.Email,
                                        label = stringResource(R.string.email),
                                        value = user.email
                                    )
                                }
                            }
                            
                            // Account Information Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.account_information),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    ProfileField(
                                        icon = Icons.Default.SupervisedUserCircle,
                                        label = stringResource(R.string.account_type),
                                        value = user.role.name
                                    )
                                    
                                    user.lastLogin?.let {
                                        ProfileField(
                                            icon = Icons.AutoMirrored.Filled.Login,
                                            label = stringResource(R.string.last_login),
                                            value = formatDate(it)
                                        )
                                    }
                                    
                                    user.lastPasswordChange?.let {
                                        ProfileField(
                                            icon = Icons.Default.LockClock,
                                            label = stringResource(R.string.last_password_change),
                                            value = formatDate(it)
                                        )
                                    }
                                    
                                    ProfileField(
                                        icon = Icons.Default.Security,
                                        label = stringResource(R.string.password_strength),
                                        value = getPasswordStrengthDescription(user.passwordStrength ?: 0)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.email_notifications),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Switch(
                                            checked = user.emailNotifications,
                                            onCheckedChange = null,
                                            enabled = false
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(R.string.notifications),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Switch(
                                            checked = user.pushNotifications,
                                            onCheckedChange = null,
                                            enabled = false
                                        )
                                    }
                                }
                            }
                            
                            // Security Section
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.security),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Button(
                                        onClick = { navController.navigate(Screen.ChangePassword.route) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.change_password))
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { showResetPasswordDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.reset_password))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                authViewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(navController.graph.id) {
                                        inclusive = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.sign_out))
                        }
                    }
                }
            }

            error?.let { currentError ->
                ErrorDialog(
                    error = currentError.message ?: stringResource(R.string.unknown_error),
                    onDismiss = { profileViewModel.clearError() }
                )
            }
            
            // Reset Password Dialog
            if (showResetPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showResetPasswordDialog = false },
                    title = { Text(stringResource(R.string.reset_password_title)) },
                    text = { 
                        Text(stringResource(R.string.reset_password_message, user?.email ?: "")) 
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                user?.email?.let { email ->
                                    authViewModel.sendPasswordResetEmail(email)
                                }
                                showResetPasswordDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.send_reset_link))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetPasswordDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (user != null) "${user.name} ${user.familyName}" else "Guest User",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = user?.email ?: "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (user?.role != null) stringResource(R.string.role_label, user.role.name) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ProfileField(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
}

private fun formatDate(date: Date): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(date)
}

private fun getPasswordStrengthDescription(strength: Int): String {
    return when {
        strength <= 0 -> "Very Weak"
        strength <= 20 -> "Weak"
        strength <= 40 -> "Fair"
        strength <= 60 -> "Good"
        strength <= 80 -> "Strong"
        else -> "Very Strong"
    }
} 