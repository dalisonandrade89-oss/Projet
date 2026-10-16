package com.projet.app.data.remote

import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

sealed interface AuthState {
    data class Token(val accessToken: String) : AuthState
    /** O usuário ainda precisa conceder acesso: a UI deve lançar o [pendingIntent]. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthState
}

/**
 * Obtém o access token OAuth do Drive com escopo `drive.appdata` (pasta privada do app).
 * Usa o AuthorizationClient (Google Play Services), sucessor do GoogleSignIn legado.
 * Pré-requisito: um OAuth Client ID "Android" no Google Cloud (package + SHA-1) — ver README.
 */
class DriveAuthManager(private val context: Context) {

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
        .build()

    suspend fun getAccessToken(): AuthState {
        val result = Identity.getAuthorizationClient(context).authorize(request).await()
        val pendingIntent = result.pendingIntent
        return if (result.hasResolution() && pendingIntent != null) {
            AuthState.NeedsConsent(pendingIntent)
        } else {
            AuthState.Token(result.accessToken ?: error("Token de acesso ausente"))
        }
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
