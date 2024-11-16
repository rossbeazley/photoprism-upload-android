package ulk.co.rossbeazley.photoprism.upload.photoserver

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.WebDavPhotoServer
import ulk.co.rossbeazley.photoprism.upload.log

class WebdavPutTask(
    appContext: Context,
    workerParams: WorkerParameters,
    val webDavPhotoServer: WebDavPhotoServer
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        log("Do some work")
        val path = inputData.getString("IMAGE_PATH") ?: return Result.failure()
        log("File path is $path")

        val doUpload = webDavPhotoServer.doUpload(path)
        return when {
            doUpload.isSuccess -> Result.success()
            doUpload.isFailure -> Result.retry()
            else -> Result.failure()
        }
    }
}
