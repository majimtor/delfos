package com.manuelalejandro.delfos.evaluation

import kotlinx.serialization.Serializable

/** The answer to one question; exactly one value field applies per question type. Mirrors `Answer` in the contract. */
@Serializable
public data class Answer(
    val questionKey: String,
    val optionKey: String? = null,
    val optionKeys: List<String>? = null,
    val number: Double? = null,
    val text: String? = null,
)
