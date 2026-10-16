package com.projet.app.ui.list

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projet.app.data.repository.SyncRepository
import com.projet.app.data.repository.SyncResult
import com.projet.app.domain.model.Project
import com.projet.app.domain.model.SortOption
import com.projet.app.domain.repository.ProjectRepository
import com.projet.app.update.AppUpdaterService
import com.projet.app.update.UpdateInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUi(val info: UpdateInfo? = null, val progress: Float? = null)

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val sort: SortOption = SortOption.MANUAL,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val isSyncing: Boolean = false,
    val update: UpdateUi = UpdateUi()
)

class ProjectListViewModel(
    private val repository: ProjectRepository,
    private val syncRepository: SyncRepository,
    private val updater: AppUpdaterService
) : ViewModel() {

    private val sort = MutableStateFlow(SortOption.MANUAL)
    private val category = MutableStateFlow<String?>(null)
    private val syncing = MutableStateFlow(false)
    private val updateUi = MutableStateFlow(UpdateUi())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _consentRequests = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    /** A UI coleta e lança o PendingIntent da tela de consentimento do Google. */
    val consentRequests: SharedFlow<PendingIntent> = _consentRequests.asSharedFlow()

    val uiState: StateFlow<ProjectListUiState> = combine(
        repository.observeProjects(), sort, category, syncing, updateUi
    ) { projects, sortOption, selectedCategory, isSyncing, update ->
        val categories = projects.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        val filtered = selectedCategory?.let { c -> projects.filter { it.category == c } } ?: projects
        ProjectListUiState(
            projects = applySort(filtered, sortOption),
            sort = sortOption,
            categories = categories,
            selectedCategory = selectedCategory,
            isSyncing = isSyncing,
            update = update
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectListUiState())

    init {
        checkForUpdate()
    }

    private fun applySort(list: List<Project>, option: SortOption): List<Project> = when (option) {
        SortOption.MANUAL -> list.sortedBy { it.position }
        SortOption.NAME_ASC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortOption.NAME_DESC -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).reversed()
        SortOption.CATEGORY -> list.sortedWith(
            compareBy<Project, String>(String.CASE_INSENSITIVE_ORDER) { it.category }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
        SortOption.PROGRESS_ASC -> list.sortedBy { it.progress }
        SortOption.PROGRESS_DESC -> list.sortedByDescending { it.progress }
    }

    fun setSort(option: SortOption) { sort.value = option }
    fun setCategory(value: String?) { category.value = value }

    fun createProject(name: String, category: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createProject(name, category, description) }
    }

    /** Reordenação manual: troca o projeto com o vizinho (delta = -1 sobe, +1 desce). */
    fun moveProject(id: String, delta: Int) {
        viewModelScope.launch {
            val ordered = repository.observeProjects().first().sortedBy { it.position }.map { it.id }.toMutableList()
            val from = ordered.indexOf(id)
            val to = from + delta
            if (from < 0 || to !in ordered.indices) return@launch
            ordered.add(to, ordered.removeAt(from))
            repository.reorderProjects(ordered)
        }
    }

    fun syncNow() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            try {
                when (val result = syncRepository.sync()) {
                    SyncResult.Success -> _messages.emit("Sincronizado com o Google Drive")
                    is SyncResult.NeedsConsent -> _consentRequests.emit(result.pendingIntent)
                    is SyncResult.Error -> _messages.emit("Erro ao sincronizar: ${result.message}")
                }
            } finally {
                syncing.value = false
            }
        }
    }

    // ------------------------------------------------------------ auto-update
    private fun checkForUpdate() {
        viewModelScope.launch { updateUi.value = UpdateUi(info = updater.checkForUpdate()) }
    }

    fun dismissUpdate() { updateUi.value = UpdateUi() }

    fun startUpdate() {
        val info = updateUi.value.info ?: return
        viewModelScope.launch {
            updateUi.update { it.copy(progress = 0f) }
            try {
                val apk = updater.download(info) { p -> updateUi.update { it.copy(progress = p) } }
                if (updater.install(apk)) {
                    updateUi.value = UpdateUi()
                } else {
                    updateUi.update { it.copy(progress = null) }
                    _messages.emit("Permita \"instalar apps desconhecidos\" para o Projet e toque em Atualizar novamente")
                }
            } catch (e: Exception) {
                updateUi.update { it.copy(progress = null) }
                _messages.emit("Falha ao atualizar: ${e.message}")
            }
        }
    }
}
