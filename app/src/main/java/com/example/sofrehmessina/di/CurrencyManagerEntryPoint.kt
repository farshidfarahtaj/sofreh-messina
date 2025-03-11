package com.example.sofrehmessina.di

import android.content.Context
import com.example.sofrehmessina.utils.CurrencyManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CurrencyManagerEntryPoint {
    fun currencyManager(): CurrencyManager
}

/**
 * Helper function to get CurrencyManager instance from any context
 */
fun Context.currencyManager(): CurrencyManager {
    val entryPoint = EntryPointAccessors.fromApplication(
        this.applicationContext,
        CurrencyManagerEntryPoint::class.java
    )
    return entryPoint.currencyManager()
} 