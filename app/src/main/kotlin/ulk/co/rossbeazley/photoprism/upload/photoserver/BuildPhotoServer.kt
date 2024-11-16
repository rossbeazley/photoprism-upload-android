package ulk.co.rossbeazley.photoprism.upload.photoserver

import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.WebDavPhotoServer

fun buildPhotoServer(): WebDavPhotoServer {
    val host = BuildConfig.webdavHostName
    val user = BuildConfig.webdavUsserName
    val webDavPhotoServer = WebDavPhotoServer(user, host, buildHttpClientForPhotoServer())
    return webDavPhotoServer
}