package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class RetryUploadUseCases {

    class Adapters(
        val fileSystem: FakeFilesystem,
        val auditLogService: CapturingAuditLogService,
        val jobSystem: CapturingBackgroundJobSystem,
        val uploadQueue: FakeSyncQueue,
        val photoServer: MockPhotoServer,
    )

    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp
    private val testDispatcher = UnconfinedTestDispatcher()

    var expectedFilePath = ""

    val maxUploadAttempts = 5

    @Before
    fun build() {
        expectedFilePath="any-file-path-at-all-${System.currentTimeMillis()}"
        adapters = Adapters(
            fileSystem = FakeFilesystem(),
            auditLogService = CapturingAuditLogService(),
            jobSystem = CapturingBackgroundJobSystem(),
            uploadQueue = FakeSyncQueue(),
            photoServer = MockPhotoServer(),
        )
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = Config("any-directory-path", maxUploadAttempts)
        )

    }

    private suspend fun uploadFails(): JobResult {
        val uploadResult: Deferred<JobResult>
        coroutineScope {
            uploadResult = async { adapters.jobSystem.runCallback() }
            adapters.photoServer.capturedContinuation?.resume(Result.failure(Exception()))
        }
        return uploadResult.await()
    }

    @Test
    fun photoUploadSucceedsOnLastRetry() = runTest(testDispatcher) {
        //given a photo is being retried

        adapters.fileSystem.flow.emit(expectedFilePath)

        (1 until maxUploadAttempts).forEach {
            uploadFails().apply { assertThat(this, isA<JobResult.Retry>()) }
        }


        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // then the queue entry is removed
        // and an audit log is created

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Success>())
    }

    @Test
    fun photoUploadFailsOnLastRetry() = runTest(testDispatcher) {

        adapters.fileSystem.flow.emit(expectedFilePath)

        (1 until maxUploadAttempts).forEach {
            uploadFails().apply { assertThat(this, isA<JobResult.Retry>()) {"Attempt $it"} }
        }


        val uploadResult: Deferred<JobResult> = async { adapters.jobSystem.runCallback() }

        // when the upload completes
        // photoserver callback complete with ok
        adapters.photoServer.capturedContinuation?.resume(Result.failure(Exception()))

        // then the queue entry is removed
        // and an audit log is created

        // and the job is marked as complete
        assertThat(uploadResult.await(), isA<JobResult.Failure>())
    }

}
