package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.*
import ulk.co.rossbeazley.photoprism.upload.AppSingleton

class WorkManagerBackgroundJobFactory(
    private val callback: suspend (String) -> JobResult,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            JobSystemWorker::class.java.name -> JobSystemWorker(callback, appContext, workerParameters)
            KeepaliveTask::class.java.name -> KeepaliveTask(appContext, workerParameters)
            WorkmanagerLoggingTask::class.java.name -> WorkmanagerLoggingTask(
                auditRepo = (appContext as AppSingleton).auditRepository,
                workManager = appContext.workManager,
                appContext = appContext,
                workerParams = workerParameters
            )
            else -> null
        }
    }

}