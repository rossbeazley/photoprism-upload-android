package ulk.co.rossbeazley.photoprism.upload.photoserver

import at.bitfire.dav4jvm.BasicDigestAuthHandler
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.log

fun buildHttpClientForPhotoServer(): OkHttpClient {
    val httpLoggingInterceptor = HttpLoggingInterceptor {
        log("HTTP::LOG::$it")
    }
    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    val authenticator = BasicDigestAuthHandler(
        domain = null,
        username = BuildConfig.authUserName,
        password = BuildConfig.authPassword
    )
    return OkHttpClient.Builder()
        .followRedirects(false)
        .addInterceptor(authenticator)
        .addInterceptor(httpLoggingInterceptor)
        .authenticator(authenticator)
        .build()
}