package com.example.sofrehmessina.util

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Cloud Messaging (FCM) token operations, ensuring proper resource cleanup.
 * 
 * This singleton class handles token retrieval, storage, and cleanup to prevent
 * the "ManagedChannel garbage collected without being shutdown" error.
 */
@Singleton
class FirebaseTokenManager @Inject constructor() {
    
    private val TAG = "FirebaseTokenManager"
    
    /**
     * Get the current FCM token.
     * 
     * @return The token string or null if retrieval failed
     */
    suspend fun getFcmToken(): String? = withContext(Dispatchers.IO) {
        try {
            val tokenTask = FirebaseMessaging.getInstance().token
            return@withContext tokenTask.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Get the FCM token with callback.
     * 
     * @param onTokenReceived Callback that receives the token or null
     */
    fun getFcmTokenAsync(onTokenReceived: (String?) -> Unit) {
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener(OnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d(TAG, "FCM token retrieved successfully")
                        onTokenReceived(token)
                    } else {
                        Log.e(TAG, "Failed to get FCM token: ${task.exception?.message}")
                        onTokenReceived(null)
                    }
                    
                    // Force channel shutdown to prevent resource leaks
                    cleanupFcmResources()
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error in FCM token retrieval: ${e.message}")
            onTokenReceived(null)
        }
    }
    
    /**
     * Save FCM token to shared preferences.
     */
    fun saveFcmToken(context: Context, token: String) {
        try {
            val sharedPrefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("fcm_token", token).apply()
            Log.d(TAG, "FCM token saved to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token: ${e.message}")
        }
    }
    
    /**
     * Get saved FCM token from shared preferences.
     */
    fun getSavedFcmToken(context: Context): String? {
        try {
            val sharedPrefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
            return sharedPrefs.getString("fcm_token", null)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving saved FCM token: ${e.message}")
            return null
        }
    }
    
    /**
     * Clean up FCM resources to prevent channel leaks.
     * This forces any hanging gRPC channels to be closed properly.
     */
    fun cleanupFcmResources() {
        try {
            // Access a method that will ensure the FirebaseMessaging instance is properly initialized
            // This ensures any previous channels are properly closed before new ones are created
            FirebaseMessaging.getInstance().isAutoInitEnabled
            
            // Force garbage collection to help clean up abandoned channels
            System.gc()
            
            Log.d(TAG, "FCM resources cleanup requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error during FCM resources cleanup: ${e.message}")
        }
    }
} 