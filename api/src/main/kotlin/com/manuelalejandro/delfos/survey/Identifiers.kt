package com.manuelalejandro.delfos.survey

import java.security.SecureRandom

/** Server-generated identifiers: URL-safe public ids for respondents and stable keys for questions and options. */
object Identifiers {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val PUBLIC_ID_LENGTH = 12
    private const val KEY_LENGTH = 8
    private val random = SecureRandom()

    fun newPublicId(): String = randomString(PUBLIC_ID_LENGTH)

    fun newQuestionKey(): String = "q-" + randomString(KEY_LENGTH)

    fun newOptionKey(): String = "o-" + randomString(KEY_LENGTH)

    private fun randomString(length: Int): String = buildString(length) {
        repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }
}
