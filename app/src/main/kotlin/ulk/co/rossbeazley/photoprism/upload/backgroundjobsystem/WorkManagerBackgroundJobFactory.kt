package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.work.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.FileWatcherService
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.util.Date
import java.util.concurrent.TimeUnit

class WorkManagerBackgroundJobFactory(
    private val callback: suspend (String) -> JobResult,
) : WorkerFactory() {

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

    class KeepaliveTask(
        appContext: Context,
        workerParams: WorkerParameters,
    ) : Worker(appContext, workerParams) {

        override fun doWork(): Result {
            (applicationContext as AppSingleton).auditRepository.log(
                Debug("Keepalive Task")
            )
            println("keepalive ${Date().toGMTString()}")
            (applicationContext as AppSingleton).scheduleWakeupInCaseOfProcessDeath()
            return Result.success()
        }
    }
}