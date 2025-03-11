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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.navigation.Screen
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.R

@Composable
fun SofrehDrawer(
    currentUser: User?,
    navController: NavController,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User profile image or placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onProfileClick() }
                ) {
                    if (currentUser?.profilePictureUrl?.isNotEmpty() == true) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            // Use Coil or similar to load image
                            // model = currentUser.profilePictureUrl,
                            painter = androidx.compose.ui.res.painterResource(
                                id = android.R.drawable.ic_menu_gallery
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // User name or Welcome message
                Text(
                    text = if (currentUser?.name?.isNotEmpty() == true) {
                        "${currentUser.name} ${currentUser.familyName}"
                    } else {
                        stringResource(R.string.welcome_to_sofrehmessina)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (currentUser != null) {
                    Text(
                        text = currentUser.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        HorizontalDivider()
        
        // Navigation Items - Wrap in a ScrollableColumn
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Use available space
                .verticalScroll(scrollState) // Make it scrollable
        ) {
            DrawerItem(
                icon = Icons.Default.Home,
                label = stringResource(R.string.home),
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
            
            DrawerItem(
                icon = Icons.Default.RestaurantMenu,
                label = stringResource(R.string.menu),
                onClick = {
                    navController.navigate(Screen.Menu.route) {
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
                icon = Icons.Default.ShoppingCart,
                label = stringResource(R.string.cart),
                onClick = {
                    navController.navigate(Screen.Cart.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    onClose()
                }
            )
            
            if (currentUser != null) {
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    label = stringResource(R.string.my_orders),
                    onClick = {
                        navController.navigate(Screen.OrderHistory.route) {
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
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.profile),
                    onClick = {
                        navController.navigate(Screen.Profile.route) {
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
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.settings),
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        onClose()
                    }
                )
            }
            
            // Admin specific items
            if (currentUser?.role == UserRole.ADMIN) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(
                    text = stringResource(R.string.admin),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                
                DrawerItem(
                    icon = Icons.Default.Dashboard,
                    label = stringResource(R.string.dashboard),
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
                    label = stringResource(R.string.manage_categories),
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
                    label = stringResource(R.string.manage_food_items),
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
                    label = stringResource(R.string.manage_orders),
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
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (currentUser != null) {
                // Add Contact Us item before Logout
                val context = LocalContext.current
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = stringResource(R.string.contact_us),
                    onClick = {
                        val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://t.me/SofrehMessina")
                        }
                        context.startActivity(telegramIntent)
                        onClose()
                    }
                )
                
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = stringResource(R.string.sign_out),
                    onClick = {
                        onLogout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        onClose()
                    }
                )
            } else {
                DrawerItem(
                    icon = Icons.AutoMirrored.Filled.Login,
                    label = stringResource(R.string.login_register),
                    onClick = {
                        navController.navigate(Screen.Login.route) {
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
                    icon = Icons.Default.PersonAdd,
                    label = stringResource(R.string.register),
                    onClick = {
                        navController.navigate(Screen.Register.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        onClose()
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 