package ulk.co.rossbeazley.photoprism.upload

class FakeSyncQueue : SyncQueue {
    var capturedQueueEntry : UploadQueueEntry? = null

    override fun put(queueEntry : UploadQueueEntry) {
        capturedQueueEntry = queueEntry
        map[queueEntry.filePath] = queueEntry
    }

    var removedQueueEntry : UploadQueueEntry? = null
    override fun remove(queueEntry: UploadQueueEntry) {
        removedQueueEntry = queueEntry
        map.remove(queueEntry.filePath)
    }

    val map : MutableMap<String, UploadQueueEntry> = mutableMapOf()

    override fun peek(id: String): UploadQueueEntry {
        return map[id]!!
    }

    override fun all(): List<UploadQueueEntry> {
        return map.values.toList()
    }
}