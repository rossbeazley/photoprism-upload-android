package ulk.co.rossbeazley.photoprism.upload.ui

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PhotoPrismWebApp(modifier: Modifier = Modifier, hostname: String) {
    val browseUrl = "https://$hostname/library/browse"
    var isEnabled by remember { mutableStateOf(true) }
    var onBack: () -> Unit = {
        Log.i("NAV", "back")
    }
    BackHandler(enabled = isEnabled) {
        Log.i("NAV", "backhandler")
        onBack()
    }

    AndroidView(
        modifier = modifier
            .fillMaxHeight(),
        factory = { context ->
            WebView(context)
                .apply {
                    settings.apply {
                        javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean
                            ) {
                                val isNotLogin =
                                    url != "https://$hostname/library/login"
                                val isNotBrowse =
                                    url != browseUrl
                                isEnabled = view?.canGoBack() == true
                                        && isNotLogin
                                        && isNotBrowse
                                Log.i("NAV", "doUpdateVisitedHistory $isEnabled : $url")
                                super.doUpdateVisitedHistory(view, url, isReload)
                            }
                        }
                        loadWithOverviewMode = false
                        useWideViewPort = false
                        setSupportZoom(false)
                        allowContentAccess = true
                        databaseEnabled = true
                        databasePath = context.cacheDir.path
                        domStorageEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onCloseWindow(window: WebView?) {
                                Log.i("NAV", "onCloseWindow")
                                super.onCloseWindow(window)
                            }
                        }
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    loadUrl(browseUrl)
                }
        },
        update = { webView ->
            onBack = {
                Log.i("NAV", "back to webview")
                webView.goBack()
            }
        },
        onRelease = { webView ->
            Log.i("NAV", "destroy webview")
            webView.destroy()
        }
    )
}