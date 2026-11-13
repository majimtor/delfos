package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.api.ApiProblemException
import com.manuelalejandro.delfos.api.generated.model.ProblemCode
import com.manuelalejandro.delfos.auth.CurrentSession
import com.manuelalejandro.delfos.persistence.SurveyRepository
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * `streamResultsEvents` of the contract. Implemented beside the generated interfaces, not through
 * them: a stream is an emitter kept open, which the generated signature cannot express; the path,
 * the parameter and the errors are the contract's.
 */
@RestController
class ResultsEventsController(private val surveys: SurveyRepository, private val broker: ResultsEventBroker) {

    @GetMapping("/api/v1/surveys/{surveyId}/results/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamResultsEvents(@PathVariable surveyId: UUID, @RequestParam(defaultValue = "20") heartbeat: Int): SseEmitter {
        if (heartbeat < 0 || heartbeat > MAX_HEARTBEAT) {
            throw ApiProblemException(HttpStatus.BAD_REQUEST, ProblemCode.VALIDATION_ERROR, "1 fields are invalid.")
        }
        surveys.findByIdAndOwnerId(surveyId, CurrentSession.require().accountId)
            ?: throw ApiProblemException(HttpStatus.NOT_FOUND, ProblemCode.NOT_FOUND, "No such survey.")
        return broker.subscribe(surveyId, heartbeat)
    }

    private companion object {
        const val MAX_HEARTBEAT = 60
    }
}
