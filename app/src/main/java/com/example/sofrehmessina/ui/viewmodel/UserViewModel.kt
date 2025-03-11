package com.example.sofrehmessina.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.data.model.Order
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UserManagementState {
    object Initial : UserManagementState()
    object Loading : UserManagementState()
    object Success : UserManagementState()
    data class Error(val message: String) : UserManagementState()
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users
    
    private val _filteredUsers = MutableStateFlow<List<User>>(emptyList())
    val filteredUsers: StateFlow<List<User>> = _filteredUsers
    
    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser
    
    private val _userOrders = MutableStateFlow<List<Order>>(emptyList())
    val userOrders: StateFlow<List<Order>> = _userOrders
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _managementState = MutableStateFlow<UserManagementState>(UserManagementState.Initial)
    val managementState: StateFlow<UserManagementState> = _managementState
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // For user operations status
    private val _operationStatus = MutableStateFlow<OperationStatus>(OperationStatus.Idle)
    val operationStatus: StateFlow<OperationStatus> = _operationStatus
    
    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                repository.getAllUsers().collect { users ->
                    if (users.isNotEmpty()) {
                        _users.value = users
                        _filteredUsers.value = users
                    } else {
                        // If we get an empty list, consider if it's due to an error or actually no users
                        _error.value = "No users found or you may not have permission to view users"
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load users: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getUser(userId)
                .onSuccess { user ->
                    _selectedUser.value = user
                }
                .onFailure { e ->
                    _error.value = e.message
                }
            
            _isLoading.value = false
        }
    }
    
    fun loadUserOrders(userId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getUserOrders(userId)
                result.onSuccess { orders ->
                    _userOrders.value = orders
                }.onFailure { e ->
                    _error.value = e.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun updateUserRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            
            val currentUser = _selectedUser.value ?: return@launch
            val updatedUser = currentUser.copy(role = newRole)
            
            repository.updateUser(updatedUser)
                .onSuccess {
                    _selectedUser.value = updatedUser
                    _operationStatus.value = OperationStatus.Success("User role updated successfully")
                    
                    // Update users list if it contains this user
                    val currentUsers = _users.value.toMutableList()
                    val userIndex = currentUsers.indexOfFirst { it.id == userId }
                    if (userIndex >= 0) {
                        currentUsers[userIndex] = updatedUser
                        _users.value = currentUsers
                        filterUsersByRole(_users.value.firstOrNull { it.id == userId }?.role)
                    }
                }
                .onFailure { e ->
                    _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to update user role")
                }
        }
    }
    
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            
            repository.sendPasswordResetEmail(email)
                .onSuccess {
                    _operationStatus.value = OperationStatus.Success("Password reset email sent")
                }
                .onFailure { e ->
                    _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to send reset email")
                }
        }
    }
    
    fun disableUserAccount(userId: String) {
        viewModelScope.launch {
            _operationStatus.value = OperationStatus.Loading
            
            repository.disableUserAccount(userId)
                .onSuccess {
                    _operationStatus.value = OperationStatus.Success("User account disabled")
                    
                    // Update users list
                    val currentUsers = _users.value.toMutableList()
                    val userIndex = currentUsers.indexOfFirst { it.id == userId }
                    if (userIndex >= 0) {
                        currentUsers.removeAt(userIndex)
                        _users.value = currentUsers
                        _filteredUsers.value = _filteredUsers.value.filter { it.id != userId }
                    }
                    
                    // Clear selected user if it's the same one
                    if (_selectedUser.value?.id == userId) {
                        _selectedUser.value = null
                    }
                }
                .onFailure { e ->
                    _operationStatus.value = OperationStatus.Error(e.message ?: "Failed to disable user account")
                }
        }
    }
    
    fun filterUsersByRole(role: UserRole?) {
        _filteredUsers.value = if (role == null) {
            _users.value
        } else {
            _users.value.filter { it.role == role }
        }
    }
    
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _filteredUsers.value = _users.value
            return
        }
        
        _filteredUsers.value = _users.value.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
            user.familyName.contains(query, ignoreCase = true) ||
            user.email.contains(query, ignoreCase = true) ||
            user.phone.contains(query, ignoreCase = true)
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun setError(errorMessage: String) {
        _error.value = errorMessage
    }
    
    fun clearManagementState() {
        _managementState.value = UserManagementState.Initial
    }
    
    fun clearOperationStatus() {
        _operationStatus.value = OperationStatus.Idle
    }
    
    /**
     * Promote a user to admin role
     * @param userId The ID of the user to promote
     * @return Result indicating success or failure
     */
    suspend fun promoteToAdmin(userId: String): Result<Unit> {
        _isLoading.value = true
        _error.value = ""
        
        return try {
            val result = repository.promoteToAdmin(userId)
            _isLoading.value = false
            
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error occurred"
            }
            
            result
        } catch (e: Exception) {
            _isLoading.value = false
            _error.value = "Error: ${e.message}"
            Result.failure(e)
        }
    }
}

sealed class OperationStatus {
    data object Idle : OperationStatus()
    data object Loading : OperationStatus()
    data class Success(val message: String) : OperationStatus()
    data class Error(val message: String) : OperationStatus()
} 