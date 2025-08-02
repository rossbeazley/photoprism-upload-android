package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

@Composable
fun SettingsScreen(
    configRepo: SharedPrefsConfigRepository,
    developerSettings: @Composable () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .photoPrismBackground()
        ) {

            Text(
                text = "Settings",
                fontSize = 46.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 24.dp, top = 24.dp)
                    .fillMaxWidth()
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
                    .padding(top = 20.dp, bottom = 20.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        var mobileData by remember { mutableStateOf(true) }
                        Checkbox(mobileData, onCheckedChange = {
                            mobileData = it
                        }, modifier = Modifier.align(Alignment.CenterVertically))
                        Text(
                            text = "Use mobile data for upload",
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxHeight()
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 4.dp),
                        value = retryCount.toString(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = {
                            retryCount = it.toInt()
                        },
                        label = {
                            Text(
                                text = "Number of times to retry upload",
                                //modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)

                    .padding(top = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)) {
                    TextField(
                        label = { Text("Server hostname") },
                        value = serverurl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        placeholder = { Text("hostname no scheme") },
                        onValueChange = { serverurl = it },

                        )
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        label = { Text(text = "Username") }
                    )
                    TextField(
                        value = password,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        label = { Text(text = "Password") }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp)
                    .align(Alignment.End)
            ) {
                Button(onClick = {
                    serverurl = BuildConfig.webdavHostName
                    username = BuildConfig.webdavUserName
                    password = BuildConfig.webdavPassword
                }, modifier = Modifier.padding(start = 4.dp)) {
                    Text(text = "Load Defaults")
                }

                Button(onClick = updateSettings, modifier = Modifier.padding(end = 4.dp)) {
                    Text(text = "Save")
                }
            }

            val flowingDevMode by configRepo.changeFlow()
                .map {
                    it.developerMode
                }
                .collectAsStateWithLifecycle(configRepo.developerMode)
            var devModeCountDown: Int by remember { mutableIntStateOf(5) }
            Text(
                text = "App version: ${BuildConfig.VERSION_NAME}", modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .clickable {
                        if (--devModeCountDown < 0 && flowingDevMode.not()) {
                            configRepo.save(developerMode = true)
                        }
                    })

            if (flowingDevMode) {
                developerSettings()
            }
        }
    }
}

@Composable
fun DeveloperSettings(
    configRepo: SharedPrefsConfigRepository,
    navigateToOnboarding: () -> Unit,
    navigateToAuditLogs: () -> Unit,
    clearWorkManager: () -> Unit,
    auditRepository: AuditRepository,
    photoPrismApp: PhotoPrismApp,
    /** TODO pass in work manager livedata or flow **/
    workManagerJobCountFlow: Flow<Int>,
    photoServer: PhotoServer,
) {
    Text(text = "Debug Settings", modifier = Modifier.padding(vertical = 4.dp))
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .border(BorderStroke(1.dp, Color.DarkGray))
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = navigateToOnboarding) {
                Text(text = "Onboarding")
            }
            TestConnectionButton(photoServer)
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