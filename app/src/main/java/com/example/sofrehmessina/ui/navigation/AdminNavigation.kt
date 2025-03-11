package com.example.sofrehmessina.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sofrehmessina.ui.screens.admin.AdminDashboardScreen
import com.example.sofrehmessina.ui.screens.admin.DiscountManagementScreen

@Composable
fun AdminNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AdminScreens.Dashboard.route
    ) {
        composable(AdminScreens.Dashboard.route) {
            AdminDashboardScreen(navController)
        }
        
        // Other admin screens would go here
        
        // Discount Management Screen
        composable(AdminScreens.Discounts.route) {
            DiscountManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
} 