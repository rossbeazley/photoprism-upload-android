package ulk.co.rossbeazley.photoprism.upload.syncqueue

sealed class    UploadQueueEntry {
    abstract val attemptCount: Int
    abstract val filePath: String
    abstract fun willAttemptUpload(): RunningFileUpload
}

data class ScheduledFileUpload(override val filePath: String) :
    UploadQueueEntry() {

    override val attemptCount: Int
        get() = 0

    override fun willAttemptUpload() = RunningFileUpload(filePath,1)
}

data class RunningFileUpload(override val filePath: String, override val attemptCount:Int = 0) :
    UploadQueueEntry() {
    override fun willAttemptUpload() = copy(attemptCount = attemptCount + 1)
    fun retryLater(): UploadQueueEntry {
        return RetryFileUpload(filePath, attemptCount)
    }

    fun failed(): UploadQueueEntry {
        return FailedFileUpload(filePath)
    }
}

data class RetryFileUpload(override val filePath: String, override val attemptCount:Int = 0) :
    UploadQueueEntry() {
    override fun willAttemptUpload() = RunningFileUpload(filePath, attemptCount = attemptCount + 1)
}

data class FailedFileUpload(override val filePath: String) :
    UploadQueueEntry() {
    override val attemptCount: Int
        get() = 0

    override fun willAttemptUpload() = RunningFileUpload(filePath, attemptCount = 1)
}


data class CompletedFileUpload(override val filePath: String) :
    UploadQueueEntry() {

    override val attemptCount: Int
        get() = 0

    override fun willAttemptUpload() = RunningFileUpload(filePath, attemptCount = attemptCount + 1)
}