package ulk.co.rossbeazley.photoprism.upload

import kotlin.coroutines.suspendCoroutine

class CapturingBackgroundJobSystem {

    var readyCallback: (String) -> Result<Unit> = { _-> Result.failure(Exception())}
    var jobFilePath : String? = null
    fun schedule(forPath: String, ready: (String)->Result<Unit>) {
        jobFilePath = forPath
        this.readyCallback = ready
    }
}