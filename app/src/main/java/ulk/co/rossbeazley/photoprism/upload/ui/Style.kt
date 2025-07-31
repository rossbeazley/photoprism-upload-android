package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.photoPrismBackground() = apply {
    return background(
        Brush.linearGradient(
            listOf(Color(0xffb8edff), Color(0xffd4b8ff)),
            start = Offset(Float.POSITIVE_INFINITY, 0.0f),
            end = Offset(0.0f, 400.0f)
        )
    )
}