package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.persistence.SurveyVersionRepository
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * The open event streams of results, fed by one tick: every few seconds the totals of each survey
 * with a listener are read once, and a `results` event goes to its listeners when they changed.
 * Between events a comment line keeps the connection through proxies. Listeners leave when their
 * connection ends or times out.
 */
@Component
class ResultsEventBroker(
    private val versions: SurveyVersionRepository,
    private val queries: AnswerQueries,
    private val clock: Clock,
) {

    private class Listener(val surveyId: UUID, val emitter: SseEmitter, val heartbeat: Duration?, var lastSent: Instant, var lastTotals: AnswerQueries.Totals?)

    private val log = LoggerFactory.getLogger(ResultsEventBroker::class.java)
    private val listeners = ConcurrentHashMap<SseEmitter, Listener>()

    /** Opens a stream for a survey and sends the current totals at once, so the client knows it works. */
    fun subscribe(surveyId: UUID, heartbeatSeconds: Int): SseEmitter {
        val emitter = SseEmitter(CONNECTION_LIMIT.toMillis())
        val listener = Listener(surveyId, emitter, heartbeatSeconds.takeIf { it > 0 }?.let { Duration.ofSeconds(it.toLong()) }, clock.instant(), null)
        listeners[emitter] = listener
        val leave = Runnable { listeners.remove(emitter) }
        emitter.onCompletion(leave)
        emitter.onTimeout(leave)
        emitter.onError { leave.run() }
        send(listener, totalsOf(surveyId))
        return emitter
    }

    @Scheduled(fixedDelayString = "\${delfos.events.tick-ms:3000}")
    fun tick() {
        val bySurvey = listeners.values.groupBy { it.surveyId }
        for ((surveyId, group) in bySurvey) {
            val totals = runCatching { totalsOf(surveyId) }.getOrElse { failure ->
                log.warn("Results events: could not read the totals of a survey", failure)
                continue
            }
            val now = clock.instant()
            for (listener in group) {
                val changed = listener.lastTotals?.let { it.responseCount != totals.responseCount || it.lastSubmittedAt != totals.lastSubmittedAt } ?: true
                when {
                    changed -> send(listener, totals)
                    listener.heartbeat != null && Duration.between(listener.lastSent, now) >= listener.heartbeat -> heartbeat(listener)
                }
            }
        }
    }

    /** How many streams are open; for tests and for a look at the demo. */
    val openStreams: Int get() = listeners.size

    private fun totalsOf(surveyId: UUID): AnswerQueries.Totals =
        queries.totals(AnswerQueries.Scope(versions.findAllBySurveyIdOrderByNumberDesc(surveyId).map { it.id }))

    private fun send(listener: Listener, totals: AnswerQueries.Totals) {
        val data = "{\"responseCount\":${totals.responseCount},\"lastSubmittedAt\":${totals.lastSubmittedAt?.let { "\"${it.utc()}\"" } ?: "null"}}"
        deliver(listener) { emitter -> emitter.send(SseEmitter.event().id(clock.instant().toEpochMilli().toString()).name("results").data(data)) }
        listener.lastTotals = totals
    }

    private fun heartbeat(listener: Listener) = deliver(listener) { emitter -> emitter.send(SseEmitter.event().comment("keep-alive")) }

    private fun deliver(listener: Listener, write: (SseEmitter) -> Unit) {
        try {
            write(listener.emitter)
            listener.lastSent = clock.instant()
        } catch (_: IOException) {
            // The client went away; the emitter's completion callback removes the listener.
            listeners.remove(listener.emitter)
        } catch (_: IllegalStateException) {
            listeners.remove(listener.emitter)
        }
    }

    private companion object {
        val CONNECTION_LIMIT: Duration = Duration.ofMinutes(10)
    }
}
