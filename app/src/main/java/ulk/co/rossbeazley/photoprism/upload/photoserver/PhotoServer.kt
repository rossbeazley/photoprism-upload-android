package ulk.co.rossbeazley.photoprism.upload.photoserver

interface PhotoServer {
        fun doUpload(
        path: String
    ): Result<Unit>

    suspend fun upload(
        path: String
    ): Result<Unit>
}