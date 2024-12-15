package ulk.co.rossbeazley.photoprism.upload

import androidx.test.platform.app.InstrumentationRegistry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RetryFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsSyncQueue
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SyncQueue
import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry
import kotlin.random.Random

class SharedPrefsSyncQueueIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    lateinit var queue: SyncQueue
    val basename = "RN${System.currentTimeMillis()}"

    @Before
    fun createFolderInCache() {
        queue = SharedPrefsSyncQueue(basename, InstrumentationRegistry.getInstrumentation().context)
    }

    val filePath = "filepath${System.nanoTime()}"

    @Test
    fun savedScheduledFileUploadEntry() = runTest(testDispatcher) {
        val savedEntry = ScheduledFileUpload(filePath)
        queue.put(savedEntry)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(savedEntry))
    }

    @Test
    fun savedRunningFileUploadEntry() = runTest(testDispatcher) {
        val attemptCount = Random.nextInt()
        val savedEntry = RunningFileUpload(filePath, attemptCount)
        queue.put(savedEntry)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(savedEntry))
    }

    @Test
    fun savedRetryFileUploadEntry() = runTest(testDispatcher) {
        val attemptCount = Random.nextInt()
        val savedEntry = RetryFileUpload(filePath, attemptCount)
        queue.put(savedEntry)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(savedEntry))
    }

    @Test
    fun savedFailedFileUploadEntry() = runTest(testDispatcher) {
        val savedEntry = FailedFileUpload(filePath)
        queue.put(savedEntry)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(savedEntry))
    }

    @Test
    fun savedScheduledFileUploadEntrySurvivesRestart() = runTest(testDispatcher) {
        savedScheduledFileUploadEntry()
        val queue = SharedPrefsSyncQueue(
            basename,
            InstrumentationRegistry.getInstrumentation().context
        )
        val expectedEntry = ScheduledFileUpload(filePath)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(expectedEntry))
    }

    @Test
    fun queuesCanBeTheirOwnUnit() = runTest(testDispatcher) {
        val queue = SharedPrefsSyncQueue(
            "one",
            InstrumentationRegistry.getInstrumentation().context
        )
        queue.put(RetryFileUpload(filePath, 1))

        val queue2 = SharedPrefsSyncQueue(
            "two",
            InstrumentationRegistry.getInstrumentation().context
        )
        queue2.put(RetryFileUpload(filePath, 2))

        val expectedEntry = RetryFileUpload(filePath, 1)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(expectedEntry))
    }

    @Test
    fun listsAllEntries() = runTest(testDispatcher) {
        queue.put(ScheduledFileUpload("${filePath}1"))
        queue.put(RetryFileUpload("${filePath}2", 4))
        queue.put(ScheduledFileUpload("${filePath}3"))
        queue.put(RunningFileUpload("${filePath}4"))
        queue.put(ScheduledFileUpload("${filePath}5"))
        assertThat(queue.all().size, equalTo(5))
        assertThat(
            queue.all(),
            List<UploadQueueEntry>::containsAll,
            listOf<UploadQueueEntry>(
                ScheduledFileUpload("${filePath}1"),
                RetryFileUpload("${filePath}2", 4),
                ScheduledFileUpload("${filePath}3"),
                RunningFileUpload("${filePath}4"),
                ScheduledFileUpload("${filePath}5"),
            )

        )
    }

    // REMOVE
    @Test
    fun removeOneEntry() = runTest(testDispatcher) {
        queue.put(ScheduledFileUpload("${filePath}1"))
        queue.put(RetryFileUpload("${filePath}2", 4))

        queue.remove(ScheduledFileUpload("${filePath}1"))

        assertThat(queue.all().size, equalTo(1))
        assertThat(
            queue.all(),
            List<UploadQueueEntry>::containsAll,
            listOf<UploadQueueEntry>(
                RetryFileUpload("${filePath}2", 4),
            )
        )
    }


    // REMOVE ALL
    @Test
    fun removeAllEntries() = runTest(testDispatcher) {
        queue.put(ScheduledFileUpload("${filePath}1"))
        queue.put(RetryFileUpload("${filePath}2", 4))
        queue.put(ScheduledFileUpload("${filePath}3"))
        queue.put(RunningFileUpload("${filePath}4"))
        queue.put(ScheduledFileUpload("${filePath}5"))

        queue.removeAll()

        assertThat(queue.all().size, equalTo(0))
    }

    @Test
    fun willStoreFiveCompletedDownloads() {
        queue.put(CompletedFileUpload("1"))
        queue.put(CompletedFileUpload("2"))
        queue.put(CompletedFileUpload("3"))
        queue.put(CompletedFileUpload("4"))
        queue.put(CompletedFileUpload("5"))
        assertThat(
            queue.all(),
            List<UploadQueueEntry>::equals,
            listOf(
                CompletedFileUpload("1"),
                CompletedFileUpload("2"),
                CompletedFileUpload("3"),
                CompletedFileUpload("4"),
                CompletedFileUpload("5"),
            )
        )

    }


    @Test
    fun oldestCompletedDownloadsRecordWillBeRemovedAfterFive() {
        queue.put(CompletedFileUpload("1"))
        queue.put(CompletedFileUpload("2"))
        queue.put(CompletedFileUpload("3"))
        queue.put(CompletedFileUpload("4"))
        queue.put(CompletedFileUpload("5"))
        queue.put(CompletedFileUpload("6"))
        assertThat(
            queue.all(),
            List<UploadQueueEntry>::equals,
            listOf(
                CompletedFileUpload("2"),
                CompletedFileUpload("3"),
                CompletedFileUpload("4"),
                CompletedFileUpload("5"),
                CompletedFileUpload("6"),
            )
        )

    }

    @After
    fun clearCache() {
        queue.removeAll()
    }


}

