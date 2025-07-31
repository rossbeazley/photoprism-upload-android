package ulk.co.rossbeazley.photoprism.upload.config

interface ReadonlyConfigRepository {
    val photoDirectory: String
    val maxUploadAttempts: Int
    val username: String
    val hostname: String
    val password: String
    fun onChange(function: () -> Unit)
    fun isConfigured(): Boolean

    val developerMode: Boolean
    val useMobileData: Boolean
}