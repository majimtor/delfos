import type { IntlShape } from 'react-intl'
import type { QuestionType } from './draft'

/** The name of a question type as the editor shows it. */
export function typeLabel(intl: IntlShape, type: QuestionType): string {
  switch (type) {
    case 'SINGLE_CHOICE':
      return intl.formatMessage({
        id: 'editor.type.SINGLE_CHOICE',
        defaultMessage: 'Single choice',
      })
    case 'MULTIPLE_CHOICE':
      return intl.formatMessage({
        id: 'editor.type.MULTIPLE_CHOICE',
        defaultMessage: 'Multiple choice',
      })
    case 'TEXT':
      return intl.formatMessage({ id: 'editor.type.TEXT', defaultMessage: 'Text' })
    case 'NUMBER':
      return intl.formatMessage({ id: 'editor.type.NUMBER', defaultMessage: 'Number' })
    case 'SCALE':
      return intl.formatMessage({ id: 'editor.type.SCALE', defaultMessage: 'Scale' })
  }
}
