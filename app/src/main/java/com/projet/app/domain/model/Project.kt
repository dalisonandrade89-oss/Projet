package com.projet.app.domain.model

import kotlin.math.roundToInt

/** Critérios de ordenação da lista de projetos. */
enum class SortOption(val label: String) {
    MANUAL("Posição manual"),
    NAME_ASC("Nome (A–Z)"),
    NAME_DESC("Nome (Z–A)"),
    CATEGORY("Categoria"),
    PROGRESS_ASC("Progresso (0–100%)"),
    PROGRESS_DESC("Progresso (100–0%)")
}

data class Project(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val position: Int,
    val updatedAt: Long,
    val steps: List<Step>
) {
    val totalSteps: Int get() = steps.size
    val completedSteps: Int get() = steps.count { it.isCompleted }

    /** Progress (%) = (Passos Concluídos / Total de Passos) * 100 — aqui como fração 0f..1f. */
    val progress: Float
        get() = if (totalSteps == 0) 0f else completedSteps.toFloat() / totalSteps

    val progressPercent: Int get() = (progress * 100f).roundToInt()

    /** Pendentes primeiro (na ordem manual); concluídos vão para o fim da fila. */
    val orderedSteps: List<Step>
        get() = steps.sortedWith(compareBy<Step>({ it.isCompleted }, { it.position }))
}
