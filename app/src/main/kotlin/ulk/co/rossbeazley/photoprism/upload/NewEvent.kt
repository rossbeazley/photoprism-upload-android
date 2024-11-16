package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry

data class NewEvent(val event : UploadQueueEntry)