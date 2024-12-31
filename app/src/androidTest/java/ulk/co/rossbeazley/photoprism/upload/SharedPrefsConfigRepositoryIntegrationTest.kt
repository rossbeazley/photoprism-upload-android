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
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SharedPrefsConfigRepositoryIntegrationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val basename = "CRN${System.currentTimeMillis()}"

    private lateinit var sut: SharedPrefsConfigRepository

    @Before
    fun createSUT() {
        sut = SharedPrefsConfigRepository(
            basename = basename,
            context = getInstrumentation().targetContext
        )
    }

    @Test
    fun createDefaults() = runTest(testDispatcher) {
        assertThat(sut.photoDirectory, equalTo("/storage/emulated/0/DCIM/Camera"))
        assertThat(sut.maxUploadAttempts, equalTo(10))
    }

    @Test
    fun update() = runTest(testDispatcher) {
        val newHostname = sut.hostname + "${System.currentTimeMillis()}"
        val newUsername = sut.username + "${System.currentTimeMillis()}"
        val newPassword = sut.password + "${System.currentTimeMillis()}"
        val newPhotoDirectory = sut.photoDirectory + "${System.currentTimeMillis()}"
        sut.save(
            hostname = newHostname,
            username = newUsername,
            password = newPassword,
            photoDirectory = newPhotoDirectory,
        )

        assertThat(sut.hostname, equalTo(newHostname))
        assertThat(sut.username, equalTo(newUsername))
        assertThat(sut.password, equalTo(newPassword))
        assertThat(sut.photoDirectory, equalTo(newPhotoDirectory))
    }

    @Test
    fun updatesPersistAcrossInstances() = runTest(testDispatcher) {
        sut.save(
            hostname = "boop.com",
            username = "boop",
            password = "secretboop",
            photoDirectory = "anotherPhotoDirectory"
        )

        createSUT()

        assertThat(sut.hostname, equalTo("boop.com"))
        assertThat(sut.username, equalTo("boop"))
        assertThat(sut.password, equalTo("secretboop"))
        assertThat(sut.photoDirectory, equalTo("anotherPhotoDirectory"))
    }

    @Test
    fun observeUpdate() = runTest(testDispatcher) {
        var counter = 0
        val countDownLatch = CountDownLatch(1)
        //val suspending: Int = suspendCoroutine<Int> { it: Continuation<Int> ->
            sut.onChange {
                counter++
                countDownLatch.countDown()
          //      it.resume(counter+1)
            }
        //}

        sut.save(
            hostname = "update.boop.com",
            username = "booped",
            password = "stolensecretboop",
        )

        countDownLatch.await(1, TimeUnit.SECONDS)


        assertThat(counter, equalTo(1))
        //assertThat(suspending, equalTo(1))
    }

    @After
    fun delete() {
        getInstrumentation().context.deleteSharedPreferences(basename)
    }
}

