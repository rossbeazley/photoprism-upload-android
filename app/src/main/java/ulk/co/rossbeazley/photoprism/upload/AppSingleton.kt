package ulk.co.rossbeazley.photoprism.upload

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit


class AppSingleton : Application() {
    lateinit var watch: PhotosDirectoryObserver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val dir = File("/storage/emulated/0/DCIM/Camera")
        val workManager = WorkManager.getInstance(this)
        watch = PhotosDirectoryObserver(dir, workManager)
        watch.startWatching()

        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            1, TimeUnit.HOURS, // repeatInterval (the period cycle)
            15, TimeUnit.MINUTES) // flexInterval
            .addTag("keepalive")
            .build()
        workManager.cancelAllWork()
        workManager.enqueueUniquePeriodicWork("keepalive", ExistingPeriodicWorkPolicy.REPLACE, keepalive)
        auditlog("Application onCreate", this)
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


fun auditlog(logMsg: String, appContext: Context) {
    PreferenceManager.getDefaultSharedPreferences(appContext)
        .edit(commit = true) {
            putString(System.nanoTime().toString(), logMsg)
        }
}