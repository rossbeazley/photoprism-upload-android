package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.fakes.Adapters
import ulk.co.rossbeazley.photoprism.upload.fakes.MockPhotoServer
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RetryFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload

@OptIn(ExperimentalCoroutinesApi::class)
class StatusUseCases {

    private lateinit var config: MutableMap<String, String>
    private lateinit var adapters: Adapters
    private lateinit var application: PhotoPrismApp
    private val testDispatcher = UnconfinedTestDispatcher()

    var expectedFilePath = ""

    @Before
    fun build() {
        expectedFilePath = "any-file-path-at-all-${System.currentTimeMillis()}"
        config = mutableMapOf("directory" to "any-directory-path")
        adapters = Adapters(
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
            lastUloadRepository = adapters.lastUloadRepository,
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
            val observedEvents = mutableListOf<FullState>()
            application.observeSyncEvents()
                .filterIsInstance(FullState::class)
                .take(1)
                .collect(observedEvents::add)
            observedEvents
        }

        //then we see them
        val expectedOobservedSyncEvents = FullState(listOf(
            ScheduledFileUpload("one"),
            ScheduledFileUpload("two"),
            RunningFileUpload("three"),
            RetryFileUpload("four"),
            FailedFileUpload("five"),
        ))

        assertThat(job.await().last(), equalTo(expectedOobservedSyncEvents))
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
                .filterIsInstance(NewEvent::class)
                .take(1)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        adapters.jobSystem.readyCallback("new")

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
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
                .filterIsInstance(NewEvent::class)
                .take(2)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        adapters.jobSystem.readyCallback("new")

        //then we see the new one (or is it a list or something)
        val expectedObservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("new")),
            NewEvent(RunningFileUpload("new", 1)),
        )

        assertThat(job.await(), equalTo(expectedObservedSyncEvents))
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
                .filterIsInstance(NewEvent::class)
                .take(3)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadFails()

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
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
                .filterIsInstance(NewEvent::class)
                .take(5)
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
                .filterIsInstance(NewEvent::class)
                .take(3)
                .collect(observedEvents::add)
            observedEvents
        }

        //when new file is synced
        adapters.fileSystem.flow.emit("new") // CONTRACT (file system) will emit flow of fil configured directory
        async { adapters.jobSystem.runCallback("new") }
        adapters.photoServer.currentUploadCompletes()

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
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
        async { application.readyToUpload("new") }
        adapters.photoServer.currentUploadCompletes()


        val job = async {
            val observedEvents = mutableListOf<NewEvent>()
            application.observeSyncEvents()
                .filterIsInstance(NewEvent::class)
                .take(1)
                .collect(observedEvents::add)
            observedEvents
        }

        adapters.fileSystem.flow.emit("new2")

        //then we see the new one (or is it a list or something)
        val expectedOobservedSyncEvents = listOf(
            NewEvent(ScheduledFileUpload("new2")),
         )

        assertThat(job.await(), equalTo(expectedOobservedSyncEvents))
    }

    @Test
    fun clearsSyncQueue() = runTest(testDispatcher) {

        val job = async {
            val observedEvents = mutableListOf<FullState>()
            application.observeSyncEvents()
                .filterIsInstance(FullState::class)
                .take(2)
                .collect(observedEvents::add)
            observedEvents
        }

        application.clearSyncQueue()
        assertThat(adapters.uploadQueue.allRemoved, equalTo(true))

        assertThat(job.await().last(), equalTo(FullState(emptyList())))

    }
}
