package ulk.co.rossbeazley.photoprism.upload

import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

fun installUncaughtExceptionLogger(auditRepository: AuditRepository) {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val baos = ByteArrayOutputStream()
        val writer = PrintWriter(baos)
        throwable.printStackTrace(writer)
        throwable.stackTrace[0]
        auditRepository.log(
            Debug(
                """UNCaught exception ${throwable.message}
                        | ${throwable.stackTrace[0]}
                        |  ${throwable.stackTrace[1]}
                        |  ${throwable.stackTrace[2]}
                        |  ${throwable.stackTrace[3]}"""
                    .trimMargin()
            )
        )
    }
}
