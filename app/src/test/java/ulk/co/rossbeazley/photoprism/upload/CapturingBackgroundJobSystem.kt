package ulk.co.rossbeazley.photoprism.upload

import kotlin.coroutines.suspendCoroutine

class CapturingBackgroundJobSystem {

    var readyCallback: (String) -> Unit = {_->}
    var jobFilePath : String? = null
    fun schedule(forPath: String, ready: (String)->Unit) {
        jobFilePath = forPath
        this.readyCallback = ready
    }
}