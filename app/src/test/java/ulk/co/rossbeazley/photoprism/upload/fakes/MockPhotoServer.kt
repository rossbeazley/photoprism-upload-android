package ulk.co.rossbeazley.photoprism.upload.fakes

import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MockPhotoServer(var autoComplete: Boolean = false) : PhotoServer {
    var path: String = "NO UPLOAD"
    var capturedContinuation: Continuation<Result<Unit>>? = null

    override suspend fun upload(path: String): Result<Unit> {
        this.path = path
        return suspendCoroutine { continuation: Continuation<Result<Unit>> ->
            capturedContinuation = continuation

            if (autoComplete) {
                complete(with = Result.success(Unit))
            }
        }
    }

    override suspend fun checkConnection(): Result<Unit> {
        TODO("Not yet implemented")
    }

    private fun complete(with: Result<Unit>) {
        capturedContinuation?.resume(with)
        capturedContinuation = null
    }

    fun currentUploadCompletes() = complete(Result.success(Unit))
    fun currentUploadFails() = complete(Result.failure(Exception()))
}