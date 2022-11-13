package ulk.co.rossbeazley.photoprism.upload

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class UploadUseCases {

    @Test
    fun photoDirectoryIsObserved() {
        // given the configuration exists
        // when the app is started
        // then the directory is observered
        // and an audit log entry is created
    }

    @Test
    fun photoUploadScheduled() {
        //given the photo directory is being watched
        class PhotoPrismApp {
            fun observedPhoto(expectedFilePath: String) {
                TODO("Not yet implemented")
            }
        }
        val application = PhotoPrismApp()
        // when a photo is found
        val expectedFilePath = "any-file-path-at-all"
        application.observedPhoto(expectedFilePath)

        // then the upload job is scheduled
        class CapturingBackgroundJobSystem {
            var jobFilePath : String? = null
            fun schedule(forPath: String) {
                jobFilePath = forPath
            }
        }
        val jobSystem = CapturingBackgroundJobSystem()
        val jobFilePath = jobSystem.jobFilePath
        assertThat(jobFilePath, equalTo(expectedFilePath))

        // and an audit log is created
        data class ScheduledAuditLog(val filePath: String)
        val expectedAuditLog = ScheduledAuditLog(expectedFilePath)
        class CapturingAuditLogService {
            var capturedAuditLog : ScheduledAuditLog? = null
            fun log(log : ScheduledAuditLog) {
                capturedAuditLog = log
            }
        }
        val auditLogService = CapturingAuditLogService()
        val capturedAuditLog = auditLogService.capturedAuditLog
        assertThat(capturedAuditLog, equalTo(expectedAuditLog))

        // and a queue entry is created as scheduled
        data class ScheduledFileUpload(val filePath: String)
        class UploadQueue {
            var capturedQueueEntry : ScheduledFileUpload? = null
            fun enququq(queueEntry : ScheduledFileUpload) {
                capturedQueueEntry = queueEntry
            }
        }
        val uploadQueue = UploadQueue()
        val expectedQueueEntry = ScheduledFileUpload(expectedFilePath)
        val capturedQueueEntry = uploadQueue.capturedQueueEntry
        assertThat(capturedQueueEntry, equalTo(expectedQueueEntry))
    }

    @Test
    fun photoUploadStarted() {
        //given a download is scheduled
        // when the system is ready to run our job
        // then the download is started
        // and an audit log is created
        // and the queue entry is updated to started
    }

    @Test
    fun photoUploadCompletes() {
        //given a photo is being uploaded
        // when the upload completes
        // then the queue entry is removed
        // and an audit log is created
        // and the job is marked as complete
    }

    @Test
    fun photoUploadIsRetried() {
        //given a photo is being uploaded
        // when the upload fails
        // then the queue entry is set to retry
        // and an audit log is created
        // and the job is marked as retry
    }

    @Test
    fun photoUploadFails() {
        //given a photo is being retried
        // when the upload fails
        // then the queue entry is removed
        // and an audit log is created
        // and the job is marked as failed
        // and a fail queue entry is created
    }
}
