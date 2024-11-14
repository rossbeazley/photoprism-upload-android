package ulk.co.rossbeazley.photoprism.upload

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import androidx.work.*
import kotlinx.coroutines.GlobalScope
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.WebdavPutTask
import ulk.co.rossbeazley.photoprism.upload.photoserver.buildPhotoServer
import java.io.File
import java.util.concurrent.TimeUnit


class AppSingleton : Application() {

    private val auditRepository : AuditRepository by lazy { AuditRepository(
        GlobalScope,
        PreferenceManager.getDefaultSharedPreferences(this)
    ) }

    var photoPrismApp: PhotoPrismApp? = null

    override fun onCreate() {

        super.onCreate()

        val workManager = AppInitializer.getInstance(this)
            .initializeComponent(WorkManagerInitialiser::class.java)

        val workManagerBackgroundJobSystem = AppInitializer.getInstance(this)
            .initializeComponent(WorkManagerBackgroundJobSystemInitialiser::class.java)

        photoPrismApp = PhotoPrismApp(
            fileSystem = AndroidFileObserverFilesystem(),
            jobSystem = workManagerBackgroundJobSystem,
            photoServer = buildPhotoServer(),
            config = Config(photoDirectory = "/storage/emulated/0/DCIM/Camera", maxUploadAttempts = 10),
            uploadQueue = SharedPrefsSyncQueue(context = this),
            auditLogService = auditRepository,
        )

        auditRepository.log("Application onCreate")
    }

    private fun startKeepAlive() {
        val uniqueWorkName = "keepalive"
        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .addTag(uniqueWorkName)
            .build()
        val workManager = WorkManager.getInstance(this)
        workManager.cancelUniqueWork(uniqueWorkName)
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            keepalive
        )
    }

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
        var STARTED = false
    }
}

