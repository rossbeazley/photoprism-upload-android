package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.*
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import ulk.co.rossbeazley.photoprism.upload.log
import java.time.Duration
import java.util.concurrent.TimeUnit

class WorkManagerBackgroundJobSystem(val context: Context, private val config: SharedPrefsConfigRepository) : BackgroundJobSystem {

    override fun schedule(forPath: String): String {
        val request = OneTimeWorkRequestBuilder<JobSystemWorker>()
            .setInputData(workDataOf("A" to forPath))
            .addTag(forPath)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        // TODO extension function
                        if(config.useMobileData)
                            NetworkType.CONNECTED
                        else
                            NetworkType.UNMETERED
                    )
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofSeconds(30))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        // TODO (rbeazley) Should this instance be gotten from app initialiser
        val instance = WorkManager.getInstance(context)
        log("About to enqueue $request ${request.id} + ${request.tags}")
        instance.enqueue(request)
        return forPath
    }

    // TODO (rbeazley) delete this
    override fun register(callback: suspend (String) -> JobResult) {

    }

    fun startKeepAlive(workManager: WorkManager) {
        val uniqueWorkName = "keepalive"
        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            repeatInterval = 3,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .addTag(uniqueWorkName)
            .build()
        workManager.cancelUniqueWork(uniqueWorkName)
        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            keepalive
        )
    }

}