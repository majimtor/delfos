import type { IntlShape } from 'react-intl'
import type { QuestionError } from './answers'

/** The stable code of the contract, translated with its parameters. */
export function errorMessage(intl: IntlShape, error: QuestionError): string {
  const p = error.params
  switch (error.code) {
    case 'MISSING_REQUIRED_ANSWER':
    case 'REQUIRED':
      return intl.formatMessage({
        id: 'error.MISSING_REQUIRED_ANSWER',
        defaultMessage: 'This question is required.',
      })
    case 'TOO_LONG':
      return intl.formatMessage(
        { id: 'error.TOO_LONG', defaultMessage: 'Use at most {max} characters.' },
        { max: numberOr(p.max, 0) },
      )
    case 'OUT_OF_RANGE':
      if (p.min !== undefined && p.max !== undefined) {
        return intl.formatMessage(
          { id: 'error.OUT_OF_RANGE', defaultMessage: 'Enter a value between {min} and {max}.' },
          { min: numberOr(p.min, 0), max: numberOr(p.max, 0) },
        )
      }
      if (p.min !== undefined) {
        return intl.formatMessage(
          { id: 'error.OUT_OF_RANGE_MIN', defaultMessage: 'Enter a value of at least {min}.' },
          { min: numberOr(p.min, 0) },
        )
      }
      return intl.formatMessage(
        { id: 'error.OUT_OF_RANGE_MAX', defaultMessage: 'Enter a value of at most {max}.' },
        { max: numberOr(p.max, 0) },
      )
    case 'NOT_INTEGER':
      return intl.formatMessage({
        id: 'error.NOT_INTEGER',
        defaultMessage: 'Enter a whole number.',
      })
    case 'TOO_FEW_SELECTIONS':
      return intl.formatMessage(
        { id: 'error.TOO_FEW_SELECTIONS', defaultMessage: 'Choose at least {min}.' },
        { min: numberOr(p.min, 1) },
      )
    case 'TOO_MANY_SELECTIONS':
      return intl.formatMessage(
        { id: 'error.TOO_MANY_SELECTIONS', defaultMessage: 'Choose at most {max}.' },
        { max: numberOr(p.max, 1) },
      )
    case 'HIDDEN_QUESTION_ANSWERED':
      return intl.formatMessage({
        id: 'error.HIDDEN_QUESTION_ANSWERED',
        defaultMessage: 'This question no longer applies.',
      })
    default:
      return intl.formatMessage({
        id: 'error.INVALID_VALUE',
        defaultMessage: 'This value is not valid.',
      })
  }
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' ? value : fallback
}
