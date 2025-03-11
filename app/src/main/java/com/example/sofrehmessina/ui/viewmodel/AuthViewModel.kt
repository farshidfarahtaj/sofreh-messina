package com.example.sofrehmessina.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sofrehmessina.BuildConfig
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.utils.AuthSecurityHelper
import com.example.sofrehmessina.utils.SecureLogger
import com.example.sofrehmessina.utils.SecureLogger.EventType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

// Add ChangePasswordState sealed class
sealed class ChangePasswordState {
    object Initial : ChangePasswordState()
    object Loading : ChangePasswordState()
    object Success : ChangePasswordState()
    data class Error(val message: String) : ChangePasswordState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Add changePasswordState StateFlow
    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Initial)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error

    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent
    
    // Track last auth time for session management
    private var lastAuthValidationTime = System.currentTimeMillis()
    private val sessionTimeout = BuildConfig.SESSION_TIMEOUT_MINUTES.minutes.inWholeMilliseconds
    
    private val _securityState = MutableStateFlow(SecurityState())
    val securityState: StateFlow<SecurityState> = _securityState

    init {
        viewModelScope.launch {
            // Set default locale for Firebase
            configureFirebaseLanguage()
            
            // Disable app verification on debug builds for easier testing
            if (BuildConfig.DEBUG) {
                val firebaseAuthSettings: FirebaseAuthSettings = auth.firebaseAuthSettings
                firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            }
            
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                loadUserProfile(currentUserId)
                validateUserSession() // Validate session on init
            }
        }
    }
    
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUserData(userId)
                .onSuccess { user ->
                    _currentUser.value = user
                    updateSecurityState(user)
                }
                .onFailure { exception ->
                    _error.value = exception
                    SecureLogger.e("Failed to load user profile: ${exception.message}", exception)
                }
            _isLoading.value = false
        }
    }
    
    // Update the security state based on user information
    private fun updateSecurityState(user: User) {
        val lastPasswordChange = user.lastPasswordChange ?: Date(0)
        val passwordAgeInDays = (System.currentTimeMillis() - lastPasswordChange.time) / (1000 * 60 * 60 * 24)
        
        _securityState.value = _securityState.value.copy(
            isPasswordExpired = passwordAgeInDays > 90,
            isAccountDisabled = user.disabled == true,
            passwordStrength = calculatePasswordStrength(user.passwordStrength ?: 0),
            lastLogin = user.lastLogin,
            lastFailedLoginAttempt = user.lastFailedLogin
        )
        
        // Log security information - only log once when the user first logs in
        // to avoid excessive logging for expired passwords
        if (passwordAgeInDays > 90 && (user.lastLogin == null || 
            (System.currentTimeMillis() - user.lastLogin.time) < 60000)) { // Only log within first minute of login
            SecureLogger.security(
                EventType.SECURITY_VIOLATION,
                "User password expired (${passwordAgeInDays.toInt()} days old)",
                SecureLogger.Level.WARNING,
                user.id
            )
        }
    }
    
    // Simple password strength categorization
    private fun calculatePasswordStrength(strengthScore: Int): PasswordStrength {
        return when {
            strengthScore <= 2 -> PasswordStrength.WEAK
            strengthScore <= 6 -> PasswordStrength.MODERATE
            else -> PasswordStrength.STRONG
        }
    }

    // Add changePassword method
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading
            try {
                // Security checks - ensure new password meets security policy
                if (newPassword.length < BuildConfig.MIN_PASSWORD_LENGTH) {
                    _changePasswordState.value = ChangePasswordState.Error(
                        "Password must be at least ${BuildConfig.MIN_PASSWORD_LENGTH} characters"
                    )
                    return@launch
                }
                
                if (!isPasswordStrong(newPassword)) {
                    _changePasswordState.value = ChangePasswordState.Error(
                        "Password must include uppercase, lowercase, number, and special character"
                    )
                    return@launch
                }
                
                // Re-authenticate to confirm current password
                val user = auth.currentUser ?: throw Exception("User not logged in")
                val email = user.email ?: throw Exception("Email not available")
                
                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider
                        .getCredential(email, currentPassword)
                    
                    user.reauthenticate(credential).await()
                    
                    // Update password
                    user.updatePassword(newPassword).await()
                    
                    // Update last password change date
                    val currentUserId = user.uid
                    val userData = _currentUser.value ?: throw Exception("User data not available")
                    val updatedUser = userData.copy(
                        lastPasswordChange = Date(),
                        passwordStrength = calculatePasswordStrengthScore(newPassword)
                    )
                    
                    repository.saveUserData(updatedUser)
                        .onSuccess {
                            _currentUser.value = updatedUser
                            updateSecurityState(updatedUser)
                            
                            SecureLogger.security(
                                EventType.PASSWORD_CHANGE,
                                "Password changed successfully for user $currentUserId",
                                SecureLogger.Level.INFO,
                                currentUserId
                            )
                            
                            _changePasswordState.value = ChangePasswordState.Success
                        }
                        .onFailure { e ->
                            SecureLogger.security(
                                EventType.PASSWORD_CHANGE,
                                "Failed to update password change date: ${e.message}",
                                SecureLogger.Level.WARNING,
                                currentUserId
                            )
                            // Password was changed but we couldn't update the metadata
                            _changePasswordState.value = ChangePasswordState.Success
                        }
                } catch (e: Exception) {
                    SecureLogger.security(
                        EventType.PASSWORD_CHANGE,
                        "Failed to change password: ${e.message}",
                        SecureLogger.Level.WARNING,
                        user.uid
                    )
                    
                    val errorMessage = when {
                        e.message?.contains("credential is invalid") == true -> 
                            "Current password is incorrect"
                        else -> e.message ?: "An unexpected error occurred"
                    }
                    
                    _changePasswordState.value = ChangePasswordState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _changePasswordState.value = ChangePasswordState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    // Calculate password strength score (0-10)
    private fun calculatePasswordStrengthScore(password: String): Int {
        var score = 0
        
        // Length checks
        if (password.length >= 8) score += 1
        if (password.length >= 10) score += 1
        if (password.length >= 12) score += 1
        
        // Character variety checks
        if (password.any { it.isUpperCase() }) score += 1
        if (password.any { it.isLowerCase() }) score += 1
        if (password.any { it.isDigit() }) score += 1
        if (password.any { !it.isLetterOrDigit() }) score += 1
        
        // Pattern checks
        if (!password.matches(Regex(".*(.)(\\1){2,}.*"))) score += 1 // No repeating characters
        if (password.matches(Regex(".*[^a-zA-Z0-9]{2,}.*"))) score += 1 // Multiple special chars
        if (password.length >= 16) score += 1 // Bonus for extra length
        
        return score
    }
    
    // Check if password meets strength requirements
    private fun isPasswordStrong(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                // Apply brute force protection
                if (BuildConfig.ENABLE_BRUTE_FORCE_PROTECTION) {
                    if (!AuthSecurityHelper.canAttemptAuth(context)) {
                        _authState.value = AuthState.Error("Too many attempts. Try again later.")
                        return@launch
                    }
                    
                    // Apply dynamic rate limiting
                    AuthSecurityHelper.applyDynamicRateLimiting(context)
                }
                
                _isLoading.value = true
                _authState.value = AuthState.Loading
                
                repository.signIn(email, password)
                    .onSuccess { user ->
                        if (BuildConfig.ENABLE_BRUTE_FORCE_PROTECTION) {
                            AuthSecurityHelper.recordSuccessfulAuth(context)
                        }
                        
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        
                        // Update the last login time
                        val updatedUser = user.copy(
                            lastLogin = Date()
                        )
                        repository.saveUserData(updatedUser)
                            .onSuccess {
                                _currentUser.value = updatedUser
                                updateSecurityState(updatedUser)
                            }
                        
                        lastAuthValidationTime = System.currentTimeMillis()
                    }
                    .onFailure { e ->
                        // Handle reCAPTCHA issues specifically
                        val errorMessage = e.message ?: "Authentication failed"
                        val finalMessage = when {
                            errorMessage.contains("CAPTCHA") || errorMessage.contains("reCAPTCHA") -> 
                                "Login temporarily blocked due to unusual activity. Please try again later or reset your password."
                            else -> errorMessage
                        }
                        
                        SecureLogger.security(
                            EventType.FAILED_LOGIN,
                            "Login failed with error: $finalMessage",
                            SecureLogger.Level.WARNING
                        )
                        
                        // Record failed attempt for brute force protection
                        if (BuildConfig.ENABLE_BRUTE_FORCE_PROTECTION) {
                            val isLocked = AuthSecurityHelper.recordFailedAttempt(context)
                            if (isLocked) {
                                _authState.value = AuthState.Error("Account locked. Try again later.")
                                return@onFailure
                            }
                        }
                        
                        _error.value = e
                        _authState.value = AuthState.Error(finalMessage)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(
        email: String, 
        password: String, 
        name: String, 
        phoneNumber: String,
        familyName: String = "",
        address: String = "",
        postalCode: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            try {
                val newUser = User(
                    name = name,
                    familyName = familyName,
                    email = email,
                    phone = phoneNumber,
                    address = address,
                    postalCode = postalCode,
                    role = UserRole.USER,
                    lastPasswordChange = Date(),
                    passwordStrength = calculatePasswordStrengthScore(password)
                )
                
                repository.signUp(email, password, newUser)
                    .onSuccess { user ->
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        lastAuthValidationTime = System.currentTimeMillis()
                    }
                    .onFailure { e ->
                        _error.value = e
                        _authState.value = AuthState.Error(e.message ?: "Registration failed")
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            try {
                repository.signInAsGuest()
                    .onSuccess { user ->
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                        lastAuthValidationTime = System.currentTimeMillis()
                    }
                    .onFailure { e ->
                        _error.value = e
                        _authState.value = AuthState.Error(e.message ?: "Guest sign-in failed")
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        val userId = auth.currentUser?.uid
        
        // Log the logout event before actually signing out
        if (userId != null) {
            SecureLogger.security(
                EventType.USER_LOGOUT,
                "User logged out: $userId",
                SecureLogger.Level.INFO,
                userId
            )
        }
        
        repository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Initial
        _logoutEvent.value = true
        
        // Clear security state
        _securityState.value = SecurityState()
    }

    fun updateProfile(
        name: String,
        familyName: String,
        phoneNumber: String,
        address: String,
        postalCode: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUserValue = _currentUser.value ?: return@launch
                
                val updatedUser = currentUserValue.copy(
                    name = name,
                    familyName = familyName,
                    phone = phoneNumber,
                    address = address,
                    postalCode = postalCode
                )
                
                repository.saveUserData(updatedUser)
                    .onSuccess {
                        _currentUser.value = updatedUser
                        _authState.value = AuthState.Authenticated(updatedUser)
                    }
                    .onFailure { e ->
                        _error.value = e
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Initial
        }
    }

    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }

    /**
     * Validates that the user's session is still active and token is valid
     * This helps prevent session hijacking and enforce proper timeouts
     * @return Boolean indicating if the session is still valid
     */
    suspend fun validateUserSession(): Boolean {
        try {
            // If there's no current user, session is invalid
            val firebaseUser = auth.currentUser ?: return false
            
            // Check if we need to validate based on timeout
            val now = System.currentTimeMillis()
            if (now - lastAuthValidationTime < 60000) { // Only check once a minute at most
                return true
            }
            
            // Update the last validation time
            lastAuthValidationTime = now
            
            // Force token refresh to check with Firebase servers
            // This will fail if the token is revoked or expired
            val tokenResult = firebaseUser.getIdToken(true).await()
            
            // Validate token integrity
            val isValidToken = repository.validateTokenIntegrity(tokenResult?.token ?: "")
            if (!isValidToken) {
                SecureLogger.security(
                    EventType.SECURITY_VIOLATION,
                    "Invalid token format detected for user ${firebaseUser.uid}",
                    SecureLogger.Level.ERROR,
                    firebaseUser.uid
                )
                return false
            }
            
            // Check if token was successfully retrieved and is valid
            val isValid = tokenResult != null && !tokenResult.token.isNullOrEmpty()
            
            // Check token expiration (tokens expire after 1 hour by default)
            val expirationTimestamp = tokenResult?.expirationTimestamp ?: 0
            val currentTime = System.currentTimeMillis()
            val isExpired = expirationTimestamp < currentTime
            
            if (isExpired) {
                SecureLogger.w("User token is expired, requesting refresh")
                // Token is expired, try to get a new one (will fail if user is truly invalid)
                val newToken = firebaseUser.getIdToken(true).await()
                return newToken != null && !newToken.token.isNullOrEmpty()
            }
            
            // For additional security, check if the user's account still exists
            val userSnapshot = repository.getCurrentUserData().getOrNull()
            val userExists = userSnapshot != null
            
            if (!userExists) {
                SecureLogger.w("User exists in auth but not in database")
            }
            
            // Also check if user account is disabled
            if (userSnapshot?.disabled == true) {
                SecureLogger.security(
                    EventType.SECURITY_VIOLATION,
                    "Disabled account attempted to use session: ${firebaseUser.uid}",
                    SecureLogger.Level.WARNING,
                    firebaseUser.uid
                )
                return false
            }
            
            // Check if session has exceeded max lifetime
            val sessionMaxLifetime = sessionTimeout
            val currentUserValue = _currentUser.value
            if (currentUserValue?.lastLogin != null) {
                val sessionDuration = currentTime - currentUserValue.lastLogin.time
                if (sessionDuration > sessionMaxLifetime) {
                    SecureLogger.security(
                        EventType.SECURITY_VIOLATION,
                        "Session exceeded maximum lifetime of ${BuildConfig.SESSION_TIMEOUT_MINUTES} minutes",
                        SecureLogger.Level.WARNING,
                        firebaseUser.uid
                    )
                    return false
                }
            }
            
            return isValid && userExists
        } catch (e: Exception) {
            SecureLogger.e("Error validating user session: ${e.message}", e)
            return false
        }
    }

    /**
     * Refreshes the current user data from the repository.
     * This is useful after role changes or user data updates.
     */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            auth.currentUser?.let { firebaseUser ->
                val userId = firebaseUser.uid
                val result = repository.getUser(userId)
                result.onSuccess { user ->
                    _currentUser.value = user
                    SecureLogger.i("User data refreshed for ID: $userId")
                }.onFailure { e ->
                    SecureLogger.e("Failed to refresh user data: ${e.message}", e)
                    _isLoading.value = false
                    _error.value = e
                }
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState.Loading
            try {
                repository.sendPasswordResetEmail(email)
                    .onSuccess {
                        SecureLogger.security(
                            EventType.SECURITY_VIOLATION,
                            "Password reset email sent to ${email.substringBefore('@')}@*****",
                            SecureLogger.Level.INFO
                        )
                        _changePasswordState.value = ChangePasswordState.Success
                    }
                    .onFailure { e ->
                        SecureLogger.security(
                            EventType.SECURITY_VIOLATION,
                            "Failed to send password reset email: ${e.message}",
                            SecureLogger.Level.WARNING
                        )
                        _changePasswordState.value = ChangePasswordState.Error(e.message ?: "Failed to send reset email")
                    }
            } catch (e: Exception) {
                _changePasswordState.value = ChangePasswordState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    // Create a separate method to configure Firebase language
    private fun configureFirebaseLanguage() {
        try {
            auth.setLanguageCode(Locale.getDefault().language)
        } catch (e: Exception) {
            // Log the exception but don't crash
            SecureLogger.e("Failed to set Firebase language: ${e.message}", e)
        }
    }

    // Add a method to get the FirebaseAuth instance
    fun getFirebaseAuth(): FirebaseAuth {
        return auth
    }

    // Add a method to refresh user data from Firestore
    fun refreshUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUserData(userId, forceRefresh = true)
                .onSuccess { user ->
                    _currentUser.value = user
                    updateSecurityState(user)
                    
                    // Log if user is admin for debugging
                    if (user.role == UserRole.ADMIN) {
                        SecureLogger.i("User refreshed with ADMIN role: ${user.id}")
                    } else {
                        SecureLogger.i("User refreshed with role: ${user.role}: ${user.id}")
                    }
                }
                .onFailure { exception ->
                    _error.value = exception
                    SecureLogger.e("Failed to refresh user profile: ${exception.message}", exception)
                }
            _isLoading.value = false
        }
    }

    /**
     * Force a complete check of admin status by directly querying Firestore
     * This bypasses all caching to ensure we have the most up-to-date role information
     */
    fun forceAdminStatusCheck(userId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // Use SERVER source to bypass all caching
                val result = repository.getUserData(userId, forceRefresh = true)
                
                result.onSuccess { user ->
                    // Update the current user in state
                    _currentUser.value = user
                    
                    // Call the callback with the admin status
                    val isAdmin = user.role == UserRole.ADMIN
                    SecureLogger.i("Admin status check: user ${user.id} isAdmin=$isAdmin")
                    onComplete(isAdmin)
                }.onFailure { exception ->
                    SecureLogger.e("Admin status check failed: ${exception.message}", exception)
                    onComplete(false) // Assume not admin on failure
                }
            } catch (e: Exception) {
                SecureLogger.e("Error in forceAdminStatusCheck: ${e.message}", e)
                onComplete(false) // Assume not admin on exception
            }
        }
    }
}

// Security state to track security-related information
data class SecurityState(
    val isPasswordExpired: Boolean = false,
    val isAccountDisabled: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.UNKNOWN,
    val lastLogin: Date? = null,
    val lastFailedLoginAttempt: Date? = null
)

// Password strength categories
enum class PasswordStrength {
    UNKNOWN, WEAK, MODERATE, STRONG
}

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
} 