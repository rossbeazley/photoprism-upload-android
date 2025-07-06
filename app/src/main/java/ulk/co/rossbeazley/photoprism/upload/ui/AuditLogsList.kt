package ulk.co.rossbeazley.photoprism.upload.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository

@Composable
fun AuditLogsList(
    audits: AuditRepository,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        val stateList by audits.observeLogs()
            .map { log ->
                log.split(")\n")
                    .filter { row -> row.isNotBlank() }
                    .map { row -> row.trim() }
                    .map { row -> row.replace("(", "\n") }
                    .map { row -> row.replace(", ", "\n") }
                    .reversed()
            }.collectAsStateWithLifecycle(initialValue = emptyList())

        val listState = rememberLazyListState()
        LazyColumn(
            modifier = modifier,
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(stateList) { log ->
                Text(
                    text = log,
                    modifier = Modifier.padding(10.dp)
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
            }
        }
    }
}