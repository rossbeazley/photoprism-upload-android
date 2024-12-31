package ulk.co.rossbeazley.photoprism.upload.photoserver

import android.content.ContentResolver
import ulk.co.rossbeazley.photoprism.upload.WebDavPhotoServer
import ulk.co.rossbeazley.photoprism.upload.config.ReadonlyConfigRepository

fun buildPhotoServer(contentResolver: ContentResolver, config: ReadonlyConfigRepository): WebDavPhotoServer {
    val clientFactory = PhotoServerHttpClientFactory(config)
    val webDavPhotoServer = WebDavPhotoServer(config::username, config::hostname, clientFactory::okHttpClient, contentResolver)
    return webDavPhotoServer
}