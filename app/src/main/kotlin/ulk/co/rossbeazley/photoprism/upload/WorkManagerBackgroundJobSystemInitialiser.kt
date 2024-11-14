package ulk.co.rossbeazley.photoprism.upload

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManager

class WorkManagerBackgroundJobSystemInitialiser : Initializer<WorkManagerBackgroundJobSystem> {
    override fun create(context: Context): WorkManagerBackgroundJobSystem {
        return WorkManagerBackgroundJobSystem(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
