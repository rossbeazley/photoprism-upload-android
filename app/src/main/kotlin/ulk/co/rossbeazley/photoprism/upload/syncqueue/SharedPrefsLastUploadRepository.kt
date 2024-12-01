package ulk.co.rossbeazley.photoprism.upload.syncqueue

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPrefsLastUploadRepository(
    val basename: String = "LastUploadRepository",
    val context: Context,
) : LastUploadRepository {

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(
            basename,
            Context.MODE_PRIVATE
        )
    }

    override fun remember(filePath: String) {
        sharedPrefs.edit {
            putString("last", filePath)
        }
    }

    override fun recall(): String {
        return sharedPrefs.getString("last","") ?: ""
    }
}