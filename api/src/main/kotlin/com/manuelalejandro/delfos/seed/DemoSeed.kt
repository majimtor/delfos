package com.manuelalejandro.delfos.seed

import com.manuelalejandro.delfos.definition.SurveyDefinition
import com.manuelalejandro.delfos.domain.AccountStatus
import com.manuelalejandro.delfos.domain.SurveyStatus
import com.manuelalejandro.delfos.domain.VersionStatus
import com.manuelalejandro.delfos.evaluation.DefinitionValidator
import com.manuelalejandro.delfos.evaluation.ValidationMode
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import com.manuelalejandro.delfos.persistence.SurveyEntity
import com.manuelalejandro.delfos.persistence.SurveyRepository
import com.manuelalejandro.delfos.persistence.SurveyVersionEntity
import com.manuelalejandro.delfos.persistence.SurveyVersionRepository
import com.manuelalejandro.delfos.respond.ResponseWriter
import com.manuelalejandro.delfos.survey.DefinitionCodec
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.random.Random
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * The demo data: three fixed accounts and the sample surveys of the plan, in Spanish and in
 * English. Seeding is
 * idempotent by stable identifiers: accounts are upserted, everything they own is removed and the
 * sample surveys are recreated, so running it again is the reset. Enabled at startup with
 * `DELFOS_SEED_ENABLED=true`; the local compose and the public demo set it, tests call it directly,
 * and [DemoController] exposes it behind a token.
 */
@Component
class DemoSeed(
    private val accounts: AccountRepository,
    private val surveys: SurveyRepository,
    private val versions: SurveyVersionRepository,
    private val writer: ResponseWriter,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(DemoSeed::class.java)

    @Transactional
    fun seed() {
        val admin = upsertAccount(ADMIN, isAdmin = true)
        val manager = upsertAccount(MANAGER, isAdmin = false)
        val manager2 = upsertAccount(MANAGER_2, isAdmin = false)
        listOf(admin, manager, manager2).forEach { it.status = AccountStatus.ACTIVE }
        // Whatever the demo accounts own goes, including what visitors created with them.
        surveys.deleteAll(surveys.findAllByOwnerIdIn(listOf(admin.id, manager.id, manager2.id)))
        surveys.flush()

        recreate("lectura", manager.id, "Hábitos de lectura", "Borrador aún no publicado, para probar el editor.", "demo-lectura") { survey ->
            version(survey, 1, DemoSurveys.readingDraft, VersionStatus.DRAFT)
        }
        recreate("movilidad", manager.id, "Movilidad urbana", "Los cinco tipos de pregunta y todos los operadores de las reglas, con cuarenta respuestas.", "demo-movilidad") { survey ->
            val v1 = version(survey, 1, DemoSurveys.mobility, VersionStatus.PUBLISHED)
            respond(v1, DemoSurveys.mobility, count = 40, seed = 1)
            survey.status = SurveyStatus.PUBLISHED
        }
        recreate("servicio", manager.id, "Satisfacción con el servicio", "Dos versiones: la primera superada, la segunda publicada, con respuestas en ambas.", "demo-servicio") { survey ->
            val v1 = version(survey, 1, DemoSurveys.serviceV1, VersionStatus.SUPERSEDED)
            respond(v1, DemoSurveys.serviceV1, count = 15, seed = 2, daysAgo = 60..31)
            val v2 = version(survey, 2, DemoSurveys.serviceV2, VersionStatus.PUBLISHED)
            respond(v2, DemoSurveys.serviceV2, count = 10, seed = 3)
            survey.status = SurveyStatus.PUBLISHED
        }
        recreate("evento", manager.id, "Encuentro anual", "Encuesta cerrada con sus resultados.", "demo-evento") { survey ->
            val v1 = version(survey, 1, DemoSurveys.event, VersionStatus.PUBLISHED)
            respond(v1, DemoSurveys.event, count = 20, seed = 4, daysAgo = 90..40)
            survey.status = SurveyStatus.CLOSED
        }
        recreate("privada", manager2.id, "Teletrabajo en el equipo", "Pertenece a otro gestor: no aparece para demo-manager.", "demo-privada") { survey ->
            val v1 = version(survey, 1, DemoSurveys.privateSurvey, VersionStatus.PUBLISHED)
            respond(v1, DemoSurveys.privateSurvey, count = 5, seed = 5)
            survey.status = SurveyStatus.PUBLISHED
        }
        recreate("reading-en", manager.id, "Reading habits", "Unpublished draft, to try the editor.", "demo-reading", language = "en") { survey ->
            version(survey, 1, EnglishDemoSurveys.readingDraft, VersionStatus.DRAFT)
        }
        recreate("mobility-en", manager.id, "Urban mobility", "The five question types and every rule operator, with twenty responses.", "demo-mobility", language = "en") { survey ->
            val v1 = version(survey, 1, EnglishDemoSurveys.mobility, VersionStatus.PUBLISHED)
            respond(v1, EnglishDemoSurveys.mobility, count = 20, seed = 11, texts = EnglishDemoSurveys.texts)
            survey.status = SurveyStatus.PUBLISHED
        }
        recreate("service-en", manager.id, "Service satisfaction", "Two versions: the first superseded, the second published, with responses in both.", "demo-service", language = "en") { survey ->
            val v1 = version(survey, 1, EnglishDemoSurveys.serviceV1, VersionStatus.SUPERSEDED)
            respond(v1, EnglishDemoSurveys.serviceV1, count = 8, seed = 12, daysAgo = 60..31, texts = EnglishDemoSurveys.texts)
            val v2 = version(survey, 2, EnglishDemoSurveys.serviceV2, VersionStatus.PUBLISHED)
            respond(v2, EnglishDemoSurveys.serviceV2, count = 6, seed = 13, texts = EnglishDemoSurveys.texts)
            survey.status = SurveyStatus.PUBLISHED
        }
        recreate("meetup-en", manager.id, "Annual meetup", "A closed survey with its results.", "demo-meetup", language = "en") { survey ->
            val v1 = version(survey, 1, EnglishDemoSurveys.meetup, VersionStatus.PUBLISHED)
            respond(v1, EnglishDemoSurveys.meetup, count = 10, seed = 14, daysAgo = 90..40, texts = EnglishDemoSurveys.texts)
            survey.status = SurveyStatus.CLOSED
        }
        recreate("remote-en", manager2.id, "Remote work in the team", "Belongs to another manager: demo-manager does not see it.", "demo-remote", language = "en") { survey ->
            val v1 = version(survey, 1, EnglishDemoSurveys.privateSurvey, VersionStatus.PUBLISHED)
            respond(v1, EnglishDemoSurveys.privateSurvey, count = 3, seed = 15, texts = EnglishDemoSurveys.texts)
            survey.status = SurveyStatus.PUBLISHED
        }
        log.info("Demo seed applied")
    }

    private fun upsertAccount(username: String, isAdmin: Boolean): AccountEntity {
        val now = clock.instant()
        val existing = accounts.findById(stableId("account:$username")).orElse(null)
        val hash = requireNotNull(passwordEncoder.encode(PASSWORD))
        return if (existing != null) {
            existing.passwordHash = hash
            existing.isAdmin = isAdmin
            existing.updatedAt = now
            existing
        } else {
            accounts.save(AccountEntity(stableId("account:$username"), username, hash, isAdmin, AccountStatus.ACTIVE, now, now))
        }
    }

    private fun recreate(name: String, ownerId: UUID, title: String, description: String, publicId: String, language: String = "es", build: (SurveyEntity) -> Unit) {
        val id = stableId("survey:$name")
        surveys.findById(id).ifPresent { surveys.delete(it); surveys.flush() }
        val now = clock.instant()
        val survey = surveys.save(SurveyEntity(id, publicId, ownerId, title, description, language = language, createdAt = now.minus(Duration.ofDays(DAYS_BACK)), updatedAt = now))
        build(survey)
    }

    private fun version(survey: SurveyEntity, number: Int, definition: SurveyDefinition, status: VersionStatus): SurveyVersionEntity {
        val errors = DefinitionValidator.validate(definition, ValidationMode.PUBLISH)
        check(errors.isEmpty()) { "seed definition of ${survey.title} version $number is invalid: $errors" }
        val createdAt = clock.instant().minus(Duration.ofDays(DAYS_BACK - number))
        val version = versions.save(
            SurveyVersionEntity(UUID.randomUUID(), survey.id, number, status, DefinitionCodec.encodeRules(definition.rules), createdAt, publishedAt = createdAt.takeIf { status != VersionStatus.DRAFT }),
        )
        version.questions += DefinitionCodec.toEntities(version, definition)
        return versions.saveAndFlush(version)
    }

    private fun respond(version: SurveyVersionEntity, definition: SurveyDefinition, count: Int, seed: Int, daysAgo: IntRange = 30..0, texts: List<String> = SPANISH_TEXTS) {
        val generator = ResponseGenerator(definition, seed, texts)
        val random = Random(seed)
        val now = clock.instant()
        repeat(count) {
            val submittedAt = now.minus(Duration.ofDays(random.nextInt(daysAgo.last, daysAgo.first + 1).toLong())).minus(Duration.ofMinutes(random.nextInt(0, 1440).toLong()))
            writer.store(version.id, definition, generator.next(), submittedAt)
        }
    }

    companion object {
        const val ADMIN = "demo-admin"
        const val MANAGER = "demo-manager"
        const val MANAGER_2 = "demo-manager-2"
        const val PASSWORD = "delfos-demo"
        private const val DAYS_BACK = 100L

        fun stableId(name: String): UUID = UUID.nameUUIDFromBytes("delfos-seed:$name".toByteArray(Charsets.UTF_8))
    }
}

@Component
@ConditionalOnProperty("delfos.seed.enabled", havingValue = "true")
class DemoSeedRunner(private val seed: DemoSeed) : ApplicationRunner {

    override fun run(args: ApplicationArguments) = seed.seed()
}
