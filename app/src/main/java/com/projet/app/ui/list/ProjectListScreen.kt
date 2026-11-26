package com.projet.app.ui.list

import android.app.Activity
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projet.app.BuildConfig
import com.projet.app.R
import com.projet.app.domain.model.SortOption
import com.projet.app.domain.search.ProjectSearch
import com.projet.app.ui.components.ProjectCard
import com.projet.app.ui.components.UpdateAvailableDialog
import com.projet.app.ui.theme.ColorOption
import com.projet.app.ui.theme.ThemePalettes
import com.projet.app.ui.theme.ThemeSettings
import com.projet.app.ui.theme.readableOn
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (String) -> Unit,
    onOpenTrash: () -> Unit,
    viewModel: ProjectListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val theme by viewModel.themeSettings.collectAsStateWithLifecycle()
    val syncNotice by viewModel.syncNotice.collectAsStateWithLifecycle()
    val lastSyncAt by viewModel.lastSyncAt.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Pesquisa: o texto fica aqui (campo sem atraso/pulo de cursor) e é repassado ao ViewModel,
    // que filtra. rememberSaveable mantém a busca aberta ao girar a tela.
    var searching by rememberSaveable { mutableStateOf(false) }
    var queryText by rememberSaveable { mutableStateOf("") }
    val tokens = remember(searching, queryText) {
        if (searching) ProjectSearch.tokens(queryText) else emptyList()
    }
    val searchActive = tokens.isNotEmpty()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun updateQuery(text: String) {
        queryText = text
        viewModel.setSearchQuery(text)
    }
    fun closeSearch() {
        searching = false
        updateQuery("")
    }
    BackHandler(enabled = searching) { closeSearch() }
    // runCatching: se o campo ainda não estiver pronto, abrir a pesquisa nunca deve derrubar o app.
    LaunchedEffect(searching) { if (searching) runCatching { focusRequester.requestFocus() } }
    // Ressincroniza o ViewModel (ex.: processo recriado) com o que o campo mostra.
    LaunchedEffect(Unit) { viewModel.setSearchQuery(if (searching) queryText else "") }

    // Consentimento do Google (Drive appData): lança a tela e, se aprovado, refaz o sync.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.syncNow() else viewModel.onConsentDenied()
    }

    LaunchedEffect(Unit) {
        viewModel.consentRequests.collect { consentLauncher.launch(IntentSenderRequest.Builder(it).build()) }
    }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (searching) {
                        IconButton(onClick = ::closeSearch) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.search_close))
                        }
                    }
                },
                title = {
                    if (searching) {
                        TextField(
                            value = queryText,
                            onValueChange = ::updateQuery,
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                        Text("Projet")
                    }
                },
                actions = {
                    if (searching) {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { updateQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear))
                            }
                        }
                    } else {
                        // Lupa ao lado do botão de ordenar.
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_open))
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.sort_projects)
                                )
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(if (option == state.sort) "✓ ${option.label}" else option.label) },
                                        onClick = {
                                            viewModel.setSort(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = { VersionFooter() },
        // Dois FABs: a linha ocupa a largura toda (FabPosition.Center = sem deslocamento),
        // engrenagem à esquerda e "+" à direita, ambos com o visual padrão de FloatingActionButton.
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FloatingActionButton(onClick = { showSettings = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_open))
                        // Sincronizando: um anel gira em volta da engrenagem.
                        if (state.isSyncing) CircularProgressIndicator(Modifier.size(40.dp), strokeWidth = 2.dp)
                    }
                }
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Novo projeto")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Filtro por categoria (escondido durante a pesquisa, que olha todos os projetos)
            if (!searching && state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.setCategory(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(state.categories) { c ->
                        FilterChip(
                            selected = state.selectedCategory == c,
                            onClick = { viewModel.setCategory(c) },
                            label = { Text(c) }
                        )
                    }
                }
            }

            if (state.projects.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchActive) {
                            stringResource(R.string.search_no_results, queryText.trim())
                        } else {
                            "Nenhum projeto ainda.\nToque em + para criar o primeiro 🚀"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                // Reordenar com a lista filtrada pela pesquisa trocaria posições de projetos ocultos.
                val manual = state.sort == SortOption.MANUAL && state.selectedCategory == null && !searchActive
                LazyColumn(
                    // Folga embaixo para o último card não ficar escondido atrás dos FABs.
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (searchActive) {
                        item(key = "search-count") {
                            Text(
                                text = stringResource(R.string.search_count, state.projects.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(state.projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            modifier = Modifier.animateItem(),
                            onMoveUp = if (manual) ({ viewModel.moveProject(project.id, -1) }) else null,
                            onMoveDown = if (manual) ({ viewModel.moveProject(project.id, +1) }) else null,
                            highlightTokens = tokens,
                            matchedSteps = ProjectSearch.matchingSteps(project, tokens)
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            isSyncing = state.isSyncing,
            lastSyncAt = lastSyncAt,
            theme = theme,
            onDismiss = { showSettings = false },
            // Fecha a folha ao sincronizar: o popup "Sincronizando…" aparece na hora e vira o
            // resultado; o anel na engrenagem também indica o andamento.
            onSync = {
                showSettings = false
                viewModel.syncNow()
            },
            onOpenTrash = {
                showSettings = false
                onOpenTrash()
            },
            onBackground = viewModel::setBackgroundColor,
            onAccent = viewModel::setAccentColor
        )
    }

    // Popup do resultado da sincronização manual (sucesso ou falha).
    syncNotice?.let { notice ->
        SyncNoticeDialog(notice = notice, onDismiss = viewModel::dismissSyncNotice)
    }

    if (showCreate) {
        CreateProjectDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, category, description ->
                viewModel.createProject(name, category, description)
                showCreate = false
            }
        )
    }

    state.update.info?.let { info ->
        UpdateAvailableDialog(
            info = info,
            progress = state.update.progress,
            onUpdate = viewModel::startUpdate,
            onDismiss = viewModel::dismissUpdate
        )
    }
}

@Composable
private fun SyncNoticeDialog(notice: SyncNotice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            when (notice) {
                SyncNotice.InProgress -> CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                SyncNotice.Success -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                else -> Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                stringResource(
                    when (notice) {
                        SyncNotice.InProgress -> R.string.sync_progress_title
                        SyncNotice.Success -> R.string.sync_done_title
                        else -> R.string.sync_failed_title
                    }
                )
            )
        },
        text = {
            Text(
                when (notice) {
                    SyncNotice.InProgress -> stringResource(R.string.sync_progress_message)
                    SyncNotice.Success -> stringResource(R.string.sync_done_message)
                    SyncNotice.ConsentDenied -> stringResource(R.string.sync_consent_denied)
                    SyncNotice.Timeout -> stringResource(R.string.sync_timeout)
                    is SyncNotice.Failure -> stringResource(R.string.sync_failed_message, notice.message)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (notice == SyncNotice.InProgress) R.string.action_hide else R.string.action_ok))
            }
        }
    )
}

/** Rodapé discreto com a versão instalada (BuildConfig.VERSION_NAME, definida pelo CI). */
@Composable
private fun VersionFooter() {
    Text(
        text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
    )
}

/** Folha de ajustes aberta pela engrenagem: sincronizar, lixeira e cores. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    isSyncing: Boolean,
    lastSyncAt: Long?,
    theme: ThemeSettings,
    onDismiss: () -> Unit,
    onSync: () -> Unit,
    onOpenTrash: () -> Unit,
    onBackground: (Int?) -> Unit,
    onAccent: (Int?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ListItem(
                modifier = Modifier.clickable(enabled = !isSyncing, onClick = onSync),
                colors = rowColors,
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.sync_drive)) },
                supportingContent = {
                    val context = LocalContext.current
                    Text(
                        when {
                            isSyncing -> stringResource(R.string.sync_drive_running)
                            lastSyncAt != null -> stringResource(
                                R.string.sync_last,
                                DateUtils.formatDateTime(
                                    context,
                                    lastSyncAt,
                                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
                                )
                            )
                            else -> stringResource(R.string.sync_drive_hint)
                        }
                    )
                }
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenTrash),
                colors = rowColors,
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.trash_title)) }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            ColorSection(
                title = stringResource(R.string.theme_background),
                options = ThemePalettes.backgrounds,
                selected = theme.background,
                onSelect = onBackground
            )
            ColorSection(
                title = stringResource(R.string.theme_accent),
                options = ThemePalettes.accents,
                selected = theme.accent,
                onSelect = onAccent
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(
    title: String,
    options: List<ColorOption>,
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            // "Padrão" = volta ao visual do sistema (Dynamic Color, claro/escuro automático).
            TextButton(onClick = { onSelect(null) }, enabled = selected != null) {
                Text(stringResource(R.string.theme_default))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                ColorSwatch(
                    option = option,
                    selected = selected == option.color.toArgb(),
                    onClick = { onSelect(option.color.toArgb()) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(option: ColorOption, selected: Boolean, onClick: () -> Unit) {
    val name = stringResource(option.nameRes)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(option.color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = option.color.readableOn())
        }
    }
}

@Composable
private fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo projeto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Categoria") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, category, description) }, enabled = name.isNotBlank()) {
                Text("Criar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
