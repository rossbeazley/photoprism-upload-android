package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import androidx.work.Configuration

class WorkManagerConfigInitialiser : Initializer<Configuration> {
    override fun create(context: Context): Configuration {
        val instance = AppInitializer.getInstance(context)
        val backgroundJobSystem = instance.initializeComponent(
            WorkManagerBackgroundJobSystemInitialiser::class.java)
        val config: Configuration = Configuration.Builder()
            .setWorkerFactory(backgroundJobSystem)
            .build()
        return config
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            WorkManagerBackgroundJobSystemInitialiser::class.java
        )
    }
}