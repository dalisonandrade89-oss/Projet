package com.projet.app.domain.model

/** Passo/tarefa de um projeto (modelo de domínio, independente do Room). */
data class Step(
    val id: String,
    val projectId: String,
    val title: String,
    val isCompleted: Boolean,
    val position: Int,
    /** Subcategoria (ex.: "Mecânica", "Estética"); vazio = sem subcategoria. */
    val subcategory: String = "",
    val completedAt: Long? = null
)

/** Chave de comparação: "mecânica", "Mecânica " e "MECÂNICA" são a mesma subcategoria. */
fun subcategoryKey(raw: String): String = raw.trim().lowercase()
