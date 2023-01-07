package ulk.co.rossbeazley.photoprism.upload

import android.os.FileObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File

class AndroidFileObserverFilesystem(testDispatcher: CoroutineDispatcher = Dispatchers.IO, private val scope: CoroutineScope = CoroutineScope(
    testDispatcher
)
) : Filesystem {
    var fileObserver: FileObserver? = null

    override fun watch(path: String): Flow<String> {
        val emptyFlow = MutableSharedFlow<String>()
        fileObserver = FlowingPathObserver(scope, path, emptyFlow).also { it.startWatching() }
        return emptyFlow
    }

    class FlowingPathObserver(
        private val dispatcher: CoroutineScope,
        private val path: String,
        private val emptyFlow: MutableSharedFlow<String>
    ) : FileObserver(File(path), CREATE or MOVED_TO) {
        override fun onEvent(p0: Int, file: String?) {
            file ?: return
            if(file.startsWith(".")) return
            dispatcher.launch { emptyFlow.emit("$path/${file}") }
        }
    }
}