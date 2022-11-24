package ulk.co.rossbeazley.photoprism.upload

import kotlin.coroutines.suspendCoroutine

class CapturingBackgroundJobSystem {

    var readyCallback: suspend (String) -> JobResult = { _-> JobResult.Failure}
    var jobFilePath : String? = null
    fun schedule(forPath: String, ready: suspend (String)->JobResult) {
        jobFilePath = forPath
        this.readyCallback = ready
    }
}