package com.example.sofrehmessina.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.sofrehmessina.di.currencyManager

/**
 * Retrieves the CurrencyManager instance in a Composable function.
 * Use this instead of trying to inject CurrencyManager via hiltViewModel()
 */
@Composable
fun rememberCurrencyManager() = LocalContext.current.let { context ->
    remember(context) {
        context.currencyManager()
    }
} 