package ulk.co.rossbeazley.photoprism.upload

sealed class AuditLog
data class ScheduledAuditLog(val filePath: String) : AuditLog()
data class UploadingAuditLog(val filePath: String) : AuditLog()
data class UploadedAuditLog(val filePath: String) : AuditLog()
data class WaitingToRetryAuditLog(val filePath: String) : AuditLog()
data class FailedAuditLog(val filePath: String) : AuditLog()
class ApplicationCreatedAuditLog : AuditLog()