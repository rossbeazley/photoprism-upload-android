package ulk.co.rossbeazley.photoprism.upload

interface AuditLogService {
    fun log(log: AuditLog)
}