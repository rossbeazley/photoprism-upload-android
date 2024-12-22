package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry
sealed interface Event
data class PartialSyncState(val event : UploadQueueEntry) : Event
data class FullSyncState(val events : List<UploadQueueEntry>) : Event
