package ulk.co.rossbeazley.photoprism.upload

data class ScheduledFileUpload(val filePath: String, val attemptCount:Int = 0) {
    fun willAttemptUpload() = copy(attemptCount = attemptCount + 1)
}