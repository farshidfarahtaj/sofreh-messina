package com.example.sofrehmessina.data.model

import com.example.sofrehmessina.util.LocaleHelper

/**
 * Data class representing user settings
 */
data class Settings(
    val displayName: String = "",
    val theme: ThemeOption = ThemeOption.SYSTEM,
    val emailNotifications: Boolean = true,
    val orderNotifications: Boolean = true,
    val promotionalNotifications: Boolean = false,
    val emailNotificationsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val language: String = LocaleHelper.DEFAULT_LANGUAGE,
    val autoLogout: Boolean = false, // Whether to automatically log out when app closes
    val autoLogoutTimeMinutes: Int = 30 // Time in minutes after which to auto-logout when app is in background
) 