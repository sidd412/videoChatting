package com.videoChatting.echat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        // We will retrieve this token in SessionManager or MainActivity to send to backend on login
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received from: ${message.from}")

        val title = message.notification?.title ?: "eChat"
        val body = message.notification?.body ?: ""
        val imageUrl = message.notification?.imageUrl?.toString()
        val type = message.data["type"] // e.g. "CONSENT_REQUEST", "CHAT_MESSAGE"
        val senderId = message.data["senderId"]
        val senderName = message.data["senderName"]
        
        // Mute notification if the user is currently on the chat screen with this sender
        if (type == "CHAT_MESSAGE" && senderId != null && com.videoChatting.echat.utils.ActiveChatManager.currentActiveChatId == senderId) {
            Log.d("FCM", "Muted notification because user is already on chat screen with $senderId")
            return
        }

        sendNotification(title, body, imageUrl, type, senderId, senderName)
    }

    private fun sendNotification(title: String, messageBody: String, imageUrl: String?, type: String?, senderId: String?, senderName: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            if (senderId != null) putExtra("senderId", senderId)
            if (senderName != null) putExtra("senderName", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "echat_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Using default launcher icon for small icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Fetch image for LargeIcon if available
        if (!imageUrl.isNullOrEmpty()) {
            val bitmap = getBitmapFromUrl(imageUrl)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "eChat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            // Enable badges/dots for this channel
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
