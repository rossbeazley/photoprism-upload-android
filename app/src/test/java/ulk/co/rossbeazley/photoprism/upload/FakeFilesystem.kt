package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ulk.co.rossbeazley.photoprism.upload.filesystem.Filesystem

class FakeFilesystem : Filesystem {
    val flow = MutableSharedFlow<String>()
    var watchedPath = "NONE"
    override fun watch(path: String): Flow<String> {
        watchedPath = path
        return flow
    }

    private var files : List<String> = emptyList()

    fun registerFilesNewestFirst(vararg files: String) {
        this.files = files.toList()
    }

    override fun list(path: String) : List<String> {
        return files
    }
}