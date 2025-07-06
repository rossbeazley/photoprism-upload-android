package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import ulk.co.rossbeazley.photoprism.upload.AppSingleton
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FABMenu(backStack: MutableList<Any>, audits: AuditRepository, doAddPhoto: () -> Unit) {

    val items =
        listOf(
            Icons.Filled.AccountBox to "Home",
            Icons.Filled.Clear to "Clear Logs",
            Icons.Filled.Add to "Add Photo",
            Icons.Filled.Search to "Sync Logs",
            Icons.Filled.Delete to "Clear Sync Queue",
            Icons.Filled.Face to "Audit Logs",
            Icons.Filled.Settings to "Settings",
        )
    val listState = rememberLazyListState()
    val fabVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    FloatingActionButtonMenu(
        //modifier = Modifier.align(Alignment.BottomEnd),
        expanded = fabMenuExpanded,
        button = {
            ToggleFloatingActionButton(
                modifier =
                    Modifier
                        .semantics {
                            traversalIndex = -1f
                            stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Toggle menu"
                        }
                        .animateFloatingActionButton(
                            visible = fabVisible || fabMenuExpanded,
                            alignment = Alignment.BottomEnd,
                        ),
                checked = fabMenuExpanded,
                onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
            ) {
                val imageVector by remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) Icons.Filled.Clear else Icons.Filled.Add
                    }
                }
                Icon(
                    painter = rememberVectorPainter(imageVector),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        val photoPrismApp =
            (LocalContext.current.applicationContext as AppSingleton).photoPrismApp

        items.forEachIndexed { i, item ->
            FloatingActionButtonMenuItem(
                modifier =
                    Modifier.semantics {
                        isTraversalGroup = true
                        // Add a custom a11y action to allow closing the menu when focusing
                        // the last menu item, since the close button comes before the first
                        // menu item in the traversal order.
                        if (i == items.size - 1) {
                            customActions =
                                listOf(
                                    CustomAccessibilityAction(
                                        label = "Close menu",
                                        action = {
                                            fabMenuExpanded = false
                                            true
                                        },
                                    )
                                )
                        }
                    },
                onClick = {
                    fabMenuExpanded = false
                    when (item.second) {
                        "Home" -> backStack.add(Home)
                        "Clear Logs" -> {
                            audits.clearAll()
                            audits.log(Debug("Cleared logs"))
                        }

                        "Clear Sync Queue" -> {
                            photoPrismApp.clearSyncQueue()
                        }

                        "Add Photo" -> doAddPhoto()
                        "Sync Logs" -> backStack.add(SyncLogs)
                        "Audit Logs" -> backStack.add(AuditLogs)
                        "Settings" -> backStack.add(Settings)
                    }

                },
                icon = { Icon(item.first, contentDescription = null) },
                text = { Text(text = item.second) },
            )
        }
    }
}