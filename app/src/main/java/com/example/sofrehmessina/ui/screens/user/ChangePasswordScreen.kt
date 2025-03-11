package com.example.sofrehmessina.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.ChangePasswordState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Create a scroll state for auto-scrolling behavior
    val scrollState = rememberScrollState()

    val changePasswordState by viewModel.changePasswordState.collectAsState()
    val isLoading = changePasswordState is ChangePasswordState.Loading

    LaunchedEffect(changePasswordState) {
        when (changePasswordState) {
            is ChangePasswordState.Success -> {
                showSuccessDialog = true
            }
            is ChangePasswordState.Error -> {
                errorMessage = (changePasswordState as ChangePasswordState.Error).message
                showErrorDialog = true
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        KeyboardAwareLayout(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            scrollState = scrollState
        ) {
            Text(
                text = "Please enter your current password and choose a new password.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .autoScrollOnFocus(scrollState),
                visualTransformation = if (showCurrentPassword) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            if (showCurrentPassword) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (showCurrentPassword) "Hide password" else "Show password"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .autoScrollOnFocus(scrollState),
                visualTransformation = if (showNewPassword) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            if (showNewPassword) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (showNewPassword) "Hide password" else "Show password"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .autoScrollOnFocus(scrollState),
                visualTransformation = if (showConfirmPassword) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            if (showConfirmPassword) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                        )
                    }
                },
                isError = newPassword != confirmPassword && confirmPassword.isNotEmpty()
            )

            if (newPassword != confirmPassword && confirmPassword.isNotEmpty()) {
                Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.changePassword(
                        currentPassword = currentPassword,
                        newPassword = newPassword
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentPassword.isNotEmpty() &&
                    newPassword.isNotEmpty() &&
                    confirmPassword.isNotEmpty() &&
                    newPassword == confirmPassword &&
                    !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Change Password")
                }
            }
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                navController.navigateUp()
            },
            title = { Text("Success") },
            text = { Text("Your password has been successfully changed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        navController.navigateUp()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
} 