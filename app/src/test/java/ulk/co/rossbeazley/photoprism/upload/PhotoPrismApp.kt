package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

class PhotoPrismApp(
    config: Map<String, String>,
    val fileSystem: Filesystem,
    private val jobSystem: CapturingBackgroundJobSystem,
    val auditLogService: CapturingAuditLogService,
    val uploadQueue: UploadQueue,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val photoServer: PhotoServer
) {

    private val scope = CoroutineScope(dispatcher)

    init {
        val flow = fileSystem.watch(config.getValue("directory"))
        scope.launch {
            flow.collect(::observedPhoto)
        }
        auditLogService.log(ApplicationCreatedAuditLog())
    }

    fun observedPhoto(expectedFilePath: String) {
        jobSystem.schedule(expectedFilePath, ::uploadPhoto)
        auditLogService.log(ScheduledAuditLog(expectedFilePath))
        uploadQueue.enququq(ScheduledFileUpload(expectedFilePath))
    }

    private fun uploadPhoto(atFilePath: String) : Result<Unit> {
        return photoServer.doUpload(atFilePath)
    }
}