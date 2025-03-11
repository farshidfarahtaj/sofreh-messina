package com.example.sofrehmessina.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDrawer(
    currentUser: User?,
    navController: NavController,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val logoutEvent by authViewModel.logoutEvent.collectAsState()
    
    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
            authViewModel.clearLogoutEvent()
        }
    }
    
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        // Header with admin info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Admin profile image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    if (currentUser?.profilePictureUrl?.isNotEmpty() == true) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Admin Profile Picture",
                            contentScale = ContentScale.Crop,
                            painter = androidx.compose.ui.res.painterResource(
                                id = android.R.drawable.ic_menu_gallery
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Admin name and role
                Text(
                    text = if (currentUser?.name?.isNotEmpty() == true) {
                        "${currentUser.name} ${currentUser.familyName}"
                    } else {
                        "Admin Panel"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Administrator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        HorizontalDivider()
        
        // Admin Navigation Items
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Take available space
                .verticalScroll(scrollState) // Make scrollable
        ) {
            DrawerItem(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                onClick = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            DrawerItem(
                icon = Icons.Default.Category,
                label = "Manage Categories",
                onClick = {
                    navController.navigate(Screen.AdminCategories.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            DrawerItem(
                icon = Icons.Default.RestaurantMenu,
                label = "Manage Food Items",
                onClick = {
                    navController.navigate(Screen.AdminFood.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                label = "Manage Orders",
                onClick = {
                    navController.navigate(Screen.AdminOrders.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            DrawerItem(
                icon = Icons.Default.People,
                label = "Manage Users",
                onClick = {
                    navController.navigate(Screen.AdminUsers.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )

            DrawerItem(
                icon = Icons.Default.PhotoLibrary,
                label = "Manage Banners",
                onClick = {
                    navController.navigate(Screen.AdminBanners.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )

            DrawerItem(
                icon = Icons.Default.LocalOffer,
                label = "Discounts",
                onClick = {
                    navController.navigate(Screen.AdminDiscounts.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )

            DrawerItem(
                icon = Icons.Default.CardGiftcard,
                label = "Coupon Management",
                onClick = {
                    navController.navigate(Screen.AdminCouponManagement.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )

            HorizontalDivider()
            
            // Currency Settings
            DrawerItem(
                icon = Icons.Default.Euro,
                label = "Currency Settings",
                onClick = {
                    navController.navigate(Screen.AdminCurrencySettings.route)
                    onClose()
                }
            )
            
            // Switch to User View
            DrawerItem(
                icon = Icons.Default.SwapHoriz,
                label = "Switch to User View",
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            // Logout option
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Logout",
                onClick = {
                    onLogout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    onClose()
                }
            )
        }
    }
} 