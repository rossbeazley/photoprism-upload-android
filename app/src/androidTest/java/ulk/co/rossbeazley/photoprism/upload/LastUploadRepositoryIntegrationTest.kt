package ulk.co.rossbeazley.photoprism.upload

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.syncqueue.LastUploadRepository
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SharedPrefsLastUploadRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LastUploadRepositoryIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    val basename = "RN${System.currentTimeMillis()}"

    var sut: LastUploadRepository? = null

    @Before
    fun createSUT() {
        sut = SharedPrefsLastUploadRepository(
            basename,
            getInstrumentation().context
        )
    }

    val filePath = "filepath${System.nanoTime()}"

    @Test
    fun remembersFilePaths() = runTest(testDispatcher) {
        sut?.remember(filePath)
        val peekedEntry = sut?.recall()
        assertThat(peekedEntry, equalTo(filePath))
    }

    @Test
    fun remembersFilePathsAcrossRestarts() = runTest(testDispatcher) {
        sut?.remember(filePath)

        createSUT()

        val peekedEntry = sut?.recall()
        assertThat(peekedEntry, equalTo(filePath))
    }

    @After
    fun clearCache() {
        getInstrumentation().context.deleteSharedPreferences(basename)
    }
}

