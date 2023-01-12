package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

class PhotoPrismApp(
    val fileSystem: Filesystem = AndroidFileObserverFilesystem(),
    private val jobSystem: CapturingBackgroundJobSystem,
    val auditLogService: CapturingAuditLogService,
    val uploadQueue: SyncQueue,
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

    private suspend fun observedPhoto(atPath: String) {
        jobSystem.schedule(atPath)
        auditLogService.log(ScheduledAuditLog(atPath))
        ScheduledFileUpload(atPath).also {
            uploadQueue.put(it)
            observer?.emit(NewEvent(it))
        }
    }

    private suspend fun uploadPhoto(atFilePath: String): JobResult {
        val queueEntry = uploadQueue
            .peek(atFilePath)
            .willAttemptUpload()
            .also(uploadQueue::put)
            .also { observer?.emit(NewEvent(it)) }

        val result: Result<Unit> = photoServer.upload(atFilePath)
        return jobResult(result, queueEntry)
    }

    private suspend fun jobResult(
        result: Result<Unit>,
        queueEntry: RunningFileUpload
    ) = when {
        result.isSuccess -> {
            uploadQueue.remove(queueEntry)
            JobResult.Success
        }
        result.isFailure && queueEntry.attemptCount == config.maxUploadAttempts -> {
            uploadQueue.put(queueEntry.failed())
            observer?.emit(NewEvent(queueEntry.failed()))
            JobResult.Failure
        }
        else -> {
            val queueEntry1 = queueEntry.retryLater()
            observer?.emit(NewEvent(queueEntry1))
            uploadQueue.put(queueEntry1)
            JobResult.Retry
        }
    }

    var observer : MutableSharedFlow<NewEvent>? = null

    fun observeSyncEvents(): Flow<NewEvent> {
        val flow = MutableSharedFlow<NewEvent>()
        val events: List<UploadQueueEntry> = uploadQueue.all()
        events.forEach { entry: UploadQueueEntry ->
            scope.launch {
                flow.emit( NewEvent(entry) )
            }
        }

        return flow.also { observer=it }
    }

}
