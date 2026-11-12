import { isChoice, type DraftState } from './draft'

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
  return errors
}
