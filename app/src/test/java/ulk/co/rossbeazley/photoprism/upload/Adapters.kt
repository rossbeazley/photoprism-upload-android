package ulk.co.rossbeazley.photoprism.upload

class Adapters(
    val fileSystem: FakeFilesystem,
    val auditLogService: CapturingAuditLogService,
    val jobSystem: CapturingBackgroundJobSystem,
    val uploadQueue: FakeSyncQueue,
    val photoServer: MockPhotoServer,
    val lastUloadRepository: FakeLastUploadRepositoy = FakeLastUploadRepositoy(),
)