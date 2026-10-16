package com.projet.app.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.projet.app.domain.model.Project
import com.projet.app.domain.model.Step

/** Relação 1-N projeto → passos (resolvida pelo Room). */
data class ProjectWithSteps(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val steps: List<StepEntity>
)

/** Converte para o modelo de domínio, ocultando passos com soft delete. */
fun ProjectWithSteps.toDomain(): Project = Project(
    id = project.id,
    name = project.name,
    category = project.category,
    description = project.description,
    position = project.position,
    updatedAt = project.updatedAt,
    steps = steps.filter { !it.isDeleted }.map { it.toDomain() }
)

fun StepEntity.toDomain(): Step = Step(
    id = id,
    projectId = projectId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    completedAt = completedAt
)
