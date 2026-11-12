import type { Schemas } from '../../api/types'
import type { DraftQuestion, QuestionType } from './draft'

export type ConditionOperator = Schemas['ConditionOperator']
export type Condition = Schemas['Condition']
export type ConditionValue = Schemas['ConditionValue']
export type MatchMode = Schemas['MatchMode']

/** What a condition's value holds, fixed by its operator. */
export type ValueShape = 'none' | 'optionKey' | 'optionKeys' | 'number' | 'range'

const NUMERIC: QuestionType[] = ['NUMBER', 'SCALE']
const EVERY: QuestionType[] = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TEXT', 'NUMBER', 'SCALE']

/** The operators as the shared module defines them: the source types each applies to and the value it takes. */
export const OPERATORS: Record<
  ConditionOperator,
  { sourceTypes: QuestionType[]; shape: ValueShape }
> = {
  EQUALS: { sourceTypes: ['SINGLE_CHOICE'], shape: 'optionKey' },
  NOT_EQUALS: { sourceTypes: ['SINGLE_CHOICE'], shape: 'optionKey' },
  IN: { sourceTypes: ['SINGLE_CHOICE'], shape: 'optionKeys' },
  INCLUDES_ANY: { sourceTypes: ['MULTIPLE_CHOICE'], shape: 'optionKeys' },
  INCLUDES_ALL: { sourceTypes: ['MULTIPLE_CHOICE'], shape: 'optionKeys' },
  EXCLUDES: { sourceTypes: ['MULTIPLE_CHOICE'], shape: 'optionKeys' },
  EQ: { sourceTypes: NUMERIC, shape: 'number' },
  NE: { sourceTypes: NUMERIC, shape: 'number' },
  GT: { sourceTypes: NUMERIC, shape: 'number' },
  GTE: { sourceTypes: NUMERIC, shape: 'number' },
  LT: { sourceTypes: NUMERIC, shape: 'number' },
  LTE: { sourceTypes: NUMERIC, shape: 'number' },
  BETWEEN: { sourceTypes: NUMERIC, shape: 'range' },
  IS_ANSWERED: { sourceTypes: EVERY, shape: 'none' },
  IS_NOT_ANSWERED: { sourceTypes: EVERY, shape: 'none' },
}

/** The operators in the order the editor lists them. */
export const OPERATOR_ORDER = Object.keys(OPERATORS) as ConditionOperator[]

export function operatorsFor(type: QuestionType): ConditionOperator[] {
  return OPERATOR_ORDER.filter((operator) => OPERATORS[operator].sourceTypes.includes(type))
}

export function shapeOf(operator: ConditionOperator): ValueShape {
  return OPERATORS[operator].shape
}

/** The fields a value must hold for a shape, and no other. */
export function fieldsOf(shape: ValueShape): string[] {
  switch (shape) {
    case 'none':
      return []
    case 'optionKey':
      return ['optionKey']
    case 'optionKeys':
      return ['optionKeys']
    case 'number':
      return ['number']
    case 'range':
      return ['min', 'max']
  }
}

/** The fields a value holds; an empty list of option keys counts as nothing chosen. */
export function presentFields(value: ConditionValue | undefined): string[] {
  if (!value) return []
  const present: string[] = []
  if (value.optionKey !== undefined) present.push('optionKey')
  if (value.optionKeys !== undefined && value.optionKeys.length > 0) present.push('optionKeys')
  if (value.number !== undefined) present.push('number')
  if (value.min !== undefined) present.push('min')
  if (value.max !== undefined) present.push('max')
  return present
}

/** A condition on a source question with its first operator and no value yet. */
export function defaultCondition(source: DraftQuestion): Condition {
  return { sourceQuestionKey: source.key, operator: operatorsFor(source.type)[0]! }
}

/** The questions a rule on `id` may depend on: those placed before it. */
export function sourcesFor(questions: DraftQuestion[], id: string): DraftQuestion[] {
  const index = questions.findIndex((q) => q.id === id)
  return index <= 0 ? [] : questions.slice(0, index)
}
