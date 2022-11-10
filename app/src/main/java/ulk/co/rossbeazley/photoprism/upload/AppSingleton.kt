package ulk.co.rossbeazley.photoprism.upload

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.content.getSystemService
import androidx.work.*
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import java.io.File
import java.util.concurrent.TimeUnit


class AppSingleton : Application(), Configuration.Provider {

    private val auditRepository : AuditRepository by lazy { AuditRepository(this) }
    private val workManager : WorkManager by lazy { WorkManager.getInstance(this) }

    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setMinimumLoggingLevel(Log.INFO)
        .setWorkerFactory(buildDelegatingWorkerFactory())
        .build()

    private fun buildDelegatingWorkerFactory(): DelegatingWorkerFactory {
        val delegatingWorkerFactory = DelegatingWorkerFactory()
        delegatingWorkerFactory.addFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return when (workerClassName) {
                    LOGGINGTask::class.java.name -> LOGGINGTask(
                        appContext,
                        workerParameters,
                        auditRepository
                    )
                    KeepaliveTask::class.java.name -> KeepaliveTask(
                        appContext,
                        workerParameters,
                        auditRepository
                    )
                    else -> null
                }
            }
        })
        return delegatingWorkerFactory
    }

    lateinit var watch: PhotosDirectoryObserver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val dir = File("/storage/emulated/0/DCIM/Camera")
        watch = PhotosDirectoryObserver(dir, workManager)
        watch.startWatching()

        startKeepAlive()
        auditRepository.log("Application onCreate")
    }

    private fun startKeepAlive() {
        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .addTag("keepalive")
            .build()
        //workManager.cancelAllWork()
        workManager.enqueueUniquePeriodicWork(
            "keepalive",
            ExistingPeriodicWorkPolicy.REPLACE,
            keepalive
        )
    }

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
        var STARTED = false
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

