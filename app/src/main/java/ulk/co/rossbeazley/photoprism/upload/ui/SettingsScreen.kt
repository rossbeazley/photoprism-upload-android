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
fun SettingsScreen(
    configRepo: SharedPrefsConfigRepository,
    developerSettings: @Composable ()->Unit,
) {
    var serverurl by remember { mutableStateOf(configRepo.hostname) }
    var username by remember { mutableStateOf(configRepo.username) }
    var password by remember { mutableStateOf(configRepo.password) }
    var retryCount by remember { mutableIntStateOf(configRepo.maxUploadAttempts) }

    val updateSettings = {
        configRepo.save(
            username = username,
            password = password,
            hostname = serverurl,
            maxUploadAttempts = retryCount
        )
    }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
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

            Text(text = "Other Settings", modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = retryCount.toString(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        retryCount = it.toInt()
                    },
                    label = { Text(text = "Number of retries", modifier = Modifier.padding(vertical = 4.dp)) }
                )
            }
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                var mobileData by remember { mutableStateOf(true) }
                Checkbox(mobileData, onCheckedChange = {
                    mobileData = it
                })
                Text(text = "Use Mobile Data", modifier = Modifier.padding(vertical = 4.dp))
            }

            var devmode by remember { mutableStateOf(configRepo.developerMode) }
            // TODO this leaks
//            configRepo.onChange {
//                devmode = configRepo.developerMode
//            }
            var devModeCountDown: Int by remember { mutableIntStateOf(5) }
            Text(
                text = "App version: ${BuildConfig.VERSION_NAME}", modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        if (--devModeCountDown < 0) {
                            configRepo.save(developerMode = true)
                            devmode = true
                        }
                    })

            if (devmode) {
                developerSettings()
            }
        }
    }
}

@Composable
fun DeveloperSettings(
    configRepo: SharedPrefsConfigRepository,
    navigateToAuditLogs: () -> Unit,
    clearWorkManager: () -> Unit,
    auditRepository: AuditRepository,
    photoPrismApp: PhotoPrismApp,
    /** TODO pass in work manager livedata or flow **/
    workManagerJobCountFlow: Flow<Int>,
) {
    Text(text = "Debug Settings", modifier = Modifier.padding(vertical = 4.dp))
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .border(BorderStroke(1.dp, Color.DarkGray))
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = navigateToAuditLogs) {
                Text(text = "View Debug Logs")
            }
            Button(onClick = auditRepository::clearAll) {
                Text(text = "Clear debug logs")
            }
            Button(onClick = photoPrismApp::clearSyncQueue) {
                Text(text = "Clear Sync Queue")
            }
            Button(onClick = clearWorkManager) {
                Text(text = "Clear work manager queue")
            }
            val count by workManagerJobCountFlow.collectAsStateWithLifecycle(0)
            Text(text = "$count work items")
            Button(onClick = {
                configRepo.save(developerMode = false)
            }) {
                Text(text = "Turn off developer settings")
            }
        }
    }
}