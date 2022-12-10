package ulk.co.rossbeazley.photoprism.upload

class UploadQueue {
    var capturedQueueEntry : ScheduledFileUpload? = null

    fun enqueue(queueEntry : ScheduledFileUpload) {
        capturedQueueEntry = queueEntry
        map[queueEntry.filePath] = queueEntry
    }

    val map : MutableMap<String, ScheduledFileUpload> = mutableMapOf()

    fun peek(id: String): ScheduledFileUpload {
        return map[id]!!
    }
}