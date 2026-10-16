package com.projet.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Consultas reativas (Flow): a UI reage a qualquer gravação, o que viabiliza o Auto-Save —
 * cada edição é gravada com @Upsert e a tela é atualizada automaticamente.
 */
@Dao
abstract class ProjectDao {

    @Transaction
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY position ASC")
    abstract fun observeProjects(): Flow<List<ProjectWithSteps>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id AND isDeleted = 0")
    abstract fun observeProject(id: String): Flow<ProjectWithSteps?>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract suspend fun getProject(id: String): ProjectEntity?

    @Query("SELECT * FROM steps WHERE id = :id")
    abstract suspend fun getStep(id: String): StepEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM projects WHERE isDeleted = 0")
    abstract suspend fun maxProjectPosition(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM steps WHERE projectId = :projectId AND isDeleted = 0")
    abstract suspend fun maxStepPosition(projectId: String): Int

    @Upsert abstract suspend fun upsertProject(project: ProjectEntity)
    @Upsert abstract suspend fun upsertProjects(projects: List<ProjectEntity>)
    @Upsert abstract suspend fun upsertStep(step: StepEntity)
    @Upsert abstract suspend fun upsertSteps(steps: List<StepEntity>)

    // --- Usadas pela sincronização (incluem tombstones) ---
    @Query("SELECT * FROM projects")
    abstract suspend fun getAllProjectsRaw(): List<ProjectEntity>

    @Query("SELECT * FROM steps")
    abstract suspend fun getAllStepsRaw(): List<StepEntity>

    /** Aplica o resultado do merge de forma atômica (projetos antes dos passos por causa da FK). */
    @Transaction
    open suspend fun applyMerged(projects: List<ProjectEntity>, steps: List<StepEntity>) {
        upsertProjects(projects)
        upsertSteps(steps)
    }
}
