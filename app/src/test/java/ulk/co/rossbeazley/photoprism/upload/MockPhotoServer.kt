package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

class MockPhotoServer : PhotoServer {
    var path: String = "NO UPLOAD"

    override fun doUpload(path: String): Result<Unit> {
        this.path = path
        return Result.success(Unit)
    }
}