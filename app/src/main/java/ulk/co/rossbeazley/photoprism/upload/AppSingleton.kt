package ulk.co.rossbeazley.photoprism.upload

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import kotlinx.coroutines.GlobalScope
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerBackgroundJobSystemInitialiser
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.photoserver.buildPhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsSyncQueue


class AppSingleton : Application() {

    val auditRepository : AuditRepository by lazy { AuditRepository(
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

        workManagerBackgroundJobSystem.startKeepAlive()

//        workmanager -> workmanager config -> workmanager factory -> photoprism app
    }

    companion object {
        const val CHANNEL_ID = "autoStartServiceChannel"
        const val CHANNEL_NAME = "Upload Service Channel"
        var STARTED = false
    }
}

