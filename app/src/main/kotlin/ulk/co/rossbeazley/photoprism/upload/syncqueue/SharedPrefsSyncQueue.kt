package ulk.co.rossbeazley.photoprism.upload.syncqueue

import android.content.Context

class SharedPrefsSyncQueue(basename: String = "boop", context: Context) : SyncQueue {

    private val sharedPrefs =
        context.getSharedPreferences(
            basename,
            Context.MODE_PRIVATE
        )

    private val sharedPrefs2 =
        context.getSharedPreferences(
            "${basename}2",
            Context.MODE_PRIVATE
        )

    override fun put(queueEntry: UploadQueueEntry) {
        sharedPrefs.edit()
            .putString(queueEntry.filePath, typeNameFrom(queueEntry))
            .commit()

        sharedPrefs2.edit()
            .putInt(queueEntry.filePath, queueEntry.attemptCount)
            .commit()
    }

    private fun typeNameFrom(queueEntry: UploadQueueEntry): String {
        return when (queueEntry) {
            is CompletedFileUpload -> "CompletedFileUpload"
            is FailedFileUpload -> "FailedFileUpload"
            is RetryFileUpload -> "RetryFileUpload"
            is RunningFileUpload -> "RunningFileUpload"
            is ScheduledFileUpload -> "ScheduledFileUpload"
        }
    }

    private fun typeFromName(
        queueEntryName: String,
        path: String,
        attempt: Int
    ): UploadQueueEntry {
        return when (queueEntryName) {
            "CompletedFileUpload" -> CompletedFileUpload(path)
            "FailedFileUpload" -> FailedFileUpload(path)
            "RetryFileUpload" -> RetryFileUpload(path, attempt)
            "RunningFileUpload" -> RunningFileUpload(path, attempt)
            "ScheduledFileUpload" -> ScheduledFileUpload(path)
            else -> throw Exception("${queueEntryName} unknown")
        }
    }

    override fun remove(queueEntry: UploadQueueEntry) {
        sharedPrefs.edit()
            .remove(queueEntry.filePath)
            .commit()
    }

    override fun peek(id: String): UploadQueueEntry {
        val string = sharedPrefs.getString(id, null) ?: "unknown"
        val attempt = sharedPrefs2.getInt(id, 0)
        return typeFromName(string, id, attempt)
    }

    override fun all(): List<UploadQueueEntry> {
        val result: List<UploadQueueEntry> = sharedPrefs.all.map {
            val path = it.key ?: "unknown"
            val type = it.value as String
            val attempt = sharedPrefs2.getInt(path, 0)
            val entry: UploadQueueEntry = typeFromName(type, path, attempt)
            entry
        }
        return result
    }

    override fun removeAll() {
        sharedPrefs.edit().clear().commit()

    }
}