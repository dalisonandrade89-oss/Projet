package com.projet.app.ui.trash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projet.app.R
import com.projet.app.domain.model.TrashItem
import com.projet.app.domain.model.TrashPolicy
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** Lixeira: projetos e passos excluídos, recuperáveis por [TrashPolicy.RETENTION_DAYS] dias. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel()
) {
    val trash by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val projectRestored = stringResource(R.string.trash_project_restored)
    val stepRestored = stringResource(R.string.trash_step_restored)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.trash_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val list = trash
        when {
            list == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            list.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.trash_empty), style = MaterialTheme.typography.bodyLarge)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.trash_info, TrashPolicy.RETENTION_DAYS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(list, key = { it.id }) { item ->
                    TrashRow(
                        item = item,
                        onRestore = {
                            viewModel.restore(item)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(
                                    if (item is TrashItem.DeletedProject) projectRestored else stepRestored
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashRow(item: TrashItem, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val unnamed = stringResource(R.string.trash_unnamed)
    val title: String
    val detail: String
    when (item) {
        is TrashItem.DeletedProject -> {
            title = item.name.ifBlank { unnamed }
            detail = if (item.stepCount == 0) {
                stringResource(R.string.trash_project_no_steps)
            } else {
                pluralStringResource(R.plurals.trash_project_steps, item.stepCount, item.stepCount)
            }
        }
        is TrashItem.DeletedStep -> {
            title = item.title.ifBlank { unnamed }
            detail = stringResource(R.string.trash_step_of, item.projectName.ifBlank { unnamed })
        }
    }

    val daysLeft = TrashPolicy.daysLeft(item.deletedAt)
    val expiry = if (daysLeft == 0) {
        stringResource(R.string.trash_expiring)
    } else {
        pluralStringResource(R.plurals.trash_days_left, daysLeft, daysLeft)
    }
    val restoreDescription = stringResource(R.string.trash_restore_item, title)

    ListItem(
        modifier = modifier,
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("$detail\n$expiry") },
        trailingContent = {
            TextButton(
                onClick = onRestore,
                modifier = Modifier.semantics { contentDescription = restoreDescription }
            ) { Text(stringResource(R.string.trash_restore)) }
        }
    )
}
