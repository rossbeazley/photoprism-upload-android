package ulk.co.rossbeazley.photoprism.upload

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerBackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.filesystem.AndroidFileObserverFilesystem
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.buildPhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsLastUploadRepository
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsSyncQueue
import java.io.ByteArrayOutputStream
import java.io.PrintWriter


class AppSingleton : Application() {

    val auditRepository: AuditRepository by lazy {
        AuditRepository(
            PreferenceManager.getDefaultSharedPreferences(this)
        )
    }

    val photoServer: PhotoServer by lazy { buildPhotoServer(contentResolver) }
    private val workManagerBackgroundJobSystem: WorkManagerBackgroundJobSystem =
        WorkManagerBackgroundJobSystem(this)

    val photoPrismApp: PhotoPrismApp by lazy {
        PhotoPrismApp(
            fileSystem = AndroidFileObserverFilesystem(),
            jobSystem = workManagerBackgroundJobSystem,
            auditLogService = auditRepository,
            uploadQueue = SharedPrefsSyncQueue(context = this),
            photoServer = photoServer,
            config = Config(
                photoDirectory = "/storage/emulated/0/DCIM/Camera",
                maxUploadAttempts = 10
            ),
            lastUloadRepository = SharedPrefsLastUploadRepository(context = this),
        )
    }

    override fun onCreate() {

        installUncaughtExceptionLogger()

        super.onCreate()

        val workManager = AppInitializer.getInstance(this)
            .initializeComponent(WorkManagerInitialiser::class.java)

        workManagerBackgroundJobSystem.startKeepAlive(workManager)
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
                DebugAuditLog(
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
                DebugAuditLog(
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

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
        var STARTED = false
    }
}

