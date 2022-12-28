package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
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

    var expectedFilePath = ""

    @Before
    fun build() {
        expectedFilePath="any-file-path-at-all-${System.currentTimeMillis()}"
        config = mutableMapOf<String, String>("directory" to "any-directory-path")
        adapters = Adapters(
            fileSystem = Filesystem(),
            auditLogService = CapturingAuditLogService(),
            jobSystem = CapturingBackgroundJobSystem(),
            uploadQueue = UploadQueue(),
            photoServer = MockPhotoServer(),
        )
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = Config("any-directory-path"),
        )
    }

    @Test // TODO missing config test
    fun photoDirectoryIsObserved() {
        // given the configuration exists
        val expectedPath = "any-directory-path"

        // then the directory is observered
        assertThat(adapters.fileSystem.watchedPath, equalTo(expectedPath))

        // and an audit log entry is created // TODO rework this so we can observe the queue without caring how its implemented
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, isA<ApplicationCreatedAuditLog>())
    }

    @Test
    fun photoUploadScheduled() = runTest(testDispatcher) {
        // when a photo is found
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
        photoUploadScheduled()

        // when the system is ready to run our job
        val job = launch { application.readyToUpload(expectedFilePath) }

        // then the download is started
        assertThat(adapters.photoServer.path, equalTo(expectedFilePath))

        // and an audit log is created

        // and the queue entry is updated to started
        val expectedQueueEntry = RunningFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // this just lets the coroutine finish, couldnt get it to cancel
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit)) // TODO enable auto complete

    }
    @Test
    fun photoUploadCompletes() = runTest(testDispatcher) {
        //given a photo is being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        val uploadResult = async { application.readyToUpload(expectedFilePath) }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        assertThat(adapters.uploadQueue.removedQueueEntry?.filePath, equalTo(expectedFilePath))

        // and an audit log is created

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Success>())
    }

    @Test // the one where the job is requested to retry
    fun photoUploadIsRetried() = runTest(testDispatcher) {
        //given a photo is being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        val uploadResult: Deferred<JobResult> = async { application.readyToUpload(expectedFilePath) }

        // when the upload fails
        adapters.photoServer.capturedContinuation?.resume(Result.failure(Exception()))

        // then the queue entry is set to retry
        val expectedQueueEntry = RetryFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // and an audit log is created

        // and the job is marked as retry
        assertThat(uploadResult.await(), isA<JobResult.Retry>())
    }

    @Test
    fun photoUploadSucceedsOnRetry() = runTest(testDispatcher) {
        //given a photo is being retried
        photoUploadIsRetried()
        val uploadResult: Deferred<JobResult> = async { application.readyToUpload(expectedFilePath) }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        // and an audit log is created

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Success>())
    }

    @Test
    fun photoUploadFails() = runTest(testDispatcher) {
        //given a photo is being retried
        photoUploadIsRetried()
        val uploadResult: Deferred<JobResult> = async { application.readyToUpload(expectedFilePath) }

        // when the upload fails
        adapters.photoServer.capturedContinuation?.resume(Result.failure(Exception()))

        // then an audit log is created
        // and the job is marked as failed
        assertThat(uploadResult.await(), isA<JobResult.Failure>())

        // and a fail queue entry is created
        val expectedQueueEntry = FailedFileUpload(expectedFilePath)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))
    }

    @Test
    fun uploadTwoFiles() = runTest(testDispatcher) {

        expectedFilePath = "one"
        //assert that upload 1 success
        photoUploadCompletes()

        expectedFilePath = "two"
        // assert that upload 2 success
        photoUploadCompletes()
    }

    @Test
    fun uploadTwoSlowFilesFirstRetries() = runTest(testDispatcher) {
        expectedFilePath = "one"
        photoUploadScheduled()

        val resultOne = async { application.readyToUpload(expectedFilePath) }
        val uploadOne = adapters.photoServer.capturedContinuation

        expectedFilePath = "two"
        photoUploadScheduled()

        val resultTwo = async { application.readyToUpload(expectedFilePath) }
        val uploadTwo = adapters.photoServer.capturedContinuation

        uploadOne?.resume(Result.failure(Exception()))

        assertThat(resultTwo.isCompleted, equalTo(false))

        uploadTwo?.resume(Result.success(Unit))
    }

    @Test
    fun uploadTwoSlowFilesBothRetries() = runTest(testDispatcher) {
        expectedFilePath = "one"
        photoUploadScheduled()

        val resultOne = async { application.readyToUpload(expectedFilePath) }
        val uploadOne = adapters.photoServer.capturedContinuation

        expectedFilePath = "two"
        photoUploadScheduled()

        val resultTwo = async { application.readyToUpload(expectedFilePath) }
        val uploadTwo = adapters.photoServer.capturedContinuation

        uploadOne?.resume(Result.failure(Exception()))

        uploadTwo?.resume(Result.failure(Exception()))

        assertThat(resultOne.await(), equalTo(JobResult.Retry))
        assertThat(resultTwo.await(), equalTo(JobResult.Retry))
    }
}
