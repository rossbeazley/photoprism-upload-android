package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.startup.AppInitializer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BackgroundJobSystemIntegrationTest {

    lateinit var backgroundJobSystem: WorkManagerBackgroundJobSystem

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        val appInitialiser = AppInitializer.getInstance(context)
        backgroundJobSystem = appInitialiser.initializeComponent(WorkManagerBackgroundJobSystemInitialiser::class.java)
        val config : Configuration = appInitialiser.initializeComponent(WorkManagerConfigInitialiser::class.java)

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun aRunningJobSucceeds() = runTest {
        val pathToDownload = "the-file-path"

        var jobPath = ""
        val callback: suspend (String) -> JobResult = {
            jobPath = it
            JobResult.Success
        }
        backgroundJobSystem.register(callback)

        val tag = backgroundJobSystem.schedule(pathToDownload)

        val workManager = WorkManager.getInstance(context)

        workManager.getWorkInfosByTagLiveData(tag)
            .asFlow()
            .first { it.filter { it.state == WorkInfo.State.SUCCEEDED }.isNotEmpty() }

        assertThat(jobPath, equalTo(pathToDownload))
    }

    @Test
    fun aRunningJobIsAskedToRetry() = runTest {
        val pathToDownload = "the-file-path"

        var jobPath = ""
        val callback: suspend (String) -> JobResult = {
            if (jobPath == it) {
                JobResult.Success
            } else {
                jobPath = it
                JobResult.Retry
            }
        }
        backgroundJobSystem.register(callback)

        val tag = backgroundJobSystem.schedule(pathToDownload)

        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(tag)
            .asFlow()
            .first { it.filter { it.state == WorkInfo.State.ENQUEUED }.isNotEmpty() }

        assertThat(jobPath, equalTo(pathToDownload))
    }

    @Test
    fun aRunningJobFails() = runTest {
        val pathToDownload = "the-file-path"

        var jobPath = ""
        val callback: suspend (String) -> JobResult = {
            jobPath = it
            JobResult.Failure
        }
        backgroundJobSystem.register(callback)

        val tag = backgroundJobSystem.schedule(pathToDownload)

        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(tag)
            .asFlow()
            .first { it.filter { it.state == WorkInfo.State.FAILED }.isNotEmpty() }

        assertThat(jobPath, equalTo(pathToDownload))
    }

}