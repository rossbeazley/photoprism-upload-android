package ulk.co.rossbeazley.photoprism.upload.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class SharedPrefsConfigRepository(basename: String = "config_repo", context: Context) :
    ReadonlyConfigRepository {

    private val sharedPrefs by lazy {
        EncryptedSharedPreferences.create(
            basename,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).also {
            it.registerOnSharedPreferenceChangeListener(listener)
        }
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if ("lastupdated" == key) {
            observers.forEach { it() }
            flow.tryEmit(this)
        }
    }

    override fun isConfigured(): Boolean {
        return hostname.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()
    }

    override val maxUploadAttempts: Int get() = sharedPrefs.getInt("maxUploadAttempts", 10)
    override val photoDirectory: String get() = sharedPrefs.getString("photoDirectory", null) ?: "/storage/emulated/0/DCIM/Camera"
    override val username: String get() = sharedPrefs.getString("username", null) ?: ""
    override val hostname: String get() = sharedPrefs.getString("hostname", null) ?: ""
    override val password: String get() = sharedPrefs.getString("password", null) ?: ""
    override val developerMode: Boolean get() = sharedPrefs.getBoolean("developerMode", false)
    override val useMobileData: Boolean get() = sharedPrefs.getBoolean("useMobileData", false)
    private val observers: MutableList<() -> Unit> = mutableListOf()

    override fun onChange(function: () -> Unit) {
        observers.add(function)
    }
    private val flow = MutableSharedFlow<ReadonlyConfigRepository>(
        extraBufferCapacity = 1,
        replay = 0,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    fun changeFlow() : Flow<ReadonlyConfigRepository> = flow

    fun save(
        photoDirectory: String = this.photoDirectory,
        hostname: String = this.hostname,
        username: String = this.username,
        password: String = this.password,
        developerMode: Boolean = this.developerMode,
        useMobileData: Boolean = this.useMobileData,
        maxUploadAttempts: Int = this.maxUploadAttempts
    ) {
        sharedPrefs.edit {
            putString("hostname", hostname)
            putString("username", username)
            putString("password", password)
            putString("photoDirectory", photoDirectory)
            putBoolean("developerMode", developerMode)
            putBoolean("useMobileData", useMobileData)
            putInt("maxUploadAttempts", maxUploadAttempts)
            putLong("lastupdated", System.currentTimeMillis())
        }
    }
}