package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
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
                SettingsScreen(configRepo = configRepo, navigateToAuditLogs = ::navigateToAuditLogs, clearWorkManager= ::clearWorkManager)
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

    private fun navigateToAuditLogs() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, AuditLogsFragment.newInstance())
            .commitNow()
    }

    private fun clearWorkManager() {
        val workManager = AppInitializer.getInstance(requireContext())
            .initializeComponent(WorkManagerInitialiser::class.java)
        workManager.cancelAllWork()
        auditRepository.log(Debug("Cleared work manager"))
    }

}
