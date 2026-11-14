package com.manuelalejandro.delfos.mobile.session

import com.manuelalejandro.delfos.mobile.api.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal sealed interface SessionState {
    data object SignedOut : SessionState

    data class SignedIn(val username: String, val isAdmin: Boolean, val token: String) : SessionState
}

/** What the store keeps: the bearer token and who it belongs to, so that the list opens without a round trip. */
@Serializable
private data class StoredSession(val token: String, val username: String, val isAdmin: Boolean)

/**
 * The session of the manager using the application. It is restored from the store at start and
 * dropped when the manager logs out or when the server answers that the session is gone.
 */
internal class SessionManager(private val store: TokenStore) {
    private val json = Json { ignoreUnknownKeys = true }
    private val state0 = MutableStateFlow(restore())
    val state: StateFlow<SessionState> = state0.asStateFlow()

    val token: String?
        get() = (state0.value as? SessionState.SignedIn)?.token

    val signedIn: Boolean
        get() = state0.value is SessionState.SignedIn

    fun signIn(session: Session) {
        val token = requireNotNull(session.token) { "a bearer session carries its token" }
        store.write(json.encodeToString(StoredSession.serializer(), StoredSession(token, session.username, session.isAdmin)))
        state0.value = SessionState.SignedIn(session.username, session.isAdmin, token)
    }

    fun signOut() {
        store.clear()
        state0.value = SessionState.SignedOut
    }

    private fun restore(): SessionState {
        val stored = store.read() ?: return SessionState.SignedOut
        val session = try {
            json.decodeFromString(StoredSession.serializer(), stored)
        } catch (e: IllegalArgumentException) {
            store.clear()
            return SessionState.SignedOut
        }
        return SessionState.SignedIn(session.username, session.isAdmin, session.token)
    }
}
