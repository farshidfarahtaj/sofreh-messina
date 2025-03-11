package com.example.sofrehmessina.ui.navigation

sealed class AdminScreens(val route: String) {
    object Dashboard : AdminScreens("admin_dashboard")
    object Categories : AdminScreens("admin_categories")
    object FoodItems : AdminScreens("admin_food_items")
    object Orders : AdminScreens("admin_orders")
    object Users : AdminScreens("admin_users")
    object Banners : AdminScreens("admin_banners")
    object Discounts : AdminScreens("admin_discounts")
    object Reports : AdminScreens("admin_reports")
    object Settings : AdminScreens("admin_settings")
} 