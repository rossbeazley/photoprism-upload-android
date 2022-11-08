package ulk.co.rossbeazley.photoprism.upload

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ulk.co.rossbeazley.photoprism.upload.ui.main.MainViewModel

class MainViewModelInstrumentationTest {

    @get:Rule
    val rule = GrantPermissionRule.grant("android.permission.INTERNET")

    lateinit var mainViewModel: MainViewModel


    @Before
    fun  boo() {
        mainViewModel = MainViewModel()

    }

    @Test
    fun testUpload() {

        InstrumentationRegistry.getInstrumentation().runOnMainSync {

            val inputStream =
                InstrumentationRegistry.getInstrumentation().context.resources.assets.open("groovy.png")

            mainViewModel.upload(inputStream)

            Thread.sleep(10_000)

            inputStream.close()

        }
    }

    @Test
    fun list() {

        InstrumentationRegistry.getInstrumentation().runOnMainSync {

            mainViewModel.list()

            Thread.sleep(10_000)

        }
    }

    @Test
    fun all() {
        list()
        testUpload()
        list()
    }
}