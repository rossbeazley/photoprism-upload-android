package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository

class ConfigurationFragment : Fragment() {

    companion object {
        fun newInstance() = ConfigurationFragment()
    }

    lateinit var auditRepository: AuditRepository  //TODO custom fragment factory

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
            val configRepo: SharedPrefsConfigRepository =
                (requireContext().applicationContext as AppSingleton).config

            setContent {
                SettingsScreen(configRepo = configRepo)
            }
        }
    }

    @Deprecated("whateva")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.config, menu)
    }

    @Deprecated("whateva")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.auditlogs -> parentFragmentManager.beginTransaction()
                .replace(R.id.container, AuditLogsFragment.newInstance())
                .commitNow()

            R.id.syncqueue -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, SyncQueueFragment.newInstance())
                    .commitNow()
                true
            }

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

}
