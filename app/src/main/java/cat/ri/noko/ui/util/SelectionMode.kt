package cat.ri.noko.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class SelectionModeState {
    var selectedIds by mutableStateOf(emptySet<String>())
    val isActive: Boolean get() = selectedIds.isNotEmpty()
    fun toggle(id: String) { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }
    fun select(id: String) { selectedIds = selectedIds + id }
    fun clear() { selectedIds = emptySet() }
    fun isSelected(id: String) = id in selectedIds
}

@Composable
fun rememberSelectionMode(): SelectionModeState = remember { SelectionModeState() }
