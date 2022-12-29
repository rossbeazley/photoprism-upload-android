package ulk.co.rossbeazley.photoprism.upload

sealed class AuditLog {

}

data class ScheduledAuditLog(val filePath: String) : AuditLog()
class ApplicationCreatedAuditLog : AuditLog()