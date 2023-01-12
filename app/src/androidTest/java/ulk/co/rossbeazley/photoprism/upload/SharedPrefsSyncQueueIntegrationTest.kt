package ulk.co.rossbeazley.photoprism.upload

import android.content.Context.MODE_PRIVATE
import androidx.test.platform.app.InstrumentationRegistry
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.Exception
import kotlin.random.Random

class SharedPrefsSyncQueueIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun createFolderInCache() {
    }

    @Test
    fun savedScheduledFileUploadEntry() = runTest(testDispatcher) {
        val queue : SyncQueue = object : SyncQueue {

            val sharedPrefs = InstrumentationRegistry.getInstrumentation().context.getSharedPreferences("boop", MODE_PRIVATE)

            override fun put(queueEntry: UploadQueueEntry) {
                sharedPrefs.edit().putInt(queueEntry.filePath,queueEntry.attemptCount).commit()
            }

            override fun remove(queueEntry: UploadQueueEntry) {

            }

            override fun peek(id: String): UploadQueueEntry {
             return ScheduledFileUpload(id,sharedPrefs.getInt(id,0))
            }

            override fun all(): List<UploadQueueEntry> {
                TODO("Not yet implemented")
            }
        }

        val attemptCount = Random.nextInt()
        val filePath = "filepath${System.nanoTime()}"
        val savedEntry = ScheduledFileUpload(filePath, attemptCount)
        queue.put(savedEntry)
        val peekedEntry = queue.peek(filePath)
        assertThat(peekedEntry, equalTo(savedEntry))

    }

    fun savedScheduledFileUploadEntrySurvivesRestart(){}

    @After
    fun clearCache() {
    }


}

