package ulk.co.rossbeazley.photoprism.upload

import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.startup.AppInitializer
import kotlinx.coroutines.launch

import ulk.co.rossbeazley.photoprism.upload.AppSingleton.Companion.STARTED
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.ui.ApplicationScaffold

private const val s = "Hello world!"

class MainActivity : ComponentActivity() {

    val requestPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) {}

    val auditRepository : AuditRepository by lazy {
        (applicationContext as AppSingleton).auditRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApplicationScaffold(
                ::doAddPhoto,
                AppInitializer.getInstance(LocalContext.current)
                    .initializeComponent(WorkManagerInitialiser::class.java),
                (LocalContext.current.applicationContext as AppSingleton).auditRepository,
                (LocalContext.current.applicationContext as AppSingleton).photoPrismApp,
                (LocalContext.current.applicationContext as AppSingleton).config
            )
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

        pickMedia = registerForActivityResult(PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentResolver = contentResolver
                val photoPrismApp =
                    (applicationContext as AppSingleton).photoPrismApp
                lifecycleScope.launch {
                    uris.forEach { uri ->
                        auditRepository.log(Debug("Selected URI: $uri"))
                        contentResolver.takePersistableUriPermission(uri, flag)
                        photoPrismApp.importPhoto(uri.toString())
                    }
                }

            } else {
                auditRepository.log(Debug("NO Selected URI"))
            }
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
                auditRepository.log(
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
        auditRepository.log(Debug("Main Activity on destroy"))
    }

    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    fun doAddPhoto() =
        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
}