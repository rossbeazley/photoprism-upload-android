package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

class PhotoPrismApp(
    val fileSystem: Filesystem,
    private val jobSystem: CapturingBackgroundJobSystem,
    val auditLogService: CapturingAuditLogService,
    val uploadQueue: UploadQueue,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val photoServer: PhotoServer,
    val config: Config,
) {

    private val scope = CoroutineScope(dispatcher)

    init {
        jobSystem.register(::readyToUpload)
        val flow = fileSystem.watch(config.photoDirectory)
        scope.launch {
            flow.collect(::observedPhoto)
        }
        auditLogService.log(ApplicationCreatedAuditLog())
    }

    suspend fun readyToUpload(expectedFilePath: String): JobResult {
        return uploadPhoto(expectedFilePath)
    }

    private fun observedPhoto(expectedFilePath: String) {
        jobSystem.schedule(expectedFilePath)
        auditLogService.log(ScheduledAuditLog(expectedFilePath))
        uploadQueue.put(ScheduledFileUpload(expectedFilePath))
    }

    private suspend fun uploadPhoto(atFilePath: String): JobResult {
        val queueEntry = uploadQueue
                            .peek(atFilePath)
                            .willAttemptUpload()
                            .also(uploadQueue::put)

        val result: Result<Unit> = photoServer.upload(atFilePath)
        return when {
            result.isSuccess -> {
                uploadQueue.remove(queueEntry)
                JobResult.Success
            }
            result.isFailure && queueEntry.attemptCount == config.maxUploadAttempts -> {
                uploadQueue.put(queueEntry.failed())
                JobResult.Failure
            }
            else -> {
                uploadQueue.put(queueEntry.retryLater())
                JobResult.Retry
            }
        }
    }
}
