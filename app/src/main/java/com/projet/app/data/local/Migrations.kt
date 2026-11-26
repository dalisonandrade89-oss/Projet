package com.projet.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: subcategoria nos passos. Os passos existentes ficam com "" (sem subcategoria),
 * então nada muda para quem já tem dados. NÃO usar fallbackToDestructiveMigration: apagaria
 * os projetos do aparelho.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE steps ADD COLUMN subcategory TEXT NOT NULL DEFAULT ''")
    }
}
