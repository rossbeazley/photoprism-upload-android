package ulk.co.rossbeazley.photoprism.upload

import android.content.Context.MODE_PRIVATE
import androidx.test.platform.app.InstrumentationRegistry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class SharedPrefsSyncQueueIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    lateinit var queue: SyncQueue
    val basename = "RN${System.currentTimeMillis()}"

    @Before
    fun createFolderInCache() {
        queue = SharedPrefsSyncQueue(basename)
    }

    class SharedPrefsSyncQueue(val basename: String = "boop") : SyncQueue {

        private val sharedPrefs =
            InstrumentationRegistry.getInstrumentation().context.getSharedPreferences(
                basename,
                MODE_PRIVATE
            )

        private val sharedPrefs2 =
            InstrumentationRegistry.getInstrumentation().context.getSharedPreferences(
                "${basename}2",
                MODE_PRIVATE
            )

        override fun put(queueEntry: UploadQueueEntry) {
            sharedPrefs.edit()
                .putString(queueEntry.filePath, typeNameFrom(queueEntry))
                .commit()

            sharedPrefs2.edit()
                .putInt(queueEntry.filePath, queueEntry.attemptCount)
                .commit()
        }

        private fun typeNameFrom(queueEntry: UploadQueueEntry): String {
            return when (queueEntry) {
                is CompletedFileUpload -> "CompletedFileUpload"
                is FailedFileUpload -> "FailedFileUpload"
                is RetryFileUpload -> "RetryFileUpload"
                is RunningFileUpload -> "RunningFileUpload"
                is ScheduledFileUpload -> "ScheduledFileUpload"
            }
        }

        private fun typeFromName(
            queueEntryName: String,
            path: String,
            attempt: Int
        ): UploadQueueEntry {
            return when (queueEntryName) {
                "CompletedFileUpload" -> CompletedFileUpload(path)
                "FailedFileUpload" -> FailedFileUpload(path)
                "RetryFileUpload" -> RetryFileUpload(path, attempt)
                "RunningFileUpload" -> RunningFileUpload(path, attempt)
                "ScheduledFileUpload" -> ScheduledFileUpload(path)
                else -> TODO()
            }
        }

        override fun remove(queueEntry: UploadQueueEntry) {
            sharedPrefs.edit()
                .remove(queueEntry.filePath)
                .commit()
        }

        override fun peek(id: String): UploadQueueEntry {
            val string = sharedPrefs.getString(id, null) ?: "unknown"
            val attempt = sharedPrefs2.getInt(id, 0)
            return typeFromName(string, id, attempt)
        }

        override fun all(): List<UploadQueueEntry> {
            val result: List<UploadQueueEntry> = sharedPrefs.all.map {
                val path = it.key ?: "unknown"
                val type = it.value as String
                val attempt = sharedPrefs2.getInt(path, 0)
                val entry: UploadQueueEntry = typeFromName(type, path, attempt)
                entry
            }
            return result
        }

        override fun removeAll() {
            sharedPrefs.edit().clear().commit()

        }
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
        val queue = SharedPrefsSyncQueue(basename)
        val expectedEntry = ScheduledFileUpload(filePath)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(expectedEntry))
    }

    @Test
    fun queuesCanBeTheirOwnUnit() = runTest(testDispatcher) {
        val queue = SharedPrefsSyncQueue("one")
        queue.put(RetryFileUpload(filePath, 1))

        val queue2 = SharedPrefsSyncQueue("two")
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

    @After
    fun clearCache() {
        queue.removeAll()
    }


}

