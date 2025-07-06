package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
data object ClearLogs
data object AddPhoto
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
    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FABMenu(backStack, auditRepository, doAddPhoto)
        }) {
        NavDisplay(
            modifier = Modifier.padding(it),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                // Slide in from right when navigating forward
                slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            },
            entryProvider = entryProvider {
                entry<Home> {
                    PhotoPrismWebApp(appUrlString = "https://photo.rossbeazley.co.uk/")
                }

                entry<AuditLogs> {
                    AuditLogsList(auditRepository)
                }

                entry<Settings> {
                    SettingsScreen(configRepo = configRepository, clearWorkManager = {
                        workManager.cancelAllWork()
                        auditRepository.log(Debug("Cleared work manager"))
                    }, navigateToAuditLogs = { backStack.add(AuditLogs) })
                }

                entry<SyncLogs> {
                    SyncQueue(app)
                }
            })
    }
}