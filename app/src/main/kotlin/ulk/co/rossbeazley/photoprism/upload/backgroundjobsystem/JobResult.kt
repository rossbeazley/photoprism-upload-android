package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

sealed class JobResult {
    object Success : JobResult()
    object Failure : JobResult()
    object Retry : JobResult()
}