package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.startup.Initializer
import ulk.co.rossbeazley.photoprism.upload.AppSingleton

class WorkManagerBackgroundJobSystemInitialiser : Initializer<WorkManagerBackgroundJobSystem> {
    override fun create(context: Context): WorkManagerBackgroundJobSystem {
        val config = (context.applicationContext as AppSingleton).config
        return WorkManagerBackgroundJobSystem(context, config)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
