package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.util.Date

class KeepaliveTask(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        (applicationContext as AppSingleton).auditRepository.log(Debug("Keepalive Task"))
        (applicationContext as AppSingleton).scheduleWakeupInCaseOfProcessDeath()
        return Result.success()
    }
}