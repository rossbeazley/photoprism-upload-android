package ulk.co.rossbeazley.photoprism.upload.ui.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    @Before
    fun starting() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun finished() {
        Dispatchers.resetMain()
    }


    @Test
    fun test() {
        val mainViewModel = MainViewModel()
        mainViewModel.list()
        Thread.sleep(3000)

        mainViewModel.files.value

        Thread.sleep(3_000)
        mainViewModel.list()

    }
}