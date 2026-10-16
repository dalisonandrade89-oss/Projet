package com.projet.app.data.repository

import com.projet.app.data.local.ProjectDao
import com.projet.app.data.local.StepEntity
import com.projet.app.data.local.ProjectEntity
import com.projet.app.data.local.toDomain
import com.projet.app.domain.model.Project
import com.projet.app.domain.repository.ProjectRepository
import com.projet.app.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    override suspend fun reorderProjects(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            val current = dao.getProject(id) ?: return@forEachIndexed
            if (current.position != index) dao.upsertProject(current.touch(position = index))
        }
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
