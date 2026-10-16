package com.projet.app.data.remote

import com.projet.app.data.local.ProjectEntity
import com.projet.app.data.local.StepEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** Conteúdo do arquivo `projects_backup.json` guardado no Drive. */
@Serializable
data class BackupFile(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val projects: List<ProjectEntity>,
    val steps: List<StepEntity>
)

@Serializable
private data class DriveFile(val id: String, val name: String = "")

@Serializable
private data class DriveFileList(val files: List<DriveFile> = emptyList())

/**
 * Upload/Download de `projects_backup.json` na `appDataFolder` via Drive REST v3
 * (pasta oculta, privada do app, não aparece no Drive do usuário).
 */
class GoogleDriveService(private val client: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonType = "application/json; charset=UTF-8".toMediaType()

    /** Retorna o backup remoto ou `null` se ainda não existir. */
    suspend fun download(accessToken: String): BackupFile? = withContext(Dispatchers.IO) {
        val fileId = findFileId(accessToken) ?: return@withContext null
        val request = Request.Builder()
            .url("$FILES_URL/$fileId?alt=media")
            .header("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive download: HTTP ${response.code}")
            json.decodeFromString<BackupFile>(response.body!!.string())
        }
    }

    /** Cria o arquivo na primeira vez; nas seguintes, atualiza o conteúdo. */
    suspend fun upload(accessToken: String, backup: BackupFile) = withContext(Dispatchers.IO) {
        val content = json.encodeToString(backup)
        val existingId = findFileId(accessToken)
        val request = if (existingId == null) {
            val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
            val body = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadata.toRequestBody(jsonType))
                .addPart(content.toRequestBody(jsonType))
                .build()
            Request.Builder().url("$UPLOAD_URL?uploadType=multipart").post(body)
        } else {
            Request.Builder()
                .url("$UPLOAD_URL/$existingId?uploadType=media")
                .patch(content.toRequestBody(jsonType))
        }.header("Authorization", "Bearer $accessToken").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive upload: HTTP ${response.code}")
        }
    }

    private fun findFileId(accessToken: String): String? {
        val url = FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$FILE_NAME' and trashed = false")
            .addQueryParameter("fields", "files(id,name)")
            .build()
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $accessToken").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive list: HTTP ${response.code}")
            json.decodeFromString<DriveFileList>(response.body!!.string()).files.firstOrNull()?.id
        }
    }

    private companion object {
        const val FILE_NAME = "projects_backup.json"
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
