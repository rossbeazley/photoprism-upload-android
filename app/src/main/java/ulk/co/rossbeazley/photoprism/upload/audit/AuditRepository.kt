package ulk.co.rossbeazley.photoprism.upload.audit

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuditRepository(
    val globalScope: CoroutineScope, preferences: SharedPreferences
                      ) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val sharedPreferences: SharedPreferences = preferences

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun log(logMsg: String) {
        sharedPreferences
            .edit(commit = true) {
                putString(System.nanoTime().toString(), logMsg)
            }
    }

    fun logs() = logLines(sharedPreferences)
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

