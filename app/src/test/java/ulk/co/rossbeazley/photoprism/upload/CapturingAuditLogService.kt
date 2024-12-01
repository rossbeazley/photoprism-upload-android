package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.audit.AuditLog
import ulk.co.rossbeazley.photoprism.upload.audit.AuditLogService

class CapturingAuditLogService : AuditLogService {
    var capturedAuditLog : AuditLog? = null
    override fun log(log : AuditLog) {
        capturedAuditLog = log
    }
}