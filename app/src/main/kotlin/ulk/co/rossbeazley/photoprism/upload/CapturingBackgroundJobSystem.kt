package ulk.co.rossbeazley.photoprism.upload

import java.lang.AssertionError

class CapturingBackgroundJobSystem {

    var jobFilePath : String? = null
    fun schedule(forPath: String) {
        jobFilePath = forPath
    }

    var readyCallback: suspend (String) -> JobResult = { _-> JobResult.Failure }
    fun register(callback: suspend (String) -> JobResult) {
        readyCallback = callback
    }

    suspend fun runCallback(forFilePath : String? = jobFilePath): JobResult {
        forFilePath ?: throw AssertionError("Ready callback not registered")
        return readyCallback(forFilePath)
    }

}