package ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem

import android.content.Context
import androidx.startup.Initializer
import ulk.co.rossbeazley.photoprism.upload.AppSingleton

class WorkManagerBackgroundJobFactoryInitialiser : Initializer<WorkManagerBackgroundJobFactory> {
    override fun create(context: Context): WorkManagerBackgroundJobFactory {
        return WorkManagerBackgroundJobFactory(
            (context.applicationContext as AppSingleton).photoPrismApp::readyToUpload,
            (context.applicationContext as AppSingleton).auditRepository,
            (context.applicationContext as AppSingleton).photoPrismApp,
        ) { (context.applicationContext as AppSingleton).workManager }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
