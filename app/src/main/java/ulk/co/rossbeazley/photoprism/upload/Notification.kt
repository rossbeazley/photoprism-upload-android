package ulk.co.rossbeazley.photoprism.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

class SyncNotification(private val appSingleton: Context) {

    init {
        createNotificationChannel(appSingleton)
    }
    val notificationManager: NotificationManager =  appSingleton.getSystemService()!!

    private fun showNotification() {
        val not = buildNotification(appSingleton)
        notificationManager.notify(1,not)

    }
    private var count = 0

    fun syncing() {
        if(count==0) {
            showNotification()
        }
        count++
    }
    fun finished() {
        count--
        if(count==0) {
            hideNotification()
        }
    }


    private fun hideNotification() {
        notificationManager.cancel(1)
    }


    private fun buildNotification(context: Context): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0, notificationIntent, FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Auto Start Service")
            .setContentText("file sync")
            .setOngoing(true)
            .setColor((ContextCompat.getColor(context, android.R.color.holo_blue_light)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(PRIORITY_LOW)
            // TODO check if this is needed
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setChannelId(CHANNEL_ID)
            .build()
        return notification
    }

    private fun createNotificationChannel(context: Context) {
        val manager: NotificationManager = context.getSystemService()!!
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.enableLights(true)
        channel.description = CHANNEL_NAME
        channel.lightColor = Color.BLUE

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
    }
}


