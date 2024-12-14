package ulk.co.rossbeazley.photoprism.upload.fakes

import ulk.co.rossbeazley.photoprism.upload.syncqueue.SyncQueue
import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry

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

    override fun removeAll() {
        TODO("Not yet implemented")
    }
}