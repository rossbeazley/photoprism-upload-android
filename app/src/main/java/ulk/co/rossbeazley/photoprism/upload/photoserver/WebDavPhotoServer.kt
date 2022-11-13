package ulk.co.rossbeazley.photoprism.upload

import at.bitfire.dav4jvm.DavCollection
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import java.io.File

class WebDavPhotoServer(
    val user: String,
    val host: String,
    val httpClient: OkHttpClient
) : PhotoServer {

    override fun doUpload(
        path: String
    ): Result<Unit> {
        try {
            val davResource = DavCollection(
                httpClient,
                "https://$user@$host/originals/groovy-${System.currentTimeMillis()}.png".toHttpUrl()
            )
            val body = File(path).asRequestBody()
            davResource.put(body = body, ifNoneMatch = true) {
                log("dav respone $it")
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            log("exception $e")
            return Result.failure(e)
        }
    }
}