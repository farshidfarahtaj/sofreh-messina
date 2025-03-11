package com.example.sofrehmessina.data.model

import java.util.Date

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val familyName: String = "",
    val phone: String = "",
    val address: String = "",
    val postalCode: String = "",
    val role: UserRole = UserRole.USER,
    val profilePictureUrl: String = "",
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val settings: Settings? = null,
    val lastPasswordChange: Date? = null,
    val disabled: Boolean? = false,
    val passwordStrength: Int? = 0,
    val lastLogin: Date? = null,
    val lastFailedLogin: Date? = null
)

enum class UserRole {
    ADMIN,
    USER,
    GUEST
} 