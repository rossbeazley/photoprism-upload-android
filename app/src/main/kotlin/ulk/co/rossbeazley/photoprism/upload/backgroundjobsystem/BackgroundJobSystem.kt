package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import ulk.co.rossbeazley.photoprism.upload.JobResult

interface BackgroundJobSystem {
    fun schedule(forPath: String): String
    fun register(callback: suspend (String) -> JobResult)
}