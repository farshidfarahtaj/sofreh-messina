package com.example.sofrehmessina.ui.state

sealed class ChangePasswordState {
    @Suppress("unused")
    data object Initial : ChangePasswordState()
    data object Loading : ChangePasswordState()
    @Suppress("unused")
    data object Success : ChangePasswordState()
    data class Error(val message: String) : ChangePasswordState()
} 