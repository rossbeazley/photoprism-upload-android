package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.work.*
import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.time.Duration
import java.util.Date
import java.util.concurrent.TimeUnit

class WorkManagerBackgroundJobSystem(val context: Context) : BackgroundJobSystem,
    WorkerFactory() {

    private var cb: suspend (String) -> JobResult = { JobResult.Retry }

    override fun schedule(forPath: String): String {
        val request = OneTimeWorkRequestBuilder<JobSystemWorker>()
            .setInputData(workDataOf("A" to forPath))
            .addTag(forPath)
            .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofSeconds(30))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        // TODO (rbeazley) Should this instance be gotten from app initialiser
        val instance = WorkManager.getInstance(context)
        log("About to enque $request ${request.id} + ${request.tags}")
        instance.enqueue(request)
        return forPath
    }

    class JobSystemWorker(
        private val cb: suspend (String) -> JobResult,
        context: Context,
        private val parameters: WorkerParameters
    ) : Worker(context, parameters) {

        override fun doWork(): Result {
            log("DOING WORK $id")
            val path = parameters.inputData.getString("A") ?: ""
            log("For path $path")
            val job: Deferred<JobResult> = GlobalScope.async {
                val deferred = async { cb(path) }
                deferred.await()
            }
            log("Getting result")
            val r: JobResult = job.asCompletableFuture().get()
            log("$r")
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
    ): ListenableWorker? {
        return when(workerClassName) {
            JobSystemWorker::class.java.name ->  JobSystemWorker(cb, appContext, workerParameters)
            KeepaliveTask::class.java.name -> KeepaliveTask(appContext, workerParameters)
            else -> null
        }
    }

    class KeepaliveTask(
        appContext: Context,
        workerParams: WorkerParameters,
    ) : Worker(appContext, workerParams) {

        override fun doWork(): Result {
            println("keepalive ${Date().toGMTString()}")
            return Result.success()
        }
    }

    fun startKeepAlive() {
        val uniqueWorkName = "keepalive"
        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .addTag(uniqueWorkName)
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(uniqueWorkName)
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            keepalive
        )
    }
}