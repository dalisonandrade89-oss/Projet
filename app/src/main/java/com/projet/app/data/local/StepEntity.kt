package com.projet.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** Passo/tarefa de um projeto. Mesmas regras de versão/soft delete de [ProjectEntity]. */
@Serializable
@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class StepEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val completedAt: Long? = null,
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
