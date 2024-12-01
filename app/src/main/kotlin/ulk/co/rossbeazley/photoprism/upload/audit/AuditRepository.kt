package ulk.co.rossbeazley.photoprism.upload.audit

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AuditRepository(
    preferences: SharedPreferences
) : AuditLogService, SharedPreferences.OnSharedPreferenceChangeListener {

    private val sharedPreferences: SharedPreferences = preferences

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    // TODO (rbeazley) TMP HACK to get logs
    override fun log(log: AuditLog) {
        sharedPreferences
                .edit {
                    putString(log.timestamp.time.toString(), log.toString())
                }
    }

    private fun logs() = logLines(sharedPreferences)
        .joinToString(separator = "\n") { it }
        .also { println(it) }

    private fun logLines(sharedPreferences: SharedPreferences) =
        sharedPreferences
            .all
            ?.toSortedMap { a: String, b -> a.toLong().compareTo(b.toLong()) }
            ?.map { "${it.value.toString()}\n" }
            ?: emptyList()

    private val flow = MutableStateFlow(logs())

    fun observeLogs(): Flow<String> {
        return flow
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        flow.tryEmit(logs())
    }

    fun clearAll() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}

