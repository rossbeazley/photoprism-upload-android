package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
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
import androidx.startup.AppInitializer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug

class AuditLogsFragment : Fragment() {

    companion object {
        fun newInstance() = AuditLogsFragment()
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
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AuditLogsList(audits = auditRepository)
            }
        }
    }

    @Deprecated("whateva")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.auditlogs, menu)
    }

    @Deprecated("whateva")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearlogs -> {
                auditRepository.clearAll()
                auditRepository.log(Debug("Cleared logs"))
                true
            }

            R.id.clearwork -> {
                val workManager = AppInitializer.getInstance(requireContext())
                    .initializeComponent(WorkManagerInitialiser::class.java)
                workManager.cancelAllWork()
                auditRepository.log(Debug("Cleared work manager"))
                true
            }

            R.id.addphoto -> {
                doAddPhoto()
                true
            }

            R.id.syncqueue -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, SyncQueueFragment.newInstance())
                    .commitNow()
                true
            }

            R.id.settings -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, ConfigurationFragment.newInstance())
                    .commitNow()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    lateinit var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>

    private fun doAddPhoto() {
        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
    }
}


@Composable
fun AuditLogsList(
    audits: AuditRepository
) {
    MaterialTheme {
        val stateList by audits.observeLogs()
            .map { log ->
                log.split(")\n")
                    .filter { row -> row.isNotBlank() }
                    .map { row -> row.trim() }
                    .map { row -> row.replace("(", "\n") }
                    .map { row -> row.replace(", ", "\n") }
                    .reversed()
            }.collectAsStateWithLifecycle(initialValue = emptyList())

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(stateList) { log ->
                Text(
                    text = log,
                    modifier = Modifier.padding(10.dp)
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
}
