package com.manuelalejandro.delfos.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findByUsernameIgnoreCase(username: String): AccountEntity?
}

interface SessionRepository : JpaRepository<SessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): SessionEntity?
}

interface SurveyRepository : JpaRepository<SurveyEntity, UUID> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): SurveyEntity?
    fun findByPublicId(publicId: String): SurveyEntity?
}

interface SurveyVersionRepository : JpaRepository<SurveyVersionEntity, UUID> {
    fun findBySurveyIdAndNumber(surveyId: UUID, number: Int): SurveyVersionEntity?
    fun findBySurveyIdAndStatus(surveyId: UUID, status: com.manuelalejandro.delfos.domain.VersionStatus): SurveyVersionEntity?
    fun findAllBySurveyIdOrderByNumberDesc(surveyId: UUID): List<SurveyVersionEntity>
}

interface SurveyResponseRepository : JpaRepository<SurveyResponseEntity, UUID> {
    fun countByVersionId(versionId: UUID): Long
}
