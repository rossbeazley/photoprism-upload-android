package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.ui.SyncQueue

class SyncQueueFragment : Fragment() {

    companion object {
        fun newInstance() = SyncQueueFragment()
    }

    lateinit var auditRepository: AuditRepository  //TODO custom fragment factory
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickMedia = registerForActivityResult(PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val contentResolver = requireContext().contentResolver
                val photoPrismApp =
                    (requireContext().applicationContext as AppSingleton).photoPrismApp
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

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // TODO inject
        auditRepository = AuditRepository(preferences)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val photoPrismApp: PhotoPrismApp =
                    (requireContext().applicationContext as AppSingleton).photoPrismApp
                SyncQueue(photoPrismApp = photoPrismApp)
            }
        }
    }

    @Deprecated("whateva")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.syncqueue, menu)
    }

    @Deprecated("whateva")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addphoto -> doAddPhoto()
            R.id.clearlogs -> (requireContext().applicationContext as AppSingleton).photoPrismApp.clearSyncQueue()
            R.id.settings -> parentFragmentManager.beginTransaction()
                .replace(R.id.container, ConfigurationFragment.newInstance())
                .commitNow()

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private fun doAddPhoto() =
        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
}
