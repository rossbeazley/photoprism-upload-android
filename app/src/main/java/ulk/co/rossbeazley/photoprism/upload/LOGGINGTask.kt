package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class LOGGINGTask(
    appContext: Context,
    workerParams: WorkerParameters,
    private val auditRepository: AuditRepository
) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val path = inputData.getString("IMAGE_PATH") ?: return Result.failure()
        val mode = inputData.getString("MODE") ?: return Result.failure()
        log("FOUND A FILE TO PROCESS: $path with mode $mode")

        val logMsg = "$mode $path"
        auditRepository.log(logMsg)
        when (mode) {
            "CREATE" -> {
                logGuessOfFinalName(path)
            }
        }

        return Result.success()
    }

    private fun logGuessOfFinalName(path: String) {
        val frags = path.split("_")
        if (frags.size == 5) {
            val epoctime = frags[3].toLong()
            val date = Date.from(Instant.ofEpochMilli(epoctime))
            val format = SimpleDateFormat("yyyyMMdd_HHmmssSSS")
                .format(date)
            val s = "expecting PXL_$format...jpg"
            auditRepository.log(s)
        }
    }
}