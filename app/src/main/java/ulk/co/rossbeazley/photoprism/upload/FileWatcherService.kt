package ulk.co.rossbeazley.photoprism.upload

import android.app.*
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.startup.AppInitializer
import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.CHANNEL_ID
import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.CHANNEL_NAME
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser

class FileWatcherService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        auditRepository().log(Debug("Service oncreate"))
    }

    override fun onDestroy() {
        auditRepository().log(Debug("Service destroy"))
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        val levelDesc = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "TRIM_MEMORY_COMPLETE"
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "TRIM_MEMORY_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "TRIM_MEMORY_UI_HIDDEN"
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "TRIM_MEMORY_BACKGROUND"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "TRIM_MEMORY_RUNNING_CRITICAL"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "TRIM_MEMORY_RUNNING_MODERATE"
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "TRIM_MEMORY_RUNNING_LOW"
            else -> level.toString()
        }
        //auditRepository().log(Debug("Service trim memory $levelDesc"))
        super.onTrimMemory(level)
    }

    private fun auditRepository() = (application as AppSingleton).auditRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stringExtra = intent?.getStringExtra("inputExtra") ?: "EMPTY-INTENT-EXTRA"
        doOnStart(stringExtra, startId)
        return START_STICKY
    }

    private fun doOnStart(stringExtra: String, startId: Int) {
        auditRepository().log(Debug("Service onstartcommand ${stringExtra} $startId"))
        startKeepaliveJobs()
        createNotificationChannel()
        val notification: Notification = buildNotification()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        auditRepository().log(Debug("Service onstartcommand complete"))
    }

    private fun startKeepaliveJobs() {
        with(application as AppSingleton) {
            val workManager = AppInitializer.getInstance(this)
                .initializeComponent(WorkManagerInitialiser::class.java)
            workManagerBackgroundJobSystem.startKeepAlive(workManager)
            scheduleWakeupInCaseOfProcessDeath()
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Start Service")
            .setContentText("file sync")
            .setOngoing(true)
            .setColor((ContextCompat.getColor(this, android.R.color.holo_blue_light)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(PRIORITY_LOW)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .setChannelId(CHANNEL_ID)
            .build()
        return notification
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

fun startService(context: Context, starter: String) {
    (context.applicationContext as AppSingleton).auditRepository.log(Debug("Calling startService for $starter"))
    val serviceIntent = Intent(context, FileWatcherService::class.java)
    serviceIntent.putExtra("inputExtra", starter)
    // context.startService(serviceIntent)
    ContextCompat.startForegroundService(context, serviceIntent)
}