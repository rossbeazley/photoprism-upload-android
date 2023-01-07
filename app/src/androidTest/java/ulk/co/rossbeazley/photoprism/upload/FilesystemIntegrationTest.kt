package ulk.co.rossbeazley.photoprism.upload

import android.os.FileObserver
import androidx.test.platform.app.InstrumentationRegistry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.Exception

class FilesystemIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    val filesystem: Filesystem = AndroidFileObserverFilesystem(CoroutineScope(testDispatcher))

    class AndroidFileObserverFilesystem(val dispatcher: CoroutineScope) : Filesystem {
        var fileObserver: FileObserver? = null

        override fun watch(path: String): Flow<String> {
            val emptyFlow = MutableSharedFlow<String>()
            fileObserver = FileObserver1(dispatcher, path, emptyFlow).also { it.startWatching() }
            return emptyFlow
        }

        class FileObserver1(
            private val dispatcher: CoroutineScope,
            private val path: String,
            private val emptyFlow: MutableSharedFlow<String>
        ) : FileObserver(File(path), CREATE or MOVED_TO) {
            override fun onEvent(p0: Int, file: String?) {
                if(file?.startsWith(".") == true) return
                file ?: return
                dispatcher.launch {
                    emptyFlow.emit("$path/${file}")
                }
            }
        }
    }

    lateinit var observedDir: File

    @Before
    fun createFolderInCache() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        observedDir = File(cacheDir, "inttest${System.nanoTime()}")
        observedDir.mkdir()
    }

    @Test
    fun emitsNewFileObservationFlow() = runTest(testDispatcher) {
        // given we are observing a folder in the cache diretory
        val flowOfDirectories = filesystem.watch(observedDir.path)
        val asyncResult = async { flowOfDirectories.first() }

        // when we create a file in that folder
        val expectedFile = File(observedDir, "expected.jpg")
        expectedFile.createNewFile()

        //then
        assertThat(asyncResult.await(), equalTo(expectedFile.path))
    }

    @Test
    fun ignoresDotFiles() = runTest(testDispatcher) {
        // given we are observing a folder in the cache diretory
        val flowOfDirectories = filesystem.watch(observedDir.path)
        val observedDirectories = mutableListOf<String>()
        val job = launch {
            flowOfDirectories.cancellable().collect {
                observedDirectories.add(it)
            }
        }

        // when we create a file in that folder
        val expectedFile = File(observedDir, ".unexpected.jpg")
        val createNewFile: Boolean = expectedFile.createNewFile()

        //then
        async {
            repeat(20) {
                if(observedDirectories.isNotEmpty()) return@async
                delay(100)
            }
            return@async
        }.await()

        assertThat(observedDirectories.count(), equalTo(0))
        job.cancel()
    }

    @Test
    fun emitsFilesCreatedFromDotFiles() = runTest(testDispatcher) {
        // given we are observing a folder in the cache diretory
        val flowOfDirectories = filesystem.watch(observedDir.path)

        val asyncResult = async { flowOfDirectories.first() }

        // when we create a file in that folder
        val expectedFile = File(observedDir, "expected.jpg")
        File(observedDir, ".temp-file.jpg").apply {
            createNewFile()
            renameTo(expectedFile)
        }

        //then
        assertThat(asyncResult.await(), equalTo(expectedFile.path))
    }

    @After
    fun clearCache() {
        try {
            observedDir.deleteRecursively()
        } catch (_: Exception) {
        }
    }
}