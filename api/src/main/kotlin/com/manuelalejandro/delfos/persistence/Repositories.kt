package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.InvitationStatus
import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.domain.VersionStatus
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock

interface AccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findByUsernameIgnoreCase(username: String): AccountEntity?
    fun findAllByUsernameNotIn(usernames: Collection<String>): List<AccountEntity>

    @Query("select a from AccountEntity a order by a.createdAt desc, a.id desc")
    fun findFirstPage(limit: Limit): List<AccountEntity>

    @Query("select a from AccountEntity a where a.createdAt < :createdAt or (a.createdAt = :createdAt and a.id < :id) order by a.createdAt desc, a.id desc")
    fun findPageAfter(@Param("createdAt") createdAt: Instant, @Param("id") id: UUID, limit: Limit): List<AccountEntity>
}

interface SessionRepository : JpaRepository<SessionEntity, UUID> {
    fun findByTokenHash(tokenHash: String): SessionEntity?
    fun findAllByAccountId(accountId: UUID): List<SessionEntity>
}

interface SurveyRepository : JpaRepository<SurveyEntity, UUID> {
    fun findByIdAndOwnerId(id: UUID, ownerId: UUID): SurveyEntity?
    fun findByPublicId(publicId: String): SurveyEntity?
    fun findAllByOwnerIdIn(ownerIds: Collection<UUID>): List<SurveyEntity>

    /** The survey row locked for the rest of the transaction, so a response quota is never exceeded by concurrent submissions. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SurveyEntity s where s.publicId = :publicId")
    fun lockByPublicId(@Param("publicId") publicId: String): SurveyEntity?

    /** The owner's survey row locked for the rest of the transaction, so that the mutations of one survey are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SurveyEntity s where s.id = :id and s.ownerId = :ownerId")
    fun lockByIdAndOwnerId(@Param("id") id: UUID, @Param("ownerId") ownerId: UUID): SurveyEntity?

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

interface InvitationRepository : JpaRepository<InvitationEntity, UUID> {
    fun findByTokenHash(tokenHash: String): InvitationEntity?
    fun findByIdAndSurveyId(id: UUID, surveyId: UUID): InvitationEntity?
    fun countBySurveyIdAndStatus(surveyId: UUID, status: InvitationStatus): Long

    /** The invitation locked for the rest of the transaction, so that two submissions with one token cannot both use it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitationEntity i where i.tokenHash = :tokenHash")
    fun lockByTokenHash(@Param("tokenHash") tokenHash: String): InvitationEntity?

    @Query("select i from InvitationEntity i where i.surveyId = :surveyId order by i.createdAt desc, i.id desc")
    fun findFirstPage(@Param("surveyId") surveyId: UUID, limit: Limit): List<InvitationEntity>

    @Query(
        "select i from InvitationEntity i where i.surveyId = :surveyId " +
            "and (i.createdAt < :createdAt or (i.createdAt = :createdAt and i.id < :id)) order by i.createdAt desc, i.id desc",
    )
    fun findPageAfter(@Param("surveyId") surveyId: UUID, @Param("createdAt") createdAt: Instant, @Param("id") id: UUID, limit: Limit): List<InvitationEntity>
}
