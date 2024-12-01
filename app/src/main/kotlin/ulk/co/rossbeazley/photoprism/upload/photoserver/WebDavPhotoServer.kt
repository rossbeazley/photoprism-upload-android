package ulk.co.rossbeazley.photoprism.upload

import at.bitfire.dav4jvm.DavCollection
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import java.io.File
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebDavPhotoServer(
    val user: String,
    val host: String,
    val httpClient: OkHttpClient
) : PhotoServer {

    override suspend fun upload(path: String): Result<Unit> {
        log("Prep upload $path")
        return suspendCoroutine { continuation ->
            try {
                val file = File(path)
                val fileName = file.toPath().fileName
                val davResource = DavCollection(
                    httpClient,
                    "https://$user@$host/originals/$fileName".toHttpUrl(),

                )
                val body = file.asRequestBody()
                log("About to put")
                davResource.put(body = body, ifNoneMatch = true) {
                    log("dav respone $it")
                    continuation.resume( Result.success(Unit) )
                }
            } catch (e: Exception) {
                log("exception $e")
                continuation.resume( Result.failure(e) )
            }

        }
    }
}