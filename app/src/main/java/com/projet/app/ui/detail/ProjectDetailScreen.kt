package com.projet.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.projet.app.domain.model.Project
import com.projet.app.ui.components.AnimatedProgress
import com.projet.app.ui.components.StepItem
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: ProjectDetailViewModel = koinViewModel(parameters = { parametersOf(projectId) })
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name?.ifBlank { "Projeto" } ?: "Projeto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir projeto")
                    }
                }
            )
        }
    ) { padding ->
        val current = project
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            DetailContent(current, viewModel, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir projeto?") },
            text = { Text("O projeto e seus passos serão removidos (inclusive dos outros dispositivos após a sincronização).") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteProject(onBack)
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DetailContent(project: Project, viewModel: ProjectDetailViewModel, modifier: Modifier) {
    // Estado local dos campos (inicializado uma vez por projeto) evita "pulos" de cursor;
    // cada alteração é enviada ao Auto-Save com debounce.
    var name by remember(project.id) { mutableStateOf(project.name) }
    var category by remember(project.id) { mutableStateOf(project.category) }
    var description by remember(project.id) { mutableStateOf(project.description) }
    var newStep by remember { mutableStateOf("") }

    fun autoSave() = viewModel.updateInfo(name, category, description)
    fun submitStep() {
        viewModel.addStep(newStep)
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
        // key = id + animateItem(): ao concluir, o passo é reordenado para o fim e desliza até lá.
        items(project.orderedSteps, key = { it.id }) { step ->
            StepItem(
                step = step,
                onToggle = { viewModel.setStepCompleted(step.id, it) },
                onTitleChange = { viewModel.updateStepTitle(step.id, it) },
                onDelete = { viewModel.deleteStep(step.id) },
                modifier = Modifier.animateItem()
            )
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
    }
}

/** Resumo de progresso do projeto (barra animada + contagem). */
@Composable
private fun ProgressSummary(project: Project) {
    androidx.compose.foundation.layout.Column(
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
