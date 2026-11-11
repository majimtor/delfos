package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.domain.VersionStatus
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findByUsernameIgnoreCase(username: String): AccountEntity?
}

interface SessionRepository : JpaRepository<SessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): SessionEntity?
}

interface SurveyRepository : JpaRepository<SurveyEntity, UUID> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): SurveyEntity?
    fun findByPublicId(publicId: String): SurveyEntity?

    /** First page of a manager's surveys, most recently updated first; the id breaks ties for a stable keyset. */
    @Query("select s from SurveyEntity s where s.ownerId = :ownerId and s.status in :statuses order by s.updatedAt desc, s.id desc")
    fun findFirstPage(@Param("ownerId") ownerId: UUID, @Param("statuses") statuses: Collection<SurveyStatus>, limit: Limit): List<SurveyEntity>

    @Query(
        "select s from SurveyEntity s where s.ownerId = :ownerId and s.status in :statuses " +
            "and (s.updatedAt < :updatedAt or (s.updatedAt = :updatedAt and s.id < :id)) order by s.updatedAt desc, s.id desc",
    )
    fun findPageAfter(
        @Param("ownerId") ownerId: UUID,
        @Param("statuses") statuses: Collection<SurveyStatus>,
        @Param("updatedAt") updatedAt: Instant,
        @Param("id") id: UUID,
        limit: Limit,
    ): List<SurveyEntity>
}

interface SurveyVersionRepository : JpaRepository<SurveyVersionEntity, UUID> {
    fun findBySurveyIdAndNumber(surveyId: UUID, number: Int): SurveyVersionEntity?
    fun findBySurveyIdAndStatus(surveyId: UUID, status: VersionStatus): SurveyVersionEntity?
    fun findAllBySurveyIdOrderByNumberDesc(surveyId: UUID): List<SurveyVersionEntity>
    fun findBySurveyIdOrderByNumberDesc(surveyId: UUID, limit: Limit): List<SurveyVersionEntity>
    fun findBySurveyIdAndNumberLessThanOrderByNumberDesc(surveyId: UUID, number: Int, limit: Limit): List<SurveyVersionEntity>
}

interface QuestionRepository : JpaRepository<QuestionEntity, UUID> {
    fun countByVersionId(versionId: UUID): Long
}

interface SurveyResponseRepository : JpaRepository<SurveyResponseEntity, UUID> {
    fun countByVersionId(versionId: UUID): Long

    @Query("select count(r) from SurveyResponseEntity r where r.versionId in (select v.id from SurveyVersionEntity v where v.surveyId = :surveyId)")
    fun countBySurveyId(@Param("surveyId") surveyId: UUID): Long
}
