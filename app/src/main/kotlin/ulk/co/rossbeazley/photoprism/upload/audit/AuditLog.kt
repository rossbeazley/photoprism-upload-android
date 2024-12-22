package ulk.co.rossbeazley.photoprism.upload.audit

import java.util.Date

sealed class AuditLog {
    abstract val timestamp : LogDate

    class LogDate : Date() {
        override fun equals(other: Any?): Boolean = true
        override fun hashCode(): Int = 0
    }
}
data class Scheduled(val filePath: String, override val timestamp : LogDate = LogDate()) : AuditLog()
data class Uploading(val filePath: String, override val timestamp : LogDate = LogDate()) : AuditLog()
data class Uploaded(val filePath: String, override val timestamp : LogDate = LogDate()) : AuditLog()
data class WaitingToRetry(
    val filePath: String,
    val attempt: Int = 0,
    val optionalThrowable: Throwable?, override val timestamp : LogDate = LogDate()
) : AuditLog()
data class Failed(val filePath: String, override val timestamp : LogDate = LogDate()) : AuditLog()
data class ApplicationCreated(override val timestamp : LogDate = LogDate()) : AuditLog()
data class Debug(val msg : String = "", override val timestamp : LogDate = LogDate()) : AuditLog()
