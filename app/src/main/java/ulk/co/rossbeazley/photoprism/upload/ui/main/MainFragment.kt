package ulk.co.rossbeazley.photoprism.upload.ui.main

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.PreferenceManager
import ulk.co.rossbeazley.photoprism.upload.R

class MainFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_main, c, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val datastore = PreferenceManager.getDefaultSharedPreferences(requireContext())
        datastore.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(datastore, null)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, s: String?) {
        logLines(sharedPreferences).also {
            view?.findViewById<TextView>(R.id.message)?.text = it
            println(it)
        }
    }

    private fun logLines(sharedPreferences: SharedPreferences?): String = sharedPreferences
        ?.all
        ?.toSortedMap { a, b -> a.toLong().compareTo(b.toLong()) }
        ?.map { "${it.key}:${it.value.toString()}\n" }
        ?.joinToString { "$it\n" }
        ?: ""
}