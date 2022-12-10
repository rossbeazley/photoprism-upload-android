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
    val photoServer: PhotoServer,
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
        uploadQueue.enqueue(ScheduledFileUpload(expectedFilePath))
    }

    private suspend fun uploadPhoto(atFilePath: String) : JobResult {
        val peek = uploadQueue.peek(atFilePath)
        val queueEntry = peek.copy(attemptCount = peek.attemptCount + 1)
        uploadQueue.enqueue(queueEntry)

        val upload = photoServer.upload(atFilePath)
        return when {
            upload.isSuccess -> {
                JobResult.Success
            }
            upload.isFailure && queueEntry.attemptCount == 2 -> {
                JobResult.Failure
            }
            else -> {
                JobResult.Retry
            }
        }
    }
}