package ulk.co.rossbeazley.photoprism.upload.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

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
        }
    }

    override val maxUploadAttempts: Int = 10
    override val photoDirectory: String get() = sharedPrefs.getString("photoDirectory", null) ?: "/storage/emulated/0/DCIM/Camera"
    override val username: String get() = sharedPrefs.getString("username", null) ?: ""
    override val hostname: String get() = sharedPrefs.getString("hostname", null) ?: ""
    override val password: String get() = sharedPrefs.getString("password", null) ?: ""
    private val observers: MutableList<() -> Unit> = mutableListOf()

    override fun onChange(function: () -> Unit) {
        observers.add(function)
    }

    fun save(
        photoDirectory: String = this.photoDirectory,
        hostname: String = this.hostname,
        username: String = this.username,
        password: String = this.password
    ) {
        sharedPrefs.edit {
            putString("hostname", hostname)
            putString("username", username)
            putString("password", password)
            putString("photoDirectory", photoDirectory)
            putLong("lastupdated", System.currentTimeMillis())
        }
    }
}