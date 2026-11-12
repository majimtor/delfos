import { isChoice, type DraftState } from './draft'
import { OPERATORS, fieldsOf, presentFields } from './rules'

/** An error in the shape of the contract's `FieldError`, with the same paths the server uses. */
export interface DraftError {
  path: string
  code: string
  params?: Record<string, unknown>
}

const MAX_TEXT = 1000
const MAX_LABEL = 200
const MIN_OPTIONS = 2
const MAX_OPTIONS = 50
const SCALE_MAX = { min: 3, max: 10 }
const SCALE_MIN = { min: 0, max: 1 }
const TEXT_LENGTH = { min: 1, max: 2000 }

const isWhole = (value: number) => Number.isInteger(value)

export interface SurveyDetails {
  title: string
  description: string
}

const MAX_TITLE = 200
const MAX_DESCRIPTION = 2000

/** The checks of the contract on the survey's own fields, with the paths `PATCH /surveys/{id}` reports. */
export function validateDetails(details: SurveyDetails): DraftError[] {
  const errors: DraftError[] = []
  if (details.title.trim() === '') errors.push({ path: 'title', code: 'REQUIRED' })
  else if (details.title.length > MAX_TITLE) {
    errors.push({ path: 'title', code: 'TOO_LONG', params: { max: MAX_TITLE } })
  }
  if (details.description.length > MAX_DESCRIPTION) {
    errors.push({ path: 'description', code: 'TOO_LONG', params: { max: MAX_DESCRIPTION } })
  }
  return errors
}

/**
 * Mirrors the checks of the contract and of the shared validator that need no server, so that the
 * editor marks the fields before a request. The server remains the authority: it validates again
 * and also the rules, and its errors replace these.
 */
export function validateDraft(state: DraftState, mode: 'save' | 'publish' = 'save'): DraftError[] {
  const errors: DraftError[] = []
  if (mode === 'publish' && state.questions.length === 0) {
    errors.push({ path: 'questions', code: 'NO_QUESTIONS' })
  }
  state.questions.forEach((question, index) => {
    const path = `questions[${index}]`
    if (question.text.trim() === '') errors.push({ path: `${path}.text`, code: 'REQUIRED' })
    else if (question.text.length > MAX_TEXT) {
      errors.push({ path: `${path}.text`, code: 'TOO_LONG', params: { max: MAX_TEXT } })
    }
    if (isChoice(question.type)) {
      const options = question.options ?? []
      if (options.length < MIN_OPTIONS) {
        errors.push({
          path: `${path}.options`,
          code: 'TOO_FEW_ITEMS',
          params: { min: MIN_OPTIONS },
        })
      } else if (options.length > MAX_OPTIONS) {
        errors.push({
          path: `${path}.options`,
          code: 'TOO_MANY_ITEMS',
          params: { max: MAX_OPTIONS },
        })
      }
      options.forEach((option, optionIndex) => {
        const optionPath = `${path}.options[${optionIndex}].label`
        if (option.label.trim() === '') errors.push({ path: optionPath, code: 'REQUIRED' })
        else if (option.label.length > MAX_LABEL) {
          errors.push({ path: optionPath, code: 'TOO_LONG', params: { max: MAX_LABEL } })
        }
      })
      if (question.type === 'MULTIPLE_CHOICE') {
        const min = question.minSelections ?? 1
        const max = question.maxSelections ?? options.length
        if (question.minSelections !== undefined && (!isWhole(min) || min < 1 || min > max)) {
          errors.push({ path: `${path}.minSelections`, code: 'INVALID_VALUE' })
        } else if (
          question.maxSelections !== undefined &&
          (!isWhole(max) || max < min || max > options.length)
        ) {
          errors.push({ path: `${path}.maxSelections`, code: 'INVALID_VALUE' })
        }
      }
    }
    if (question.type === 'TEXT' && question.maxLength !== undefined) {
      const length = question.maxLength
      if (!isWhole(length) || length < TEXT_LENGTH.min || length > TEXT_LENGTH.max) {
        errors.push({ path: `${path}.maxLength`, code: 'OUT_OF_RANGE', params: TEXT_LENGTH })
      }
    }
    if (question.type === 'NUMBER') {
      if (question.min !== undefined && question.max !== undefined && question.min > question.max) {
        errors.push({ path: `${path}.max`, code: 'INVALID_VALUE' })
      }
    }
    if (question.type === 'SCALE') {
      const max = question.max
      if (max === undefined) errors.push({ path: `${path}.max`, code: 'REQUIRED' })
      else if (!isWhole(max) || max < SCALE_MAX.min || max > SCALE_MAX.max) {
        errors.push({ path: `${path}.max`, code: 'OUT_OF_RANGE', params: SCALE_MAX })
      }
      const min = question.min
      if (min !== undefined && (!isWhole(min) || min < SCALE_MIN.min || min > SCALE_MIN.max)) {
        errors.push({ path: `${path}.min`, code: 'OUT_OF_RANGE', params: SCALE_MIN })
      }
    }
  })
  validateRules(state, errors)
  return errors
}

function validateRules(state: DraftState, errors: DraftError[]) {
  const byKey = new Map(
    state.questions.map((question, index) => [question.key, { question, index }]),
  )
  const targets = new Set<string>()
  state.rules.forEach((rule, ruleIndex) => {
    const rulePath = `rules[${ruleIndex}]`
    const target = byKey.get(rule.targetQuestionKey)
    if (!target) errors.push({ path: `${rulePath}.targetQuestionKey`, code: 'UNKNOWN_QUESTION' })
    else if (targets.has(rule.targetQuestionKey)) {
      errors.push({ path: `${rulePath}.targetQuestionKey`, code: 'DUPLICATE_RULE' })
    } else targets.add(rule.targetQuestionKey)
    if (rule.conditions.length === 0) {
      errors.push({ path: `${rulePath}.conditions`, code: 'EMPTY_CONDITIONS' })
    }
    rule.conditions.forEach((condition, conditionIndex) => {
      const path = `${rulePath}.conditions[${conditionIndex}]`
      const source = byKey.get(condition.sourceQuestionKey)
      const operator = OPERATORS[condition.operator]
      if (!source) errors.push({ path: `${path}.sourceQuestionKey`, code: 'UNKNOWN_QUESTION' })
      else {
        if (target && source.index === target.index) {
          errors.push({ path: `${path}.sourceQuestionKey`, code: 'SELF_REFERENCE' })
        } else if (target && source.index > target.index) {
          errors.push({
            path: `${path}.sourceQuestionKey`,
            code: 'FORWARD_REFERENCE',
            params: { sourcePosition: source.index + 1, targetPosition: target.index + 1 },
          })
        }
        if (!operator.sourceTypes.includes(source.question.type)) {
          errors.push({
            path: `${path}.operator`,
            code: 'OPERATOR_TYPE_MISMATCH',
            params: { sourceType: source.question.type },
          })
        }
      }
      const present = presentFields(condition.value)
      const expected = fieldsOf(operator.shape)
      const value = condition.value
      if (
        present.length !== expected.length ||
        expected.some((field) => !present.includes(field))
      ) {
        errors.push({ path: `${path}.value`, code: 'VALUE_TYPE_MISMATCH' })
      } else if (condition.operator === 'BETWEEN' && value!.min! > value!.max!) {
        errors.push({ path: `${path}.value`, code: 'VALUE_TYPE_MISMATCH' })
      } else if (
        source &&
        operator.sourceTypes.includes(source.question.type) &&
        source.question.options
      ) {
        const keys = new Set(source.question.options.map((option) => option.key))
        if (expected.includes('optionKey') && !keys.has(value!.optionKey!)) {
          errors.push({ path: `${path}.value.optionKey`, code: 'UNKNOWN_OPTION' })
        }
        if (expected.includes('optionKeys')) {
          value!.optionKeys!.forEach((key, keyIndex) => {
            if (!keys.has(key)) {
              errors.push({ path: `${path}.value.optionKeys[${keyIndex}]`, code: 'UNKNOWN_OPTION' })
            }
          })
        }
      }
    })
  })
}
