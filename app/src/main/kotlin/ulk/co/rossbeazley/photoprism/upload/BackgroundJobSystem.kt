package ulk.co.rossbeazley.photoprism.upload

import java.util.*

interface BackgroundJobSystem {
    fun schedule(forPath: String): UUID
    fun register(callback: suspend (String) -> JobResult)
}