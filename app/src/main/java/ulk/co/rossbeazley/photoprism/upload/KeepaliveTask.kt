package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class KeepaliveTask(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        auditlog("keepalive ${Date().toGMTString()}", appContext)
        return Result.success()
    }
}