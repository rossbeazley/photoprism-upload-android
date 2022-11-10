package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class KeepaliveTask(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val auditRepository: AuditRepository
) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        auditRepository.log("keepalive ${Date().toGMTString()}")
        return Result.success()
    }
}