package ulk.co.rossbeazley.photoprism.upload.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

@Composable
fun TestConnectionButton(photoServer: PhotoServer) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Button(onClick = {
        coroutineScope.launch(Dispatchers.IO) {
            val result = photoServer.checkConnection()
            val message = when {
                result.isSuccess -> "Success!"
                else -> "Failed!"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }, modifier = Modifier.Companion.padding(end = 4.dp)) {
        Text(text = "Test Connection")
    }
}