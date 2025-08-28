package ulk.co.rossbeazley.photoprism.upload.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun UseLightStatusBarIcons() {
    val systemUiController = rememberSystemUiController()
    systemUiController?.isAppearanceLightStatusBars = false
}

@Composable
private fun rememberSystemUiController(
    window: Window? = findWindow(),
): WindowInsetsControllerCompat? {
    val view = LocalView.current
    return remember(view, window) { window?.run { WindowCompat.getInsetsController(window, view) } }
}

@Composable
private fun findWindow(): Window? =
    (LocalView.current.parent as? DialogWindowProvider)?.window
        ?: LocalView.current.context.findWindow()

private tailrec fun Context.findWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.findWindow()
        else -> null
    }