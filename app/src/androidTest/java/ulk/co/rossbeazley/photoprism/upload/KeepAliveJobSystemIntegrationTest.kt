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
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerConfigInitialiser
import java.util.*
import java.util.concurrent.TimeUnit

class KeepAliveJobSystemIntegrationTest {

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        val appInitialiser = AppInitializer.getInstance(context)

        val config : Configuration = appInitialiser.initializeComponent(WorkManagerConfigInitialiser::class.java)

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun aPeriodicJobIsEnqueued() = runTest {

        val workManager = WorkManager.getInstance(context)

        startKeepAlive(workManager)

        val workInfo: WorkInfo = workManager.getWorkInfosByTagLiveData("keepalive")
            .asFlow()
            .first { it.filter { it.state == WorkInfo.State.ENQUEUED }.isNotEmpty() }
            .first()

        assertThat(workInfo.runAttemptCount, equalTo(0))
    }


    @Test
    fun aPeriodicJobIsRun() = runTest {

        val workManager = WorkManager.getInstance(context)

        val uuid = startKeepAlive(workManager)

        WorkManagerTestInitHelper.getTestDriver(context)?.let {
            it.setPeriodDelayMet(uuid)
            it.setInitialDelayMet(uuid)
            it.setAllConstraintsMet(uuid)
        }

        val workInfo: WorkInfo = workManager.getWorkInfosByTagLiveData("keepalive")
            .asFlow()
            .first { it.filter { it.state == WorkInfo.State.ENQUEUED }.isNotEmpty() }
            .first()

        assertThat(workInfo.runAttemptCount, equalTo(1))
    }


    private fun startKeepAlive(workManager: WorkManager): UUID {
        val keepalive = PeriodicWorkRequestBuilder<KeepaliveTask>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .addTag("keepalive")
            .build()
        //workManager.cancelAllWork()
        workManager.enqueueUniquePeriodicWork(
            "keepalive",
            ExistingPeriodicWorkPolicy.REPLACE,
            keepalive
        )

        return keepalive.id
    }
}