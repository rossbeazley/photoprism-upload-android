package ulk.co.rossbeazley.photoprism.upload.photoserver

import android.content.ContentResolver
import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.WebDavPhotoServer

fun buildPhotoServer(contentResolver: ContentResolver): WebDavPhotoServer {
    val host = BuildConfig.webdavHostName
    val user = BuildConfig.webdavUsserName
    val webDavPhotoServer = WebDavPhotoServer(user, host, buildHttpClientForPhotoServer(), contentResolver)
    return webDavPhotoServer
}