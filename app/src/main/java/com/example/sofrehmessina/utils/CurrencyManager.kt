package com.example.sofrehmessina.utils

import android.content.Context
import com.example.sofrehmessina.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages currency formatting and preferences throughout the app.
 * Default currency is Euro (€).
 */
@Singleton
class CurrencyManager @Inject constructor(
    private val context: Context,
    private val firebaseRepository: FirebaseRepository
) {
    
    companion object {
        const val CURRENCY_EURO = "EUR"
        const val CURRENCY_DOLLAR = "USD"
        const val CURRENCY_RIAL = "IRR"
        
        private const val PREF_NAME = "currency_prefs"
        private const val PREF_CURRENCY = "app_currency"
        private const val PREF_USE_ADMIN_CURRENCY = "use_admin_currency"
    }
    
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // State to hold the admin-defined currency
    private val _adminCurrency = MutableStateFlow(CURRENCY_EURO)
    val adminCurrency: StateFlow<String> = _adminCurrency.asStateFlow()
    
    // State to track if admin currency changes are enabled
    private val _useAdminCurrency = MutableStateFlow(true)
    val useAdminCurrency: StateFlow<Boolean> = _useAdminCurrency.asStateFlow()
    
    init {
        // Load preferences
        _useAdminCurrency.value = prefs.getBoolean(PREF_USE_ADMIN_CURRENCY, true)
        
        // Start listening for admin currency changes
        refreshAdminCurrency()
    }
    
    /**
     * Refresh the admin currency from Firestore
     */
    fun refreshAdminCurrency() {
        firebaseRepository.getAppSettings { settings ->
            settings?.defaultCurrency?.let { currency ->
                _adminCurrency.value = currency
            }
        }
    }
    
    /**
     * Update the default currency (admin only)
     */
    suspend fun updateDefaultCurrency(currencyCode: String): Boolean {
        return firebaseRepository.updateAppSettings(mapOf("defaultCurrency" to currencyCode))
    }
    
    /**
     * Set whether to use admin-defined currency for all users
     */
    fun setUseAdminCurrency(enabled: Boolean) {
        _useAdminCurrency.value = enabled
        prefs.edit().putBoolean(PREF_USE_ADMIN_CURRENCY, enabled).apply()
    }
    
    /**
     * Get the current currency code
     * If useAdminCurrency is true, return the admin-defined currency
     * Otherwise, return the user's preference (defaults to EUR)
     */
    fun getCurrency(): String {
        return if (_useAdminCurrency.value) {
            _adminCurrency.value
        } else {
            prefs.getString(PREF_CURRENCY, CURRENCY_EURO) ?: CURRENCY_EURO
        }
    }
    
    /**
     * Set the current currency code (user preference)
     */
    fun setCurrency(currencyCode: String) {
        prefs.edit().putString(PREF_CURRENCY, currencyCode).apply()
    }
    
    /**
     * Get the symbol for the specified currency code
     */
    fun getSymbol(currencyCode: String = getCurrency()): String {
        return when(currencyCode) {
            CURRENCY_EURO -> "€"
            CURRENCY_DOLLAR -> "$"
            CURRENCY_RIAL -> "﷼"
            else -> "€"
        }
    }
    
    /**
     * Format a price according to the current or specified currency
     */
    fun formatPrice(amount: Double, currencyCode: String = getCurrency()): String {
        val formatter = when(currencyCode) {
            CURRENCY_EURO -> NumberFormat.getCurrencyInstance(Locale.GERMANY)
            CURRENCY_DOLLAR -> NumberFormat.getCurrencyInstance(Locale.US)
            CURRENCY_RIAL -> NumberFormat.getCurrencyInstance(Locale("fa", "IR"))
            else -> NumberFormat.getCurrencyInstance(Locale.GERMANY)
        }
        
        formatter.currency = Currency.getInstance(currencyCode)
        return formatter.format(amount)
    }
    
    /**
     * Format a price without currency symbol
     */
    fun formatPriceWithoutSymbol(amount: Double, currencyCode: String = getCurrency()): String {
        val formatter = NumberFormat.getNumberInstance(
            when(currencyCode) {
                CURRENCY_EURO -> Locale.GERMANY
                CURRENCY_DOLLAR -> Locale.US
                CURRENCY_RIAL -> Locale("fa", "IR")
                else -> Locale.GERMANY
            }
        )
        
        return formatter.format(amount)
    }
    
    /**
     * Format a price with custom options
     */
    fun formatPriceCustom(amount: Double, showSymbol: Boolean = true, currencyCode: String = getCurrency()): String {
        return if (showSymbol) {
            formatPrice(amount, currencyCode)
        } else {
            formatPriceWithoutSymbol(amount, currencyCode)
        }
    }
} 