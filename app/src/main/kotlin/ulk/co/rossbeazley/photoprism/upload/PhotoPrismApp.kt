package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import ulk.co.rossbeazley.photoprism.upload.audit.ApplicationCreatedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.AuditLogService
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.FailedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.ScheduledAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.UploadedAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.UploadingAuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.WaitingToRetryAuditLog
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.BackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.JobResult
import ulk.co.rossbeazley.photoprism.upload.filesystem.Filesystem
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.LastUploadRepository
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.SyncQueue

class PhotoPrismApp(
    private val fileSystem: Filesystem,
    private val jobSystem: BackgroundJobSystem,
    private val auditLogService: AuditLogService,
    private val uploadQueue: SyncQueue,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val photoServer: PhotoServer,
    private val config: Config,
    private val lastUloadRepository: LastUploadRepository,
) {

    private val scope = CoroutineScope(dispatcher)
    private var flow : MutableSharedFlow<NewEvent> = MutableSharedFlow<NewEvent>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        jobSystem.register(::readyToUpload)
        val flow = fileSystem.watch(config.photoDirectory)
        scope.launch {
            findFilesMissingSinceLastLaunch()
            flow.collect(::observedPhoto)
        }
        auditLogService.log(ApplicationCreatedAuditLog())
    }

    private suspend fun PhotoPrismApp.findFilesMissingSinceLastLaunch() {
        val list = fileSystem.list(config.photoDirectory)
        val element = lastUloadRepository.recall()
        val indexOf = list.indexOf(element)
        if (indexOf > 0) {
            auditLogService.log(DebugAuditLog("Found some missed files to sync"))
            list.subList(0, indexOf).forEach {
                observedPhoto(it)
            }
            auditLogService.log(DebugAuditLog("Finished some missed files to sync $indexOf"))

        }
    }

    suspend fun readyToUpload(expectedFilePath: String): JobResult {
        log("Upload from work with cb")
        return uploadPhoto(expectedFilePath)
    }

    suspend fun pickPhoto(withUri: String){
        return observedPhoto(withUri)
    }

    private suspend fun observedPhoto(atPath: String) {
        log("observedPhoto(atPath: $atPath)")
        auditLogService.log(ScheduledAuditLog(atPath))
        ScheduledFileUpload(atPath).also {
            uploadQueue.put(it)
            flow.emit(NewEvent(it))
        }
        jobSystem.schedule(atPath)
    }

    private suspend fun uploadPhoto(atFilePath: String): JobResult {
        val queueEntry = uploadQueue
            .peek(atFilePath)
            .willAttemptUpload()
            .also(uploadQueue::put)
            .also { flow.emit(NewEvent(it)) }

        auditLogService.log(UploadingAuditLog(atFilePath))
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
            lastUloadRepository.remember(queueEntry.filePath)
            auditLogService.log(UploadedAuditLog(queueEntry.filePath))
            flow.emit(NewEvent(CompletedFileUpload(queueEntry.filePath)))
            JobResult.Success
        }
        result.isFailure && queueEntry.attemptCount == config.maxUploadAttempts -> {
            uploadQueue.put(queueEntry.failed())
            auditLogService.log(FailedAuditLog(queueEntry.filePath))
            flow.emit(NewEvent(queueEntry.failed()))
            JobResult.Failure
        }
        else -> {
            val queueEntry1 = queueEntry.retryLater()
            auditLogService.log(WaitingToRetryAuditLog(queueEntry.filePath, queueEntry.attemptCount, result.exceptionOrNull()))
            flow.emit(NewEvent(queueEntry1))
            uploadQueue.put(queueEntry1)
            JobResult.Retry
        }
    }

    fun observeSyncEvents(): Flow<NewEvent> {
        return flow
    }

}
