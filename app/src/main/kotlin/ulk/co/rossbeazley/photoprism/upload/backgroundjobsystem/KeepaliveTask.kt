package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.scheduleWakeupInCaseOfProcessDeath

class KeepaliveTask(
    app: Context,
    workerParams: WorkerParameters,
    private val auditRepository: AuditRepository,
    private val photoPrismApp: PhotoPrismApp,
    private val workManager: WorkManager,
) : CoroutineWorker(app, workerParams) {

    override suspend fun doWork(): Result {
        //start(applicationContext)
        try {
            auditRepository.log(Debug("Keepalive Task"))
            scheduleWakeupInCaseOfProcessDeath(applicationContext, auditRepository)
            photoPrismApp.findFilesMissingSinceLastLaunch()
            startContentUriWatching(workManager)
        } catch (e : Exception) {
            auditRepository.log(Debug(" KeepaliveTask exception ${e.message}"))
        }
        //stop(applicationContext)
        return Result.success()
    }
}