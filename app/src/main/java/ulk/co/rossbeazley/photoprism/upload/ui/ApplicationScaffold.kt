package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.work.WorkManager
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import ulk.co.rossbeazley.photoprism.upload.config.SharedPrefsConfigRepository

data object Home
data object SyncLogs
data object AuditLogs
data object Settings

@Composable
fun ApplicationScaffold(
    doAddPhoto: () -> Unit,
    workManager: WorkManager,
    auditRepository: AuditRepository,
    app: PhotoPrismApp,
    configRepository: SharedPrefsConfigRepository
) {
    val backStack = remember { mutableStateListOf<Any>(Home) }

    var extraAction by remember { mutableStateOf<MenuItem?>(null) }

    val menuItems = mutableListOf(
        MenuItem(label = "Home", icon = Icons.Filled.AccountBox) {
            backStack.clear()
            backStack.add(Home)
            extraAction = null
        },
        MenuItem(label = "Import Photo", icon = Icons.Filled.Add) {
            doAddPhoto()
        },
        MenuItem(label = "Sync Queue", icon = Icons.Filled.Menu) {
            backStack.add(SyncLogs)
            extraAction = MenuItem(label = "Clear Sync Queue", icon = Icons.Filled.Clear) {
                app.clearSyncQueue()
            }
        },
        MenuItem(label = "Settings", icon = Icons.Filled.Settings) {
            backStack.add(Settings)
            extraAction = null
        },
    )

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = { FABMenu(menuItems, extraAction) }
    ) {
        NavDisplay(
            modifier = Modifier.padding(it),
            backStack = backStack,
            transitionSpec = {
                // Slide in from right when navigating forward
                slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
            },
            onBack = {
                repeat(it) { backStack.removeAt(backStack.lastIndex) }
                extraAction = null // if we pop back to something with an extra action, how do we show it?
                // we could pass in a lambda to set this action so the screen always sets it ?
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
            entryProvider = entryProvider {
                entry<Home> {
                    PhotoPrismWebApp(appUrlString = "https://photo.rossbeazley.co.uk/library/browse")
                }

                entry<AuditLogs> {
                    AuditLogsList(auditRepository)
                }

                entry<Settings> {
                    SettingsScreen(
                        configRepo = configRepository,
                        clearWorkManager = {
                            workManager.cancelAllWork()
                            auditRepository.log(Debug("Cleared work manager"))
                        },
                        navigateToAuditLogs = {
                            backStack.add(AuditLogs)
                            extraAction = MenuItem(label = "Clear Debug Logs", icon = Icons.Filled.Clear) {
                                auditRepository.clearAll()
                                auditRepository.log(Debug("Cleared logs"))
                            }
                        },
                        auditRepository = auditRepository,
                        photoPrismApp = app,
                    )
                }

                entry<SyncLogs> {
                    SyncQueue(app)
                }
            })
    }
}