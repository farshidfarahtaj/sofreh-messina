package com.example.sofrehmessina.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {

    @Suppress("unused")
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Initial)
    @Suppress("unused")
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private var loadJob: Job? = null

    fun loadUserProfile(userId: String) {
        // Cancel any previous loading job
        loadJob?.cancel()
        
        // Start a new loading job
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _profileState.value = ProfileState.Loading
            _error.value = null // Reset error state

            try {
                // Add a timeout for the repository operation to prevent hanging
                withTimeoutOrNull(3000) { // 3-second timeout, shorter to prevent ANR
                    repository.getUser(userId)
                        .onSuccess { userData ->
                            withContext(Dispatchers.Main) { // Switch to Main thread for UI updates
                                _user.value = userData
                                _profileState.value = ProfileState.Success(userData)
                                Log.d("ProfileViewModel", "Successfully loaded profile data for $userId")
                            }
                        }
                        .onFailure { e ->
                            withContext(Dispatchers.Main) { // Switch to Main thread for UI updates
                                _error.value = e
                                _profileState.value = ProfileState.Error(e)
                            }
                            Log.e("ProfileViewModel", "Failed to load user profile: ${e.message}", e)
                        }
                } ?: run {
                    // This runs if the timeout is reached
                    withContext(Dispatchers.Main) {
                        val timeoutError = Exception("Profile loading timed out. Please try again.")
                        _error.value = timeoutError
                        _profileState.value = ProfileState.Error(timeoutError)
                    }
                    Log.e("ProfileViewModel", "Timeout loading profile for user: $userId")
                }
            } catch (e: Exception) {
                // Handle timeouts and other exceptions
                val finalError = when (e) {
                    is kotlinx.coroutines.TimeoutCancellationException -> 
                        Exception("Failed to load profile: operation timed out", e)
                    is CancellationException -> {
                        Log.d("ProfileViewModel", "Profile loading was cancelled")
                        null // Don't report cancellation as an error
                    }
                    else -> e
                }
                
                finalError?.let {
                    withContext(Dispatchers.Main) {
                        _error.value = it
                        _profileState.value = ProfileState.Error(it)
                    }
                    Log.e("ProfileViewModel", "Error loading profile: ${it.message}", it)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                _profileState.value = ProfileState.Loading
            }
            
            try {
                // Add a timeout to prevent hanging
                withTimeoutOrNull(3000) {
                    repository.updateUser(user)
                        .onSuccess { updatedUser ->
                            withContext(Dispatchers.Main) {
                                _user.value = updatedUser
                                _profileState.value = ProfileState.Success(updatedUser)
                            }
                        }
                        .onFailure { e ->
                            withContext(Dispatchers.Main) {
                                _error.value = e
                                _profileState.value = ProfileState.Error(e)
                            }
                        }
                } ?: run {
                    // This runs if the timeout is reached
                    withContext(Dispatchers.Main) {
                        val timeoutError = Exception("Profile update timed out. Please try again.")
                        _error.value = timeoutError
                        _profileState.value = ProfileState.Error(timeoutError)
                    }
                    Log.e("ProfileViewModel", "Timeout updating profile for user: ${user.id}")
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Don't treat cancellation as an error
                    Log.d("ProfileViewModel", "Profile update was cancelled")
                } else {
                    withContext(Dispatchers.Main) {
                        _error.value = e
                        _profileState.value = ProfileState.Error(e)
                    }
                    Log.e("ProfileViewModel", "Error updating profile: ${e.message}", e)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    @Suppress("unused")
    fun createUserProfile(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _profileState.value = ProfileState.Loading
            try {
                repository.createUser(user)
                    .onSuccess { createdUser ->
                        _user.value = createdUser
                        _profileState.value = ProfileState.Success(createdUser)
                    }
                    .onFailure { e ->
                        _error.value = e
                        _profileState.value = ProfileState.Error(e)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
        if (_profileState.value is ProfileState.Error) {
            _profileState.value = ProfileState.Initial
        }
    }

    sealed class ProfileState {
        data object Initial : ProfileState()
        data object Loading : ProfileState()
        data class Success(val user: User) : ProfileState()
        data class Error(val error: Throwable) : ProfileState()
    }
} 