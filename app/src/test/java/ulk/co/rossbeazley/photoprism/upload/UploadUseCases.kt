package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.audit.ApplicationCreatedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.FailedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.ScheduledAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.UploadedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.UploadingAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.WaitingToRetryAuditLog
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.JobResult
import ulk.co.rossbeazley.photoprism.upload.fakes.Adapters
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RetryFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class UploadUseCases {

    private lateinit var config: MutableMap<String, String>
    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp

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
            dispatcher = adapters.testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = Config("any-directory-path"),
            lastUloadRepository = adapters.lastUloadRepository,
        )
    }

    @Test // TODO missing config test
    fun appInitialisation() {
        // given the configuration exists
        val expectedPath = "any-directory-path"

        // then the directory is observered
        assertThat(adapters.fileSystem.watchedPath, equalTo(expectedPath)) // CONTRACT (file system) will watch configured directory

        // and an audit log entry is created // TODO rework this so we can observe the queue without caring how its implemented
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, isA<ApplicationCreatedAuditLog>())
    }

    @Test
    fun photoUploadScheduled() = runTest(adapters.testDispatcher) {
        // when a photo is found
        adapters.fileSystem.flow.emit(expectedFilePath) // CONTRACT (file system) will emit flow of fil configured directory

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
    fun photoUploadStarted() = runTest(adapters.testDispatcher) {
        //given a download is scheduled
        photoUploadScheduled()

        // when the system is ready to run our job
        val job = launch { adapters.jobSystem.runCallback() }

        // then the download is started
        assertThat(adapters.photoServer.path, equalTo(expectedFilePath))

        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, equalTo(UploadingAuditLog(expectedFilePath)))

        // and the queue entry is updated to started
        val expectedQueueEntry = RunningFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // this just lets the coroutine finish, couldnt get it to cancel
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit)) // TODO enable auto complete

    }

    @Test
    fun photoUploadStartedThroughPrimaryPort() = runTest(adapters.testDispatcher) {
        //given a download is scheduled
        photoUploadScheduled()

        // when the system is ready to run our job

        val job = launch { application.readyToUpload(adapters.jobSystem.jobFilePath!!) }
         // TODO add expect method to fake

        // then the download is started
        assertThat(adapters.photoServer.path, equalTo(expectedFilePath))

        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, equalTo(UploadingAuditLog(expectedFilePath)))

        // and the queue entry is updated to started
        val expectedQueueEntry = RunningFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // this just lets the coroutine finish, couldnt get it to cancel
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit)) // TODO enable auto complete

    }

    @Test
    fun photoUploadCompletes() = runTest(adapters.testDispatcher) {
        //given a photo is being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        assertThat(adapters.uploadQueue.removedQueueEntry?.filePath, equalTo(expectedFilePath))

        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog, equalTo(UploadedAuditLog(expectedFilePath)))

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Success>())
    }

    @Test // the one where the job is requested to retry
    fun photoUploadIsRetried() = runTest(adapters.testDispatcher) {
        //given a photo is being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload fails
        val photoServerException = Exception()
        adapters.photoServer.capturedContinuation?.resume(Result.failure(photoServerException))

        // then the queue entry is set to retry
        val expectedQueueEntry = RetryFileUpload(expectedFilePath, 1)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))

        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog, equalTo(WaitingToRetryAuditLog(
            expectedFilePath,
            attempt = 1,
            optionalThrowable = photoServerException
        )))

        // and the job is marked as retry
        assertThat(uploadResult.await(), isA<JobResult.Retry>())
    }

    @Test
    fun photoUploadSucceedsOnRetry() = runTest(adapters.testDispatcher) {
        //given a photo is being retried
        photoUploadIsRetried()
        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        // and an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
        assertThat(capturedAuditLog, equalTo(UploadedAuditLog(expectedFilePath)))

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Success>())
    }

    @Test
    fun photoUploadFails() = runTest(adapters.testDispatcher) {
        //given a photo is being retried
        photoUploadIsRetried()
        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload fails
        adapters.photoServer.capturedContinuation?.resume(Result.failure(Exception()))

        // then an audit log is created
        val capturedAuditLog = adapters.auditLogService.capturedAuditLog
            assertThat(capturedAuditLog, equalTo(FailedAuditLog(expectedFilePath)))

        // and the job is marked as failed
        assertThat(uploadResult.await(), isA<JobResult.Failure>())

        // and a fail queue entry is created
        val expectedQueueEntry = FailedFileUpload(expectedFilePath)
        assertThat(adapters.uploadQueue.capturedQueueEntry, equalTo(expectedQueueEntry))
    }

    @Test
    fun uploadTwoFiles() = runTest(adapters.testDispatcher) {

        expectedFilePath = "one"
        //assert that upload 1 success
        photoUploadCompletes()

        expectedFilePath = "two"
        // assert that upload 2 success
        photoUploadCompletes()
    }

    @Test
    fun uploadTwoSlowFilesFirstRetries() = runTest(adapters.testDispatcher) {
        expectedFilePath = "one"
        photoUploadScheduled()

        val resultOne = async { adapters.jobSystem.runCallback() }
        val uploadOne = adapters.photoServer.capturedContinuation

        expectedFilePath = "two"
        photoUploadScheduled()

        val resultTwo = async { adapters.jobSystem.runCallback() }
        val uploadTwo = adapters.photoServer.capturedContinuation

        uploadOne?.resume(Result.failure(Exception()))

        assertThat(resultTwo.isCompleted, equalTo(false))

        uploadTwo?.resume(Result.success(Unit))
    }

    @Test
    fun uploadTwoSlowFilesBothRetries() = runTest(adapters.testDispatcher) {
        expectedFilePath = "one"
        photoUploadScheduled()

        val resultOne = async { adapters.jobSystem.runCallback() }
        val uploadOne = adapters.photoServer.capturedContinuation

        expectedFilePath = "two"
        photoUploadScheduled()

        val resultTwo = async { adapters.jobSystem.runCallback() }
        val uploadTwo = adapters.photoServer.capturedContinuation

        uploadOne?.resume(Result.failure(Exception()))

        uploadTwo?.resume(Result.failure(Exception()))

        assertThat(resultOne.await(), equalTo(JobResult.Retry))
        assertThat(resultTwo.await(), equalTo(JobResult.Retry))
    }
}
