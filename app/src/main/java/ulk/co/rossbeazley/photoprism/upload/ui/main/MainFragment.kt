package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_main, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val auditRepository = AuditRepository(requireContext()) //TODO custom fragment factory
        val findViewById = view.findViewById<TextView>(R.id.message)
        lifecycleScope.launch {
            auditRepository.observeLogs().collect {
                findViewById?.text = it
            }
        }
    }
}
