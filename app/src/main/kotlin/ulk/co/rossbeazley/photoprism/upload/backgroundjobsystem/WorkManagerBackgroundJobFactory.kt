package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.*

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
            else -> null
        }
    }

}