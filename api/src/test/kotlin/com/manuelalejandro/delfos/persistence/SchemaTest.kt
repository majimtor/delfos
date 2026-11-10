package com.manuelalejandro.delfos.persistence

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.domain.VersionStatus
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate

/**
 * The application only starts when the entities match the migrated schema, so every test of this
 * class already proves that. These cases exercise what the schema promises beyond column shapes.
 */
class SchemaTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var surveys: SurveyRepository
    @Autowired lateinit var versions: SurveyVersionRepository
    @Autowired lateinit var transactions: TransactionTemplate

    @Test
    fun `a survey with a version, questions and options is stored and read back in order`() {
        val now = Instant.now()
        val owner = accounts.save(AccountEntity(UUID.randomUUID(), "owner-${UUID.randomUUID()}", "hash", createdAt = now, updatedAt = now))
        val survey = surveys.save(SurveyEntity(UUID.randomUUID(), "pub-${UUID.randomUUID().toString().take(8)}", owner.id, "Title", language = "es", createdAt = now, updatedAt = now))
        val version = SurveyVersionEntity(UUID.randomUUID(), survey.id, 1, createdAt = now, rules = """[{"targetQuestionKey":"q2","match":"ALL","conditions":[{"sourceQuestionKey":"q1","operator":"EQUALS","value":{"optionKey":"yes"}}]}]""")
        val q1 = QuestionEntity(UUID.randomUUID(), version, "q1", 0, "SINGLE_CHOICE", "Do you own a car?")
        q1.options += QuestionOptionEntity(UUID.randomUUID(), q1, "yes", 0, "Yes")
        q1.options += QuestionOptionEntity(UUID.randomUUID(), q1, "no", 1, "No")
        val q2 = QuestionEntity(UUID.randomUUID(), version, "q2", 1, "TEXT", "Which model?", config = """{"maxLength":100}""")
        version.questions += listOf(q1, q2)
        versions.save(version)

        val loaded = transactions.execute {
            val v = versions.findBySurveyIdAndNumber(survey.id, 1)!!
            Triple(v.questions.map { it.key }, v.questions.first().options.map { it.key }, v.rules)
        }!!
        assertThat(loaded.first).containsExactly("q1", "q2")
        assertThat(loaded.second).containsExactly("yes", "no")
        assertThat(loaded.third).contains("\"targetQuestionKey\"")
        assertThat(versions.findBySurveyIdAndStatus(survey.id, VersionStatus.DRAFT)?.number).isEqualTo(1)
    }

    @Test
    fun `the database refuses a second draft for the same survey`() {
        val now = Instant.now()
        val owner = accounts.save(AccountEntity(UUID.randomUUID(), "owner-${UUID.randomUUID()}", "hash", createdAt = now, updatedAt = now))
        val survey = surveys.save(SurveyEntity(UUID.randomUUID(), "pub-${UUID.randomUUID().toString().take(8)}", owner.id, "Title", language = "en", createdAt = now, updatedAt = now))
        versions.save(SurveyVersionEntity(UUID.randomUUID(), survey.id, 1, createdAt = now))

        assertThatThrownBy { versions.saveAndFlush(SurveyVersionEntity(UUID.randomUUID(), survey.id, 2, createdAt = now)) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `usernames are unique regardless of case`() {
        val now = Instant.now()
        val name = "Manager-${UUID.randomUUID().toString().take(8)}"
        accounts.save(AccountEntity(UUID.randomUUID(), name, "hash", createdAt = now, updatedAt = now))

        assertThat(accounts.findByUsernameIgnoreCase(name.lowercase())?.username).isEqualTo(name)
        assertThatThrownBy { accounts.saveAndFlush(AccountEntity(UUID.randomUUID(), name.uppercase(), "hash", createdAt = now, updatedAt = now)) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
