package cat.ri.noko.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.model.PersonaEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaPickerSheet(
    title: String,
    entries: List<PersonaEntry>,
    selectedId: String?,
    onSelect: (PersonaEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = remember(entries, searchQuery) {
            if (searchQuery.isBlank()) entries
            else entries.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }

            if (entries.size >= 5) {
                item {
                    NokoSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search..",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        showLeadingIcon = false,
                        showClearButton = false,
                    )
                }
            }

            if (entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No entries yet. Create one in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    val isSelected = entry.id == selectedId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entry) },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NokoAvatar(
                                entry = entry,
                                fallbackIcon = Icons.Filled.Person,
                                size = 40,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (entry.description.isNotBlank()) {
                                    Text(
                                        entry.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
