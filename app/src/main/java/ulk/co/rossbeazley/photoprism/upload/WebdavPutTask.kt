package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

class WebdavPutTask(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    val httpClient: OkHttpClient

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor {
            log(it)
        }
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        httpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(httpLoggingInterceptor)
            .authenticator(
                BasicDigestAuthHandler(
                    domain = null,
                    username = "",
                    password = ""
                )
            )
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    override fun doWork(): Result {

        log("Do some work")

        val path = inputData.getString("IMAGE_PATH") ?: return Result.failure()

        log("File path is $path")

        val host = ""
        val user = ""

        val davResource = DavCollection(
            httpClient,
            "https://$user@$host/originals/groovy-${System.currentTimeMillis()}.png".toHttpUrl()
        )
        val body = File(path).asRequestBody()
        try {
            davResource.put(body = body, ifNoneMatch = true) {
                log("dav respone $it")
            }
            return Result.success()
        }catch (e : Exception) {
            log("exception $e")
            return Result.retry()
        }
    }

}