package com.projet.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projet.app.domain.model.Project
import com.projet.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Distingue "carregando" de "não existe mais" (excluído aqui ou em outro aparelho via sync). */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Content(val project: Project) : DetailUiState
}

/** Sem botão "salvar": tudo é persistido instantaneamente (ou com debounce, no caso de texto). */
class ProjectDetailViewModel(
    private val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = repository.observeProject(projectId)
        .map<Project?, DetailUiState> { project ->
            if (project == null) DetailUiState.NotFound else DetailUiState.Content(project)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)

    fun updateInfo(name: String, category: String, description: String) =
        repository.updateProjectInfo(projectId, name, category, description)

    fun addStep(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addStep(projectId, title.trim()) }
    }

    fun setStepCompleted(stepId: String, completed: Boolean) {
        viewModelScope.launch { repository.setStepCompleted(stepId, completed) }
    }

    fun updateStepTitle(stepId: String, title: String) = repository.updateStepTitle(stepId, title)

    fun deleteStep(stepId: String) {
        viewModelScope.launch { repository.deleteStep(stepId) }
    }

    /**
     * Sem callback de navegação: ao excluir, o Flow emite `null` → [DetailUiState.NotFound] e a
     * tela fecha sozinha (um único caminho de saída, sem pop duplo).
     */
    fun deleteProject() {
        viewModelScope.launch { repository.deleteProject(projectId) }
    }
}
