package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.await
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.util.concurrent.TimeUnit

class WorkmanagerLoggingTask(
    private val auditRepo: AuditRepository,
    appContext: Context,
    workerParams: WorkerParameters,
    val workManager: WorkManager,
) : CoroutineWorker(appContext, workerParams) {

    @SuppressLint("RestrictedApi")
    override suspend fun doWork(): Result {
        //auditRepo.log(Debug("Workmanager Logging Task"))
        val workInfosFuture = workManager.getWorkInfos(
            WorkQuery
                .Builder
                .fromTags(listOf("urimon"))
                .build()
        )
        val workInfos = workInfosFuture.await()
        auditRepo.log(Debug("Workmanager urimon count: ${workInfos.size}"))
        return Result.success()
    }
}


fun startWorkmanagerLogging(workManager: WorkManager) {
    val uniqueWorkName = "workManagerLogging"
    val keepalive = PeriodicWorkRequestBuilder<WorkmanagerLoggingTask>(
        repeatInterval = 1,
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


