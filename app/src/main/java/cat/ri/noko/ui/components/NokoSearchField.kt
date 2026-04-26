package cat.ri.noko.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import cat.ri.noko.R
import cat.ri.noko.ui.theme.NokoFieldShape

@Composable
fun NokoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    showLeadingIcon: Boolean = true,
    showClearButton: Boolean = true,
    focusManager: FocusManager? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = if (showLeadingIcon) {
            {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
        trailingIcon = if (showClearButton && value.isNotEmpty()) {
            {
                IconButton(onClick = {
                    onValueChange("")
                    focusManager?.clearFocus()
                }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.common_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else null,
        singleLine = true,
        shape = NokoFieldShape,
        modifier = modifier,
    )
}
