package ulk.co.rossbeazley.photoprism.upload

import android.app.ActivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ulk.co.rossbeazley.photoprism.upload.ui.main.MainFragment

import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.STARTED

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        startService(this)
        if (STARTED) return
        STARTED = true
        logAppExitReason()
    }

    private fun logAppExitReason() {
        getSystemService(ActivityManager::class.java)
            .getHistoricalProcessExitReasons(
                applicationContext.packageName,
                0,
                1
            )
            .firstOrNull()
            ?.let {
                auditlog(
                    "Last close: ${it.description} ${it.reason} ${it.timestamp}",
                    MainActivity@ this
                )
            }
    }
}