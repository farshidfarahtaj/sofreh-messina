package com.example.sofrehmessina.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.AppLogo
import com.example.sofrehmessina.ui.components.GradientBorderCard
import com.example.sofrehmessina.ui.components.GradientButton
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.theme.GradientEnd
import com.example.sofrehmessina.ui.theme.GradientStart
import com.example.sofrehmessina.ui.viewmodel.AuthState
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import com.example.sofrehmessina.ui.viewmodel.ChangePasswordState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import com.example.sofrehmessina.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for the login screen, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        
        // Animation states
        val logoState = remember { MutableTransitionState(false).apply { targetState = true } }
        val formState = remember { MutableTransitionState(false).apply { targetState = true } }
        val buttonsState = remember { MutableTransitionState(false).apply { targetState = true } }
        
        // Add state for forgot password dialog
        var showForgotPasswordDialog by remember { mutableStateOf(false) }
        var forgotPasswordEmail by remember { mutableStateOf("") }
        var forgotPasswordError by remember { mutableStateOf<String?>(null) }

        val authState by viewModel.authState.collectAsState()
        val currentUser by viewModel.currentUser.collectAsState()
        val changePasswordState by viewModel.changePasswordState.collectAsState()
        
        // Add a LaunchedEffect to handle password reset state changes
        LaunchedEffect(changePasswordState) {
            when (changePasswordState) {
                is ChangePasswordState.Success -> {
                    showForgotPasswordDialog = false
                    forgotPasswordEmail = ""
                    showError = true
                    errorMessage = "Password reset email sent. Please check your inbox."
                }
                is ChangePasswordState.Error -> {
                    forgotPasswordError = (changePasswordState as ChangePasswordState.Error).message
                }
                else -> {}
            }
        }

        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Authenticated -> {
                    currentUser?.let { user ->
                        when (user.role) {
                            UserRole.ADMIN -> navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                            else -> navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
                is AuthState.Error -> {
                    showError = true
                    errorMessage = (authState as AuthState.Error).message
                }
                else -> {}
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Using KeyboardAwareLayout instead of Box/Column
            KeyboardAwareLayout(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with animation
                    AnimatedVisibility(
                        visibleState = logoState,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 700)
                        ) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialOffsetY = { -200 }
                        ),
                    ) {
                        AppLogo(
                            modifier = Modifier.padding(bottom = 24.dp),
                            logoSize = 1.5f
                        )
                    }
                    
                    // Welcome text
                    AnimatedVisibility(
                        visibleState = formState,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 700, delayMillis = 300)
                        ) + expandVertically(
                            animationSpec = tween(durationMillis = 700, delayMillis = 300),
                            expandFrom = Alignment.Top
                        ),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.welcome_to_sofrehmessina),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(R.string.sign_in_to_continue),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form card with gradient border
                    AnimatedVisibility(
                        visibleState = formState,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 700, delayMillis = 500)
                        ) + expandVertically(
                            animationSpec = tween(durationMillis = 700, delayMillis = 500),
                            expandFrom = Alignment.Top
                        ),
                    ) {
                        GradientBorderCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Email field
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text(stringResource(R.string.email)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        ) 
                                    },
                                    singleLine = true,
                                    isError = showError,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrLtr
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Password field
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(R.string.password)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { 
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        ) 
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    isError = showError,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.ContentOrLtr
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { 
                                            keyboardController?.hide()
                                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                                showError = false
                                                viewModel.signIn(email, password)
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                if (showError) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    AnimatedVisibility(
                                        visible = showError,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Text(
                                            text = errorMessage,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons section
                    AnimatedVisibility(
                        visibleState = buttonsState,
                        enter = fadeIn(
                            animationSpec = tween(durationMillis = 700, delayMillis = 700)
                        ) + expandVertically(
                            animationSpec = tween(durationMillis = 700, delayMillis = 700),
                            expandFrom = Alignment.Top
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Login button
                            GradientButton(
                                text = stringResource(R.string.sign_in),
                                onClick = {
                                    keyboardController?.hide()
                                    showError = false
                                    viewModel.signIn(email, password)
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(
                                    onClick = { navController.navigate(Screen.Register.route) }
                                ) {
                                    Text(
                                        text = stringResource(R.string.sign_up),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }

                                TextButton(
                                    onClick = { 
                                        // Show the forgot password dialog
                                        forgotPasswordEmail = email // Pre-fill with current email if available
                                        forgotPasswordError = null
                                        showForgotPasswordDialog = true
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.forgot_password),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Decorative circle in background (optional)
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GradientStart.copy(alpha = 0.1f),
                                GradientEnd.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
            )
            
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GradientEnd.copy(alpha = 0.1f),
                                GradientStart.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .align(Alignment.BottomStart)
                    .offset(x = (-50).dp, y = 50.dp)
            )
        }

        // Forgot Password Dialog with animation
        AnimatedVisibility(
            visible = showForgotPasswordDialog,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
        ) {
            // Get string resources at the Composable level
            val resetPasswordString = stringResource(R.string.reset_password)
            val emailRequiredString = stringResource(R.string.email_required)
            
            AlertDialog(
                onDismissRequest = { 
                    showForgotPasswordDialog = false 
                    forgotPasswordError = null
                },
                title = { 
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                text = { 
                    Column {
                        Text(
                            text = stringResource(R.string.forgot_password_instructions),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = { forgotPasswordEmail = it },
                            label = { Text(stringResource(R.string.email)) },
                            isError = forgotPasswordError != null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        if (forgotPasswordError != null) {
                            Text(
                                text = forgotPasswordError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    GradientButton(
                        text = resetPasswordString,
                        onClick = {
                            if (forgotPasswordEmail.isNotEmpty()) {
                                viewModel.sendPasswordResetEmail(forgotPasswordEmail)
                            } else {
                                forgotPasswordError = emailRequiredString
                            }
                        }
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showForgotPasswordDialog = false 
                            forgotPasswordError = null
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
} 