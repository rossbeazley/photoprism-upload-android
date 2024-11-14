package ulk.co.rossbeazley.photoprism.upload

class CapturingAuditLogService : AuditLogService {
    var capturedAuditLog : AuditLog? = null
    override fun log(log : AuditLog) {
        capturedAuditLog = log
    }
}