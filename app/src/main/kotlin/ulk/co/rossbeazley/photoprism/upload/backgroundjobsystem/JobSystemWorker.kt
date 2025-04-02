package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.Debug

class JobSystemWorker(
    private val cb: suspend (String) -> JobResult,
    context: Context,
    private val parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val appSingleton = (applicationContext as AppSingleton)
        appSingleton.auditRepository.log(Debug("Running JobSystemWorker"))
        appSingleton.syncNotification.syncing()
        val path = parameters.inputData.getString("A") ?: ""
        val r = cb(path)
        return when (r) {
            JobResult.Retry -> Result.retry()
            JobResult.Failure -> Result.failure()
            else -> Result.success()
        }.also {
            appSingleton.syncNotification.finished()
            appSingleton.auditRepository.log(Debug("Finished JobSystemWorker"))
        }
    }
}