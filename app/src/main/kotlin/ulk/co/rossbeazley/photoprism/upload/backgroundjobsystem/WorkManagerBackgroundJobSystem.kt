package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.JobResult
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog
import ulk.co.rossbeazley.photoprism.upload.log
import java.time.Duration
import java.util.Date
import java.util.concurrent.TimeUnit

class WorkManagerBackgroundJobSystem(val context: Context) : BackgroundJobSystem {

    private var cb: suspend (String) -> JobResult = { JobResult.Retry }

    override fun schedule(forPath: String): String {
        val request = OneTimeWorkRequestBuilder<WorkManagerBackgroundJobFactory.JobSystemWorker>()
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

    override fun register(callback: suspend (String) -> JobResult) {
        this.cb = callback
    }

    fun startKeepAlive() {
        val uniqueWorkName = "keepalive"
        val keepalive = PeriodicWorkRequestBuilder<WorkManagerBackgroundJobFactory.KeepaliveTask>(
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