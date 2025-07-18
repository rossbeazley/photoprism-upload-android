package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.config.InMemoryConfigRepository
import ulk.co.rossbeazley.photoprism.upload.fakes.Adapters
import ulk.co.rossbeazley.photoprism.upload.fakes.CapturingBackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.fakes.FakeFilesystem
import ulk.co.rossbeazley.photoprism.upload.fakes.FakeLastUploadRepositoy
import ulk.co.rossbeazley.photoprism.upload.fakes.MockPhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class UploadAfterRestartUseCases {

    private lateinit var config: MutableMap<String, String>
    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp
    private val testDispatcher = UnconfinedTestDispatcher()

    var expectedFilePath = ""

    @Before
    fun build() {
        expectedFilePath="any-file-path-at-all-${System.currentTimeMillis()}"
        config = mutableMapOf<String, String>("directory" to "any-directory-path")
        adapters = Adapters()
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = InMemoryConfigRepository("any-directory-path"),
            lastUploadRepository = adapters.lastUloadRepository,
        )
    }

    @Test
    fun photoUploadStarted() = runTest(testDispatcher) {
        // given a photo is found and download is scheduled
        adapters.fileSystem.flow.emit(expectedFilePath)


        val adapters = Adapters(
            fileSystem = FakeFilesystem(),
            auditLogService = adapters.auditLogService,
            jobSystem = CapturingBackgroundJobSystem(),
            uploadQueue = adapters.uploadQueue,
            photoServer = MockPhotoServer(),
        )

        val application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = InMemoryConfigRepository("any-directory-path"),
            lastUploadRepository = FakeLastUploadRepositoy(),
        )

        // when the system is ready to run our job
        async { adapters.jobSystem.runCallback(expectedFilePath) }


        // then the download is started
        assertThat(adapters.photoServer.path, equalTo(expectedFilePath))

        // and an audit log is created

        // and the queue entry is updated to started
        val expectedQueueEntry = RunningFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // this just lets the coroutine finish, couldnt get it to cancel
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit)) // TODO enable auto complete

    }
}
