package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager

class WorkManagerInitialiser : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val instance = AppInitializer.getInstance(context)
        val config: Configuration = instance.initializeComponent(WorkManagerConfigInitialiser::class.java)
        WorkManager.initialize(context,config)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            WorkManagerConfigInitialiser::class.java
        )
    }
}