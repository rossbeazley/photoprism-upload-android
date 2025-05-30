package ulk.co.rossbeazley.photoprism.upload.filesystem

import android.os.FileObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.log
import java.io.File
import java.util.Arrays

class AndroidFileObserverFilesystem(testDispatcher: CoroutineDispatcher = Dispatchers.IO, private val scope: CoroutineScope = CoroutineScope(
    testDispatcher
)
) : Filesystem {
    var fileObserver: FlowingPathObserver? = null

    override fun watch(path: String): Flow<String> {
        if(fileObserver!=null) {
            log("ALREADY WATCHING!!!")
            return fileObserver!!.emptyFlow
        }
        val flowingPathObserver = FlowingPathObserver(scope, path)
        fileObserver = flowingPathObserver.also { it.startWatching() }
        log("Watching $path")
        return flowingPathObserver.emptyFlow
    }

    override fun list(path: String): List<String> {
        val listFiles = File(path).listFiles() ?: return emptyList()
        Arrays.sort(listFiles) { o1: File, o2: File ->
            o2.lastModified().compareTo(o1.lastModified())
        }
        return listFiles.map { it.path }
    }

    class FlowingPathObserver(
        private val scope: CoroutineScope,
        private val path: String,
        internal val emptyFlow: MutableSharedFlow<String> = MutableSharedFlow()
    ) : FileObserver(File(path), CREATE or MOVED_TO) {
        override fun onEvent(p0: Int, file: String?) {
            log("File watch event $p0 $file")
            file ?: return
            if(file.startsWith(".")) return
            if(file.contains(".trashed")) return
            scope.launch {
                try {
                    emptyFlow.emit("$path/${file}")
                    log("emitted $path/${file}")
                } catch (e : Exception) {
                    log("Exception During emit $path/${file} ${e.message}")
                }
            }
            log("File watch event done")
        }

        override fun stopWatching() {
            log("File watcher stop watching")
            super.stopWatching()
        }

        override fun finalize() {
            log("File watcher finalize")
            super.finalize()
        }
    }
}