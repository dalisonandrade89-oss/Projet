package com.projet.app.data.repository

import com.projet.app.data.local.ProjectDao
import com.projet.app.data.local.ProjectEntity
import com.projet.app.data.local.StepEntity
import com.projet.app.data.local.toDomain
import com.projet.app.data.local.toTrashItem
import com.projet.app.domain.model.Project
import com.projet.app.domain.model.TrashItem
import com.projet.app.domain.model.TrashPolicy
import com.projet.app.domain.repository.ProjectRepository
import com.projet.app.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositório unificado (Room + agendamento de sync).
 *
 * Auto-Save: edições de texto são "debounced" por chave (ex.: "p:<id>", "s:<id>"). Cada nova
 * edição cancela a gravação pendente e reinicia o timer; após [DEBOUNCE_MS] sem digitação,
 * grava. O escopo é do app (não da tela), então a gravação sobrevive ao fechamento da tela.
 * Toda gravação incrementa `version`/`updatedAt` e agenda um sync em segundo plano.
 *
 * Lixeira: excluir = soft delete (tombstone). Restaurar desfaz o tombstone com `version`/`updatedAt`
 * novos, então a restauração também vence no merge entre aparelhos.
 */
class ProjectRepositoryImpl(
    private val dao: ProjectDao,
    private val syncScheduler: SyncScheduler,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ProjectRepository {

    private val pending = ConcurrentHashMap<String, Job>()

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeProjects().map { list -> list.map { it.toDomain() } }

    override fun observeProject(id: String): Flow<Project?> =
        dao.observeProject(id).map { it?.toDomain() }

    override suspend fun createProject(name: String, category: String, description: String): String {
        val entity = ProjectEntity(
            name = name.trim(),
            category = category.trim(),
            description = description.trim(),
            position = dao.maxProjectPosition() + 1
        )
        dao.upsertProject(entity)
        syncScheduler.schedule()
        return entity.id
    }

    override suspend fun deleteProject(id: String) {
        val current = dao.getProject(id) ?: return
        dao.upsertProject(current.touch(isDeleted = true))
        syncScheduler.schedule()
    }

    /**
     * Uma única leitura + um único `upsertProjects` (o Room executa o upsert de lista numa
     * transação): a UI não "pisca" com estados intermediários e o app morrer no meio não deixa
     * a ordem pela metade.
     */
    override suspend fun reorderProjects(orderedIds: List<String>) {
        val byId = dao.getAllProjectsRaw().associateBy { it.id }
        val changed = orderedIds.mapIndexedNotNull { index, id ->
            byId[id]?.takeIf { it.position != index }?.touch(position = index)
        }
        if (changed.isEmpty()) return
        dao.upsertProjects(changed)
        syncScheduler.schedule()
    }

    override fun updateProjectInfo(id: String, name: String, category: String, description: String) {
        debounced("p:$id") {
            val current = dao.getProject(id) ?: return@debounced
            dao.upsertProject(current.touch(name = name, category = category, description = description))
        }
    }

    override suspend fun addStep(projectId: String, title: String) {
        dao.upsertStep(
            StepEntity(
                projectId = projectId,
                title = title,
                position = dao.maxStepPosition(projectId) + 1
            )
        )
        syncScheduler.schedule()
    }

    override suspend fun setStepCompleted(stepId: String, completed: Boolean) {
        val current = dao.getStep(stepId) ?: return
        val now = System.currentTimeMillis()
        dao.upsertStep(
            current.copy(
                isCompleted = completed,
                completedAt = if (completed) now else null,
                version = current.version + 1,
                updatedAt = now
            )
        )
        syncScheduler.schedule()
    }

    override suspend fun deleteStep(stepId: String) {
        val current = dao.getStep(stepId) ?: return
        dao.upsertStep(
            current.copy(isDeleted = true, version = current.version + 1, updatedAt = System.currentTimeMillis())
        )
        syncScheduler.schedule()
    }

    override fun updateStepTitle(stepId: String, title: String) {
        debounced("s:$stepId") {
            val current = dao.getStep(stepId) ?: return@debounced
            dao.upsertStep(
                current.copy(title = title, version = current.version + 1, updatedAt = System.currentTimeMillis())
            )
        }
    }

    // ---------------------------------------------------------------- Lixeira

    override fun observeTrash(): Flow<List<TrashItem>> = combine(
        dao.observeDeletedProjects(),
        dao.observeDeletedSteps()
    ) { projects, steps ->
        val items: List<TrashItem> = projects.map { it.toTrashItem() } + steps.map { it.toTrashItem() }
        items.sortedByDescending { it.deletedAt }
    }

    /** O projeto volta para o fim da lista (posição nova), com os passos que tinha. */
    override suspend fun restoreProject(id: String) {
        val current = dao.getProject(id) ?: return
        if (!current.isDeleted) return
        dao.upsertProject(current.touch(isDeleted = false, position = dao.maxProjectPosition() + 1))
        syncScheduler.schedule()
    }

    /** O passo volta para o fim da lista de passos do projeto. */
    override suspend fun restoreStep(stepId: String) {
        val current = dao.getStep(stepId) ?: return
        if (!current.isDeleted) return
        dao.upsertStep(
            current.copy(
                isDeleted = false,
                position = dao.maxStepPosition(current.projectId) + 1,
                version = current.version + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        syncScheduler.schedule()
    }

    override suspend fun purgeExpiredTrash() {
        dao.purgeTrash(TrashPolicy.cutoff())
    }

    // ---------------------------------------------------------------- helpers

    private fun debounced(key: String, block: suspend () -> Unit) {
        pending[key]?.cancel()
        val job = scope.launch {
            delay(DEBOUNCE_MS)
            block()
            syncScheduler.schedule()
        }
        pending[key] = job
        job.invokeOnCompletion { pending.remove(key, job) }
    }

    private fun ProjectEntity.touch(
        name: String = this.name,
        category: String = this.category,
        description: String = this.description,
        position: Int = this.position,
        isDeleted: Boolean = this.isDeleted
    ) = copy(
        name = name, category = category, description = description,
        position = position, isDeleted = isDeleted,
        version = version + 1, updatedAt = System.currentTimeMillis()
    )

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
