package ulk.co.rossbeazley.photoprism.upload

import android.os.FileObserver
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

class PhotosDirectoryObserver(val dir: File, val workManager: WorkManager) : FileObserver(
    dir,
    MOVED_TO or CREATE
) {

    override fun onEvent(p0: Int, filePath: String?) {
        log("FOUND A FILE $filePath + ${p0.eventToString()}")
        if (filePath == null) return

        val myWorkRequest = OneTimeWorkRequestBuilder<LOGGINGTask>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(workDataOf("IMAGE_PATH" to filePath, "MODE" to p0.eventToString()))
            .build()
        workManager.enqueue(myWorkRequest)
    }
}