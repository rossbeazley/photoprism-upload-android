package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ulk.co.rossbeazley.photoprism.upload.BuildConfig
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository
import ulk.co.rossbeazley.photoprism.upload.photoserver.PhotoServer

@Composable
fun OnboardingScreen(
    configRepo: SharedPrefsConfigRepository,
    photoServer: PhotoServer,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .photoPrismBackground()
        ) {
            Text(
                "Lets get you connected to your server",
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp)
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    label = { Text("Server hostname") },
                    value = serverurl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("hostname no scheme") },
                    onValueChange = { serverurl = it },

                    )
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text(text = "Username") }
                )
                TextField(
                    value = password,
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = { Text(text = "Password") }
                )

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                    TestConnectionButton(photoServer)
                    Button(onClick = {
                        serverurl = BuildConfig.webdavHostName
                        username = BuildConfig.webdavUserName
                        password = BuildConfig.webdavPassword
                    }, modifier = Modifier.padding(start = 4.dp)) {
                        Text(text = "Load Defaults")
                    }
                }

                Button(
                    onClick = updateSettings,
                    modifier = Modifier
                        .fillMaxWidth(0.66f)
                        .padding(vertical = 24.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "Save and Continue")
                }
            }
        }
    }
}

