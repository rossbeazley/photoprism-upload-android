package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

interface BackgroundJobSystem {
    fun schedule(forPath: String): String
    fun register(callback: suspend (String) -> JobResult)
}