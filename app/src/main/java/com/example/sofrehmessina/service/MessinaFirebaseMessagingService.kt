package com.example.sofrehmessina.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sofrehmessina.MainActivity
import com.example.sofrehmessina.R
import com.example.sofrehmessina.util.FirebaseTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MessinaFirebaseMessagingService : FirebaseMessagingService() {
    private val tag = "MessinaFCM"
    private val channelId = "messina_notifications"
    private val channelName = "Messina Notifications"
    
    @Inject
    lateinit var tokenManager: FirebaseTokenManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(tag, "From: ${remoteMessage.from}")

        try {
            // Check if message contains a notification payload
            remoteMessage.notification?.let { notification ->
                Log.d(tag, "Message Notification Body: ${notification.body}")
                
                // Display notification
                val title = notification.title ?: "Sofreh Messina"
                val body = notification.body ?: "You have a new notification"
                sendNotification(title, body)
            }
    
            // Check if message contains data payload
            if (remoteMessage.data.isNotEmpty()) {
                Log.d(tag, "Message data payload: ${remoteMessage.data}")
                
                // Process data payload
                val title = remoteMessage.data["title"] ?: "Sofreh Messina"
                val body = remoteMessage.data["body"] ?: "You have a new notification"
                val type = remoteMessage.data["type"]
                
                // Handle different notification types
                when (type) {
                    "order_status" -> {
                        val orderId = remoteMessage.data["orderId"]
                        sendNotification(title, body, orderId)
                    }
                    "promotion" -> {
                        sendNotification(title, body)
                    }
                    else -> {
                        sendNotification(title, body)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing message: ${e.message}")
        } finally {
            // Clean up resources to prevent channel leaks
            tokenManager.cleanupFcmResources()
        }
    }

    override fun onNewToken(token: String) {
        Log.d(tag, "Refreshed token: $token")
        
        try {
            // Save the token to shared preferences
            tokenManager.saveFcmToken(applicationContext, token)
            
            // Send the new token to your server
            sendRegistrationToServer(token)
        } catch (e: Exception) {
            Log.e(tag, "Error handling new token: ${e.message}")
        } finally {
            // Clean up resources to prevent channel leaks
            tokenManager.cleanupFcmResources()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Ensure we clean up FCM resources when service is destroyed
        tokenManager.cleanupFcmResources()
    }
    
    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement this method to send token to your app server
        // This would typically involve a network call to your backend
        Log.d(tag, "Sending FCM token to server: $token")
    }
    
    private fun sendNotification(title: String, body: String, orderId: String? = null) {
        try {
            // Create intent to open the app when notification is clicked
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Add any extras needed for deep linking
                orderId?.let {
                    putExtra("orderId", it)
                    putExtra("notification_type", "order_status")
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
    
            // Build the notification
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
    
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
            // Create notification channel for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sofreh Messina Notifications"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
    
            // Show the notification
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: Exception) {
            Log.e(tag, "Error sending notification: ${e.message}")
        }
    }
} 