package com.example.sofrehmessina.utils

import android.util.Log
import com.example.sofrehmessina.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Secure logging utility that carefully handles sensitive information
 * and provides audit functionality for security-relevant events.
 */
object SecureLogger {
    
    // Log tags
    private const val TAG = "SecureLogger"
    private const val AUTH_TAG = "SecureAuth"
    private const val DATA_TAG = "SecureData"
    private const val PAYMENT_TAG = "SecurePayment"
    
    // Log levels
    enum class Level {
        DEBUG, INFO, WARNING, ERROR, SECURITY
    }
    
    // Event types for security audit
    enum class EventType {
        USER_LOGIN,
        USER_LOGOUT,
        FAILED_LOGIN,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        DATA_ACCESS,
        DATA_CHANGE,
        PERMISSION_CHANGE,
        SECURITY_VIOLATION,
        API_ACCESS,
        SETTINGS_CHANGED,
        UNAUTHORIZED_ACCESS,
        USER_CREATED
    }
    
    // User-identifiable information patterns to redact
    private val sensitivePatterns = listOf(
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b"), // Email
        Regex("\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"), // Phone
        Regex("\\b(?:\\d[ -]*?){13,16}\\b"), // Credit card
        Regex("\\b(?:\\d{3,4}[- ]?){3,4}\\d{3,4}\\b"), // Potential card number formats
        Regex("\\b[A-Z0-9]{5,10}\\b"), // Postal codes (various formats)
        Regex("password\\s*[=:]\\s*\\S+"), // Password in key-value format
        Regex("key\\s*[=:]\\s*\\S+"), // API keys in key-value format
        Regex("token\\s*[=:]\\s*\\S+"), // Tokens in key-value format
        Regex("secret\\s*[=:]\\s*\\S+") // Secrets in key-value format
    )
    
    // CoroutineScope for async operations
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Log a debug message (not security sensitive)
     */
    fun d(message: String, tag: String = TAG) {
        if (!BuildConfig.DEBUG) return // Only log debug in debug builds
        Log.d(tag, redactSensitiveInfo(message))
    }
    
    /**
     * Log an info message (general information)
     */
    fun i(message: String, tag: String = TAG) {
        Log.i(tag, redactSensitiveInfo(message))
    }
    
    /**
     * Log a warning message
     */
    fun w(message: String, tag: String = TAG) {
        Log.w(tag, redactSensitiveInfo(message))
    }
    
    /**
     * Log an error message
     */
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        Log.e(tag, redactSensitiveInfo(message), throwable)
    }
    
    /**
     * Log a security event and store in audit trail if severe enough
     */
    fun security(
        eventType: EventType,
        message: String,
        level: Level = Level.SECURITY,
        userId: String? = null,
        storeInAuditTrail: Boolean = true
    ) {
        val redactedMessage = redactSensitiveInfo(message)
        
        // Log locally
        when (level) {
            Level.DEBUG -> Log.d(AUTH_TAG, redactedMessage)
            Level.INFO -> Log.i(AUTH_TAG, redactedMessage)
            Level.WARNING -> Log.w(AUTH_TAG, redactedMessage)
            Level.ERROR, Level.SECURITY -> Log.e(AUTH_TAG, redactedMessage)
        }
        
        // If it's a significant security event, store it in the audit trail
        if (storeInAuditTrail && (level == Level.WARNING || level == Level.ERROR || level == Level.SECURITY)) {
            storeSecurityEvent(eventType, redactedMessage, level, userId)
        }
    }
    
    /**
     * Store security event in Firestore for audit purposes
     */
    private fun storeSecurityEvent(
        eventType: EventType,
        message: String,
        level: Level,
        userId: String?
    ) {
        scope.launch {
            try {
                val timestamp = Date()
                val db = FirebaseFirestore.getInstance()
                
                val auditEvent = hashMapOf(
                    "id" to UUID.randomUUID().toString(),
                    "timestamp" to timestamp,
                    "eventType" to eventType.name,
                    "message" to message,
                    "level" to level.name,
                    "userId" to (userId ?: "unknown")
                )
                
                // Store in Firestore
                db.collection("security_audit")
                    .add(auditEvent)
                    .addOnSuccessListener {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Security event logged to audit trail: ${eventType.name}")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to log security event to audit trail", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing security event", e)
            }
        }
    }
    
    /**
     * Redact potentially sensitive information from log messages
     */
    private fun redactSensitiveInfo(message: String): String {
        var redactedMessage = message
        
        // Replace sensitive patterns with redacted text
        sensitivePatterns.forEach { pattern ->
            redactedMessage = redactedMessage.replace(pattern) { matchResult ->
                val match = matchResult.value
                when {
                    // Email redaction - preserve domain
                    pattern.pattern.contains("@") && match.contains("@") -> {
                        val atIndex = match.indexOf('@')
                        "****@${match.substring(atIndex + 1)}"
                    }
                    // Phone number redaction - show last 4 digits
                    pattern.pattern.contains("\\d{3}") && match.length >= 4 -> {
                        val lastFour = match.takeLast(4)
                        "****$lastFour"
                    }
                    // Credit card - show only last 4
                    pattern.pattern.contains("(?:\\d[ -]*?){13,16}") && match.length >= 4 -> {
                        val digits = match.filter { it.isDigit() }
                        val lastFour = digits.takeLast(4)
                        "****$lastFour"
                    }
                    // Password/key/token/secret redaction - redact completely
                    pattern.pattern.contains("password|key|token|secret") -> {
                        val keyPart = match.substringBefore("=").trim()
                        "$keyPart=[REDACTED]"
                    }
                    // Default redaction
                    else -> "[REDACTED]"
                }
            }
        }
        
        return redactedMessage
    }
    
    /**
     * Get formatted timestamp for logging
     */
    private fun getTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date())
    }
} 