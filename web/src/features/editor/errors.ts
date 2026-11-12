import type { IntlShape } from 'react-intl'
import type { DraftError } from './validation'

/** The message of an error of the draft, from its stable code and its parameters. */
export function draftErrorMessage(intl: IntlShape, error: DraftError): string {
  const p = error.params ?? {}
  switch (error.code) {
    case 'REQUIRED':
      return intl.formatMessage({ id: 'editor.error.required', defaultMessage: 'Required.' })
    case 'TOO_LONG':
      return intl.formatMessage(
        { id: 'error.TOO_LONG', defaultMessage: 'Use at most {max} characters.' },
        { max: numberOr(p.max, 0) },
      )
    case 'TOO_FEW_ITEMS':
      return intl.formatMessage(
        { id: 'editor.error.tooFewItems', defaultMessage: 'Add at least {min}.' },
        { min: numberOr(p.min, 1) },
      )
    case 'TOO_MANY_ITEMS':
      return intl.formatMessage(
        { id: 'editor.error.tooManyItems', defaultMessage: 'Add at most {max}.' },
        { max: numberOr(p.max, 1) },
      )
    case 'DUPLICATE':
    case 'DUPLICATE_OPTION':
      return intl.formatMessage({ id: 'editor.error.duplicate', defaultMessage: 'Duplicated.' })
    case 'OUT_OF_RANGE':
      return intl.formatMessage(
        { id: 'error.OUT_OF_RANGE', defaultMessage: 'Enter a value between {min} and {max}.' },
        { min: numberOr(p.min, 0), max: numberOr(p.max, 0) },
      )
    case 'NOT_INTEGER':
      return intl.formatMessage({
        id: 'error.NOT_INTEGER',
        defaultMessage: 'Enter a whole number.',
      })
    case 'NO_QUESTIONS':
      return intl.formatMessage({
        id: 'editor.error.noQuestions',
        defaultMessage: 'Add at least one question before publishing.',
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
