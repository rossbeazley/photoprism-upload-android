package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.NewEvent
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog

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
                        auditRepository.log(DebugAuditLog("Selected URI: $uri"))
                        contentResolver.takePersistableUriPermission(uri, flag)
                        photoPrismApp.importPhoto(uri.toString())
                    }
                }

            } else {
                auditRepository.log(DebugAuditLog("NO Selected URI"))
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

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_main, c, false)

    private var logs = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val findViewById = view.findViewById<TextView>(R.id.message) ?: return
        lifecycleScope.launch {
            val photoPrismApp =
                (requireContext().applicationContext as AppSingleton).photoPrismApp

            photoPrismApp.observeSyncEvents().collect {
                logs += "\n" + it.event
                findViewById.text = logs
            }
        }
    }

    @Deprecated("whateva")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.syncqueue, menu)
    }

    @Deprecated("whateva")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addphoto -> {
                doAddPhoto()
                true
            }

            R.id.auditlogs -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, AuditLogsFragment.newInstance())
                    .commitNow()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private fun doAddPhoto() =
        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
}
