package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.startup.Initializer

class WorkManagerBackgroundJobSystemInitialiser : Initializer<WorkManagerBackgroundJobSystem> {
    override fun create(context: Context): WorkManagerBackgroundJobSystem {
        return WorkManagerBackgroundJobSystem(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
