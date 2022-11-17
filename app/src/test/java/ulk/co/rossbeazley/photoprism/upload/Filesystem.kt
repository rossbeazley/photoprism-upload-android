package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class Filesystem {
    val flow = MutableSharedFlow<String>()
    var watchedPath = "NONE"
    fun watch(path: String): Flow<String> {
        watchedPath = path
        return flow
    }
}