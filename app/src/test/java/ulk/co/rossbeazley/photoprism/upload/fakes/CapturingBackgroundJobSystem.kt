package ulk.co.rossbeazley.photoprism.upload.fakes

import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.BackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.JobResult
import java.lang.AssertionError

class CapturingBackgroundJobSystem : BackgroundJobSystem {

    var jobFilePath : String? = null
    val jobFilePaths : MutableList<String> = mutableListOf()
    override fun schedule(forPath: String) : String {
        jobFilePath = forPath
        jobFilePaths += forPath
        return forPath
    }

    var readyCallback: suspend (String) -> JobResult = { _-> JobResult.Failure }
    override fun register(callback: suspend (String) -> JobResult) {
        readyCallback = callback
    }

    // TODO could we make this return async? this fake needs context that way
    suspend fun runCallback(forFilePath : String? = jobFilePath): JobResult {
        forFilePath ?: throw AssertionError("Ready callback not registered")
        return readyCallback(forFilePath)
    }
}
