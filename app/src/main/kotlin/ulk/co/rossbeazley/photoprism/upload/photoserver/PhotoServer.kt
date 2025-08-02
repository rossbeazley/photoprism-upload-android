package ulk.co.rossbeazley.photoprism.upload.photoserver

interface PhotoServer {
    suspend fun upload(
        path: String
    ): Result<Unit>

    suspend fun checkConnection(): Result<Unit>
}