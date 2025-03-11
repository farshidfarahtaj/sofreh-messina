package com.example.sofrehmessina.data.model

import java.util.Date

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotal: Double = 0.0,
    val discounts: Map<String, Double> = emptyMap(), // Map of categoryId to discount amount
    val total: Double = 0.0,
    val address: String = "",
    val phone: String = "",
    val paymentMethod: String = "",
    val notes: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val couponCode: String? = null // Coupon code used for this order, if any
) 