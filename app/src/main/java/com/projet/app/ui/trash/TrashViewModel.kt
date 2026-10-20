package com.projet.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projet.app.domain.model.TrashItem
import com.projet.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: ProjectRepository) : ViewModel() {

    /** `null` = carregando; lista vazia = lixeira vazia. */
    val items: StateFlow<List<TrashItem>?> = repository.observeTrash()
        .stateIn<List<TrashItem>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.DeletedProject -> repository.restoreProject(item.id)
                is TrashItem.DeletedStep -> repository.restoreStep(item.id)
            }
        }
    }
}
