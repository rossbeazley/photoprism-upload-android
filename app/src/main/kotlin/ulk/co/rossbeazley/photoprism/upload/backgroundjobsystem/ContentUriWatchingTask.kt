package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.startup.AppInitializer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.util.Date

class ContentUriWatchingTask(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val appSingleton = applicationContext as AppSingleton
        appSingleton.auditRepository.log(Debug("Content URI Watcher"))
        startContentUriWatching(appSingleton.workManager)
        appSingleton.auditRepository.log(Debug("Content URI Watcher Done"))
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
