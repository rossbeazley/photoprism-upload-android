package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import android.util.Log
import kotlinx.coroutines.test.runTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlinx.coroutines.future.asCompletableFuture

class BackgroundJobSystemIntegrationTest {

    lateinit var backgroundJobSystem: WorkManagerBackgroundJobSystem

    class WorkManagerBackgroundJobSystem(val context: Context) : BackgroundJobSystem,
        WorkerFactory() {

        private lateinit var cb: suspend (String) -> JobResult

        override fun schedule(forPath: String) {
            val request = OneTimeWorkRequestBuilder<JobSystemWorker>()
                .setInputData(workDataOf("A" to forPath))
                .addTag(forPath)
                .build()

            val instance = WorkManager.getInstance(context)
            instance.enqueue(request)
        }

        class JobSystemWorker(
            private val cb: suspend (String) -> JobResult,
            context: Context,
            private val parameters: WorkerParameters
        ) : Worker(context, parameters) {

            override fun doWork(): Result {
                val path = parameters.inputData.getString("A") ?: ""

                val job: Deferred<JobResult> = GlobalScope.async {
                    val deferred = async { cb(path) }
                    deferred.await()
                }
                val r: JobResult = job.asCompletableFuture().get()
                return when (r) {
                    else -> Result.success()
                }
            }
        }

        override fun register(callback: suspend (String) -> JobResult) {
            this.cb = callback
        }

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return JobSystemWorker(cb, appContext, workerParameters)
        }

    }

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        backgroundJobSystem = WorkManagerBackgroundJobSystem(context)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(backgroundJobSystem)  //TOSO work this out!
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun aRunningJobSucceeds() = runTest {
        var jobPath = ""
        val callback: suspend (String) -> JobResult = {
            jobPath = it
            JobResult.Success
        }
        backgroundJobSystem.register(callback)

        backgroundJobSystem.schedule("the-file-path")

        val workManager = WorkManager.getInstance(context)

        delay(3000)

        // TODO - get live data and await SUCCEEDED
        workManager.getWorkInfosByTagLiveData("the-file-path")

        //assertThat(workInfo.state, equalTo(WorkInfo.State.SUCCEEDED))
        assertThat(jobPath, equalTo("the-file-path"))
    }

    @Test
    fun aRunningJobIsAskedToRetry() {
    }

    @Test
    fun aRunningJobFails() {
    }
}