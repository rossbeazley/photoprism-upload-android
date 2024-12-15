package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry
sealed interface Event
data class NewEvent(val event : UploadQueueEntry) : Event
data class FullState(val events : List<UploadQueueEntry>) : Event
