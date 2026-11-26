package com.projet.app.domain.repository

import com.projet.app.domain.model.Project
import com.projet.app.domain.model.TrashItem
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>
    fun observeProject(id: String): Flow<Project?>

    suspend fun createProject(name: String, category: String, description: String): String
    suspend fun deleteProject(id: String)
    suspend fun reorderProjects(orderedIds: List<String>)

    /** Auto-Save com debounce: pode ser chamado a cada tecla; só grava após uma pausa. */
    fun updateProjectInfo(id: String, name: String, category: String, description: String)

    suspend fun addStep(projectId: String, title: String, subcategory: String = "")
    suspend fun setStepCompleted(stepId: String, completed: Boolean)
    suspend fun deleteStep(stepId: String)

    /** Auto-Save com debounce do título do passo. */
    fun updateStepTitle(stepId: String, title: String)

    /** Define a subcategoria do passo (vazio = nenhuma). O passo mantém sua posição. */
    suspend fun setStepSubcategory(stepId: String, subcategory: String)

    /**
     * Prioridade manual: sobe (-1) ou desce (+1) o passo entre os pendentes da sua subcategoria.
     * Concluídos ficam sempre no fim e não se movem.
     */
    suspend fun moveStep(stepId: String, delta: Int)

    // ---------------------------------------------------------------- Lixeira (30 dias)

    /** Projetos e passos excluídos ainda recuperáveis, do mais recente para o mais antigo. */
    fun observeTrash(): Flow<List<TrashItem>>
    suspend fun restoreProject(id: String)
    suspend fun restoreStep(stepId: String)

    /** Apaga definitivamente (local) o que passou de [com.projet.app.domain.model.TrashPolicy.RETENTION_DAYS]. */
    suspend fun purgeExpiredTrash()
}
