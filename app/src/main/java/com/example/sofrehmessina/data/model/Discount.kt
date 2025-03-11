package com.example.sofrehmessina.data.model

import java.util.Date

/**
 * Represents a discount rule in the system
 */
data class Discount(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "", // The category this discount applies to
    val specificFoodIds: List<String>? = null, // Specific food items this discount applies to (if null or empty, applies to entire category)
    val minQuantity: Int = 0, // Minimum number of items needed to trigger discount
    val percentOff: Double = 0.0, // Discount percentage (0-100)
    val active: Boolean = true, // Whether this discount is currently active
    val startDate: Date? = null, // Optional start date for the discount
    val endDate: Date? = null, // Optional end date for the discount
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val couponCode: String? = null, // Coupon code for this discount (if null, not a coupon-based discount)
    val isCustomerSpecific: Boolean = false // Whether this coupon is meant for specific customers
) 