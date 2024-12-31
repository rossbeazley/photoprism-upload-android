package ulk.co.rossbeazley.photoprism.upload.photoserver

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ulk.co.rossbeazley.photoprism.upload.config.ReadonlyConfigRepository
import ulk.co.rossbeazley.photoprism.upload.log

fun build(config: ReadonlyConfigRepository): OkHttpClient {
    val httpLoggingInterceptor = HttpLoggingInterceptor {
        log("HTTP::LOG::$it")
    }
    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    val authenticator = BasicDigestAuthHandler(
        domain = null,
        username = config.username,
        password = config.password
    )
    return OkHttpClient.Builder()
        .followRedirects(false)
        .addInterceptor(authenticator)
        .addInterceptor(httpLoggingInterceptor)
        .authenticator(authenticator)
        .build()
}

class PhotoServerHttpClientFactory(config: ReadonlyConfigRepository) {
    val okHttpClient : OkHttpClient
        get() = instance

    private var instance : OkHttpClient = build(config)
    init {
        config.onChange {
            instance = build(config)
        }
    }
}