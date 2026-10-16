package com.projet.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.projet.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long
)

@Serializable
private data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0
)

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAsset> = emptyList()
)

/**
 * Auto-update in-app via GitHub Releases:
 * 1) [checkForUpdate] consulta `/releases/latest` e compara a tag com BuildConfig.VERSION_NAME;
 * 2) [download] baixa o `.apk` anexado à release para cacheDir/updates;
 * 3) [install] abre o instalador do sistema (atualiza "por cima", preservando dados).
 *
 * ATENÇÃO: o Android só instala por cima se o APK novo tiver a MESMA assinatura do instalado.
 * Por isso o CI assina o release com um keystore fixo (ver README).
 */
class AppUpdaterService(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val release = json.decodeFromString<GhRelease>(response.body!!.string())
                val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                if (release.draft || release.prerelease || apk == null) return@use null
                val remoteVersion = release.tag.removePrefix("v")
                if (!isNewer(remoteVersion, BuildConfig.VERSION_NAME)) return@use null
                UpdateInfo(remoteVersion, release.body.orEmpty(), apk.url, apk.size)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // sem rede / repo privado / rate limit: simplesmente não oferece update
        }
    }

    /** Baixa o APK reportando progresso (0f..1f). */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "projet-update.apk")
            val request = Request.Builder().url(info.apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download falhou: HTTP ${response.code}")
                val body = response.body ?: throw IOException("Resposta vazia")
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var done = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            done += read
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            target
        }

    /**
     * Abre o instalador. Retorna `false` se o usuário ainda precisa liberar
     * "instalar apps desconhecidos" para o Projet (a tela de ajustes é aberta).
     */
    fun install(apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    /** Compara versões "1.2.3" numericamente (ignora sufixos como "-beta"). */
    private fun isNewer(remote: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val c = parts(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
