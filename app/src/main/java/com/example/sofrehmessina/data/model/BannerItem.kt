package com.example.sofrehmessina.data.model

/**
 * Data class representing a banner item to be displayed in the home screen slideshow
 */
data class BannerItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String = "",
    val actionUrl: String = "", // Could be a deep link or category ID
    val active: Boolean = true,
    val order: Int = 0
) 