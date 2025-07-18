package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository

@Composable
fun OnboardingScreen(
    configRepo: SharedPrefsConfigRepository,
) {
    var serverurl by remember { mutableStateOf(configRepo.hostname) }
    var username by remember { mutableStateOf(configRepo.username) }
    var password by remember { mutableStateOf(configRepo.password) }

    val updateSettings = {
        configRepo.save(
            username = username,
            password = password,
            hostname = serverurl,
        )
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Text("Lets connect to your server", modifier = Modifier.fillMaxWidth())
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    label = { Text("Server hostname") },
                    value = serverurl,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("hostname no scheme") },
                    onValueChange = { serverurl = it },

                    )
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Username") }
                )
                TextField(
                    value = password,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Password") }
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = updateSettings, modifier = Modifier.padding(end = 4.dp)) {
                        Text(text = "Save")
                    }
                    Button(onClick = {
                        serverurl = BuildConfig.webdavHostName
                        username = BuildConfig.webdavUserName
                        password = BuildConfig.webdavPassword
                    }, modifier = Modifier.padding(start = 4.dp)) {
                        Text(text = "Load Defaults")
                    }
                }
            }
        }
    }
}
