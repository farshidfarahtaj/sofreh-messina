package com.example.sofrehmessina.data.model

data class CartItem(
    val food: Food = Food(),
    val quantity: Int = 1,
    val notes: String = ""
) 