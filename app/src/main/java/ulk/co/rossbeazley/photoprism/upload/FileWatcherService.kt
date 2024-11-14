package ulk.co.rossbeazley.photoprism.upload

import android.app.*
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.CHANNEL_ID
import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.CHANNEL_NAME

class FileWatcherService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        log("Service oncreate")
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        log("Service onstart")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        log("onstartcommand")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, AppSingleton.CHANNEL_ID)
            .setContentTitle("Auto Start Service")
            .setContentText("file sync")
            .setOngoing(true)
            .setColor( (ContextCompat.getColor(this, android.R.color.holo_blue_light)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(PRIORITY_LOW)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setChannelId(CHANNEL_ID)
            .build()

        log("onstartcommand notificaion built")
        //ServiceCompat.startForeground
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        log("onstartcommand started")
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {

        val manager: NotificationManager = getSystemService()!!

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
}

fun startService(context: Context) {
    val serviceIntent = Intent(context, FileWatcherService::class.java)
    serviceIntent.putExtra("inputExtra", "AutoStartService")
    ContextCompat.startForegroundService(context, serviceIntent)
}