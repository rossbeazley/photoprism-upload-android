package ulk.co.rossbeazley.photoprism.upload

import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import ulk.co.rossbeazley.photoprism.upload.ui.main.AuditLogsFragment

import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.STARTED
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog

class MainActivity : AppCompatActivity() {

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { it: Map<String, @JvmSuppressWildcards Boolean>? ->
            startService(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, AuditLogsFragment.newInstance())
                .commitNow()
        }

        if (
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startService(this)
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.FOREGROUND_SERVICE,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                )
            )
        }
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
                (applicationContext as AppSingleton).auditRepository.log(
                    DebugAuditLog(
                        "Last close: ${it.description}\n" +
                                "rss:${it.rss} pss:${it.pss}\n" +
                                "${it.reason} ${it.timestamp}"
                    )
                )
            }
    }
}