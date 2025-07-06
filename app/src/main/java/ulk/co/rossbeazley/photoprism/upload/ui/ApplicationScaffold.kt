package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.layout.PaddingValues
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
import ulk.co.rossbeazley.photoprism.upload.ui.SettingsScreen


data object Home
data object ClearLogs
data object AddPhoto
data object SyncLogs
data object AuditLogs
data object Settings

@Composable
fun FabPage(
    backStack: MutableList<Any>,
    auditRepository: AuditRepository,
    doAddPhoto: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FABMenu(backStack, auditRepository, doAddPhoto)
        }
    ) {
        content(it)
    }
}

@Composable
fun ApplicationScaffold(
    doAddPhoto: () -> Unit,
    workManager: WorkManager,
    auditRepository: AuditRepository,
    app: PhotoPrismApp,
    configRepository: SharedPrefsConfigRepository
) {

    val backStack = remember { mutableStateListOf<Any>(Home) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                FabPage(backStack, auditRepository, doAddPhoto, {
                    PhotoPrismWebApp(
                        modifier = Modifier.padding(it),
                        appUrlString = "https://photo.rossbeazley.co.uk/",
                    )
                })
            }

            entry<AuditLogs> {
                FabPage(backStack, auditRepository, doAddPhoto) {
                    AuditLogsList(
                        auditRepository,
                        modifier = Modifier.padding(it),
                    )
                }
            }

            entry<Settings> {
                FabPage(backStack, auditRepository, doAddPhoto) {
                    SettingsScreen(
                        configRepo = configRepository,
                        clearWorkManager = {
                            workManager.cancelAllWork()
                            auditRepository.log(Debug("Cleared work manager"))
                        },
                        navigateToAuditLogs = { backStack.add(AuditLogs) }
                    )
                }
            }

            entry<SyncLogs> {
                FabPage(backStack, auditRepository, doAddPhoto) {
                    SyncQueue(
                        app,
                    )
                }
            }




        }
    )


}