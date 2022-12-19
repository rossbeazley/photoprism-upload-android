package ulk.co.rossbeazley.photoprism.upload

sealed class UploadQueueEntry {
    abstract val attemptCount: Any
    abstract val filePath: String
    abstract fun willAttemptUpload(): UploadQueueEntry
}

data class ScheduledFileUpload(override val filePath: String, override val attemptCount:Int = 0) :
    UploadQueueEntry() {
    override fun willAttemptUpload() = RunningFileUpload(filePath,1)
}

data class RunningFileUpload(override val filePath: String, override val attemptCount:Int = 0) :
    UploadQueueEntry() {
    override fun willAttemptUpload() = copy(attemptCount = attemptCount + 1)
}