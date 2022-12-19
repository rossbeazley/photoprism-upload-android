package ulk.co.rossbeazley.photoprism.upload

class UploadQueue {
    var capturedQueueEntry : UploadQueueEntry? = null

    fun put(queueEntry : UploadQueueEntry) {
        capturedQueueEntry = queueEntry
        map[queueEntry.filePath] = queueEntry
    }

    var removedQueueEntry : UploadQueueEntry? = null
    fun remove(queueEntry: UploadQueueEntry) {
        removedQueueEntry = queueEntry
    }

    val map : MutableMap<String, UploadQueueEntry> = mutableMapOf()

    fun peek(id: String): UploadQueueEntry {
        return map[id]!!
    }
}