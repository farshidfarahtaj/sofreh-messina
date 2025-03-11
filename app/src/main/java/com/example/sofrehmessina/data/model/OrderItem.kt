package com.example.sofrehmessina.data.model

data class OrderItem(
    val foodId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val notes: String = ""
) 