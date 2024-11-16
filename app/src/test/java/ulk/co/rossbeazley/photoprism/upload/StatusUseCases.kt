package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RetryFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload

@OptIn(ExperimentalCoroutinesApi::class)
class StatusUseCases {

    class Adapters(
        val fileSystem: FakeFilesystem,
        val auditLogService: CapturingAuditLogService,
        val jobSystem: CapturingBackgroundJobSystem,
        val uploadQueue: FakeSyncQueue,
        val photoServer: MockPhotoServer,
    )

    private lateinit var config: MutableMap<String, String>
    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp
    private val testDispatcher = UnconfinedTestDispatcher()

    var expectedFilePath = ""

    @Before
    fun build() {
        expectedFilePath = "any-file-path-at-all-${System.currentTimeMillis()}"
        config = mutableMapOf<String, String>("directory" to "any-directory-path")
        adapters = Adapters(
            fileSystem = FakeFilesystem(),
            auditLogService = CapturingAuditLogService(),
            jobSystem = CapturingBackgroundJobSystem(),
            uploadQueue = FakeSyncQueue(),
            photoServer = MockPhotoServer(true),
        )
        application = PhotoPrismApp(
            fileSystem = adapters.fileSystem,
            jobSystem = adapters.jobSystem,
            auditLogService = adapters.auditLogService,
            uploadQueue = adapters.uploadQueue,
            dispatcher = testDispatcher,
            photoServer = adapters.photoServer as PhotoServer,
            config = Config("any-directory-path", 2),
        )
    }

    @Test
    fun observeSyncQueue() = runTest(testDispatcher) {

        //given some items
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
            put(ScheduledFileUpload("two"))
            put(RunningFileUpload("three"))
            put(RetryFileUpload("four"))
            put(FailedFileUpload("five"))
        }

        //when we observe
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(5)
                .collect(observedEvents::add)
            observedEvents
        }

        //then we see them
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("two")),
            NewEvent(RunningFileUpload("three")),
            NewEvent(RetryFileUpload("four")),
            NewEvent(FailedFileUpload("five")),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    @Test
    fun observeNewEntry() = runTest(testDispatcher) {
        //given observing some items
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(2)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        adapters.jobSystem.readyCallback("new")

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new")),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    @Test
    fun observeRunningEntry() = runTest(testDispatcher) {
        //given observing some items
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(3)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        adapters.jobSystem.readyCallback("new")

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new")),
            NewEvent(RunningFileUpload("new", 1)),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }


    @Test
    fun observeRetryEntry() = runTest(testDispatcher) {
        //given observing some items
        adapters.photoServer.autoComplete = false
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(4)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadFails()

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new")),
            NewEvent(RunningFileUpload("new", 1)),
            NewEvent(RetryFileUpload("new", 1)),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    @Test
    fun observeFailedEntry() = runTest(testDispatcher) {
        //given observing some items
        adapters.photoServer.autoComplete = false
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(6)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadFails()

        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadFails()

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new")),
            NewEvent(RunningFileUpload("new", 1)),
            NewEvent(RetryFileUpload("new", 1)),
            NewEvent(RunningFileUpload("new", 2)),
            NewEvent(FailedFileUpload("new")),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    // download completes

    @Test
    fun observeCompletion() = runTest(testDispatcher) {
        //given observing some items
        adapters.photoServer.autoComplete = false
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }
        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(4)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadCompletes()

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new")),
            NewEvent(RunningFileUpload("new", 1)),
            NewEvent(CompletedFileUpload("new")),
        )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    @Test
    fun observePostCompletion() = runTest(testDispatcher) {
        //given observing some items
        adapters.photoServer.autoComplete = false
        adapters.uploadQueue.apply {
            put(ScheduledFileUpload("one"))
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadCompletes()


        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .take(2)
                .collect(observedEvents::add)
            observedEvents
        }

        adapters.fileSystem.flow.emit("new2")

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("one")),
            NewEvent(ScheduledFileUpload("new2")),
         )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }
}
