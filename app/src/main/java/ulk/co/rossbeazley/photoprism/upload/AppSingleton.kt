package ulk.co.rossbeazley.photoprism.upload

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerBackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.config.InMemoryConfigRepository
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import ulk.co.rossbeazley.photoprism.upload.filesystem.AndroidFileObserverFilesystem
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.buildPhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsLastUploadRepository
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsSyncQueue
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.util.concurrent.TimeUnit


class AppSingleton : Application() {

    val config: SharedPrefsConfigRepository by lazy {
        SharedPrefsConfigRepository(
            context = this
        )
    }

    val auditRepository: AuditRepository by lazy {
        AuditRepository(
            PreferenceManager.getDefaultSharedPreferences(this)
        )
    }

    val photoServer: PhotoServer by lazy { buildPhotoServer(contentResolver, config) }
    private val workManagerBackgroundJobSystem: WorkManagerBackgroundJobSystem =
        WorkManagerBackgroundJobSystem(this)

    val photoPrismApp: PhotoPrismApp by lazy {
        PhotoPrismApp(
            fileSystem = AndroidFileObserverFilesystem(),
            jobSystem = workManagerBackgroundJobSystem,
            auditLogService = auditRepository,
            uploadQueue = SharedPrefsSyncQueue(context = this),
            photoServer = photoServer,
            config = config,
            lastUloadRepository = SharedPrefsLastUploadRepository(context = this),
        )
    }

    override fun onCreate() {

        installUncaughtExceptionLogger()

        super.onCreate()

        val workManager = AppInitializer.getInstance(this)
            .initializeComponent(WorkManagerInitialiser::class.java)

        workManagerBackgroundJobSystem.startKeepAlive(workManager)
        scheduleWakeupInCaseOfProcessDeath()
        // photoprism app -> background job system -> workmanager
        // -> workmanager config -> workmanager factory -> photoprism app

        maybeStartService()
    }

    private fun maybeStartService() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                startService(this)
            } catch (e: Exception) {
                val baos = ByteArrayOutputStream()
                val writer = PrintWriter(baos)
                e.printStackTrace(writer)
                Debug(
                    "Caught exception starting service ${e.message} + ${baos.toString()}"
                )
            }
        }
    }

    private fun installUncaughtExceptionLogger() {
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            val baos = ByteArrayOutputStream()
            val writer = PrintWriter(baos)
            throwable.printStackTrace(writer)
            throwable.stackTrace[0]
            auditRepository.log(
                Debug(
                    """UNCaught exception ${throwable.message}
                        | ${throwable.stackTrace[0]}
                        |  ${throwable.stackTrace[1]}
                        |  ${throwable.stackTrace[2]}
                        |  ${throwable.stackTrace[3]}"""
                        .trimMargin()
                )
            )
        }
    }

    fun scheduleWakeupInCaseOfProcessDeath() {
        val serviceIntent = Intent(this, FileWatcherService::class.java)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntentRequestCode = 0

        // maybe cancel the current alarm
        val alarmIntent = PendingIntent.getService(
            this,
            pendingIntentRequestCode, serviceIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (alarmIntent != null && alarmManager != null) {
            alarmManager.cancel(alarmIntent)
        }

        // schedule an alarm for a couple of hours time
        alarmManager?.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + TimeUnit.HOURS.toMillis(2),
            PendingIntent.getService(
                this,
                pendingIntentRequestCode, serviceIntent, PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
        var STARTED = false
    }
}

