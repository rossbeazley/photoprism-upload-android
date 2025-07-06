package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ulk.co.rossbeazley.photoprism.upload.Event
import ulk.co.rossbeazley.photoprism.upload.FullSyncState
import ulk.co.rossbeazley.photoprism.upload.PartialSyncState
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.UploadQueueEntry

@Composable
fun SyncQueue(
    photoPrismApp: PhotoPrismApp,
    startState: MutableMap<String, UploadQueueEntry> = mutableMapOf()
) {
    MaterialTheme {
        val syncQueue by photoPrismApp.observeSyncEvents()
            .map { event: Event ->
                when (event) {
                    is FullSyncState -> {
                        startState.clear()
                        startState.putAll(event.events.associateBy { it.filePath })
                    }

                    is PartialSyncState -> startState[event.event.filePath] = event.event
                }
                startState.values.toList()
            }
            .collectAsStateWithLifecycle(initialValue = emptyList())

        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(syncQueue) { log ->
                Column {
                    Text(
                        text = log.javaClass.simpleName,
                        fontSize = 8.sp,
                    )
                    Text(
                        text = log.filePath,
                        fontSize = 8.sp,
                    )
                    when (log) {
                        is CompletedFileUpload -> Icon(
                            painter = painterResource(id = R.drawable.ic_completed_24),
                            contentDescription = null,
                        )

                        is FailedFileUpload -> Button(onClick = {
                            coroutineScope.launch { photoPrismApp.importPhoto(log.filePath) }
                        }) {
                            Text(text = "Retry")
                        }

                        else -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_uploading_24),
                                contentDescription = null,
                            )
                            Text(
                                text = log.attemptCount.toString(),
                                fontSize = 8.sp,
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
}