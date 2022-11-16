package ulk.co.rossbeazley.photoprism.upload

class UploadQueue {
    var capturedQueueEntry : ScheduledFileUpload? = null
    fun enququq(queueEntry : ScheduledFileUpload) {
        capturedQueueEntry = queueEntry
    }
}