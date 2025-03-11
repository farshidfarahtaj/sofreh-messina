package com.example.sofrehmessina.data.model

/**
 * Represents the different states an order can be in.
 */
enum class OrderStatus {
    /** Initial state when order is first created */
    PENDING,
    /** Order has been accepted by the restaurant */
    CONFIRMED,
    /** Order is being prepared in the kitchen */
    PREPARING,
    /** Order is ready for pickup/delivery */
    READY,
    /** Order has been delivered to the customer */
    DELIVERED,
    /** Order has been cancelled */
    CANCELLED;

    fun toDisplayName(): String = when (this) {
        PENDING -> "Pending"
        CONFIRMED -> "Confirmed"
        PREPARING -> "Preparing"
        READY -> "Ready"
        DELIVERED -> "Delivered"
        CANCELLED -> "Cancelled"
    }
} 