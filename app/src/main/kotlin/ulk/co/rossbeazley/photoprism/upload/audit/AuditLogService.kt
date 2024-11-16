package ulk.co.rossbeazley.photoprism.upload.audit

interface AuditLogService {
    fun log(log: AuditLog)
}