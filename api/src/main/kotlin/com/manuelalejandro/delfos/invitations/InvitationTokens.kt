package com.manuelalejandro.delfos.invitations

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Invitation tokens: 128 random bits handed out once in a URL-safe form, stored only as a SHA-256 hash. */
object InvitationTokens {

    private const val TOKEN_BYTES = 16
    private val random = SecureRandom()

    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
