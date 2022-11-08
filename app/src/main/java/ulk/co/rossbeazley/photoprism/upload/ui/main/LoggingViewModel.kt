package ulk.co.rossbeazley.photoprism.upload.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.dav4jvm.*
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.DisplayName
import com.github.sardine.impl.SardineImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.Okio
import okio.Source
import okio.source
import java.io.InputStream


class LoggingViewModel : ViewModel() {


    fun results() {

    }


}