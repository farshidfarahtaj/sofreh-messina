package com.example.sofrehmessina.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.components.AppLogo
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
// // import com.example.sofrehmessina.ui.viewmodel.AuthState (removed)
import com.example.sofrehmessina.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SplashScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val context = LocalContext.current
    
    // This flag is no longer used to force logout
    val wasLanguageChanged = remember {
        // Check if app was restarted due to language change
        (context as? android.app.Activity)?.intent?.getBooleanExtra("LANGUAGE_CHANGED", false) ?: false
    }
    
//     // val authState by authViewModel.authState.collectAsState() (removed)
    val currentUser by authViewModel.currentUser.collectAsState()
    
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = EaseOutBack
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500
            )
        )
        
        // Use a shorter delay if the app was restarted due to language change
        if (wasLanguageChanged) {
            delay(500)
        } else {
            delay(1500)
        }
        
        // Check if user is already authenticated
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            // Try to load the user profile
            authViewModel.loadUserProfile(firebaseUser.uid)
            delay(300) // Short delay to allow user data to load
            
            // Check if we have current user data
            if (currentUser != null) {
                // Navigate based on user role
                val route = if (currentUser?.role == UserRole.ADMIN) {
                    Screen.AdminDashboard.route
                } else {
                    Screen.Home.route
                }
                
                navController.navigate(route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            } else {
                // If we couldn't load user data, go to login
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                }
            }
        } else {
            // No authenticated user, go to login
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "SofrehMessina",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(scale.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Authentic Persian Cuisine",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
} 
