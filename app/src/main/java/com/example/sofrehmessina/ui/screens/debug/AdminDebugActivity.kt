package com.example.sofrehmessina.ui.screens.debug

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.sofrehmessina.data.model.User
import com.example.sofrehmessina.data.model.UserRole
import com.example.sofrehmessina.data.repository.FirebaseRepository
import com.example.sofrehmessina.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.example.sofrehmessina.R

/**
 * Debug activity to create an admin user
 * Call with:
 * adb shell am start -n com.example.sofrehmessina/.ui.screens.debug.AdminDebugActivity --es email "admin@example.com" --es password "Admin@123456" --es name "Admin User"
 */
@AndroidEntryPoint
class AdminDebugActivity : AppCompatActivity() {
    
    @Inject
    lateinit var firebaseRepository: FirebaseRepository
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_debug)
        
        val emailInput = findViewById<EditText>(R.id.admin_email)
        val passwordInput = findViewById<EditText>(R.id.admin_password)
        val nameInput = findViewById<EditText>(R.id.admin_name)
        val createButton = findViewById<Button>(R.id.create_admin_button)
        val fixCurrentUserButton = findViewById<Button>(R.id.fix_current_user_button)

        createButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty()) {
                createAdminAccount(email, password, name)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        fixCurrentUserButton.setOnClickListener {
            fixCurrentUserAdminStatus()
        }
    }
    
    private fun createAdminAccount(email: String, password: String, name: String) {
        lifecycleScope.launch {
            try {
                // Attempt to create the user - createAdminUser will handle the case
                // where the email already exists
                createAdminUser(email, password, name)
            } catch (e: Exception) {
                SecureLogger.e("AdminDebugActivity: Fatal error: ${e.message}", e)
                finish()
            }
        }
    }
    
    private fun createAdminUser(email: String, password: String, name: String) {
        lifecycleScope.launch {
            try {
                val user = User(
                    name = name,
                    email = email,
                    role = UserRole.ADMIN,
                    lastPasswordChange = Date(),
                    passwordStrength = 10 // Strong password
                )
                
                firebaseRepository.signUp(email, password, user)
                    .onSuccess {
                        SecureLogger.i("AdminDebugActivity: Admin user created successfully: $email")
                        finish()
                    }
                    .onFailure { e ->
                        val errorMessage = e.message ?: ""
                        // Check if the error is due to the email already being in use
                        if (errorMessage.contains("email-already-in-use", ignoreCase = true) ||
                            errorMessage.contains("email already in use", ignoreCase = true) ||
                            errorMessage.contains("email exists", ignoreCase = true)) {
                            SecureLogger.i("AdminDebugActivity: Email already exists: $email")
                            // Try to promote the user to admin
                            promoteToAdmin(email)
                        } else {
                            SecureLogger.e("AdminDebugActivity: Failed to create admin: ${e.message}", e)
                            finish()
                        }
                    }
            } catch (e: Exception) {
                SecureLogger.e("AdminDebugActivity: Exception: ${e.message}", e)
                // Try to promote in case the error is related to the user already existing
                promoteToAdmin(email)
            }
        }
    }
    
    private fun promoteToAdmin(email: String) {
        lifecycleScope.launch {
            try {
                // Get user by email
                firebaseRepository.getUserByEmail(email)
                    .onSuccess { user ->
                        if (user != null) {
                            // Promote to admin
                            firebaseRepository.promoteToAdmin(user.id)
                                .onSuccess {
                                    SecureLogger.i("AdminDebugActivity: User promoted to admin: $email")
                                }
                                .onFailure { e ->
                                    SecureLogger.e("AdminDebugActivity: Failed to promote: ${e.message}", e)
                                }
                        } else {
                            SecureLogger.e("AdminDebugActivity: User not found for email: $email")
                        }
                    }
                    .onFailure { e ->
                        SecureLogger.e("AdminDebugActivity: Failed to get user: ${e.message}", e)
                    }
            } catch (e: Exception) {
                SecureLogger.e("AdminDebugActivity: Exception: ${e.message}", e)
            }
            finish()
        }
    }
    
    private fun fixCurrentUserAdminStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = firebaseRepository.getCurrentUserId()
                if (userId != null) {
                    val result = firebaseRepository.promoteToAdmin(userId)
                    
                    runOnUiThread {
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@AdminDebugActivity, 
                                "Admin status fixed for current user", 
                                Toast.LENGTH_LONG
                            ).show()
                            SecureLogger.i("Current user admin status fixed")
                        } else {
                            Toast.makeText(
                                this@AdminDebugActivity, 
                                "Failed to fix admin status: ${result.exceptionOrNull()?.message}", 
                                Toast.LENGTH_LONG
                            ).show()
                            SecureLogger.e("Failed to fix admin status", result.exceptionOrNull())
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@AdminDebugActivity, 
                            "No user is currently logged in", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@AdminDebugActivity, 
                        "Error: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                SecureLogger.e("Error fixing admin status", e)
            }
        }
    }
} 