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

/** Passos de uma mesma subcategoria. [name] vazio = passos sem subcategoria. */
data class StepGroup(val name: String, val steps: List<Step>) {
    val total: Int get() = steps.size
    val completed: Int get() = steps.count { it.isCompleted }
    val progress: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

/** Dentro de um grupo: pendentes primeiro (ordem manual), concluídos no fim. */
private val stepOrder = compareBy<Step>({ it.isCompleted }, { it.position }, { it.id })
private val manualStepOrder = compareBy<Step>({ it.position }, { it.id })

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

    /**
     * Pendentes primeiro (na ordem manual); concluídos vão para o fim da fila.
     * O `id` desempata posições iguais (dois aparelhos criando passos offline), mantendo a
     * ordem estável entre emissões do Room.
     */
    val orderedSteps: List<Step>
        get() = steps.sortedWith(stepOrder)

    /** Há algum passo com subcategoria? Sem isso a tela mostra a lista simples, sem cabeçalhos. */
    val hasSubcategories: Boolean get() = steps.any { it.subcategory.isNotBlank() }

    /**
     * Passos agrupados por subcategoria. Os grupos seguem a ordem do seu passo de menor posição
     * (trocar passos de lugar dentro do grupo não embaralha os grupos); em cada grupo, pendentes
     * primeiro e concluídos no fim. A grafia exibida não muda ao reordenar (vem do menor id).
     */
    val stepGroups: List<StepGroup>
        get() = steps
            .groupBy { subcategoryKey(it.subcategory) }
            .values
            .map { members ->
                val anchor = members.minWith(manualStepOrder)          // define a posição do grupo
                val displayName = members.minBy { it.id }.subcategory.trim() // grafia estável
                anchor to StepGroup(displayName, members.sortedWith(stepOrder))
            }
            .sortedWith(compareBy<Pair<Step, StepGroup>>({ it.first.position }, { it.first.id }))
            .map { it.second }

    /** Nomes das subcategorias em uso (sem repetição, sem o grupo "sem subcategoria"). */
    val subcategories: List<String>
        get() = stepGroups.map { it.name }.filter { it.isNotBlank() }
}
