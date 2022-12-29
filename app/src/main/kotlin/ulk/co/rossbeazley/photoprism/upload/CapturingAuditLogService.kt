package ulk.co.rossbeazley.photoprism.upload

class CapturingAuditLogService {
    var capturedAuditLog : AuditLog? = null
    fun log(log : AuditLog) {
        capturedAuditLog = log
    }
}