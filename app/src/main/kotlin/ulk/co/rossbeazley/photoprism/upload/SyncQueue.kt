package ulk.co.rossbeazley.photoprism.upload

interface SyncQueue {
    fun put(queueEntry : UploadQueueEntry)
    fun remove(queueEntry: UploadQueueEntry)
    fun peek(id: String): UploadQueueEntry
    fun all(): List<UploadQueueEntry>
    fun removeAll()
}