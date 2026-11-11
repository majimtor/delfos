import { useEffect, useRef, useState } from 'react'
import { api } from '../../api/client'
import type { Answer, Question } from '../../api/types'
import { answersToSend, attributeErrors, type Answers, type QuestionError } from './answers'

export interface Evaluation {
  visible: ReadonlySet<string>
  errors: QuestionError[]
  missingRequired: ReadonlySet<string>
  sent: Answer[]
  /** True while the answers on screen differ from the ones last evaluated. */
  pending: boolean
}

interface Evaluated extends Omit<Evaluation, 'pending'> {
  evaluatedFor: Answers | null
}

const EMPTY: Evaluated = {
  visible: new Set(),
  errors: [],
  missingRequired: new Set(),
  sent: [],
  evaluatedFor: null,
}

/**
 * Asks the server which questions are visible and which answers are wrong, a short while after
 * every change. Only answers of questions visible in the previous evaluation are sent, so a hidden
 * question never carries an answer. Stale responses are ignored.
 */
export function useEvaluation(
  publicId: string,
  versionNumber: number,
  questions: Question[],
  answers: Answers,
  delayMs = 300,
): Evaluation {
  const [evaluation, setEvaluation] = useState<Evaluated>(EMPTY)
  const visibleRef = useRef<ReadonlySet<string>>(new Set(questions.map((q) => q.key)))
  const requestRef = useRef(0)

  useEffect(() => {
    const request = ++requestRef.current
    const controller = new AbortController()
    const sent = answersToSend(questions, answers, visibleRef.current)
    const timer = setTimeout(async () => {
      const { data } = await api.POST('/public/surveys/{publicId}/evaluations', {
        params: { path: { publicId } },
        body: { versionNumber, answers: sent },
        signal: controller.signal,
      })
      if (request !== requestRef.current || !data) return
      const visible = new Set(data.visibleQuestionKeys)
      visibleRef.current = visible
      setEvaluation({
        visible,
        errors: attributeErrors(data.errors, sent),
        missingRequired: new Set(data.missingRequiredQuestionKeys),
        sent,
        evaluatedFor: answers,
      })
    }, delayMs)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [publicId, versionNumber, questions, answers, delayMs])

  return { ...evaluation, pending: evaluation.evaluatedFor !== answers }
}
