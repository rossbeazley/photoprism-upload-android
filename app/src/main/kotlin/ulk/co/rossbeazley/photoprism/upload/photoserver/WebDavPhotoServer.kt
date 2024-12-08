package ulk.co.rossbeazley.photoprism.upload

import android.content.ContentResolver
import android.net.Uri
import at.bitfire.dav4jvm.DavCollection
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.BufferedSink
import okio.source
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import java.io.File
import java.io.InputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebDavPhotoServer(
    val user: String,
    val host: String,
    val httpClient: OkHttpClient,
    val contentResolver: ContentResolver
) : PhotoServer {

    override suspend fun upload(path: String): Result<Unit> {
        log("Prep upload $path")
        return when {
            path.startsWith("content:") -> uploadContentUri(path)
            else -> uploadFilePath(path)
        }
    }

    suspend fun uploadContentUri(path: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            val contentUri = Uri.parse(path)
            val toMediaTypeOrNull = contentResolver.getType(contentUri)?.toMediaTypeOrNull()
            val body = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return toMediaTypeOrNull
                }
                override fun writeTo(sink: BufferedSink) {
                    contentResolver.openInputStream(contentUri)?.use { stream ->
                        sink.writeAll(stream.source())
                    }
                }
            }

            val fileExtension = when (toMediaTypeOrNull.toString()) {
                "video/mp4" -> ".mp4"
                else -> ".png"
            }
            val fileName = path.substringAfterLast("/") + fileExtension
            uploadRequestBody(fileName,body,continuation)
        }
    }


    suspend fun uploadFilePath(path: String): Result<Unit> {
        return suspendCoroutine { continuation ->
            val file = File(path)
            val fileName = file.toPath().fileName.toString()
            val body = file.asRequestBody()
            uploadRequestBody(fileName, body, continuation)
        }
    }

    private fun uploadRequestBody(
        fileName: String,
        body: RequestBody,
        continuation: Continuation<Result<Unit>>
    ) {
        try {
            val davResource = DavCollection(
                httpClient,
                "https://$user@$host/originals/$fileName".toHttpUrl(),
            )
            log("About to put")
            davResource.put(body = body, ifNoneMatch = true) {
                log("dav respone $it")
                continuation.resume(Result.success(Unit))
            }
        } catch (e: Exception) {
            log("exception $e")
            continuation.resume(Result.failure(e))
        }
    }
}