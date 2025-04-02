package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug

class ContentUriWatchingTask(
    appContext: Context,
    workerParams: WorkerParameters,
    private val auditRepository: AuditRepository,
    private val photoPrismApp: PhotoPrismApp,
    val workManager: WorkManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            auditRepository.log(Debug("Content URI Watcher"))
            startContentUriWatching(workManager)
            photoPrismApp.findFilesMissingSinceLastLaunch()
        } catch (e: Exception) {
            auditRepository.log(Debug(" ContentUriWatchingTask exception ${e.message}"))
        }
        return Result.success()
    }
}

fun startContentUriWatching(workManager: WorkManager) {
    val MEDIA_URI = Uri.parse("content://${MediaStore.AUTHORITY}/")
    val uniqueWorkName = "urimon"
    val workRequest = OneTimeWorkRequestBuilder<ContentUriWatchingTask>()
        .setConstraints(
            Constraints.Builder()
                .addContentUriTrigger(MEDIA_URI, true)
                .build()
        )
        .addTag(uniqueWorkName)
        .build()
    workManager.enqueueUniqueWork(
        uniqueWorkName,
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}
