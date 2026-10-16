package com.projet.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projet.app.update.UpdateInfo

/** Diálogo "Nova versão disponível". [progress] != null indica download em andamento. */
@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    progress: Float?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val downloading = progress != null
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("Nova versão ${info.version}") },
        text = {
            Column {
                if (info.notes.isNotBlank()) {
                    Text(
                        text = info.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    Text("Uma atualização do Projet está disponível.")
                }
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = !downloading) {
                Text(if (downloading) "Baixando…" else "Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !downloading) { Text("Depois") }
        }
    )
}
