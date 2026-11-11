import type { Answer, FieldError, Question } from '../../api/types'

/** What the respondent typed or chose for a question, before it becomes an answer of the contract. */
export interface AnswerValue {
  optionKey?: string
  optionKeys?: string[]
  number?: number
  text?: string
}

export type Answers = Record<string, AnswerValue>

/** The answers to send: only questions visible in the last evaluation, only with a value. */
export function answersToSend(
  questions: Question[],
  answers: Answers,
  visible: ReadonlySet<string>,
): Answer[] {
  const result: Answer[] = []
  for (const question of questions) {
    if (!visible.has(question.key)) continue
    const value = answers[question.key]
    const answer = toAnswer(question, value)
    if (answer) result.push(answer)
  }
  return result
}

function toAnswer(question: Question, value: AnswerValue | undefined): Answer | null {
  if (!value) return null
  switch (question.type) {
    case 'SINGLE_CHOICE':
      return value.optionKey ? { questionKey: question.key, optionKey: value.optionKey } : null
    case 'MULTIPLE_CHOICE':
      return value.optionKeys && value.optionKeys.length > 0
        ? { questionKey: question.key, optionKeys: value.optionKeys }
        : null
    case 'TEXT':
      return value.text && value.text.length > 0
        ? { questionKey: question.key, text: value.text }
        : null
    case 'NUMBER':
    case 'SCALE':
      return value.number !== undefined && Number.isFinite(value.number)
        ? { questionKey: question.key, number: value.number }
        : null
  }
}

/** A field error of the contract attributed to the question it concerns. */
export interface QuestionError {
  questionKey: string
  code: FieldError['code']
  params: Record<string, unknown>
}

const ANSWER_INDEX = /^answers\[(\d+)\]/

/** Maps the paths of the contract (`answers[2].number`) back to question keys through the array that was sent. */
export function attributeErrors(errors: FieldError[], sent: Answer[]): QuestionError[] {
  const result: QuestionError[] = []
  for (const error of errors) {
    const params = (error.params ?? {}) as Record<string, unknown>
    if (error.code === 'MISSING_REQUIRED_ANSWER' && typeof params.questionKey === 'string') {
      result.push({ questionKey: params.questionKey, code: error.code, params })
      continue
    }
    const match = ANSWER_INDEX.exec(error.path)
    const answer = match ? sent[Number(match[1])] : undefined
    if (answer) result.push({ questionKey: answer.questionKey, code: error.code, params })
  }
  return result
}
