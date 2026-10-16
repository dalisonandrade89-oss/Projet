package com.projet.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projet.app.domain.model.Project
import com.projet.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sem botão "salvar": tudo é persistido instantaneamente (ou com debounce, no caso de texto). */
class ProjectDetailViewModel(
    private val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    val project: StateFlow<Project?> = repository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    fun deleteProject(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            onDone()
        }
    }
}
