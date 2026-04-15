package cat.ri.noko.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.ui.theme.NokoFieldShape
import cat.ri.noko.ui.util.rememberNokoHaptics
import cat.ri.noko.ui.util.shake
import kotlinx.coroutines.launch

@Composable
fun ExportPassphraseDialog(
    exportCount: Int,
    onConfirm: (passphrase: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var passphraseConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (exportCount == 1) "Export as .nokc"
                else "Export $exportCount characters as .nokc",
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (exportCount == 1) "Set a passphrase to encrypt this character."
                    else "Set a passphrase to encrypt ${exportCount} characters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        if (it.length <= 256) {
                            passphrase = it
                            error = null
                        }
                    },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    shape = NokoFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.toInt(), 0) },
                )
                OutlinedTextField(
                    value = passphraseConfirm,
                    onValueChange = {
                        if (it.length <= 256) {
                            passphraseConfirm = it
                            error = null
                        }
                    },
                    label = { Text("Confirm passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { err -> { Text(err) } },
                    shape = NokoFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.toInt(), 0) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        passphrase.length < 8 -> {
                            error = "At least 8 characters"
                            scope.launch {
                                haptics.reject()
                                shakeOffset.shake()
                            }
                        }
                        passphrase != passphraseConfirm -> {
                            error = "Passphrases don't match"
                            scope.launch {
                                haptics.reject()
                                shakeOffset.shake()
                            }
                        }
                        else -> {
                            onConfirm(passphrase)
                        }
                    }
                },
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
