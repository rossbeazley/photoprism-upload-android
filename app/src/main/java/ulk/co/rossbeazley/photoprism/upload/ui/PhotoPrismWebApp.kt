package ulk.co.rossbeazley.photoprism.upload.ui

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PhotoPrismWebApp(modifier: Modifier, appUrlString: String) {
    AndroidView(
        modifier = modifier
            .fillMaxHeight(),
        factory = { context ->
            WebView(context)
                .apply {
                    settings.apply {
                        javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        loadWithOverviewMode = false
                        useWideViewPort = false
                        setSupportZoom(false)
                        allowContentAccess = true
                        databaseEnabled = true
                        databasePath = context.cacheDir.path
                        domStorageEnabled = true

                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
        },
        update = { webView ->
            webView.loadUrl(appUrlString)
        }
    )
}