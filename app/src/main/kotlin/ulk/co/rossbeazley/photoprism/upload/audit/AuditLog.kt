package ulk.co.rossbeazley.photoprism.upload.audit

sealed class AuditLog {
    abstract val timestamp : Date

    class Date : java.util.Date() {
        override fun equals(other: Any?): Boolean = true
    }
}
data class ScheduledAuditLog(val filePath: String, override val timestamp : Date = Date()) : AuditLog()
data class UploadingAuditLog(val filePath: String, override val timestamp : Date = Date()) : AuditLog()
data class UploadedAuditLog(val filePath: String, override val timestamp : Date = Date()) : AuditLog()
data class WaitingToRetryAuditLog(
    val filePath: String,
    val attempt: Int = 0,
    val optionalThrowable: Throwable?, override val timestamp : Date = Date()
) : AuditLog()
data class FailedAuditLog(val filePath: String, override val timestamp : Date = Date()) : AuditLog()
data class ApplicationCreatedAuditLog(override val timestamp : Date = Date()) : AuditLog()
data class DebugAuditLog(val msg : String = "", override val timestamp : Date = Date()) : AuditLog()
