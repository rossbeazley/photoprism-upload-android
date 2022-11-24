package ulk.co.rossbeazley.photoprism.upload

sealed class JobResult {
    object Success : JobResult()
    object Failure : JobResult()
    object Retry : JobResult()
}