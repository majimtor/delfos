package com.manuelalejandro.delfos.mobile

import kotlinx.serialization.Serializable

@Serializable
internal data object HomeRoute

/** The answer flow of one survey, with the invitation token when the link carried one. */
@Serializable
public data class AnswerRoute(
    val publicId: String,
    val invitation: String? = null,
)

private val PUBLIC_ID = Regex("^[A-Za-z0-9_-]{8,32}$")

/**
 * Reads a survey code or a survey link as the web publishes them, `<origin>/s/<publicId>` with an
 * optional `invitation` query parameter. Returns null when the text is neither.
 */
public fun parseSurveyLink(input: String): AnswerRoute? {
    val text = input.trim()
    if (PUBLIC_ID.matches(text)) return AnswerRoute(text)
    val marker = text.indexOf("/s/")
    if (marker < 0) return null
    val rest = text.substring(marker + 3)
    val publicId = rest.takeWhile { it != '?' && it != '/' && it != '#' }
    if (!PUBLIC_ID.matches(publicId)) return null
    val query = rest.substringAfter('?', "").substringBefore('#')
    val invitation = query.split('&')
        .map { it.substringBefore('=') to it.substringAfter('=', "") }
        .firstOrNull { (name, value) -> name == "invitation" && value.isNotEmpty() }
        ?.second
    return AnswerRoute(publicId, invitation)
}
