package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.Event
import ulk.co.rossbeazley.photoprism.upload.FullSyncState
import ulk.co.rossbeazley.photoprism.upload.PartialSyncState
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry

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
            R.id.auditlogs -> parentFragmentManager.beginTransaction()
                .replace(R.id.container, AuditLogsFragment.newInstance())
                .commitNow()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private fun doAddPhoto() =
        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
}

@Composable
fun SyncQueue(
    photoPrismApp: PhotoPrismApp,
    startState: MutableMap<String, UploadQueueEntry> = mutableMapOf()
) {
    MaterialTheme {
        val syncQueue by photoPrismApp.observeSyncEvents()
            .map { event: Event ->
                when (event) {
                    is FullSyncState -> {
                        startState.clear()
                        startState.putAll(event.events.associateBy { it.filePath })
                    }
                    is PartialSyncState -> startState[event.event.filePath] = event.event
                }
                startState.values.toList()
            }
            .collectAsStateWithLifecycle(initialValue = emptyList())

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(syncQueue)  { log ->
                Text(
                    text = log.toString(),
                    modifier = Modifier.padding(10.dp)
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
}