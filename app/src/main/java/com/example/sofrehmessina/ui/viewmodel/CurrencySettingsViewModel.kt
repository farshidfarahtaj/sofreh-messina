package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.utils.CurrencyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencySettingsViewModel @Inject constructor(
    private val currencyManager: CurrencyManager,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(currencyManager.getCurrency())
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // List of available currencies
    val availableCurrencies = listOf(
        CurrencyManager.CURRENCY_EURO,
        CurrencyManager.CURRENCY_DOLLAR,
        CurrencyManager.CURRENCY_RIAL
    )

    // Currency names for display
    val currencyNames = mapOf(
        CurrencyManager.CURRENCY_EURO to "Euro (€)",
        CurrencyManager.CURRENCY_DOLLAR to "US Dollar ($)",
        CurrencyManager.CURRENCY_RIAL to "Iranian Rial (﷼)"
    )

    init {
        // Check if user is admin
        viewModelScope.launch {
            val currentUserId = firebaseRepository.getCurrentUserId()
            if (currentUserId != null) {
                val adminResult = firebaseRepository.isCurrentUserAdmin()
                _isAdmin.value = adminResult.getOrNull() ?: false
            }
        }

        // Get current currency
        refreshCurrency()
    }

    fun refreshCurrency() {
        viewModelScope.launch {
            currencyManager.refreshAdminCurrency()
            _selectedCurrency.value = currencyManager.getCurrency()
        }
    }

    fun updateCurrency(currencyCode: String) {
        if (_isAdmin.value) {
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                try {
                    val success = currencyManager.updateDefaultCurrency(currencyCode)
                    if (success) {
                        _selectedCurrency.value = currencyCode
                        _successMessage.value = "Currency updated successfully to ${currencyNames[currencyCode]}"
                        refreshCurrency()
                    } else {
                        _errorMessage.value = "Failed to update currency. Please try again."
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _errorMessage.value = "Only admins can update the default currency"
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
} 