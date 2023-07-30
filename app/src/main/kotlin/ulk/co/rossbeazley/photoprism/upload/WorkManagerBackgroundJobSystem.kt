package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

class WorkManagerBackgroundJobSystem(val context: Context) : BackgroundJobSystem,
    WorkerFactory() {

    private lateinit var cb: suspend (String) -> JobResult

    override fun schedule(forPath: String): String {
        val request = OneTimeWorkRequestBuilder<JobSystemWorker>()
            .setInputData(workDataOf("A" to forPath))
            .addTag(forPath)
            .build()

        val instance = WorkManager.getInstance(context)
        instance.enqueue(request)
        return forPath
    }

    class JobSystemWorker(
        private val cb: suspend (String) -> JobResult,
        context: Context,
        private val parameters: WorkerParameters
    ) : Worker(context, parameters) {

        override fun doWork(): Result {
            val path = parameters.inputData.getString("A") ?: ""

            val job: Deferred<JobResult> = GlobalScope.async {
                val deferred = async { cb(path) }
                deferred.await()
            }
            val r: JobResult = job.asCompletableFuture().get()
            return when (r) {
                JobResult.Retry -> Result.retry()
                JobResult.Failure -> Result.failure()
                else -> Result.success()
            }
        }
    }

    override fun register(callback: suspend (String) -> JobResult) {
        this.cb = callback
    }

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        return JobSystemWorker(cb, appContext, workerParameters)
    }

}