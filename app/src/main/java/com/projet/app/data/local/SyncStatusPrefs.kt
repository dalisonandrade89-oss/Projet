package com.projet.app.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda quando o último sync bem-sucedido terminou (manual ou em segundo plano). */
class SyncStatusPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _lastSyncAt = MutableStateFlow(
        prefs.takeIf { it.contains(KEY_LAST_SYNC) }?.getLong(KEY_LAST_SYNC, 0L)
    )
    /** Epoch ms do último sync concluído; `null` = ainda não sincronizou neste aparelho. */
    val lastSyncAt: StateFlow<Long?> = _lastSyncAt.asStateFlow()

    fun markSynced(at: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_LAST_SYNC, at) }
        _lastSyncAt.value = at
    }

    private companion object {
        const val FILE = "projet_sync"
        const val KEY_LAST_SYNC = "last_sync_at"
    }
}
