package ulk.co.rossbeazley.photoprism.upload

import java.util.*

interface BackgroundJobSystem {
    fun schedule(forPath: String): String
    fun register(callback: suspend (String) -> JobResult)
}