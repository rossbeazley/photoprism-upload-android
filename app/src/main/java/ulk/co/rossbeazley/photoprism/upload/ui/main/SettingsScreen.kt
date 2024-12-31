package ulk.co.rossbeazley.photoprism.upload.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository

@Composable
fun SettingsScreen(configRepo: SharedPrefsConfigRepository) {
    var serverurl by remember { mutableStateOf(configRepo.hostname) }
    var username by remember { mutableStateOf(configRepo.username) }
    var password by remember { mutableStateOf(configRepo.password) }
    val updateSettings = {
        configRepo.save(
            username = username,
            password = password,
            hostname = serverurl
        )
    }

    MaterialTheme {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            TextField(
                label = { Text("Server URL") },
                value = serverurl,
                modifier = Modifier.width(IntrinsicSize.Max),
                onValueChange = { serverurl = it }
            )
            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.width(IntrinsicSize.Max),
                label = { Text(text = "Username") }
            )
            TextField(
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { password = it },
                modifier = Modifier.width(IntrinsicSize.Max),
                label = { Text(text = "Password") }
            )
            Button(onClick = updateSettings) {
                Text(text = "Save")
            }
        }
    }
}