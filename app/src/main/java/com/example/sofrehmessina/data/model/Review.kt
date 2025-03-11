package com.example.sofrehmessina.data.model

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val userId: String = "",
    val foodId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Timestamp = Timestamp.now()
) 