package com.example.sofrehmessina.ui.viewmodel

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal for providing a single CartViewModel instance throughout the app
 */
val LocalCartViewModel = compositionLocalOf<CartViewModel> {
    error("CartViewModel not provided")
} 