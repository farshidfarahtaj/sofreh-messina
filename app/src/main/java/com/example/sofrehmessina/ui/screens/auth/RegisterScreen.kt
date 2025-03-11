package com.example.sofrehmessina.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.AppLogo
import com.example.sofrehmessina.ui.components.GradientBorderCard
import com.example.sofrehmessina.ui.components.GradientButton
import com.example.sofrehmessina.ui.components.KeyboardAwareLayout
import com.example.sofrehmessina.ui.components.autoScrollOnFocus
import com.example.sofrehmessina.ui.theme.GradientEnd
import com.example.sofrehmessina.ui.theme.GradientStart
import com.example.sofrehmessina.ui.viewmodel.AuthState
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.sofrehmessina.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Always use LTR layout for the register screen, regardless of language
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        
        var name by remember { mutableStateOf("") }
        var familyName by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var postalCode by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var showError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var showSuccessDialog by remember { mutableStateOf(false) }

        // Animation states
        val headerState = remember { MutableTransitionState(false).apply { targetState = true } }
        val formState = remember { MutableTransitionState(false).apply { targetState = true } }
        val buttonState = remember { MutableTransitionState(false).apply { targetState = true } }

        val authState by viewModel.authState.collectAsState()
        val scrollState = rememberScrollState()
        
        // Store string resources that will be used in lambdas
        val passwordsDoNotMatchError = stringResource(R.string.passwords_do_not_match)
        val welcomeTitle = stringResource(R.string.welcome_to_sofreh_persian_food)
        val successMessage = stringResource(R.string.registration_success_message)

        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Authenticated -> {
                    showSuccessDialog = true
                }
                is AuthState.Error -> {
                    showError = true
                    val errorMsg = (authState as AuthState.Error).message
                    
                    // Check if this is a permission denied error and provide more specific guidance
                    errorMessage = if (errorMsg.contains("PERMISSION_DENIED") || 
                                       errorMsg.contains("PERM-101")) {
                        "Registration failed due to permissions. Please verify that your Firebase rules allow user creation. You may need to deploy updated rules using the Firebase console."
                    } else {
                        errorMsg
                    }
                }
                else -> {}
            }
        }

        // Success dialog with animation
        AnimatedVisibility(
            visible = showSuccessDialog,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.CenterVertically),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
        ) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
                title = { 
                    Text(
                        text = welcomeTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                text = { 
                    Text(
                        text = successMessage,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) 
                },
                confirmButton = {
                    GradientButton(
                        text = stringResource(R.string.ok),
                        onClick = {
                            showSuccessDialog = false
                            // Sign out the user to ensure they log in fresh
                            viewModel.signOut()
                            // Navigate to login screen
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
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
                )
        ) {
            // Use the KeyboardAwareLayout with shared scrollState
            KeyboardAwareLayout(
                modifier = Modifier.fillMaxSize(),
                scrollState = scrollState
            ) {
                // Header with logo and title
                AnimatedVisibility(
                    visibleState = headerState,
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppLogo(
                            modifier = Modifier.padding(bottom = 8.dp), 
                            logoSize = 1.2f
                        )
                        
                        Text(
                            text = stringResource(R.string.create_account),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.fill_details_below),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Registration form with animation
                AnimatedVisibility(
                    visibleState = formState,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 700, delayMillis = 300)
                    ) + expandVertically(
                        animationSpec = tween(durationMillis = 700, delayMillis = 300),
                        expandFrom = Alignment.Top
                    ),
                ) {
                    GradientBorderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Personal information fields
                            StylishTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.name),
                                icon = Icons.Default.Person,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = familyName,
                                onValueChange = { familyName = it },
                                label = stringResource(R.string.family_name),
                                icon = Icons.Default.Person,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = stringResource(R.string.phone_number),
                                icon = Icons.Default.Phone,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = stringResource(R.string.address),
                                icon = Icons.Default.Home,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = postalCode,
                                onValueChange = { postalCode = it },
                                label = stringResource(R.string.postal_code),
                                icon = Icons.Default.Place,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Account information fields
                            StylishTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = stringResource(R.string.email),
                                icon = Icons.Default.Email,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = stringResource(R.string.password),
                                icon = Icons.Default.Lock,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                isPassword = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            StylishTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = stringResource(R.string.confirm_password),
                                icon = Icons.Default.Lock,
                                scrollState = scrollState,
                                showError = showError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { 
                                        keyboardController?.hide()
                                        if (password != confirmPassword) {
                                            showError = true
                                            errorMessage = passwordsDoNotMatchError
                                        } else if (validateInputs()) {
                                            showError = false
                                            viewModel.signUp(name, familyName, email, password, phoneNumber, address, postalCode)
                                        }
                                    }
                                ),
                                isPassword = true
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
                
                // Button section with animation
                AnimatedVisibility(
                    visibleState = buttonState,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 700, delayMillis = 500)
                    ) + expandVertically(
                        animationSpec = tween(durationMillis = 700, delayMillis = 500),
                        expandFrom = Alignment.Top
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GradientButton(
                            text = stringResource(R.string.create_account),
                            onClick = {
                                keyboardController?.hide()
                                if (password != confirmPassword) {
                                    showError = true
                                    errorMessage = passwordsDoNotMatchError
                                } else if (validateInputs()) {
                                    showError = false
                                    viewModel.signUp(name, familyName, email, password, phoneNumber, address, postalCode)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        TextButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Text(
                                text = stringResource(R.string.already_have_account),
                                color = MaterialTheme.colorScheme.primary
                            )
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
    }
}

// Reusable styled text field component for the registration form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StylishTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    showError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .autoScrollOnFocus(scrollState),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        singleLine = true,
        isError = showError,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textDirection = TextDirection.ContentOrLtr
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

// Helper function to validate inputs
private fun validateInputs(): Boolean {
    // Implement validation logic if needed
    return true
} 