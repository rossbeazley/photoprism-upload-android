package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture

class JobSystemWorker(
    private val cb: suspend (String) -> JobResult,
    context: Context,
    private val parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val path = parameters.inputData.getString("A") ?: ""
        val job: Deferred<JobResult> = GlobalScope.async {
            val deferred = async { cb(path) }
            deferred.await()
        }
        val r: JobResult = job.asCompletableFuture().get()
        return when (r) {
            JobResult.Retry -> Result.retry()
            JobResult.Failure -> Result.failure()
            else -> Result.success()
        }
    }
}