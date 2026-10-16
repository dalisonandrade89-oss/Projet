package com.projet.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Projeto persistido no Room.
 *
 * - [id] é UUID (gerado no cliente) para permitir merge entre dispositivos sem colisão.
 * - [version] é incrementada a cada edição (desempate no merge).
 * - [updatedAt] (epoch ms) é a base do Last-Write-Wins.
 * - [isDeleted] = soft delete (tombstone), para que exclusões também sejam sincronizadas.
 * Também é @Serializable porque o mesmo objeto vai para o JSON do Drive.
 */
@Serializable
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "",
    val description: String = "",
    val position: Int = 0,
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
