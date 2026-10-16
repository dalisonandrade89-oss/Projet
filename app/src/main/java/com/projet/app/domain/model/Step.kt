package com.projet.app.domain.model

/** Passo/tarefa de um projeto (modelo de domínio, independente do Room). */
data class Step(
    val id: String,
    val projectId: String,
    val title: String,
    val isCompleted: Boolean,
    val position: Int,
    val completedAt: Long? = null
)
