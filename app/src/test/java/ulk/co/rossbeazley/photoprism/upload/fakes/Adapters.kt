package ulk.co.rossbeazley.photoprism.upload.fakes

import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class Adapters(
    val fileSystem: FakeFilesystem = FakeFilesystem(),
    val auditLogService: CapturingAuditLogService = CapturingAuditLogService(),
    val jobSystem: CapturingBackgroundJobSystem = CapturingBackgroundJobSystem(),
    val uploadQueue: FakeSyncQueue = FakeSyncQueue(),
    val photoServer: MockPhotoServer = MockPhotoServer(),
    val lastUloadRepository: FakeLastUploadRepositoy = FakeLastUploadRepositoy(),
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
)
