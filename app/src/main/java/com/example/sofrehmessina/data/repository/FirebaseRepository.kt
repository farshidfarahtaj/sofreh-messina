package com.example.sofrehmessina.data.repository

import com.example.sofrehmessina.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*
import android.util.Log
import android.content.Context
import com.example.sofrehmessina.BuildConfig
import com.example.sofrehmessina.utils.SecurityUtils
import com.example.sofrehmessina.utils.SecureLogger
import com.example.sofrehmessina.utils.SecureLogger.EventType
import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.sofrehmessina.data.model.Category
import com.example.sofrehmessina.data.model.CategoryTranslation
import com.google.gson.Gson
import kotlinx.coroutines.delay
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

@Singleton
class FirebaseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val application: Application
) {
    companion object {
        private const val USER_CACHE_TTL = 60000L // 60 seconds TTL for user cache
        private const val TAG = "FirebaseRepository"
    }
    
    // Cache for categories and food items to improve performance
    private var _categoriesCache: List<Category>? = null
    private var _foodItemsCache: List<Food>? = null
    
    // Add a user cache to prevent repeated database calls
    private val userCache = ConcurrentHashMap<String, Pair<User, Long>>()
    
    /**
     * Returns the application context
     * Used for memory management and diagnostics
     */
    fun getApplicationContext(): Context {
        return application
    }
    
    /**
     * Safely converts Any? to Map<String, Any> with proper type checking
     * Eliminates unchecked cast warnings
     */
    @Suppress("UNCHECKED_CAST")
    private fun safeMapCast(value: Any?): Map<String, Any>? {
        return if (value is Map<*, *>) {
            try {
                // Verify that all keys are strings
                if (value.keys.all { it is String }) {
                    value as Map<String, Any>
                } else {
                    SecureLogger.e("Map contains non-string keys", null, TAG)
                    null
                }
            } catch (e: Exception) {
                SecureLogger.e("Error casting to Map<String, Any>: ${e.message}", e, TAG)
                null
            }
        } else {
            null
        }
    }
    
    private fun <T> handleException(e: Exception, operationName: String): Result<T> {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "$operationName was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error during $operationName: ${e.message}", e)
            return Result.failure(e)
        }
    }
    
    // Safely process user data by encrypting sensitive fields using our utility class
    private fun encryptUserData(user: User): User {
        if (!BuildConfig.ENABLE_ENCRYPTION) return user
        
        // Don't encrypt non-sensitive fields like contact information
        // Only encrypt truly sensitive fields (currently no fields need encryption)
        return user
        
        // Previous implementation that encrypted contact info:
        /*
        return user.copy(
            address = if (user.address.isNotEmpty()) SecurityUtils.encrypt(user.address) else user.address,
            phone = if (user.phone.isNotEmpty()) SecurityUtils.encrypt(user.phone) else user.phone,
            postalCode = if (user.postalCode.isNotEmpty()) SecurityUtils.encrypt(user.postalCode) else user.postalCode
        )
        */
    }
    
    // Safely retrieves user data by decrypting sensitive fields using our utility class
    private fun decryptUserData(user: User): User {
        if (!BuildConfig.ENABLE_ENCRYPTION) return user
        
        // Attempt to decrypt fields that might have been encrypted previously
        // This ensures backward compatibility with previously stored encrypted data
        return try {
            user.copy(
                address = if (user.address.isNotEmpty()) SecurityUtils.decrypt(user.address) else user.address,
                phone = if (user.phone.isNotEmpty()) SecurityUtils.decrypt(user.phone) else user.phone,
                postalCode = if (user.postalCode.isNotEmpty()) SecurityUtils.decrypt(user.postalCode) else user.postalCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting user data: ${e.message}", e)
            // Return original user if decryption fails
            user
        }
    }

    // Authentication
    suspend fun signIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Log sign in attempt (masking part of the email for privacy)
            SecureLogger.security(
                EventType.USER_LOGIN,
                "Sign in attempt for email: ${email.substringBefore('@')}@*****",
                SecureLogger.Level.INFO
            )
            
            // Input validation for security
            if (email.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("Email and password cannot be empty"))
            }
            
            // Check password strength for security auditing (but don't block login)
            if (password.length < BuildConfig.MIN_PASSWORD_LENGTH) {
                SecureLogger.w("Password is below minimum recommended length: ${password.length} < ${BuildConfig.MIN_PASSWORD_LENGTH}")
            }
            
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("User ID not available after sign in")
                
                // Log successful auth
                SecureLogger.security(
                    EventType.USER_LOGIN,
                    "Successful sign in for user ID: $userId",
                    SecureLogger.Level.INFO,
                    userId
                )
                
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
                
                val baseUser = userDoc.toObject(User::class.java)
                
                // Get the settings field from the user document
                val settingsMap = safeMapCast(userDoc.get("settings"))
                val userSettings = if (settingsMap != null) {
                    try {
                        // Convert the map to a Settings object
                        val gson = Gson()
                        val json = gson.toJson(settingsMap)
                        gson.fromJson(json, Settings::class.java)
                    } catch (e: Exception) {
                        SecureLogger.e("Error deserializing settings in signIn: ${e.message}", e)
                        Settings() // Fallback to default settings
                    }
                } else {
                    Settings() // Default settings if the field doesn't exist
                }
                
                val user = baseUser?.copy(id = userId, settings = userSettings)
                    ?: throw Exception("User data not found")
                    
                // Check if user account is disabled
                if (user.disabled == true) {
                    SecureLogger.security(
                        EventType.SECURITY_VIOLATION,
                        "Attempt to sign in to disabled account: $userId",
                        SecureLogger.Level.WARNING,
                        userId
                    )
                    return@withContext Result.failure(Exception("Your account has been disabled. Please contact support."))
                }
                
                return@withContext Result.success(user)
            } catch (e: Exception) {
                // Handle Firebase auth exceptions more specifically
                val errorMessage = when {
                    e.message?.contains("There is no user record") == true -> 
                        "No account found with this email"
                    e.message?.contains("password is invalid") == true -> 
                        "Incorrect password"
                    e.message?.contains("CAPTCHA") == true || e.message?.contains("reCAPTCHA") == true ->
                        "Login blocked temporarily for security reasons. Please try again later or reset your password."
                    e.message?.contains("blocked") == true || e.message?.contains("temporarily disabled") == true ->
                        "This account is temporarily locked due to too many failed login attempts. Please try again later."
                    else -> e.message ?: "Authentication failed"
                }
                
                SecureLogger.security(
                    EventType.FAILED_LOGIN,
                    "Failed login attempt: $errorMessage",
                    SecureLogger.Level.WARNING
                )
                
                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            // Handle any unexpected exceptions
            SecureLogger.e("Unexpected error during sign in: ${e.message}", e, TAG)
            return@withContext Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, newUser: User): Result<User> {
        return try {
            // DEBUG: Log the start of the signup process
            Log.d(TAG, "Starting signUp process for email: ${email.substringBefore('@')}@***")
            
            // DEBUG: Check current auth state
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "WARNING: Already logged in as ${currentUser.uid} - will sign out first")
                auth.signOut()
            }
            
            // DEBUG: Test Firestore connection
            try {
                val testWrite = firestore.collection("debug_test").document("test_${System.currentTimeMillis()}").set(mapOf("test" to true))
                testWrite.await()
                Log.d(TAG, "Firestore test write succeeded - connection is working")
            } catch (testE: Exception) {
                Log.e(TAG, "Firestore test write FAILED! Connection issue detected: ${testE.message}", testE)
            }
            
            // Step 1: Create the user in Firebase Auth
            Log.d(TAG, "Attempting to create user in Firebase Authentication")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")
            
            Log.d(TAG, "User created in Authentication with ID: $userId")
            
            // DEBUG: Check auth token
            try {
                val token = authResult.user?.getIdToken(true)?.await()?.token
                Log.d(TAG, "Auth token first 10 chars: ${token?.take(10)}*** (length: ${token?.length ?: 0})")
            } catch (tokenE: Exception) {
                Log.e(TAG, "Failed to get auth token: ${tokenE.message}", tokenE)
            }
            
            // Create a complete user object with all fields
            val fullUser = newUser.copy(id = userId)
            
            // DEBUG: Log the exact user data we're saving
            Log.d(TAG, "Saving complete user data to Firestore: $fullUser")
            
            try {
                Log.d(TAG, "Attempting to save COMPLETE user data to Firestore")
                
                // Use the saveUserData method to save the complete user profile
                saveUserData(fullUser).onSuccess {
                    Log.d(TAG, "User data saved successfully using saveUserData")
                    Result.success(fullUser)
                }.onFailure { e ->
                    Log.e(TAG, "Error saving user data with saveUserData: ${e.message}", e)
                    throw e
                }
                
                Result.success(fullUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user to Firestore: ${e.message}", e)
                Log.e(TAG, "Full exception stacktrace", e)
                
                // Extract more details from the exception
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    Log.e(TAG, "PERMISSION DENIED - Firestore rules are blocking this write!")
                }
                if (e.message?.contains("UNAVAILABLE") == true) {
                    Log.e(TAG, "SERVICE UNAVAILABLE - Network or Firebase service issue!")
                }
                
                // Clean up auth user if Firestore save fails
                try {
                    Log.d(TAG, "Cleaning up auth user after Firestore save failure")
                    authResult.user?.delete()?.await()
                    Log.d(TAG, "Cleaned up auth user after Firestore save failure")
                } catch (deleteEx: Exception) {
                    Log.e(TAG, "Failed to clean up auth user: ${deleteEx.message}", deleteEx)
                }
                Result.failure(Exception("Failed to save user data: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during user signup: ${e.message}", e)
            Log.e(TAG, "Full exception stacktrace", e)
            Result.failure(e)
        }
    }
    
    // Check if password meets strength requirements
    private fun isPasswordStrong(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar
    }
    
    /**
     * Refreshes the categories data from Firestore
     * Used when language changes to ensure we have the latest translations
     */
    suspend fun refreshCategories() {
        try {
            Log.d(TAG, "Refreshing categories data from server")
            // Clear any cached category data
            _categoriesCache = null
            
            // Force a new fetch from Firestore with cache disabled
            val categories = firestore.collection("categories")
                .get(com.google.firebase.firestore.Source.SERVER) // Force server fetch, no cache
                .await()
                .documents
                .mapNotNull { 
                    try {
                        val category = it.toObject(Category::class.java)?.copy(id = it.id)
                        Log.d(TAG, "Refreshed category ${it.id} with ${category?.translations?.size ?: 0} translations")
                        category
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category ${it.id}: ${e.message}")
                        null
                    }
                }
                
            // Update the cache with fresh data
            _categoriesCache = categories
            Log.d(TAG, "Categories refresh complete. Loaded ${categories.size} categories")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing categories: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Refreshes the food items data from Firestore
     * Used when language changes to ensure we have the latest translations
     */
    suspend fun refreshFoodItems() {
        try {
            Log.d(TAG, "Refreshing food items data from server")
            // Clear any cached food data
            _foodItemsCache = null
            
            // Force a new fetch from Firestore with cache disabled
            val foodItems = firestore.collection("food")
                .get(com.google.firebase.firestore.Source.SERVER) // Force server fetch, no cache
                .await()
                .documents
                .mapNotNull { 
                    try {
                        val food = it.toObject(Food::class.java)?.copy(id = it.id)
                        Log.d(TAG, "Refreshed food item ${it.id} with ${food?.translations?.size ?: 0} translations")
                        food
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing food item ${it.id}: ${e.message}")
                        null
                    }
                }
                
            // Update the cache with fresh data
            _foodItemsCache = foodItems
            
            Log.d(TAG, "Food items refresh complete. Loaded ${foodItems.size} food items")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing food items: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Gets current user data with proper decryption
     */
    suspend fun getCurrentUserData(): Result<User> {
        try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("No authenticated user"))
            val userId = currentUser.uid
            
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
                
            val baseUser = userDoc.toObject(User::class.java)
                
            // Get the settings field from the user document
            val settingsMap = safeMapCast(userDoc.get("settings"))
            val userSettings = if (settingsMap != null) {
                try {
                    // Convert the map to a Settings object
                    val gson = Gson()
                    val json = gson.toJson(settingsMap)
                    gson.fromJson(json, Settings::class.java)
                } catch (e: Exception) {
                    SecureLogger.e("Error deserializing settings in getCurrentUserData: ${e.message}", e)
                    Settings() // Fallback to default settings
                }
            } else {
                Settings() // Default settings if the field doesn't exist
            }
            
            val userData = baseUser?.copy(id = userId, settings = userSettings) 
                ?: return Result.failure(Exception("User data not found"))
                
            // Decrypt sensitive data before returning
            val decryptedUser = decryptUserData(userData)
            
            SecureLogger.security(
                EventType.DATA_ACCESS,
                "User data retrieved for ID: $userId",
                SecureLogger.Level.INFO,
                userId
            )
            
            return Result.success(decryptedUser)
        } catch (e: Exception) {
            SecureLogger.e("Get current user error: ${e.message}", e, TAG)
            return Result.failure(e)
        }
    }

    suspend fun signInAsGuest(): Result<User> {
        try {
            SecureLogger.security(
                EventType.USER_LOGIN,
                "Guest sign-in attempt",
                SecureLogger.Level.INFO
            )
            
            // First sign in anonymously to get an auth token
            val authResult = auth.signInAnonymously().await()
            val anonymousUserId = authResult.user?.uid ?: throw Exception("Failed to create anonymous user")
            
            // Check if this anonymous user already has a user document
            val existingUserDoc = firestore.collection("users")
                .document(anonymousUserId)
                .get()
                .await()
            
            if (existingUserDoc.exists()) {
                // If this anonymous user already has a document, use it
                val baseUser = existingUserDoc.toObject(User::class.java)
                
                // Get the settings field from the user document
                val settingsMap = safeMapCast(existingUserDoc.get("settings"))
                val userSettings = if (settingsMap != null) {
                    try {
                        // Convert the map to a Settings object
                        val gson = Gson()
                        val json = gson.toJson(settingsMap)
                        gson.fromJson(json, Settings::class.java)
                    } catch (e: Exception) {
                        SecureLogger.e("Error deserializing settings in signInAsGuest: ${e.message}", e)
                        Settings() // Fallback to default settings
                    }
                } else {
                    Settings() // Default settings if the field doesn't exist
                }
                
                val existingUser = baseUser?.copy(id = anonymousUserId, settings = userSettings)
                    ?: throw Exception("Failed to retrieve user data")
                    
                SecureLogger.security(
                    EventType.USER_LOGIN,
                    "Existing guest sign-in successful for ID: $anonymousUserId",
                    SecureLogger.Level.INFO,
                    anonymousUserId
                )
                    
                return Result.success(existingUser)
            } else {
                // Create a new guest user with the anonymous user's ID
                val guestUser = User(
                    id = anonymousUserId,
                    role = UserRole.GUEST,
                    name = "Guest",
                    email = "guest@sofrehmessina.com"
                )
                
                // Save the guest user to Firestore
                firestore.collection("users")
                    .document(anonymousUserId)
                    .set(guestUser)
                    .await()
                    
                SecureLogger.security(
                    EventType.USER_LOGIN,
                    "New guest sign-in successful for ID: $anonymousUserId",
                    SecureLogger.Level.INFO,
                    anonymousUserId
                )
                    
                return Result.success(guestUser)
            }
        } catch (e: Exception) {
            SecureLogger.security(
                EventType.FAILED_LOGIN,
                "Guest sign-in failed: ${e.message}",
                SecureLogger.Level.WARNING
            )
            return Result.failure(e)
        }
    }

    fun signOut() {
        val userId = auth.currentUser?.uid
        
        if (userId != null) {
            SecureLogger.security(
                EventType.USER_LOGOUT,
                "User signed out: $userId",
                SecureLogger.Level.INFO,
                userId
            )
        }
        
        auth.signOut()
    }

    // User Data
    suspend fun saveUserData(user: User): Result<Unit> = try {
        // Encrypt sensitive data before saving
        val encryptedUser = encryptUserData(user)
        
        firestore.collection("users")
            .document(user.id)
            .set(encryptedUser)
            .await()
            
        SecureLogger.security(
            EventType.DATA_ACCESS,
            "User data updated for ID: ${user.id}",
            SecureLogger.Level.INFO,
            user.id
        )
            
        Result.success(Unit)
    } catch (e: Exception) {
        SecureLogger.e("Save user data error: ${e.message}", e, TAG)
        Result.failure(e)
    }

    suspend fun getUserData(userId: String, forceRefresh: Boolean = false): Result<User> {
        try {
            // If force refresh, use Source.SERVER to bypass cache
            val source = if (forceRefresh) com.google.firebase.firestore.Source.SERVER else com.google.firebase.firestore.Source.DEFAULT
            
            Log.d(TAG, "Getting user data for $userId, forceRefresh=$forceRefresh")
            
            val userDoc = firestore.collection("users")
                .document(userId)
                .get(source)
                .await()
                
            if (!userDoc.exists()) {
                Log.w(TAG, "User document doesn't exist for $userId")
                return Result.failure(Exception("User not found"))
            }
                
            // Try to get the user object
            val baseUser = userDoc.toObject(User::class.java)
            
            // Log user role for debugging
            val roleString = userDoc.getString("role")
            Log.d(TAG, "User $userId has raw role string: $roleString")
            
            // Explicitly convert the role string to UserRole enum
            val userRole = when (roleString?.uppercase()) {
                "ADMIN" -> UserRole.ADMIN
                "USER" -> UserRole.USER
                "GUEST" -> UserRole.GUEST
                else -> {
                    Log.w(TAG, "Unknown user role '$roleString' for user $userId, defaulting to USER")
                    UserRole.USER
                }
            }
            
            // Get the settings field from the user document
            val settingsMap = safeMapCast(userDoc.get("settings"))
            val userSettings = if (settingsMap != null) {
                try {
                    // Convert the map to a Settings object
                    val gson = Gson()
                    val json = gson.toJson(settingsMap)
                    gson.fromJson(json, Settings::class.java)
                } catch (e: Exception) {
                    SecureLogger.e("Error deserializing settings in getUserData: ${e.message}", e)
                    Settings() // Fallback to default settings
                }
            } else {
                Settings() // Default settings if the field doesn't exist
            }
            
            // Create user with explicit role and ID
            val user = (baseUser?.copy(id = userId, settings = userSettings, role = userRole)
                ?: User(id = userId, role = userRole, settings = userSettings))
            
            // Log final resolved role
            Log.d(TAG, "Final role for user $userId: ${user.role}")
            
            // Decrypt sensitive data before returning
            val decryptedUser = decryptUserData(user)
            
            SecureLogger.security(
                EventType.DATA_ACCESS,
                "User data retrieved for ID: $userId",
                SecureLogger.Level.INFO,
                userId
            )
            
            // Clear any user cache for this ID
            userCache.remove(userId)
            
            return Result.success(decryptedUser)
        } catch (e: Exception) {
            // ... existing code ...
            SecureLogger.e("Get user data error: ${e.message}", e, TAG)
            return Result.failure(e)
        }
    }

    // Get all users - limited to admins via security rules
    fun getAllUsers(): Flow<List<User>> = flow {
        try {
            val snapshot = firestore.collection("users")
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    val baseUser = doc.toObject(User::class.java)
                    
                    // Get the settings field from the user document
                    val settingsMap = safeMapCast(doc.get("settings"))
                    val userSettings = if (settingsMap != null) {
                        try {
                            // Convert the map to a Settings object
                            val gson = Gson()
                            val json = gson.toJson(settingsMap)
                            gson.fromJson(json, Settings::class.java)
                        } catch (e: Exception) {
                            SecureLogger.e("Error deserializing settings in getAllUsers: ${e.message}", e)
                            Settings() // Fallback to default settings
                        }
                    } else {
                        Settings() // Default settings if the field doesn't exist
                    }
                    
                    // Don't decrypt all users - only decrypt when specific user is needed
                    // This minimizes encryption/decryption operations
                    baseUser?.copy(id = doc.id, settings = userSettings)
                } catch (e: Exception) {
                    SecureLogger.e("Error deserializing user: ${e.message}", e, TAG)
                    null
                }
            }
            
            emit(users)
        } catch (e: Exception) {
            // Don't throw the exception, just log it and emit an empty list
            SecureLogger.e("Error getting all users: ${e.message}", e, TAG)
            emit(emptyList<User>())
        }
    }

    // Categories
    fun getCategories(): Flow<List<Category>> = callbackFlow {
        // First, emit cached data if available
        if (_categoriesCache != null) {
            Log.d(TAG, "Returning cached categories: ${_categoriesCache!!.size} items")
            trySend(_categoriesCache!!)
        }
        
        // Set up real-time listener for category changes
        val listenerRegistration = firestore.collection("categories")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for category changes: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            val category = doc.toObject(Category::class.java)
                            // Ensure the id field is populated from the document ID
                            category?.copy(id = doc.id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing category: ${e.message}", e)
                            null
                        }
                    }.sortedBy { it.name }
                    
                    // Update cache
                    _categoriesCache = categories
                    
                    // Send to flow
                    trySend(categories)
                    Log.d(TAG, "Categories updated via listener: ${categories.size} items")
                }
            }
        
        // Clean up listener when flow is cancelled
        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun addCategory(category: Category): Result<String> = try {
        // Log the category before adding to Firestore
        Log.d("FirebaseRepository", "Adding category: ${category.id}")
        Log.d("FirebaseRepository", "Category translations: ${category.translations}")
        
        // Ensure all translations are present and properly structured
        val completeTranslations = Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
            category.translations[langCode] ?: CategoryTranslation("", "")
        }
        
        // Create a copy of the category with complete translations
        val completeCategory = category.copy(translations = completeTranslations)
        
        val docRef = firestore.collection("categories")
            .add(completeCategory)
            .await()
        
        Log.d("FirebaseRepository", "Category added successfully with ID: ${docRef.id}")
        Result.success(docRef.id)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Adding category was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error adding category: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Unit> = try {
        // Log the category before updating in Firestore
        Log.d("FirebaseRepository", "Updating category with ID: ${category.id}")
        Log.d("FirebaseRepository", "Category translations: ${category.translations}")
        
        // Ensure all translations are present and properly structured
        val completeTranslations = Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
            category.translations[langCode] ?: CategoryTranslation("", "")
        }
        
        // Create a copy of the category with complete translations
        val completeCategory = category.copy(translations = completeTranslations)
        
        firestore.collection("categories")
            .document(category.id)
            .set(completeCategory)
            .await()
        
        Log.d("FirebaseRepository", "Category updated successfully with ID: ${category.id}")
        Result.success(Unit)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Updating category was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error updating category: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = try {
        firestore.collection("categories")
            .document(categoryId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Food Items
    fun getFoodItems(categoryId: String? = null): Flow<List<Food>> = callbackFlow {
        // First, emit cached data if available
        if (categoryId == null && _foodItemsCache != null) {
            Log.d(TAG, "Returning cached food items: ${_foodItemsCache!!.size} items")
            trySend(_foodItemsCache!!)
        }
        
        // Set up real-time listener for food items
        val query = firestore.collection("food")
            .let { ref ->
                if (categoryId != null) ref.whereEqualTo("categoryId", categoryId)
                else ref
            }
            .orderBy("name")
            
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for food item changes: ${error.message}", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val foodItems = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Get the raw document data
                        val data = doc.data
                        
                        // Check if we need to migrate availability fields
                        if (data != null) {
                            val isAvailable = data["isAvailable"] as? Boolean
                            val available = data["available"] as? Boolean
                            val foodAvailable = data["foodAvailable"] as? Boolean
                            
                            // Migrate to new format if needed
                            val needsMigration = foodAvailable == null || 
                                               (isAvailable != null && isAvailable != foodAvailable) ||
                                               (available != null && available != foodAvailable)
                            
                            if (needsMigration) {
                                // Determine the correct value to use
                                val correctValue = when {
                                    foodAvailable != null -> foodAvailable
                                    isAvailable != null -> isAvailable
                                    available != null -> available
                                    else -> true // Default to true if all are null
                                }
                                
                                Log.d(TAG, "Migrating food item ${doc.id} to use foodAvailable=$correctValue")
                                
                                // Update the document with the correct availability values
                                try {
                                    firestore.collection("food").document(doc.id).update(
                                        mapOf(
                                            "foodAvailable" to correctValue,
                                            "available" to correctValue,
                                            // Remove the conflicting field to avoid future issues
                                            "isAvailable" to FieldValue.delete()
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error migrating availability fields: ${e.message}", e)
                                }
                            }
                        }
                        
                        // Deserialize as normal
                        val food = doc.toObject(Food::class.java)
                        // Ensure the id field is populated from the document ID
                        food?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing food item: ${e.message}", e)
                        null
                    }
                }.sortedBy { it.name }
                
                // Update cache if no category filter
                if (categoryId == null) {
                    _foodItemsCache = foodItems
                }
                
                // Send to flow
                trySend(foodItems)
                Log.d(TAG, "Food items updated via listener: ${foodItems.size} items")
            }
        }
        
        // Clean up listener when flow is cancelled
        awaitClose {
            listenerRegistration.remove()
        }
    }

    suspend fun getFoodItem(foodId: String): Result<Food> = try {
        val foodDoc = firestore.collection("food").document(foodId).get().await()
        
        // Log the raw data and specifically the availability fields
        Log.d("FirebaseRepository", "======= FOOD ITEM FETCHED FROM DATABASE =======")
        Log.d("FirebaseRepository", "Food ID: $foodId")
        Log.d("FirebaseRepository", "Document exists: ${foodDoc.exists()}")
        
        // Get all document data
        val allData = foodDoc.data
        Log.d("FirebaseRepository", "All document data: $allData")
        
        // Specifically check all availability fields
        val isAvailableRaw = foodDoc.getBoolean("isAvailable")
        val availableRaw = foodDoc.getBoolean("available")
        val foodAvailableRaw = foodDoc.getBoolean("foodAvailable")
        
        Log.d("FirebaseRepository", "Raw isAvailable field: $isAvailableRaw")
        Log.d("FirebaseRepository", "Raw available field: $availableRaw")
        Log.d("FirebaseRepository", "Raw foodAvailable field: $foodAvailableRaw")
        
        // If we have inconsistent values or missing new format, fix the document directly
        if (allData != null) {
            // Migrate to new format if needed
            val needsMigration = foodAvailableRaw == null || 
                              (isAvailableRaw != null && foodAvailableRaw != isAvailableRaw) ||
                              (availableRaw != null && foodAvailableRaw != availableRaw)
            
            if (needsMigration) {
                // Determine the correct value to use
                val correctValue = when {
                    foodAvailableRaw != null -> foodAvailableRaw
                    isAvailableRaw != null -> isAvailableRaw
                    availableRaw != null -> availableRaw
                    else -> true // Default to true if all are null
                }
                
                Log.d("FirebaseRepository", "Migrating document to new availability format. Using value: $correctValue")
                
                // Update all fields in the document
                try {
                    firestore.collection("food").document(foodId).update(
                        mapOf(
                            "foodAvailable" to correctValue,
                            "available" to correctValue,
                            // Remove the problematic field that causes conflicts
                            "isAvailable" to FieldValue.delete()
                        )
                    ).await()
                    Log.d("FirebaseRepository", "Successfully migrated availability fields in document")
                } catch (e: Exception) {
                    Log.e("FirebaseRepository", "Error migrating availability fields: ${e.message}", e)
                    // Continue as we'll still return the food item
                }
            }
        }
        
        // Now deserialize the object (potentially after fixing the document)
        val food = foodDoc.toObject(Food::class.java)?.copy(id = foodDoc.id)
        
        Log.d("FirebaseRepository", "Deserialized food item availability: ${food?.foodAvailable}")
        Log.d("FirebaseRepository", "=========================================")
        
        if (food != null) {
            Result.success(food)
        } else {
            Result.failure(Exception("Food item not found"))
        }
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Getting food item was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error getting food item: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCategory(categoryId: String): Result<Category> = try {
        val categoryDoc = firestore.collection("categories").document(categoryId).get().await()
        val category = categoryDoc.toObject(Category::class.java)?.copy(id = categoryDoc.id)
        if (category != null) {
            Result.success(category)
        } else {
            Result.failure(Exception("Category not found"))
        }
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Getting category was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error getting category: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addFoodItem(food: Food): Result<Unit> = try {
        // Log the food item before saving to Firestore
        Log.d("FirebaseRepository", "===============================================")
        Log.d("FirebaseRepository", "ADDING FOOD ITEM")
        Log.d("FirebaseRepository", "Name: ${food.name}")
        Log.d("FirebaseRepository", "FoodAvailable: ${food.foodAvailable}")
        Log.d("FirebaseRepository", "Price: ${food.price}")
        Log.d("FirebaseRepository", "Category: ${food.categoryId}")
        Log.d("FirebaseRepository", "===============================================")
        
        // Ensure all translations are present and properly structured
        val completeTranslations = Languages.SUPPORTED_LANGUAGES.associateWith { langCode ->
            food.translations[langCode] ?: FoodTranslation("", "")
        }
        
        // Create a copy of the food item with complete translations
        val completeFoodItem = food.copy(translations = completeTranslations)
        
        // Create a map with all fields explicitly set, including both availability field names
        val foodMap = hashMapOf<String, Any?>(
            "id" to completeFoodItem.id,
            "name" to completeFoodItem.name,
            "description" to completeFoodItem.description,
            "price" to completeFoodItem.price,
            "imageUrl" to completeFoodItem.imageUrl,
            "categoryId" to completeFoodItem.categoryId,
            "translations" to completeFoodItem.translations,
            // Set availability fields for consistency
            "foodAvailable" to completeFoodItem.foodAvailable,
            "available" to completeFoodItem.available
        )
        
        // Add optional fields only if they're present
        completeFoodItem.discountedPrice?.let { foodMap["discountedPrice"] = it }
        completeFoodItem.discountPercentage?.let { foodMap["discountPercentage"] = it }
        completeFoodItem.discountEndDate?.let { foodMap["discountEndDate"] = it }
        completeFoodItem.discountMessage?.let { foodMap["discountMessage"] = it }
        
        // Add the food item to Firestore using the map
        val docRef = firestore.collection("food").add(foodMap).await()
        
        // Verify the item was created with correct availability 
        try {
            val createdDoc = docRef.get().await()
            if (createdDoc.exists()) {
                val createdFoodAvailable = createdDoc.getBoolean("foodAvailable")
                val createdAvailable = createdDoc.getBoolean("available")
                Log.d("FirebaseRepository", "Created food item - foodAvailable: $createdFoodAvailable, available: $createdAvailable")
                
                // If needed, update the fields to ensure consistency
                if (createdFoodAvailable != completeFoodItem.foodAvailable || createdAvailable != completeFoodItem.available) {
                    Log.w("FirebaseRepository", "Fixing inconsistent availability fields in new food item")
                    val fixMap = mapOf(
                        "foodAvailable" to completeFoodItem.foodAvailable,
                        "available" to completeFoodItem.available
                    )
                    docRef.update(fixMap).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error verifying new food item: ${e.message}", e)
            // Continue as the food item was still created
        }
        
        Log.d("FirebaseRepository", "Food item added successfully with ID: ${docRef.id}")
        Result.success(Unit)
        
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Adding food item was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error adding food item: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateFoodItem(food: Food): Result<Unit> {
        try {
            // Log everything about this food item for debugging
            Log.d("FirebaseRepository", "===============================================")
            Log.d("FirebaseRepository", "UPDATING FOOD ITEM: ${food.id}")
            Log.d("FirebaseRepository", "Name: ${food.name}")
            Log.d("FirebaseRepository", "FoodAvailable: ${food.foodAvailable}")
            Log.d("FirebaseRepository", "Price: ${food.price}")
            Log.d("FirebaseRepository", "Category: ${food.categoryId}")
            Log.d("FirebaseRepository", "===============================================")
            
            // Ensure we have a valid ID to work with
            if (food.id.isBlank()) {
                Log.e("FirebaseRepository", "Cannot update food item with blank ID")
                return Result.failure(IllegalArgumentException("Food ID cannot be blank"))
            }
            
            // Reference to the food document in Firestore
            val docRef = firestore.collection("food").document(food.id)
            
            // First, check if the document exists
            val docSnapshot = docRef.get().await()
            if (!docSnapshot.exists()) {
                Log.e("FirebaseRepository", "Cannot update food item with ID ${food.id} - document doesn't exist")
                return Result.failure(IllegalStateException("Food item doesn't exist"))
            }
            
            // Check if current document has inconsistent availability fields
            val currentIsAvailable = docSnapshot.getBoolean("isAvailable")
            val currentAvailable = docSnapshot.getBoolean("available")
            val currentFoodAvailable = docSnapshot.getBoolean("foodAvailable")
            
            Log.d("FirebaseRepository", "Current document state - isAvailable: $currentIsAvailable, available: $currentAvailable, foodAvailable: $currentFoodAvailable")
            Log.d("FirebaseRepository", "Updating to - foodAvailable: ${food.foodAvailable}, available: ${food.available}")
            
            // Create a complete update map with all fields - use set instead of update to ensure all fields are updated
            val updates = hashMapOf<String, Any?>(
                "id" to food.id,
                "name" to food.name,
                "description" to food.description,
                "price" to food.price,
                "imageUrl" to food.imageUrl,
                "categoryId" to food.categoryId,
                "foodAvailable" to food.foodAvailable,
                "available" to food.available, // ALWAYS ensure both fields are set to the same value
                "translations" to food.translations,
                // Remove the problematic field that causes conflicts
                "isAvailable" to FieldValue.delete()
            )
            
            // Add optional fields only if they're present
            food.discountedPrice?.let { updates["discountedPrice"] = it }
            food.discountPercentage?.let { updates["discountPercentage"] = it }
            food.discountEndDate?.let { updates["discountEndDate"] = it }
            food.discountMessage?.let { updates["discountMessage"] = it }
            
            // Use set with merge option to ensure all fields are properly updated
            docRef.set(updates, SetOptions.merge()).await()
            
            // Verification - get the updated document and check for consistency
            try {
                val updatedDoc = docRef.get().await()
                val updatedFoodAvailable = updatedDoc.getBoolean("foodAvailable")
                val updatedAvailable = updatedDoc.getBoolean("available")
                
                Log.d("FirebaseRepository", "Verification - after update: foodAvailable=$updatedFoodAvailable, available=$updatedAvailable")
                
                // If still inconsistent, try one more fix with a higher priority update
                if (updatedFoodAvailable != updatedAvailable || updatedFoodAvailable != food.foodAvailable) {
                    Log.w("FirebaseRepository", "⚠️ Inconsistency detected after update, attempting final fix")
                    val finalFix = mapOf(
                        "foodAvailable" to food.foodAvailable,
                        "available" to food.available
                    )
                    docRef.update(finalFix).await()
                    
                    // Double-check after the final fix
                    val finalDoc = docRef.get().await()
                    val finalFoodAvailable = finalDoc.getBoolean("foodAvailable")
                    val finalAvailable = finalDoc.getBoolean("available")
                    Log.d("FirebaseRepository", "Final verification - foodAvailable=$finalFoodAvailable, available=$finalAvailable")
                    
                    if (finalFoodAvailable != food.foodAvailable || finalAvailable != food.available) {
                        Log.e("FirebaseRepository", "❌ CRITICAL ERROR: Could not ensure availability consistency after multiple attempts")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "Error during verification: ${e.message}", e)
                // Continue as the update was already applied
            }
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            // Special handling for coroutine cancellation
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            } else {
                Log.e("FirebaseRepository", "Error updating food item: ${e.message}", e)
                return Result.failure(e)
            }
        }
    }

    suspend fun deleteFoodItem(foodId: String): Result<Unit> = try {
        firestore.collection("food")
            .document(foodId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Deleting food item was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error deleting food item: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Orders
    fun getOrders(): Flow<List<Order>> = flow {
        try {
            val snapshot = firestore.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
            emit(orders)
        } catch (e: Exception) {
            // Handle cancellation exceptions properly
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d("FirebaseRepository", "Getting orders was cancelled due to navigation")
                throw e
            } else if (e.message?.contains("FAILED_PRECONDITION") == true && e.message?.contains("requires an index") == true) {
                // Log the error with the index link for easier access
                val indexLink = e.message?.let {
                    val startIndex = it.indexOf("https://")
                    val endIndex = it.indexOf("\n", startIndex)
                    if (startIndex != -1 && endIndex != -1) {
                        it.substring(startIndex, endIndex)
                    } else {
                        null
                    }
                }
                Log.w("FirebaseRepository", "Firestore index needed! Create it here: $indexLink")
                emit(emptyList<Order>())
            } else {
                Log.e("FirebaseRepository", "Error getting orders: ${e.message}", e)
                throw e
            }
        }
    }

    fun getPendingOrders(): Flow<List<Order>> = callbackFlow {
        // Try first without ordering to avoid index issues initially
        val subscription = try {
            firestore.collection("orders")
                .whereEqualTo("status", OrderStatus.PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.message?.contains("FAILED_PRECONDITION") == true && 
                        error.message?.contains("requires an index") == true) {
                            // Fallback to just filter without order if index is missing
                            try {
                                firestore.collection("orders")
                                    .whereEqualTo("status", OrderStatus.PENDING)
                                    .get().addOnSuccessListener { result ->
                                        val orders = result.documents.mapNotNull { doc ->
                                            doc.toObject(Order::class.java)?.copy(id = doc.id)
                                        }
                                        trySend(orders)
                                    }.addOnFailureListener {
                                        close(it)
                                    }
                            } catch (e: Exception) {
                                close(e)
                            }
                        } else {
                            close(error)
                        }
                        return@addSnapshotListener
                    }

                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } ?: emptyList()

                    trySend(orders)
                }
        } catch (e: Exception) {
            if (e.message?.contains("FAILED_PRECONDITION") == true && 
                e.message?.contains("requires an index") == true) {
                // Fallback to just filter without order if index is missing
                firestore.collection("orders")
                    .whereEqualTo("status", OrderStatus.PENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        
                        val orders = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Order::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        
                        trySend(orders)
                    }
            } else {
                throw e
            }
        }

        awaitClose { 
            try {
                subscription.remove() 
            } catch (e: Exception) {
                // Ignore if already closed
            }
        }
    }

    suspend fun createOrder(order: Order): Result<Order> = try {
        val orderWithTimestamp = order.copy(
            createdAt = Date(),
            updatedAt = Date()
        )
        val docRef = firestore.collection("orders").add(orderWithTimestamp).await()
        Result.success(orderWithTimestamp.copy(id = docRef.id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Order> = try {
        val orderRef = firestore.collection("orders").document(orderId)
        val orderDoc = orderRef.get().await()
        val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)
            ?: throw Exception("Order not found")
        
        // Update order status and timestamp
        orderRef.update(
            mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        
        // Get user's FCM token to send notification
        val userId = order.userId
        val fcmTokenDoc = firestore.collection("fcmTokens").document(userId).get().await()
        val fcmToken = fcmTokenDoc.getString("token")
        
        // If there's a token, this would be where we'd call a cloud function to send the notification
        // For now, we'll just log it since this would be handled by Firebase Cloud Functions
        if (fcmToken != null) {
            Log.d("FirebaseRepository", "Would send notification to token: $fcmToken for order: $orderId with status: $status")
            // In a real app, we'd call a cloud function here to send the notification
            // For example: httpsCallable("sendOrderStatusNotification").call(mapOf("orderId" to orderId, "status" to status))
        }
        
        Result.success(order.copy(status = status))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // User operations
    suspend fun createUser(user: User): Result<User> = try {
        val userDoc = firestore.collection("users").document(user.id)
        userDoc.set(user).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            // Check cache first - if we have a recent entry, use it
            val cachedUser = userCache[userId]
            if (cachedUser != null && (System.currentTimeMillis() - cachedUser.second < USER_CACHE_TTL)) {
                Log.d(TAG, "Using cached user data for $userId")
                return Result.success(cachedUser.first)
            }
            
            // Add timeout to prevent hanging
            withTimeout(2500) { // 2.5-second timeout to prevent ANR
                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    
                    if (!userDoc.exists()) {
                        return@withTimeout Result.failure(Exception("User document not found"))
                    }
                    
                    // Get the user object
                    val baseUser = userDoc.toObject(User::class.java)
                    
                    // Create a user with the correct ID
                    val user = baseUser?.copy(id = userId)
                    
                    if (user != null) {
                        try {
                            // Skip decryption in most cases as it's rarely needed
                            // and can cause performance issues
                            val finalUser = user
                            
                            // Cache the user for future requests
                            userCache[userId] = Pair(finalUser, System.currentTimeMillis())
                            
                            Result.success(finalUser)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing user data: ${e.message}", e)
                            // If there's an error, still return the user without additional processing
                            userCache[userId] = Pair(user, System.currentTimeMillis())
                            Result.success(user)
                        }
                    } else {
                        Result.failure(Exception("User data could not be parsed"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user document: ${e.message}", e)
                    throw e
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout while getting user: ${e.message}", e)
            Result.failure(Exception("Network timeout. Please check your connection and try again."))
        } catch (e: Exception) {
            if (e is CancellationException) {
                // Don't log cancellations as errors
                Log.d(TAG, "User fetch cancelled for $userId")
                throw e
            } else {
                Log.e(TAG, "Error getting user: ${e.message}", e)
                Result.failure(Exception("Could not load user profile. Please try again later."))
            }
        }
    }

    suspend fun updateUser(user: User): Result<User> = try {
        val userDoc = firestore.collection("users").document(user.id)
        userDoc.set(user).await()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Order operations
    suspend fun getOrder(orderId: String): Result<Order> = try {
        val orderDoc = firestore.collection("orders").document(orderId).get().await()
        val order = orderDoc.toObject(Order::class.java)?.copy(id = orderDoc.id)
        if (order != null) {
            Result.success(order)
        } else {
            Result.failure(Exception("Order not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserOrders(userId: String): Result<List<Order>> = try {
        try {
            val orders = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> 
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
            Result.success(orders)
        } catch (e: Exception) {
            if (e.message?.contains("FAILED_PRECONDITION") == true && e.message?.contains("requires an index") == true) {
                // Fallback to just filter without ordering if index doesn't exist
                val orders = firestore.collection("orders")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> 
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    }
                Result.success(orders)
            } else {
                throw e
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Get orders within a specific date range
    suspend fun getOrdersInDateRange(startDate: Date, endDate: Date): Result<List<Order>> = try {
        try {
            val orders = firestore.collection("orders")
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc -> 
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
            Result.success(orders)
        } catch (e: Exception) {
            if (e.message?.contains("FAILED_PRECONDITION") == true && e.message?.contains("requires an index") == true) {
                // Fallback if compound index doesn't exist
                val orders = firestore.collection("orders")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc -> 
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    }
                    .filter { order -> 
                        order.createdAt.time >= startDate.time && order.createdAt.time <= endDate.time 
                    }
                Result.success(orders)
            } else {
                throw e
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Review operations
    suspend fun createReview(review: Review): Result<Review> = try {
        val reviewDoc = firestore.collection("reviews").document()
        val reviewWithId = review.copy(id = reviewDoc.id)
        reviewDoc.set(reviewWithId).await()
        Result.success(reviewWithId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getFoodReviews(foodId: String): Result<List<Review>> = try {
        try {
            val reviews = firestore.collection("reviews")
                .whereEqualTo("foodId", foodId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Review::class.java) }
            Result.success(reviews)
        } catch (e: Exception) {
            if (e.message?.contains("FAILED_PRECONDITION") == true && e.message?.contains("requires an index") == true) {
                // Fallback to just filter without ordering if index doesn't exist
                val reviews = firestore.collection("reviews")
                    .whereEqualTo("foodId", foodId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(Review::class.java) }
                Result.success(reviews)
            } else {
                throw e
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Settings
    suspend fun getUserSettings(): Result<Settings> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        val userDoc = firestore.collection("users")
            .document(userId)
            .get()
            .await()
            
        // Get the settings field from the user document
        val settings = safeMapCast(userDoc.get("settings"))
        val userSettings = if (settings != null) {
            try {
                // Convert the map to a Settings object
                val gson = Gson()
                val json = gson.toJson(settings)
                gson.fromJson(json, Settings::class.java)
            } catch (e: Exception) {
                SecureLogger.e("Error deserializing settings: ${e.message}", e)
                Settings() // Fallback to default settings
            }
        } else {
            Settings() // Default settings if the field doesn't exist
        }
        
        Result.success(userSettings)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateUserSettings(settings: Settings): Result<Unit> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(userId)
            .update("settings", settings)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun clearUserData(): Result<Unit> = try {
        val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
        firestore.collection("users")
            .document(userId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // New methods for user management
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun disableUserAccount(userId: String): Result<Unit> = try {
        // In a real app, we might have a "disabled" field in the user document
        // For this example, we'll just update the user's role to indicate they're disabled
        val userDoc = firestore.collection("users").document(userId)
        
        // Check if user exists before updating
        val userSnapshot = userDoc.get().await()
        if (!userSnapshot.exists()) {
            throw Exception("User not found")
        }
        
        // Mark the user as disabled by adding a field
        userDoc.update("disabled", true).await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Helper method to get the current user ID
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Saves the FCM token for the current user to enable push notifications
     */
    suspend fun saveFCMToken(token: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            
            if (userId == null) {
                Log.w("FirebaseRepository", "Cannot save FCM token: No user is logged in")
                return Result.failure(Exception("User not logged in"))
            }
            
            Log.d("FirebaseRepository", "Saving FCM token for user: $userId")
            
            // Save the token in a separate collection for easier querying
            firestore.collection("fcmTokens")
                .document(userId)
                .set(mapOf(
                    "userId" to userId,
                    "token" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            
            Log.d("FirebaseRepository", "FCM token saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error saving FCM token: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Discount Management
    
    /**
     * Get all discounts
     */
    fun getDiscounts(): Flow<List<Discount>> = flow {
        try {
            val snapshot = firestore.collection("discounts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val discounts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Discount::class.java)?.copy(id = doc.id)
            }
            emit(discounts)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting discounts: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    /**
     * Get active discounts for a specific category
     */
    suspend fun getActiveDiscountsForCategory(categoryId: String): Result<List<Discount>> = try {
        val currentDate = Date()
        val discounts = firestore.collection("discounts")
            .whereEqualTo("categoryId", categoryId)
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> 
                doc.toObject(Discount::class.java)?.copy(id = doc.id)
            }
            .filter { discount ->
                // Filter by date range if start/end dates are set
                (discount.startDate == null || discount.startDate.before(currentDate)) &&
                (discount.endDate == null || discount.endDate.after(currentDate))
            }
        Result.success(discounts)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Getting category discounts was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error getting discounts for category: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all active discounts across all categories
     * @return Result containing all active discounts
     */
    suspend fun getAllActiveDiscounts(): Result<List<Discount>> = try {
        val currentDate = Date()
        val discounts = firestore.collection("discounts")
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> 
                doc.toObject(Discount::class.java)?.copy(id = doc.id)
            }
            .filter { discount ->
                // Filter by date range if start/end dates are set
                (discount.startDate == null || discount.startDate.before(currentDate)) &&
                (discount.endDate == null || discount.endDate.after(currentDate))
            }
        Result.success(discounts)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Getting active discounts was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error getting all active discounts: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get only regular (non-coupon) active discounts across all categories
     * This method is used for displaying discounts in the UI outside of checkout
     * @return Result containing all active regular discounts (excluding coupon discounts)
     */
    suspend fun getAllRegularActiveDiscounts(): Result<List<Discount>> = try {
        val currentDate = Date()
        val discounts = firestore.collection("discounts")
            .whereEqualTo("active", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> 
                doc.toObject(Discount::class.java)?.copy(id = doc.id)
            }
            .filter { discount ->
                // Filter by date range if start/end dates are set
                val dateValid = (discount.startDate == null || discount.startDate.before(currentDate)) &&
                               (discount.endDate == null || discount.endDate.after(currentDate))
                
                // Only include non-coupon discounts
                val isRegularDiscount = discount.couponCode.isNullOrEmpty() && !discount.isCustomerSpecific
                
                dateValid && isRegularDiscount
            }
        
        Log.d("FirebaseRepository", "Fetched ${discounts.size} regular active discounts (excluded coupon-based discounts)")
        Result.success(discounts)
    } catch (e: Exception) {
        // Special handling for coroutine cancellation
        if (e is kotlinx.coroutines.CancellationException) {
            Log.d("FirebaseRepository", "Getting regular active discounts was cancelled due to navigation")
            // We should rethrow CancellationException to properly propagate coroutine cancellation
            throw e
        } else {
            Log.e("FirebaseRepository", "Error getting regular active discounts: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new discount
     */
    suspend fun createDiscount(discount: Discount): Result<Discount> = try {
        val docRef = firestore.collection("discounts")
            .add(discount)
            .await()
        Result.success(discount.copy(id = docRef.id))
    } catch (e: Exception) {
        Log.e("FirebaseRepository", "Error creating discount: ${e.message}", e)
        Result.failure(e)
    }
    
    /**
     * Update an existing discount
     */
    suspend fun updateDiscount(discount: Discount): Result<Unit> = try {
        firestore.collection("discounts")
            .document(discount.id)
            .set(discount)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseRepository", "Error updating discount: ${e.message}", e)
        Result.failure(e)
    }
    
    /**
     * Delete a discount
     */
    suspend fun deleteDiscount(discountId: String): Result<Unit> = try {
        firestore.collection("discounts")
            .document(discountId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseRepository", "Error deleting discount: ${e.message}", e)
        Result.failure(e)
    }
    
    /**
     * Calculate applicable discounts for an order
     */
    suspend fun calculateDiscounts(items: List<CartItem>): Result<Map<String, Double>> = try {
        // Group items by category
        val itemsByCategory = items.groupBy { it.food.categoryId }
        
        // For each category, check if there are applicable discounts
        val discountsByCategory = mutableMapOf<String, Double>()
        
        for ((categoryId, categoryItems) in itemsByCategory) {
            // Skip if category is empty
            if (categoryId.isBlank()) continue
            
            // Get total quantity for this category
            val totalQuantity = categoryItems.sumOf { it.quantity }
            
            // Get applicable discounts for this category
            val discountsResult = getActiveDiscountsForCategory(categoryId)
            if (discountsResult.isSuccess) {
                val discounts = discountsResult.getOrNull() ?: emptyList()
                
                // Find the best applicable discount
                val bestDiscount = discounts
                    .filter { it.minQuantity <= totalQuantity }
                    .maxByOrNull { it.percentOff }
                
                if (bestDiscount != null) {
                    // Calculate the discount amount
                    val categoryTotal = categoryItems.sumOf { it.food.price * it.quantity }
                    val discountAmount = categoryTotal * (bestDiscount.percentOff / 100.0)
                    
                    discountsByCategory[categoryId] = discountAmount
                }
            }
        }
        
        Result.success(discountsByCategory)
    } catch (e: Exception) {
        Log.e("FirebaseRepository", "Error calculating discounts: ${e.message}", e)
        Result.failure(e)
    }

    /**
     * Track discount usage in Firestore for analytics
     * @param discountId The ID of the discount being applied
     * @param foodId The ID of the food item the discount was applied to
     * @param categoryId The category of the food item
     * @return Result indicating success or failure
     */
    suspend fun trackDiscountUsage(discountId: String, foodId: String, categoryId: String): Result<Unit> = try {
        val analyticsRef = firestore.collection("analytics").document("discounts")
        val usageData = hashMapOf(
            "discountId" to discountId,
            "foodId" to foodId,
            "categoryId" to categoryId,
            "timestamp" to FieldValue.serverTimestamp()
        )
        
        // Add to usage collection
        analyticsRef.collection("usage").add(usageData).await()
        
        // Update counters atomically
        analyticsRef.update(
            "totalUsageCount", FieldValue.increment(1),
            "discountUsage.$discountId", FieldValue.increment(1)
        ).await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("FirebaseRepository", "Error tracking discount usage: ${e.message}", e)
        Result.failure(e)
    }
    
    /**
     * Get discount analytics data from Firestore
     * @return Result containing a map of discount IDs to usage counts
     */
    // Method removed as per requirements - discount analytics functionality no longer needed
    
    /**
     * Validates token integrity to prevent tampering
     * @param token The JWT token to validate
     * @return True if the token appears valid
     */
    fun validateTokenIntegrity(token: String): Boolean {
        return SecurityUtils.isValidAuthToken(token)
    }
    
    /**
     * Promote a user to admin role
     * This can be called directly from your app to fix permission issues
     * @param userId The ID of the user to promote
     * @return Result indicating success or failure
     */
    suspend fun promoteToAdmin(userId: String): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            
            if (!userDoc.exists()) {
                return Result.failure(Exception("User not found"))
            }
            
            // Update the user's role to ADMIN
            userRef.update("role", UserRole.ADMIN.name).await()
            
            SecureLogger.security(
                EventType.PERMISSION_CHANGE,
                "User promoted to admin: $userId",
                SecureLogger.Level.INFO,
                userId
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLogger.e("Error promoting user to admin: ${e.message}", e, TAG)
            Result.failure(e)
        }
    }
    
    /**
     * Check if current user has admin role
     * @return Result containing a boolean indicating if the user is an admin
     */
    suspend fun isCurrentUserAdmin(): Result<Boolean> {
        return try {
            val currentUser = auth.currentUser ?: return Result.success(false)
            val userId = currentUser.uid
            
            val userDoc = firestore.collection("users").document(userId).get().await()
            val role = userDoc.getString("role")
            
            Result.success(role == UserRole.ADMIN.name)
        } catch (e: Exception) {
            SecureLogger.e("Error checking admin status: ${e.message}", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * App Settings data class to store global application settings
     */
    data class AppSettings(
        val defaultCurrency: String = "EUR",
        val taxRate: Double = 0.0,
        val serviceCharge: Double = 0.0,
        val minimumOrderAmount: Double = 0.0,
        val freeDeliveryThreshold: Double = 0.0,
        val deliveryFee: Double = 0.0,
        val lastUpdated: Date = Date(),
        val updatedBy: String = ""
    )

    /**
     * Get the application settings
     */
    fun getAppSettings(callback: (AppSettings?) -> Unit) {
        SecureLogger.d(TAG, "Getting app settings")
        
        firestore.collection("appSettings")
            .document("global")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val settings = document.toObject(AppSettings::class.java)
                        callback(settings)
                    } catch (e: Exception) {
                        SecureLogger.e("Error parsing app settings: ${e.message}", e, TAG)
                        callback(null)
                    }
                } else {
                    // Create default settings if they don't exist
                    val defaultSettings = AppSettings()
                    saveDefaultSettings(defaultSettings)
                    callback(defaultSettings)
                }
            }
            .addOnFailureListener { e ->
                SecureLogger.e("Error getting app settings: ${e.message}", e, TAG)
                callback(null)
            }
    }

    /**
     * Save default settings if they don't exist
     */
    private fun saveDefaultSettings(settings: AppSettings) {
        SecureLogger.d(TAG, "Creating default app settings")
        
        firestore.collection("appSettings")
            .document("global")
            .set(settings)
            .addOnFailureListener { e ->
                SecureLogger.e("Error creating default app settings: ${e.message}", e, TAG)
            }
    }

    /**
     * Update application settings (admin only)
     */
    suspend fun updateAppSettings(updates: Map<String, Any>): Boolean = try {
        val userId = auth.currentUser?.uid
        
        // Only admins can update app settings
        if (userId != null && isCurrentUserAdmin().getOrNull() == true) {
            val updatedFields = updates.toMutableMap().apply {
                put("lastUpdated", FieldValue.serverTimestamp())
                put("updatedBy", userId)
            }
            
            firestore.collection("appSettings")
                .document("global")
                .set(updatedFields, SetOptions.merge())
                .await()
                
            SecureLogger.security(
                EventType.SETTINGS_CHANGED,
                "App settings updated by admin: $userId",
                SecureLogger.Level.INFO,
                userId
            )
            
            true
        } else {
            SecureLogger.security(
                EventType.UNAUTHORIZED_ACCESS,
                "Non-admin attempted to update app settings: $userId",
                SecureLogger.Level.WARNING,
                userId
            )
            
            false
        }
    } catch (e: Exception) {
        SecureLogger.e("Error updating app settings: ${e.message}", e, TAG)
        false
    }

    /**
     * Get a user by their email address
     * @param email The email address to look up
     * @return Result containing the user if found, or null if not found
     */
    suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents.first()
                val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                Result.success(user)
            }
        } catch (e: Exception) {
            SecureLogger.e("Error getting user by email: ${e.message}", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * Retrieves a list of common image paths that should be cached
     * These are images that are frequently used in the app
     * 
     * @return List of image paths to be cached
     */
    suspend fun getCommonImagePaths(): List<String> = withContext(Dispatchers.IO) {
        try {
            // Try to get the common image paths from Firestore
            val configDoc = firestore.collection("app_config")
                .document("image_cache_config")
                .get()
                .await()
            
            // Get the list of paths from the document
            @Suppress("UNCHECKED_CAST")
            val paths = configDoc.get("common_image_paths") as? List<String>
            
            if (!paths.isNullOrEmpty()) {
                Log.d(TAG, "Retrieved ${paths.size} common image paths from Firestore")
                return@withContext paths
            } else {
                Log.d(TAG, "No common image paths found in Firestore, using defaults")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving common image paths: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Validate a coupon code and retrieve the associated discount
     * @param code The coupon code to validate
     * @return Result containing the discount if valid, or failure if invalid
     */
    suspend fun validateCouponCode(code: String): Result<Discount?> {
        return try {
            if (code.isBlank()) {
                return Result.failure(IllegalArgumentException("Coupon code cannot be empty"))
            }
            
            val result = firestore.collection("discounts")
                .whereEqualTo("couponCode", code)
                .whereEqualTo("active", true)
                .get()
                .await()
            
            if (result.isEmpty) {
                Result.success(null) // No discount found with this code
            } else {
                // Get the first matching discount
                val discount = result.documents.firstOrNull()?.toObject(Discount::class.java)
                
                // Check if the discount is currently valid based on dates
                val now = Date()
                if (discount != null && 
                    (discount.startDate == null || discount.startDate <= now) &&
                    (discount.endDate == null || discount.endDate >= now)) {
                    Result.success(discount)
                } else {
                    Result.success(null) // Expired discount
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error validating coupon code: ${e.message}", e)
            Result.failure(e)
        }
    }
} 