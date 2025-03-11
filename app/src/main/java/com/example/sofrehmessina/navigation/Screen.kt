package com.example.sofrehmessina.navigation

sealed interface Screen {
    val route: String

    // Splash Screen
    data object Splash : Screen {
        override val route = "splash"
    }
    
    // Auth Screens
    data object Login : Screen {
        override val route = "login"
    }
    
    data object Register : Screen {
        override val route = "register"
    }

    // User Screens
    data object Home : Screen {
        override val route = "home"
    }
    
    data object Menu : Screen {
        override val route = "menu"
    }
    
    data object Cart : Screen {
        override val route = "cart"
    }
    
    data object Profile : Screen {
        override val route = "profile"
    }
    
    data object Settings : Screen {
        override val route = "settings"
    }
    
    data object ChangePassword : Screen {
        override val route = "change_password"
    }
    
    data object Terms : Screen {
        override val route = "terms"
    }
    
    data object Privacy : Screen {
        override val route = "privacy"
    }
    
    data object OrderHistory : Screen {
        override val route = "order_history"
    }
    
    data object OrderTracking : Screen {
        override val route = "order_tracking"
    }
    
    data object Checkout : Screen {
        override val route = "checkout"
    }
    
    // Detail Screens
    data object FoodDetails : Screen {
        override val route = "food_details"
    }
    
    data object CategoryDetails : Screen {
        override val route = "category_details"
    }
    
    data object SpecialOffer : Screen {
        override val route = "special_offer"
    }
    
    // Admin Screens
    data object AdminDashboard : Screen {
        override val route = "admin_dashboard"
    }
    
    data object AdminCategories : Screen {
        override val route = "admin_categories"
    }
    
    data object AdminFood : Screen {
        override val route = "admin_food"
    }
    
    data object AdminFoodItems : Screen {
        override val route = "admin_food_items"
    }
    
    data object AdminOrders : Screen {
        override val route = "admin_orders"
    }
    
    data object AdminUsers : Screen {
        override val route = "admin_users"
    }
    
    data object AdminUserDetails : Screen {
        override val route = "admin_user_details"
    }
    
    data object AdminOrderDetails : Screen {
        override val route = "admin_order_details"
    }
    
    data object AdminBanners : Screen {
        override val route = "admin_banners"
    }
    
    data object AdminDiscounts : Screen {
        override val route = "admin_discounts"
    }
    
    data object AdminCurrencySettings : Screen {
        override val route = "admin_currency_settings"
    }
    
    data object AdminCouponManagement : Screen {
        override val route = "admin_coupon_management"
    }
    
    // Settings Screens
    data object AdminAccess : Screen {
        override val route = "admin_access"
    }
    
    companion object {
        fun createRoute(route: String, vararg params: String): String {
            return buildString {
                append(route)
                params.forEach { param ->
                    append("/$param")
                }
            }
        }
    }
} 