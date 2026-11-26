package com.projet.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projet.app.R
import com.projet.app.domain.model.Project
import com.projet.app.domain.model.StepGroup
import com.projet.app.domain.model.TrashPolicy
import com.projet.app.domain.model.subcategoryKey
import com.projet.app.ui.components.AnimatedProgress
import com.projet.app.ui.components.StepItem
import com.projet.app.ui.components.SubcategoryPicker
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: ProjectDetailViewModel = koinViewModel(parameters = { parametersOf(projectId) })
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    // Modo organizar: setas de prioridade + subcategoria em cada passo (sobrevive à rotação).
    var organizing by rememberSaveable { mutableStateOf(false) }

    val title = (ui as? DetailUiState.Content)?.project?.name?.ifBlank { "Projeto" } ?: "Projeto"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { organizing = !organizing }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sort),
                            contentDescription = stringResource(R.string.organize_steps),
                            tint = if (organizing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir projeto")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = ui) {
            DetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            // Cobre exclusão local e remota (via sync): a tela simplesmente fecha, uma única vez.
            DetailUiState.NotFound -> LaunchedEffect(Unit) { onBack() }

            is DetailUiState.Content -> DetailContent(
                project = state.project,
                organizing = organizing,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir projeto?") },
            text = { Text(stringResource(R.string.delete_project_message, TrashPolicy.RETENTION_DAYS)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteProject()
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DetailContent(
    project: Project,
    organizing: Boolean,
    viewModel: ProjectDetailViewModel,
    modifier: Modifier
) {
    // Estado local dos campos (inicializado uma vez por projeto) evita "pulos" de cursor;
    // cada alteração é enviada ao Auto-Save com debounce.
    var name by remember(project.id) { mutableStateOf(project.name) }
    var category by remember(project.id) { mutableStateOf(project.category) }
    var description by remember(project.id) { mutableStateOf(project.description) }
    var newStep by remember { mutableStateOf("") }
    // Lembra a subcategoria escolhida para cadastrar vários passos seguidos no mesmo grupo.
    var newStepSubcategory by remember(project.id) { mutableStateOf("") }

    val groups = project.stepGroups
    val subcategoryNames = groups.map { it.name }.filter { it.isNotBlank() }
    // Sem nenhuma subcategoria a lista é simples (sem cabeçalhos), como antes.
    val showHeaders = project.hasSubcategories

    fun autoSave() = viewModel.updateInfo(name, category, description)
    fun submitStep() {
        viewModel.addStep(newStep, newStepSubcategory)
        newStep = ""
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; autoSave() },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it; autoSave() },
                label = { Text("Categoria / classe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; autoSave() },
                label = { Text("Descrição") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            ProgressSummary(project)
        }
        if (organizing) {
            item {
                Text(
                    text = stringResource(R.string.organize_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        groups.forEach { group ->
            if (showHeaders) {
                item(key = "group:${subcategoryKey(group.name)}") {
                    GroupHeader(group, Modifier.animateItem())
                }
            }
            // Só pendentes se movem; as setas respeitam as pontas do grupo.
            val pending = group.steps.filter { !it.isCompleted }
            // key = id + animateItem(): ao concluir/mover, o passo desliza até o novo lugar.
            items(group.steps, key = { it.id }) { step ->
                val index = pending.indexOfFirst { it.id == step.id }
                StepItem(
                    step = step,
                    onToggle = { viewModel.setStepCompleted(step.id, it) },
                    onTitleChange = { viewModel.updateStepTitle(step.id, it) },
                    onDelete = { viewModel.deleteStep(step.id) },
                    modifier = Modifier.animateItem(),
                    organizing = organizing,
                    subcategoryOptions = subcategoryNames,
                    canMoveUp = index > 0,
                    canMoveDown = index >= 0 && index < pending.lastIndex,
                    onMove = { delta -> viewModel.moveStep(step.id, delta) },
                    onSubcategoryChange = { viewModel.setStepSubcategory(step.id, it) }
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newStep,
                    onValueChange = { newStep = it },
                    label = { Text("Novo passo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitStep() }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = ::submitStep, enabled = newStep.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar passo")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.subcategory_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                )
                SubcategoryPicker(
                    current = newStepSubcategory,
                    options = subcategoryNames,
                    onSelect = { newStepSubcategory = it }
                )
            }
        }
    }
}

/** Cabeçalho de subcategoria: nome, contagem e uma barrinha de progresso só do grupo. */
@Composable
private fun GroupHeader(group: StepGroup, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = group.name.ifBlank { stringResource(R.string.no_subcategory) },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${group.completed}/${group.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { group.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

/** Resumo de progresso do projeto (barra animada + contagem). */
@Composable
private fun ProgressSummary(project: Project) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedProgress(project.progress)
        Text(
            text = "${project.completedSteps} de ${project.totalSteps} passos concluídos · ${project.progressPercent}%",
            style = MaterialTheme.typography.labelLarge
        )
    }
}
