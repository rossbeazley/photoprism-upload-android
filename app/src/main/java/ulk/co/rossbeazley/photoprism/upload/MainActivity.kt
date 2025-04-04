package ulk.co.rossbeazley.photoprism.upload

import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.STARTED
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.ui.main.SyncQueueFragment

class MainActivity : AppCompatActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SyncQueueFragment.newInstance())
                .commitNow()
        }

        val checkSelfPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
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
                    Debug(
                        "Last close: ${it.description}\n" +
                                "rss:${it.rss} pss:${it.pss}\n" +
                                "${it.reason} ${it.timestamp}"
                    )
                )
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        (applicationContext as AppSingleton).auditRepository
            .log(Debug("Main Activity on destroy"))
    }
}