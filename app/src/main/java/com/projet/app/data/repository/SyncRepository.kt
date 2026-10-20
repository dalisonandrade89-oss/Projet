package com.projet.app.data.repository

import android.app.PendingIntent
import com.projet.app.data.local.ProjectDao
import com.projet.app.data.remote.AuthState
import com.projet.app.data.remote.BackupFile
import com.projet.app.data.remote.DriveAuthManager
import com.projet.app.data.remote.GoogleDriveService
import com.projet.app.domain.model.TrashPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SyncResult {
    data object Success : SyncResult
    data class NeedsConsent(val pendingIntent: PendingIntent) : SyncResult
    data class Error(val message: String) : SyncResult
}

/**
 * Sincronização offline-first: o Room é a fonte da verdade; o Drive é um espelho.
 *
 * Fluxo: limpa a Lixeira local vencida → baixa o JSON remoto → faz merge com o banco local
 * (Last-Write-Wins por `updatedAt`, desempate por `version`, depois prefere o local) → descarta
 * tombstones vencidos (> [TrashPolicy.RETENTION_DAYS] dias) → grava no Room o que veio do remoto →
 * envia ao Drive apenas se o resultado difere do que já está lá (sync incremental).
 * Exclusões são tombstones (`isDeleted = true`), então também propagam.
 *
 * Trade-off conhecido: se um aparelho ficar offline por mais que o prazo da Lixeira com um
 * registro ativo que outro aparelho excluiu e já purgou, o registro "ressuscita" no próximo sync.
 */
class SyncRepository(
    private val dao: ProjectDao,
    private val drive: GoogleDriveService,
    private val auth: DriveAuthManager
) {
    private val mutex = Mutex() // evita duas sincronizações simultâneas

    suspend fun sync(): SyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                when (val state = auth.getAccessToken()) {
                    is AuthState.NeedsConsent -> SyncResult.NeedsConsent(state.pendingIntent)
                    is AuthState.Token -> {
                        runSync(state.accessToken)
                        SyncResult.Success
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Falha desconhecida na sincronização")
            }
        }
    }

    private suspend fun runSync(token: String) {
        val cutoff = TrashPolicy.cutoff()
        dao.purgeTrash(cutoff)

        val remote = drive.download(token)
        val localProjects = dao.getAllProjectsRaw()
        val localSteps = dao.getAllStepsRaw()

        // Tombstones vencidos não voltam do remoto e deixam de ser enviados ao Drive.
        val mergedProjects = mergeLww(localProjects, remote?.projects.orEmpty(),
            { it.id }, { it.updatedAt }, { it.version })
            .filterNot { it.isDeleted && it.updatedAt < cutoff }
        val validIds = mergedProjects.mapTo(HashSet()) { it.id }
        // Passos órfãos (projeto inexistente) são descartados para não violar a FK.
        val mergedSteps = mergeLww(localSteps, remote?.steps.orEmpty(),
            { it.id }, { it.updatedAt }, { it.version })
            .filter { it.projectId in validIds && !(it.isDeleted && it.updatedAt < cutoff) }

        // 1) Remoto → local: só o que mudou.
        val localPSet = localProjects.toSet()
        val localSSet = localSteps.toSet()
        val toWriteP = mergedProjects.filter { it !in localPSet }
        val toWriteS = mergedSteps.filter { it !in localSSet }
        if (toWriteP.isNotEmpty() || toWriteS.isNotEmpty()) dao.applyMerged(toWriteP, toWriteS)

        // 2) Local → remoto: só se o Drive ainda não reflete o estado mesclado.
        val upToDate = remote != null &&
            remote.projects.toSet() == mergedProjects.toSet() &&
            remote.steps.toSet() == mergedSteps.toSet()
        if (!upToDate) {
            drive.upload(token, BackupFile(
                exportedAt = System.currentTimeMillis(),
                projects = mergedProjects,
                steps = mergedSteps
            ))
        }
    }

    /** Last-Write-Wins genérico: vence o maior updatedAt; empate → maior version; empate → local. */
    private fun <T> mergeLww(
        local: List<T>, remote: List<T>,
        id: (T) -> String, updatedAt: (T) -> Long, version: (T) -> Int
    ): List<T> {
        val result = HashMap<String, T>(local.size + remote.size)
        local.forEach { result[id(it)] = it }
        remote.forEach { r ->
            val l = result[id(r)]
            val remoteWins = l == null ||
                updatedAt(r) > updatedAt(l) ||
                (updatedAt(r) == updatedAt(l) && version(r) > version(l))
            if (remoteWins) result[id(r)] = r
        }
        return result.values.toList()
    }
}
