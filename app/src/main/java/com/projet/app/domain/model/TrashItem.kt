package com.projet.app.domain.model

import java.util.concurrent.TimeUnit

/** Regras da Lixeira: quanto tempo um item excluído fica recuperável. */
object TrashPolicy {
    const val RETENTION_DAYS = 30

    private val DAY_MS = TimeUnit.DAYS.toMillis(1)
    val RETENTION_MS = TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())

    /** Tombstones com `updatedAt` anterior a este instante já expiraram. */
    fun cutoff(now: Long = System.currentTimeMillis()): Long = now - RETENTION_MS

    /** Dias restantes (arredondado para cima); 0 = já expirou e só aguarda a limpeza. */
    fun daysLeft(deletedAt: Long, now: Long = System.currentTimeMillis()): Int {
        val remaining = deletedAt + RETENTION_MS - now
        if (remaining <= 0) return 0
        return ((remaining + DAY_MS - 1) / DAY_MS).toInt()
    }
}

/** Item exibido na Lixeira: um projeto inteiro ou um passo excluído de um projeto ainda ativo. */
sealed interface TrashItem {
    val id: String
    val deletedAt: Long

    data class DeletedProject(
        override val id: String,
        val name: String,
        val stepCount: Int,
        override val deletedAt: Long
    ) : TrashItem

    data class DeletedStep(
        override val id: String,
        val title: String,
        val projectName: String,
        override val deletedAt: Long
    ) : TrashItem
}
