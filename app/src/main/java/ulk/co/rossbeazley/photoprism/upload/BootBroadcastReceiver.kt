package ulk.co.rossbeazley.photoprism.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import androidx.startup.AppInitializer
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BOOT_COMPLETED) return
        AppSingleton.STARTED = true
    }
}
