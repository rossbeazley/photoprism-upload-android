package ulk.co.rossbeazley.photoprism.upload.syncqueue

interface LastUploadRepository {
    fun remember(filePath: String)
    fun recall(): String
}