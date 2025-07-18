package ulk.co.rossbeazley.photoprism.upload

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import androidx.work.WorkManager
import androidx.work.WorkQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerBackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerBackgroundJobSystemInitialiser
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.startContentUriWatching
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.startWorkmanagerLogging
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import ulk.co.rossbeazley.photoprism.upload.filesystem.AndroidFileObserverFilesystem
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.buildPhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsLastUploadRepository
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsSyncQueue


class AppSingleton : Application() {

    val config: SharedPrefsConfigRepository by lazy {
        SharedPrefsConfigRepository(context = this)
    }

    val auditRepository: AuditRepository by lazy {
        AuditRepository(PreferenceManager.getDefaultSharedPreferences(this))
    }

    val syncNotification: SyncNotification by lazy { SyncNotification(this) }

    val photoServer: PhotoServer by lazy { buildPhotoServer(contentResolver, config) }
    val workManagerBackgroundJobSystem: WorkManagerBackgroundJobSystem by lazy {
        AppInitializer.getInstance(applicationContext).initializeComponent(
            WorkManagerBackgroundJobSystemInitialiser::class.java
        )
    }
    val photoPrismApp: PhotoPrismApp by lazy {
        PhotoPrismApp(
            fileSystem = AndroidFileObserverFilesystem(),
            jobSystem = workManagerBackgroundJobSystem,
            auditLogService = auditRepository,
            uploadQueue = SharedPrefsSyncQueue(context = this),
            photoServer = photoServer,
            config = config,
            lastUploadRepository = SharedPrefsLastUploadRepository(context = this),
        )
    }

    val workManager: WorkManager by lazy {
        AppInitializer.getInstance(applicationContext)
            .initializeComponent(WorkManagerInitialiser::class.java)
    }

    override fun onCreate() {
        installUncaughtExceptionLogger(auditRepository)
        super.onCreate()
        startContentUriWatching(workManager)
        logNumberOfWorkManagerJobs()
        startWorkmanagerLogging(workManager)
        workManagerBackgroundJobSystem.startKeepAlive(workManager)
    }

    // TODO do we really need this ?
    private fun logNumberOfWorkManagerJobs() {
        val workInfos = workManager.getWorkInfos(
            WorkQuery
                .Builder
                .fromTags(listOf("urimon"))
                .build()
        )
        workInfos.addListener(
            {
                auditRepository.log(Debug("Workmanager get work infos count: ${workInfos.get().size}"))
            },
            Dispatchers.Default.asExecutor()
        )
    }

    companion object {
        var STARTED = false
    }
}

