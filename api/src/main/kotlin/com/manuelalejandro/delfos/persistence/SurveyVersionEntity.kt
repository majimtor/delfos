package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.domain.VersionStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/** One definition of a survey. `rules` holds the visibility rules as the JSON of the contract. */
@Entity
@Table(name = "survey_version")
class SurveyVersionEntity(
    @Id
    val id: UUID,
    @Column(name = "survey_id", nullable = false)
    val surveyId: UUID,
    @Column(nullable = false)
    val number: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VersionStatus = VersionStatus.DRAFT,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    var rules: String = "[]",
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
) {
    @OneToMany(mappedBy = "version", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position")
    var questions: MutableList<QuestionEntity> = mutableListOf()
}

/** A question of a version. `config` holds the per-type configuration as the JSON of the contract. */
@Entity
@Table(name = "question")
class QuestionEntity(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    val version: SurveyVersionEntity,
    @Column(nullable = false)
    val key: String,
    @Column(nullable = false)
    var position: Int,
    @Column(nullable = false)
    val type: String,
    @Column(nullable = false)
    var text: String,
    @Column(nullable = false)
    var required: Boolean = false,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    var config: String = "{}",
) {
    @OneToMany(mappedBy = "question", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position")
    var options: MutableList<QuestionOptionEntity> = mutableListOf()
}

@Entity
@Table(name = "question_option")
class QuestionOptionEntity(
    @Id
    val id: UUID,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    val question: QuestionEntity,
    @Column(nullable = false)
    val key: String,
    @Column(nullable = false)
    var position: Int,
    @Column(nullable = false)
    var label: String,
)
