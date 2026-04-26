package cat.ri.noko.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import cat.ri.noko.R
import kotlinx.coroutines.delay

@Composable
fun CountdownDeleteDialog(
    showCountdown: Boolean,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var countdown by remember { mutableStateOf(if (showCountdown) 4 else 0) }

    LaunchedEffect(Unit) {
        if (showCountdown) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = text,
        confirmButton = {
            TextButton(
                enabled = countdown == 0,
                onClick = onConfirm,
            ) {
                Text(
                    if (countdown > 0) stringResource(R.string.countdown_delete_pending, countdown)
                    else stringResource(R.string.common_delete),
                    color = if (countdown == 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
