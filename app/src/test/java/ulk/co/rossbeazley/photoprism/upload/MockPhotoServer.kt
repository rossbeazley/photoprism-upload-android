package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class MockPhotoServer : PhotoServer {
    var path: String = "NO UPLOAD"

    override fun doUpload(path: String): Result<Unit> {
        this.path = path
        return Result.success(Unit)
    }

    var capturedContinuation : Continuation<Result<Unit>>? = null

    override suspend fun upload(path: String): Result<Unit> {
        this.path = path
        return suspendCoroutine { continuation: Continuation<Result<Unit>> ->
            capturedContinuation = continuation
        }
    }
}