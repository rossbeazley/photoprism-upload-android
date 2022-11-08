package ulk.co.rossbeazley.photoprism.upload.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.dav4jvm.*
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.DisplayName
import com.github.sardine.impl.SardineImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.Okio
import okio.Source
import okio.source
import java.io.InputStream


class MainViewModel : ViewModel() {
    fun upload(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                //SardineImpl().put("https://pydio.rossbeazley.co.uk/dav/personal-files/webdavdev/groovy-${System.currentTimeMillis()}.png", readBytes)
                //val dr = DavResource
                val davResource = DavCollection(
                    httpClient,
//                    "https://pydio.rossbeazley.co.uk/dav/personal-files/webdavdev/groovy-${System.currentTimeMillis()}.png".toHttpUrl()
                    "https://admin@photo.rossbeazley.co.uk/originals/groovy-${System.currentTimeMillis()}.png".toHttpUrl()
                )
                val readBytes = inputStream.readBytes()
                val body = readBytes.toRequestBody("image/png".toMediaType())
                println("PUT")
                davResource.put(body = body, ifNoneMatch = true) {
                    println(it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val files: MutableLiveData<String> = MutableLiveData()
    val httpClient: OkHttpClient

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor {
            println(it)
        }
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        httpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(httpLoggingInterceptor)
                .authenticator(BasicDigestAuthHandler(
                    domain = null,
                    username = "rdlb",
//                    password = "03april2012pydio"
                    password = "03april2012photoprism"
                ))
            .protocols( listOf(Protocol.HTTP_1_1))
            .build()
    }

    fun list() {
        viewModelScope.launch(Dispatchers.IO) {
            davrList(this)
        }
    }

    private fun davrList(coroutineScope: CoroutineScope) {
        val davCollection = DavCollection(
            httpClient,
            "https://admin@photo.rossbeazley.co.uk/originals/".toHttpUrl()
        )

        davCollection.propfind(1, DisplayName.NAME) { a: Response, b: Response.HrefRelation ->
            println(a)
        }

    }
}