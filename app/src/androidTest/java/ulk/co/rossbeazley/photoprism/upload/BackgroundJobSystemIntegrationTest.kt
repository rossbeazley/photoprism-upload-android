package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import kotlinx.coroutines.test.runTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.future.asCompletableFuture
import java.util.*

class BackgroundJobSystemIntegrationTest {

    lateinit var backgroundJobSystem: WorkManagerBackgroundJobSystem

    class WorkManagerBackgroundJobSystem(val context: Context) : BackgroundJobSystem,
        WorkerFactory() {

        private lateinit var cb: suspend (String) -> JobResult

        override fun schedule(forPath: String): UUID {
            val request = OneTimeWorkRequestBuilder<JobSystemWorker>()
                .setInputData(workDataOf("A" to forPath))
                .addTag(forPath)
                .build()

            val instance = WorkManager.getInstance(context)
            instance.enqueue(request)
            return request.id
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
                    JobResult.Retry -> Result.retry()
                    JobResult.Failure -> Result.failure()
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
            .setWorkerFactory(backgroundJobSystem)  //TODO work this circular ref out!
            .build()

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

        backgroundJobSystem.schedule(pathToDownload)

        val workManager = WorkManager.getInstance(context)

        //delay(3000)
        workManager.getWorkInfosByTagLiveData(pathToDownload) // todo broken encapsulation
            .asFlow()
            .filter { it.filter { it.state == WorkInfo.State.SUCCEEDED }.isNotEmpty() }
            //.map { it.filter { it.state == WorkInfo.State.SUCCEEDED }.first() }
            .first()

        //assertThat(workInfo.state, equalTo(WorkInfo.State.SUCCEEDED))
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

        val uuid = backgroundJobSystem.schedule(pathToDownload)

        val workManager = WorkManager.getInstance(context)

//        WorkManagerTestInitHelper.getTestDriver(context)?.let {
//            it.setInitialDelayMet(uuid)
//            it.setAllConstraintsMet(uuid)
//            //it.setPeriodDelayMet(uuid)
//        }

        //delay(3000)
        workManager.getWorkInfosByTagLiveData(pathToDownload) // todo broken encapsulation
            .asFlow()
            .filter { it.filter { it.state == WorkInfo.State.ENQUEUED }.isNotEmpty() }
            .first()

        //assertThat(workInfo.state, equalTo(WorkInfo.State.SUCCEEDED))
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

        backgroundJobSystem.schedule(pathToDownload)

        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfosByTagLiveData(pathToDownload) // todo broken encapsulation
            .asFlow()
            .filter { it.filter { it.state == WorkInfo.State.FAILED }.isNotEmpty() }
            .first()

        //assertThat(workInfo.state, equalTo(WorkInfo.State.SUCCEEDED))
        assertThat(jobPath, equalTo(pathToDownload))
    }

}