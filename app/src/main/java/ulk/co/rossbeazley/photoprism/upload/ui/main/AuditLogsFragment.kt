package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.startup.AppInitializer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.NewEvent
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.backgroundjobsystem.WorkManagerInitialiser
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.DebugAuditLog

class AuditLogsFragment : Fragment() {

    companion object {
        fun newInstance() = AuditLogsFragment()
    }

    lateinit var auditRepository: AuditRepository  //TODO custom fragment factory

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // TODO inject
        auditRepository = AuditRepository(GlobalScope, preferences)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_main, c, false)

    private var events = ""
    private var logs = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val findViewById = view.findViewById<TextView>(R.id.message) ?: return
        lifecycleScope.launch {
            auditRepository.observeLogs().collect {
                logs = it.split("\n")
                    .reversed()
                    .joinToString("\n") { log ->
                        log.replace("(", "\n")
                            .replace(")", "")
                    }

                findViewById.text = events + logs

            }

            (requireContext().applicationContext as AppSingleton)
                .photoPrismApp?.observeSyncEvents()?.collect { newevent: NewEvent ->
                    events += newevent
                    findViewById.text = events + logs

                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.auditlogs, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.clearlogs -> {
                auditRepository.clearAll()
                auditRepository.log(DebugAuditLog("Cleared logs"))
                true
            }

            R.id.clearwork -> {
                val workManager = AppInitializer.getInstance(requireContext())
                    .initializeComponent(WorkManagerInitialiser::class.java)
                workManager.cancelAllWork()
                auditRepository.log(DebugAuditLog("Cleared work manager"))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
