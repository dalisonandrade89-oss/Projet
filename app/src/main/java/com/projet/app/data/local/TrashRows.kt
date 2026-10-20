package com.projet.app.data.local

import androidx.room.Embedded
import com.projet.app.domain.model.TrashItem

/** Projeto excluído + quantidade de passos ativos que voltam junto com ele. */
data class DeletedProjectRow(
    @Embedded val project: ProjectEntity,
    val stepCount: Int
)

/** Passo excluído + nome do projeto (ainda ativo) a que pertence. */
data class DeletedStepRow(
    @Embedded val step: StepEntity,
    val projectName: String
)

fun DeletedProjectRow.toTrashItem(): TrashItem = TrashItem.DeletedProject(
    id = project.id,
    name = project.name,
    stepCount = stepCount,
    deletedAt = project.updatedAt
)

fun DeletedStepRow.toTrashItem(): TrashItem = TrashItem.DeletedStep(
    id = step.id,
    title = step.title,
    projectName = projectName,
    deletedAt = step.updatedAt
)
