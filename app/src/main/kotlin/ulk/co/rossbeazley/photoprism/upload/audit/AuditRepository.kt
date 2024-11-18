package ulk.co.rossbeazley.photoprism.upload.audit

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuditRepository(
    val globalScope: CoroutineScope, preferences: SharedPreferences
                      ) : AuditLogService, SharedPreferences.OnSharedPreferenceChangeListener {

    private val sharedPreferences: SharedPreferences = preferences

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    // TODO (rbeazley) TMP HACK to get logs
    override fun log(log: AuditLog) = log(log.toString())

    private fun log(logMsg: String) {
        sharedPreferences
            .edit(commit = true) {
                putString(System.nanoTime().toString(), logMsg)
            }
    }

    private fun logs() = logLines(sharedPreferences)
        .joinToString(separator = "\n") { it }
        .also { println(it) }

    private fun logLines(sharedPreferences: SharedPreferences) =
        sharedPreferences
            .all
            ?.toSortedMap { a, b -> a.toLong().compareTo(b.toLong()) }
            ?.map { "${it.value.toString()}\n" }
            ?: emptyList()

    private val flow = MutableStateFlow(logs())

    fun observeLogs(): Flow<String> {
        return flow
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        globalScope.launch {
            flow.emit(logs())
        }
    }

    fun clearAll() {
        sharedPreferences.edit()
            .clear()
            .apply()
    }
}

