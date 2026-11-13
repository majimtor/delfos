package com.manuelalejandro.delfos.domain

enum class AccountStatus { ACTIVE, DISABLED }

enum class SurveyStatus { DRAFT, PUBLISHED, CLOSED }

enum class RespondentMode { ANONYMOUS, INVITED }

enum class VersionStatus { DRAFT, PUBLISHED, SUPERSEDED }

enum class InvitationStatus { PENDING, USED, REVOKED }

enum class Language(val code: String) {
    ES("es"),
    EN("en"),
    ;

    companion object {
        fun fromCode(code: String): Language = entries.first { it.code == code }
    }
}
