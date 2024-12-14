package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.fakes.Adapters
import ulk.co.rossbeazley.photoprism.upload.fakes.CapturingAuditLogService
import ulk.co.rossbeazley.photoprism.upload.fakes.CapturingBackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.fakes.FakeFilesystem
import ulk.co.rossbeazley.photoprism.upload.fakes.FakeLastUploadRepositoy
import ulk.co.rossbeazley.photoprism.upload.fakes.FakeSyncQueue
import ulk.co.rossbeazley.photoprism.upload.fakes.MockPhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import kotlin.coroutines.resume

@OptIn(ExperimentalCoroutinesApi::class)
class SyncMissedFilesUseCases {

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
            lastUloadRepository = adapters.lastUloadRepository,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = Config("any-directory-path"),
        )
    }

    @Test
    fun syncedFileDosntExistAnyMore() = runTest(testDispatcher) {

        //given a photo has being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        launch { application.readyToUpload(expectedFilePath) }
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // process dies

        // there is a new file
        val newFilePath = expectedFilePath + "new"
        val newFilePath2 = expectedFilePath + "new2"
        adapters.fileSystem.registerFilesNewestFirst(newFilePath, newFilePath2)
        adapters.jobSystem.jobFilePath = null

        //and process comes back to life
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            lastUloadRepository = adapters.lastUloadRepository,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer,
            config = Config("any-directory-path"),
        )

        // then photoUploadScheduled
        assertThat(adapters.jobSystem.jobFilePath, equalTo(null))
    }


    @Test
    fun findsOneMissingFileOnStart() = runTest(testDispatcher) {

        //given a photo has being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        launch { application.readyToUpload(expectedFilePath) }
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // process dies

        // there is a new file
        val newFilePath = expectedFilePath + "new"
        adapters.fileSystem.registerFilesNewestFirst(newFilePath, expectedFilePath)

        //and process comes back to life
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            lastUloadRepository = adapters.lastUloadRepository,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer,
            config = Config("any-directory-path"),
        )

        // then photoUploadScheduled
        assertThat(adapters.jobSystem.jobFilePath, equalTo(newFilePath))
    }



    @Test
    fun findsMultipleMissingFilesOnStart() = runTest(testDispatcher) {

        //given a photo has being uploaded
        adapters.fileSystem.flow.emit(expectedFilePath)
        launch { application.readyToUpload(expectedFilePath) }
        adapters.photoServer.capturedContinuation?.resume(Result.success(Unit))

        // process dies

        // there is a new file
        val newFilePath = expectedFilePath + "new1"
        val newFilePath2 = expectedFilePath + "new2"
        val newFilePath3 = expectedFilePath + "new3"
        adapters.fileSystem.registerFilesNewestFirst(newFilePath, newFilePath2, newFilePath3, expectedFilePath)

        adapters.jobSystem.jobFilePaths.clear()

        //and process comes back to life
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            lastUloadRepository = adapters.lastUloadRepository,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer,
            config = Config("any-directory-path"),
        )

        // then photoUploadScheduled
        assertThat(adapters.jobSystem.jobFilePaths, equalTo(listOf(newFilePath, newFilePath2, newFilePath3)))
    }



    // on app init find the newest file

    // remember the last file that was synced

    // special case of no-op when no synced files, maybe remember newest

    // when keepalive is run list files and see if last synced is still the newest

    // otherwise get list of new files
    // need to mirror the file mask stuff from file watcher

}
