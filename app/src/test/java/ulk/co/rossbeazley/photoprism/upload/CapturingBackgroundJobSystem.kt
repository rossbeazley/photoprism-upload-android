package ulk.co.rossbeazley.photoprism.upload

class CapturingBackgroundJobSystem {
    var jobFilePath : String? = null
    fun schedule(forPath: String) {
        jobFilePath = forPath
    }
}