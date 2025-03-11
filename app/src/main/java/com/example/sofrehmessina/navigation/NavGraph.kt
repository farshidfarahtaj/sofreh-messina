package com.example.sofrehmessina.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sofrehmessina.ui.screens.admin.*
import com.example.sofrehmessina.ui.screens.auth.*
import com.example.sofrehmessina.ui.screens.user.*
import com.example.sofrehmessina.ui.screens.SplashScreen
import com.example.sofrehmessina.ui.screens.settings.PromoteAdminScreen
import com.example.sofrehmessina.ui.utils.AnimationUtils
import com.example.sofrehmessina.ui.viewmodel.CartViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sofrehmessina.ui.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {
    // Get CartViewModel for checkout completion
    val cartViewModel: CartViewModel = hiltViewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(
            route = Screen.Splash.route,
            enterTransition = { AnimationUtils.specialEnterTransition() },
            exitTransition = { AnimationUtils.specialExitTransition() },
            popEnterTransition = { AnimationUtils.specialEnterTransition() },
            popExitTransition = { AnimationUtils.specialExitTransition() }
        ) {
            SplashScreen(
                navController = navController
            )
        }
        
        // Auth Screens
        composable(
            route = Screen.Login.route,
            enterTransition = { AnimationUtils.enterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) {
            LoginScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.Register.route,
            enterTransition = { AnimationUtils.enterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) {
            RegisterScreen(
                navController = navController
            )
        }

        // User Screens
        composable(
            route = Screen.Home.route,
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.zoomExitTransition() },
            popEnterTransition = { AnimationUtils.springEnterTransition() },
            popExitTransition = { AnimationUtils.zoomExitTransition() }
        ) {
            HomeScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.Menu.route,
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.zoomExitTransition() },
            popEnterTransition = { AnimationUtils.springEnterTransition() },
            popExitTransition = { AnimationUtils.zoomExitTransition() }
        ) {
            MenuScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.Cart.route,
            enterTransition = { AnimationUtils.bottomSheetEnterTransition() },
            exitTransition = { AnimationUtils.bottomSheetExitTransition() },
            popEnterTransition = { AnimationUtils.bottomSheetEnterTransition() },
            popExitTransition = { AnimationUtils.bottomSheetExitTransition() }
        ) {
            CartScreen(
                onNavigateBack = { navController.navigateUp() },
                onNavigateToCheckout = { navController.navigate(Screen.Checkout.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(
            route = Screen.Profile.route,
            enterTransition = { AnimationUtils.enterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) {
            ProfileScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { AnimationUtils.verticalEnterTransition() },
            exitTransition = { AnimationUtils.verticalExitTransition() },
            popEnterTransition = { AnimationUtils.verticalEnterTransition() },
            popExitTransition = { AnimationUtils.verticalExitTransition() }
        ) {
            SettingsScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.ChangePassword.route,
            enterTransition = { AnimationUtils.enterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) {
            ChangePasswordScreen(
                navController = navController
            )
        }
        composable(Screen.Terms.route) {
            TermsScreen(
                navController = navController
            )
        }
        composable(Screen.Privacy.route) {
            PrivacyScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.OrderHistory.route,
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) {
            val authViewModel = hiltViewModel<AuthViewModel>()
            val currentUser by authViewModel.currentUser.collectAsState()
            
            OrderHistoryScreen(
                navController = navController,
                userId = currentUser?.id ?: ""
            )
        }
        composable(Screen.OrderTracking.route) {
            OrderTrackingScreen(
                navController = navController
            )
        }
        composable(
            route = Screen.Checkout.route,
            enterTransition = { AnimationUtils.bottomSheetEnterTransition() },
            exitTransition = { AnimationUtils.bottomSheetExitTransition() },
            popEnterTransition = { AnimationUtils.bottomSheetEnterTransition() },
            popExitTransition = { AnimationUtils.bottomSheetExitTransition() }
        ) {
            CheckoutScreen(
                onNavigateBack = { navController.navigateUp() },
                onCheckoutComplete = { 
                    // Navigate to orders screen after checkout
                    cartViewModel.clearCart()
                    navController.navigate(Screen.OrderHistory.route) {
                        // Clear backstack up to Home
                        popUpTo(Screen.Home.route) {
                            // Keep the Home screen in the backstack
                            inclusive = false
                            // Save state so when we return to Home, it's in the same state
                            saveState = true
                        }
                        // Avoid duplicate entries in the backstack
                        launchSingleTop = true
                        // Restore state when navigating back to a previously visited screen
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.SpecialOffer.route) {
            SpecialOfferScreen(
                navController = navController
            )
        }
        composable(Screen.AdminAccess.route) {
            PromoteAdminScreen(
                navController = navController
            )
        }
        
        // Admin Screens
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                navController = navController
            )
        }
        composable(Screen.AdminCategories.route) {
            ManageCategoriesScreen(
                navController = navController
            )
        }
        composable(Screen.AdminFood.route) {
            ManageFoodScreen(
                navController = navController
            )
        }
        composable(Screen.AdminFoodItems.route) {
            ManageFoodItemsScreen(
                navController = navController
            )
        }
        composable(Screen.AdminOrders.route) {
            ManageOrdersScreen(
                navController = navController
            )
        }
        composable(Screen.AdminUsers.route) {
            ManageUsersScreen(
                navController = navController
            )
        }
        composable(Screen.AdminBanners.route) {
            ManageBannersScreen(
                navController = navController
            )
        }
        composable(Screen.AdminDiscounts.route) {
            DiscountManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.AdminCurrencySettings.route) {
            CurrencySettingsScreen(
                navController = navController
            )
        }
        composable(Screen.AdminCouponManagement.route) {
            CouponManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Parameterized routes
        composable(
            route = "${Screen.FoodDetails.route}/{foodId}/{categoryId}",
            arguments = listOf(
                navArgument("foodId") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType }
            ),
            enterTransition = { AnimationUtils.zoomEnterTransition() },
            exitTransition = { AnimationUtils.zoomExitTransition() },
            popEnterTransition = { AnimationUtils.zoomEnterTransition() },
            popExitTransition = { AnimationUtils.zoomExitTransition() }
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId") ?: ""
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            FoodDetailsScreen(
                foodId = foodId,
                categoryId = categoryId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                onNavigateToCategory = { catId -> 
                    navController.navigate("${Screen.CategoryDetails.route}/$catId")
                }
            )
        }
        
        composable(
            route = "${Screen.CategoryDetails.route}/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            ),
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            CategoryDetailsScreen(
                categoryId = categoryId,
                navController = navController
            )
        }
        
        composable(
            route = "${Screen.AdminUserDetails.route}/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            AdminUserDetailsScreen(
                userId = userId,
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "${Screen.AdminOrderDetails.route}/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            enterTransition = { AnimationUtils.enterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            AdminOrderDetailsScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Special routes with custom animations
        composable(
            route = Screen.FoodDetails.route,
            arguments = listOf(
                navArgument("foodId") { type = NavType.StringType }
            ),
            enterTransition = { AnimationUtils.zoomEnterTransition() },
            exitTransition = { AnimationUtils.zoomExitTransition() },
            popEnterTransition = { AnimationUtils.zoomEnterTransition() },
            popExitTransition = { AnimationUtils.zoomExitTransition() }
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId")
            if (foodId != null) {
                FoodDetailsScreen(
                    foodId = foodId,
                    categoryId = "",
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                    onNavigateToCategory = { catId -> 
                        navController.navigate("${Screen.CategoryDetails.route}/$catId")
                    }
                )
            }
        }
        
        composable(
            route = Screen.CategoryDetails.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            ),
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            if (categoryId != null) {
                CategoryDetailsScreen(
                    categoryId = categoryId,
                    navController = navController
                )
            }
        }
        
        composable(
            route = "${Screen.OrderHistory.route}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { AnimationUtils.springEnterTransition() },
            exitTransition = { AnimationUtils.exitTransition() },
            popEnterTransition = { AnimationUtils.popEnterTransition() },
            popExitTransition = { AnimationUtils.popExitTransition() }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            OrderHistoryScreen(
                navController = navController,
                userId = userId
            )
        }
    }
} 