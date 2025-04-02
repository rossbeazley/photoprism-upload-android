package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug

class WorkManagerBackgroundJobFactory(
    private val callback: suspend (String) -> JobResult,
    private val auditRepository: AuditRepository,
    private val photoPrismApp: PhotoPrismApp,
    private val workManager: () -> WorkManager,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            JobSystemWorker::class.java.name -> JobSystemWorker(
                cb = callback,
                context = appContext,
                parameters = workerParameters
            )
            KeepaliveTask::class.java.name -> KeepaliveTask(
                auditRepository = auditRepository,
                workManager = workManager(),
                photoPrismApp = photoPrismApp,
                app = appContext,
                workerParams = workerParameters,
            )
            WorkmanagerLoggingTask::class.java.name -> WorkmanagerLoggingTask(
                auditRepo = auditRepository,
                workManager = workManager(),
                appContext = appContext,
                workerParams = workerParameters
            )
            ContentUriWatchingTask::class.java.name -> ContentUriWatchingTask(
                auditRepository = auditRepository,
                photoPrismApp = photoPrismApp,
                workManager = workManager(),
                appContext = appContext,
                workerParams = workerParameters,
            )
            else -> {
                auditRepository.log(Debug("Unknown Worker $workerClassName"))
                null
            }
        }
    }

}