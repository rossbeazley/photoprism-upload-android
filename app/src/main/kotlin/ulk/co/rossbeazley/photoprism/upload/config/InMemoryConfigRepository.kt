package ulk.co.rossbeazley.photoprism.upload.config

import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import java.lang.ref.WeakReference

class InMemoryConfigRepository(
    override val photoDirectory: String,
    override val maxUploadAttempts: Int = 2,
    override val username: String = BuildConfig.webdavUserName,
    override val hostname: String = BuildConfig.webdavHostName,
    override val password: String = BuildConfig.webdavPassword,
    override val developerMode: Boolean = true,
    override val useMobileData: Boolean = true,
    ) : ReadonlyConfigRepository {

    private val observers: MutableList<WeakReference<() -> Unit>> = mutableListOf()
    override fun onChange(function: () -> Unit) {
        observers.add(WeakReference(function))
    }
}