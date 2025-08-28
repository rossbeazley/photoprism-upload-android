package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import ulk.co.rossbeazley.photoprism.upload.Event
import ulk.co.rossbeazley.photoprism.upload.FullSyncState
import ulk.co.rossbeazley.photoprism.upload.PartialSyncState
import ulk.co.rossbeazley.photoprism.upload.PhotoPrismApp
import ulk.co.rossbeazley.photoprism.upload.R
import ulk.co.rossbeazley.photoprism.upload.syncqueue.CompletedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.FailedFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RetryFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.RunningFileUpload
import ulk.co.rossbeazley.photoprism.upload.syncqueue.ScheduledFileUpload
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

        var imagePath by rememberSaveable { mutableStateOf("") }
        if (imagePath.isNotEmpty()) {
            MinimalDialog(imagePath) {
                imagePath = ""
            }
        }

        val coroutineScope = rememberCoroutineScope()
        LazyColumn(
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .photoPrismBackground()
                .windowInsetsPadding(WindowInsets.statusBars)

        ) {
            items(syncQueue) { log: UploadQueueEntry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(log.filePath)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .width(80.dp)
                                .height(80.dp)
                                .clickable {
                                    imagePath = log.filePath
                                }
                        )

                        Column {
                            Row {
                                Text(text = log.toViewData(), fontSize = 16.sp)
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
                                        Text(text = log.attemptCount.toString(), fontSize = 8.sp)
                                    }
                                }
                            }
                            Text(text = log.filePath, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalDialog(
    filePath: String,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                //.height(200.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(filePath)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .zoomable(rememberZoomableState())
            )
        }
    }
}

fun UploadQueueEntry.toViewData(): String =
    when (this) {
        is CompletedFileUpload -> "Upload complete"
        is FailedFileUpload -> "Upload failed"
        is RetryFileUpload -> "Retrying"
        is RunningFileUpload -> "Uploading..."
        is ScheduledFileUpload -> "Queued for upload"
    }
