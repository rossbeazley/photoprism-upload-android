package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

class PhotoPrismApp(
    private val fileSystem: Filesystem,
    private val jobSystem: BackgroundJobSystem,
    private val auditLogService: AuditLogService,
    private val uploadQueue: SyncQueue /* = SharedPrefsSyncQueue(context) */,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val photoServer: PhotoServer,
    private val config: Config,
) {

    private val scope = CoroutineScope(dispatcher)

    init {
        jobSystem.register(::readyToUpload)
        val flow = fileSystem.watch(config.photoDirectory)
        scope.launch {
            flow.collect(::observedPhoto)
        }
        auditLogService.log(ApplicationCreatedAuditLog())
        log("App init")
    }

    suspend fun readyToUpload(expectedFilePath: String): JobResult {
        log("Upload from work with cb")
        return uploadPhoto(expectedFilePath)
    }

    private suspend fun observedPhoto(atPath: String) {
        log("observedPhoto(atPath: $atPath)")
        auditLogService.log(ScheduledAuditLog(atPath))
        ScheduledFileUpload(atPath).also {
            uploadQueue.put(it)
            observer?.emit(NewEvent(it))
        }
        jobSystem.schedule(atPath)
    }

    private suspend fun uploadPhoto(atFilePath: String): JobResult {
        val queueEntry = uploadQueue
            .peek(atFilePath)
            .willAttemptUpload()
            .also(uploadQueue::put)
            .also { observer?.emit(NewEvent(it)) }

        val result: Result<Unit> = photoServer.upload(atFilePath)
        log("Result $result")
        return jobResult(result, queueEntry)
    }

    private suspend fun jobResult(
        result: Result<Unit>,
        queueEntry: RunningFileUpload
    ) = when {
        result.isSuccess -> {
            uploadQueue.remove(queueEntry)
            observer?.emit(NewEvent(CompletedFileUpload(queueEntry.filePath)))
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
