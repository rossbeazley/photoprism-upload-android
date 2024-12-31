package ulk.co.rossbeazley.photoprism.upload

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import ulk.co.rossbeazley.photoprism.upload.audit.ApplicationCreated
import ulk.co.rossbeazley.photoprism.upload.audit.AuditLogService
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.audit.Failed
import ulk.co.rossbeazley.photoprism.upload.audit.Scheduled
import ulk.co.rossbeazley.photoprism.upload.audit.Uploaded
import ulk.co.rossbeazley.photoprism.upload.audit.Uploading
import ulk.co.rossbeazley.photoprism.upload.audit.WaitingToRetry
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.BackgroundJobSystem
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.JobResult
import ulk.co.rossbeazley.photoprism.upload.config.ReadonlyConfigRepository
import ulk.co.rossbeazley.photoprism.upload.filesystem.Filesystem
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer
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
    private val config: ReadonlyConfigRepository,
    private val lastUloadRepository: LastUploadRepository,
) {

    private val scope = CoroutineScope(dispatcher)
    private var flow: MutableSharedFlow<Event> =
        MutableSharedFlow(replay = 0, extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        jobSystem.register(::readyToUpload)
        val flow = fileSystem.watch(config.photoDirectory) // TODO work out how to GC this
        scope.launch {
            findFilesMissingSinceLastLaunch()
            flow.collect(::observedPhoto)
        }
        auditLogService.log(ApplicationCreated())
    }

    private suspend fun PhotoPrismApp.findFilesMissingSinceLastLaunch() {
        val list = fileSystem.list(config.photoDirectory)
        val element = lastUloadRepository.recall()
        val indexOf = list.indexOf(element)
        if (indexOf > 0) {
            auditLogService.log(Debug("Found some missed files to sync"))
            list.subList(0, indexOf).forEach {
                observedPhoto(it)
            }
            auditLogService.log(Debug("Finished some missed files to sync $indexOf"))

        }
    }

    suspend fun readyToUpload(expectedFilePath: String): JobResult {
        log("Upload from work with cb")
        return uploadPhoto(expectedFilePath)
    }

    suspend fun importPhoto(withUri: String) {
        return observedPhoto(withUri)
    }

    private suspend fun observedPhoto(atPath: String) {
        log("observedPhoto(atPath: $atPath)")
        auditLogService.log(Scheduled(atPath))
        ScheduledFileUpload(atPath).also {
            uploadQueue.put(it)
            flow.emit(PartialSyncState(it))
        }
        jobSystem.schedule(atPath)
    }

    private suspend fun uploadPhoto(atFilePath: String): JobResult {
        val queueEntry = uploadQueue
            .peek(atFilePath)
            .willAttemptUpload()
            .also(uploadQueue::put)
            .also { flow.emit(PartialSyncState(it)) }

        auditLogService.log(Uploading(atFilePath))
        val result: Result<Unit> = photoServer.upload(atFilePath)
        log("Result $result")
        return jobResult(result, queueEntry)
    }

    private suspend fun jobResult(
        result: Result<Unit>,
        queueEntry: RunningFileUpload
    ) = when {
        result.isSuccess -> {
            val completedFileUpload = queueEntry.complete()
            uploadQueue.put(completedFileUpload)
            if(queueEntry.filePath.startsWith("/")) lastUloadRepository.remember(queueEntry.filePath)
            auditLogService.log(Uploaded(queueEntry.filePath))
            flow.emit(PartialSyncState(completedFileUpload))
            JobResult.Success
        }

        result.isFailure && queueEntry.attemptCount == config.maxUploadAttempts -> {
            uploadQueue.put(queueEntry.failed())
            auditLogService.log(Failed(queueEntry.filePath))
            flow.emit(PartialSyncState(queueEntry.failed()))
            JobResult.Failure
        }

        else -> {
            val queueEntry1 = queueEntry.retryLater()
            auditLogService.log(
                WaitingToRetry(
                    queueEntry.filePath,
                    queueEntry.attemptCount,
                    result.exceptionOrNull()
                )
            )
            flow.emit(PartialSyncState(queueEntry1))
            uploadQueue.put(queueEntry1)
            JobResult.Retry
        }
    }

    fun observeSyncEvents(): Flow<Event> {
        return flow.onSubscription {

        emit(
                FullSyncState(
                    uploadQueue.all()
                )
            )
        }
    }

    fun clearSyncQueue() {
        uploadQueue.removeAll()
        flow.tryEmit(FullSyncState(emptyList()))
    }

}
