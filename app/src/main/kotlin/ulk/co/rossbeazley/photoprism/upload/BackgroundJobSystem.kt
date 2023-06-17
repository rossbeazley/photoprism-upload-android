package ulk.co.rossbeazley.photoprism.upload

interface BackgroundJobSystem {
    fun schedule(forPath: String)
    fun register(callback: suspend (String) -> JobResult)
}