package ulk.co.rossbeazley.photoprism.upload

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.DavCollection.Companion.SYNC_COLLECTION
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import java.io.File
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebDavPhotoServer(
    val user: ()->String,
    val host: ()->String,
    val httpClientFactory: ()->OkHttpClient,
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

            val mimeTypeMap = MimeTypeMap.getSingleton()
            val fileExtension =
                mimeTypeMap.getExtensionFromMimeType(toMediaTypeOrNull.toString()) ?: "image/jpeg"

            val fileName = path.substringAfterLast("/") + "." + fileExtension
            uploadRequestBody(fileName, body, continuation)
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
            val destinationFolder = "originals"   // TODO make this configurable
            val davResource = DavCollection(
                httpClientFactory(),
                "https://${user()}@${host()}/$destinationFolder/$fileName".toHttpUrl(),
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

    override suspend fun checkConnection(): Result<Unit> {
        //return pingApi()

        return suspendCoroutine { continuation ->
            try {
                val destinationFolder = "originals"   // TODO make this configurable
                val davResource = DavCollection(
                    httpClientFactory(),
                    "https://${user()}@${host()}/$destinationFolder".toHttpUrl(),
                )
                log("About to propfind")
                davResource.propfind(1,SYNC_COLLECTION) { it: at.bitfire.dav4jvm.Response, two: at.bitfire.dav4jvm.Response.HrefRelation ->
                    log("dav respone $it")
                    continuation.resume(Result.success(Unit))
                }
            } catch (e: Exception) {
                log("exception $e")
                continuation.resume(Result.failure(e))
            }
        }
    }

    private suspend fun pingApi(): Result<Unit> = suspendCoroutine { continuation ->
        try {
            log("About to getUserInfo")
            val call = httpClientFactory().newCall(
                Request.Builder() //${user()}@
                    .url("https://${host()}/api/v1/oauth/authorize")
                    .get()
                    .header("accept", "application/json")
                    .build()
            )

            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        log("exception $e")
                        continuation.resume(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        log("dav respone $response")
                        continuation.resume(Result.success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            log("exception $e")
            continuation.resume(Result.failure(e))
        }
    }
}