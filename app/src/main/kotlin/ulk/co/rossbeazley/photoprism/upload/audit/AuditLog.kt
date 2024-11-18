package ulk.co.rossbeazley.photoprism.upload.audit

import java.util.Date

sealed class AuditLog(open val timestamp : Date = Date())
data class ScheduledAuditLog(val filePath: String) : AuditLog()
data class UploadingAuditLog(val filePath: String) : AuditLog()
data class UploadedAuditLog(val filePath: String) : AuditLog()
data class WaitingToRetryAuditLog(val filePath: String) : AuditLog()
data class FailedAuditLog(val filePath: String) : AuditLog()
data class ApplicationCreatedAuditLog(override val timestamp : Date = Date()) : AuditLog()
data class DebugAuditLog(val msg : String = "") : AuditLog()