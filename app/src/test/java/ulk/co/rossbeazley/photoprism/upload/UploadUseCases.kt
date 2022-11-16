package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Test

class UploadUseCases {

    class Filesystem {
        val flow = MutableSharedFlow<String>()
        var watchedPath = "NONE"
        fun watch(path: String): Flow<String> {
            watchedPath = path
            return flow
        }
    }

    @Test // TODO missing config test
    fun photoDirectoryIsObserved() {
        val fileSystem = Filesystem()

        // given the configuration exists
        val config = mapOf<String, String>("directory" to "any-directory-path")
        // when the app is started
        val auditLogService = CapturingAuditLogService()
        val jobSystem = CapturingBackgroundJobSystem()
        val uploadQueue = UploadQueue()
        val application = PhotoPrismApp(config, fileSystem, jobSystem, auditLogService, uploadQueue)

        // then the directory is observered
        assertThat(fileSystem.watchedPath, equalTo("any-directory-path"))

        // and an audit log entry is created
        val capturedAuditLog = auditLogService.capturedAuditLog
        assertThat(capturedAuditLog!!, isA<ApplicationCreatedAuditLog>())
    }

    @Test
    fun photoUploadScheduled() {
        // todo this will be extracted to setup
        val context = UnconfinedTestDispatcher()
        runTest(context) {

            val auditLogService = CapturingAuditLogService()
            val jobSystem = CapturingBackgroundJobSystem()
            val uploadQueue = UploadQueue()

            //given the photo directory is being watched
            val fileSystem = Filesystem()
            val application = PhotoPrismApp(
                mapOf<String, String>("directory" to "any-directory-path"),
                fileSystem,
                jobSystem,
                auditLogService,
                uploadQueue,
                context
                )
            // when a photo is found
            val expectedFilePath = "any-file-path-at-all"
            fileSystem.flow.emit(expectedFilePath)

            // then the upload job is scheduled
            val jobFilePath = jobSystem.jobFilePath
            assertThat(jobFilePath, equalTo(expectedFilePath))

            // and an audit log is created
            val expectedAuditLog = ScheduledAuditLog(expectedFilePath)
            val capturedAuditLog = auditLogService.capturedAuditLog
            assertThat(capturedAuditLog, equalTo(expectedAuditLog))

            // and a queue entry is created as scheduled
            val expectedQueueEntry = ScheduledFileUpload(expectedFilePath)
            val capturedQueueEntry = uploadQueue.capturedQueueEntry
            assertThat(capturedQueueEntry, equalTo(expectedQueueEntry))
        }
    }

    @Test @Ignore("todo")
    fun photoUploadStarted() {
        //given a download is scheduled
        // when the system is ready to run our job
        // then the download is started
        // and an audit log is created
        // and the queue entry is updated to started
    }

    @Test @Ignore("todo")
    fun photoUploadCompletes() {
        //given a photo is being uploaded
        // when the upload completes
        // then the queue entry is removed
        // and an audit log is created
        // and the job is marked as complete
    }

    @Test @Ignore("todo")
    fun photoUploadIsRetried() {
        //given a photo is being uploaded
        // when the upload fails
        // then the queue entry is set to retry
        // and an audit log is created
        // and the job is marked as retry
    }

    @Test @Ignore("todo")
    fun photoUploadFails() {
        //given a photo is being retried
        // when the upload fails
        // then the queue entry is removed
        // and an audit log is created
        // and the job is marked as failed
        // and a fail queue entry is created
    }
}
