package ulk.co.rossbeazley.photoprism.upload.syncqueue

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

private const val COMPLETED_LIST_KEY = "completedlist"

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

    private val sharedPrefsCompletedQueue =
        context.getSharedPreferences(
            "${basename}Completed",
            Context.MODE_PRIVATE
        )

    override fun put(queueEntry: UploadQueueEntry) {

        garbaggeCollectAnyOldCompletedEntries(queueEntry)

        sharedPrefs.edit {
            putString(queueEntry.filePath, typeNameFrom(queueEntry))
        }

        sharedPrefs2.edit {
            putInt(queueEntry.filePath, queueEntry.attemptCount)
        }
    }

    private fun garbaggeCollectAnyOldCompletedEntries(queueEntry: UploadQueueEntry) {
        if (queueEntry is CompletedFileUpload) {
            val jsonArray = JSONArray(
                sharedPrefs2
                    .getString(COMPLETED_LIST_KEY, "[]")
            ).put(queueEntry.filePath)

            if (jsonArray.length() > 5) {
                val remove: String = jsonArray.remove(0) as String
                sharedPrefs2.edit {
                    remove(remove)
                }
            }
            sharedPrefsCompletedQueue.edit {
                putString(COMPLETED_LIST_KEY, jsonArray.toString())
            }
        }
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
        sharedPrefs.edit {
            remove(queueEntry.filePath)
        }
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
        // non-complete first
        // then complete in order of SP2
        return result
    }

    override fun removeAll() {
        sharedPrefs.edit().clear().apply()
        sharedPrefs2.edit().clear().apply()
        sharedPrefsCompletedQueue.edit().clear().apply()
    }
}