package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.scheduleWakeupInCaseOfProcessDeath

class KeepaliveTask(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = (applicationContext as AppSingleton)
        app.auditRepository.log(Debug("Keepalive Task"))
        scheduleWakeupInCaseOfProcessDeath(app)
        return Result.success()
    }
}