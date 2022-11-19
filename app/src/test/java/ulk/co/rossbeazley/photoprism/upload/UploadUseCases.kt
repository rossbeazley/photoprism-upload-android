package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class UploadUseCases {

    class Adapters(
        val fileSystem: Filesystem,
        val auditLogService: CapturingAuditLogService,
        val jobSystem: CapturingBackgroundJobSystem,
        val uploadQueue: UploadQueue,
        val photoServer: MockPhotoServer,
    )

    private lateinit var config: MutableMap<String, String>
    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun build() {
        config = mutableMapOf<String, String>("directory" to "any-directory-path")
        adapters = Adapters(
            fileSystem = Filesystem(),
            auditLogService = CapturingAuditLogService(),
            jobSystem = CapturingBackgroundJobSystem(),
            uploadQueue = UploadQueue(),
            photoServer = MockPhotoServer()
        )
        application = PhotoPrismApp(
            config = config,
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            photoServer = adapters.photoServer as PhotoServer,
            dispatcher = testDispatcher,
        )
    }

    @Test // TODO missing config test
    fun photoDirectoryIsObserved() {
        // given the configuration exists
        val expectedPath = "any-directory-path"

        // then the directory is observered
        assertThat(adapters.fileSystem.watchedPath, equalTo(expectedPath))

        // and an audit log entry is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, isA<ApplicationCreatedAuditLog>())
    }

    @Test
    fun photoUploadScheduled() = runTest(testDispatcher) {
        // when a photo is found
        val expectedFilePath = "any-file-path-at-all"
        adapters.fileSystem.flow.emit(expectedFilePath)

        // then the upload job is scheduled
        assertThat(adapters.jobSystem.jobFilePath, equalTo(expectedFilePath))

        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog, equalTo(ScheduledAuditLog(expectedFilePath)))

        // and a queue entry is created as scheduled
        val expectedQueueEntry = ScheduledFileUpload(expectedFilePath)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

    }

    @Test
    fun photoUploadStarted() = runTest(testDispatcher) {
        //given a download is scheduled
        val expectedFilePath = "any-file-path-at-all"
        adapters.fileSystem.flow.emit(expectedFilePath)

        // when the system is ready to run our job
        adapters.jobSystem.readyCallback(expectedFilePath)

        // then the download is started
        assertThat(adapters.photoServer.path, equalTo(expectedFilePath))

        // and an audit log is created
        // and the queue entry is updated to started
    }

    @Test
    fun photoUploadCompletes() = runTest(testDispatcher) {
        //given a photo is being uploaded
        val expectedFilePath = "any-file-path-at-all"
        adapters.fileSystem.flow.emit(expectedFilePath)
        val uploadResult = adapters.jobSystem.readyCallback(expectedFilePath)

        // when the upload completes
        // photoserver callback complete with ok
        // THIS IS EASY WITH CALLBACKS
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        // and an audit log is created

        // and the job is marked as complete
        assertThat(uploadResult.isSuccess, equalTo(true))
    }

    @Test
    @Ignore("todo")
    fun photoUploadIsRetried() {
        //given a photo is being uploaded
        // when the upload fails
        // then the queue entry is set to retry
        // and an audit log is created
        // and the job is marked as retry
    }

    @Test
    @Ignore("todo")
    fun photoUploadFails() {
        //given a photo is being retried
        // when the upload fails
        // then the queue entry is removed
        // and an audit log is created
        // and the job is marked as failed
        // and a fail queue entry is created
    }
}
