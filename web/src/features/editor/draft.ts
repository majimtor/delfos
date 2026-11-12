import type { Schemas } from '../../api/types'
import {
  defaultCondition,
  shapeOf,
  sourcesFor,
  type Condition,
  type ConditionOperator,
  type ConditionValue,
  type MatchMode,
} from './rules'

export type QuestionType = Schemas['QuestionType']
export type VisibilityRule = Schemas['VisibilityRule']
export type DraftDefinition = Schemas['DraftDefinition']
export type SurveyVersion = Schemas['SurveyVersion']
type QuestionInput = Schemas['QuestionInput']

export interface DraftOption {
  key: string
  label: string
}

/** A question as the editor holds it: `id` is its identity on screen, `key` its identity in the contract. */
export interface DraftQuestion {
  id: string
  key: string
  type: QuestionType
  text: string
  required: boolean
  options?: DraftOption[]
  minSelections?: number
  maxSelections?: number
  maxLength?: number
  multiline?: boolean
  min?: number
  max?: number
  integerOnly?: boolean
  minLabel?: string
  maxLabel?: string
}

export interface DraftState {
  questions: DraftQuestion[]
  rules: VisibilityRule[]
}

export const EMPTY_DRAFT: DraftState = { questions: [], rules: [] }

export const QUESTION_TYPES: QuestionType[] = [
  'SINGLE_CHOICE',
  'MULTIPLE_CHOICE',
  'TEXT',
  'NUMBER',
  'SCALE',
]

export function isChoice(type: QuestionType): boolean {
  return type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE'
}

type Direction = 'up' | 'down'

/** The fields of a question that are edited in place; identity, type and options have their own actions. */
export type QuestionPatch = Partial<Omit<DraftQuestion, 'id' | 'key' | 'type' | 'options'>>

export type DraftAction =
  | { type: 'loaded'; version: SurveyVersion }
  | { type: 'addQuestion'; questionType: QuestionType }
  | { type: 'removeQuestion'; id: string }
  | { type: 'moveQuestion'; id: string; direction: Direction }
  | { type: 'updateQuestion'; id: string; patch: QuestionPatch }
  | { type: 'changeType'; id: string; questionType: QuestionType }
  | { type: 'addOption'; id: string }
  | { type: 'removeOption'; id: string; optionKey: string }
  | { type: 'moveOption'; id: string; optionKey: string; direction: Direction }
  | { type: 'updateOption'; id: string; optionKey: string; label: string }
  | { type: 'addRule'; id: string }
  | { type: 'removeRule'; id: string }
  | { type: 'setRuleMatch'; id: string; match: MatchMode }
  | { type: 'addCondition'; id: string }
  | { type: 'removeCondition'; id: string; index: number }
  | { type: 'setConditionSource'; id: string; index: number; sourceKey: string }
  | { type: 'setConditionOperator'; id: string; index: number; operator: ConditionOperator }
  | { type: 'setConditionValue'; id: string; index: number; value: ConditionValue | undefined }

const ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'

/** A key in the shape the server assigns, so a new question can be referenced by a rule before the first save. */
export function newKey(prefix: 'q' | 'o'): string {
  const bytes = crypto.getRandomValues(new Uint8Array(8))
  return `${prefix}-${Array.from(bytes, (b) => ALPHABET[b % ALPHABET.length]).join('')}`
}

function newOption(): DraftOption {
  return { key: newKey('o'), label: '' }
}

function typeDefaults(type: QuestionType): Partial<DraftQuestion> {
  switch (type) {
    case 'SINGLE_CHOICE':
    case 'MULTIPLE_CHOICE':
      return { options: [newOption(), newOption()] }
    case 'SCALE':
      return { max: 5 }
    default:
      return {}
  }
}

function newQuestion(type: QuestionType): DraftQuestion {
  const key = newKey('q')
  return { id: key, key, type, text: '', required: false, ...typeDefaults(type) }
}

/** Changing the type gives the question a new key: it is no longer the same question for the results. */
function retyped(question: DraftQuestion, type: QuestionType): DraftQuestion {
  if (question.type === type) return question
  const key = newKey('q')
  const kept =
    isChoice(question.type) && isChoice(type) ? { options: question.options } : typeDefaults(type)
  return { id: question.id, key, type, text: question.text, required: question.required, ...kept }
}

function withoutKey(rules: VisibilityRule[], key: string): VisibilityRule[] {
  return rules.filter(
    (rule) =>
      rule.targetQuestionKey !== key &&
      !rule.conditions.some((condition) => condition.sourceQuestionKey === key),
  )
}

/** After a type change: the rule on the question follows its new key; the rules that depended on it go, their operators no longer fit. */
function retargeted(rules: VisibilityRule[], oldKey: string, newKey: string): VisibilityRule[] {
  return rules
    .filter((rule) => !rule.conditions.some((condition) => condition.sourceQuestionKey === oldKey))
    .map((rule) =>
      rule.targetQuestionKey === oldKey ? { ...rule, targetQuestionKey: newKey } : rule,
    )
}

function mapRule(
  state: DraftState,
  id: string,
  change: (rule: VisibilityRule, question: DraftQuestion) => VisibilityRule | null,
): DraftState {
  const question = state.questions.find((q) => q.id === id)
  if (!question) return state
  const rules = state.rules.flatMap((rule) => {
    if (rule.targetQuestionKey !== question.key) return [rule]
    const next = change(rule, question)
    return next ? [next] : []
  })
  return { ...state, rules }
}

function mapCondition(
  state: DraftState,
  id: string,
  index: number,
  change: (condition: Condition) => Condition,
): DraftState {
  return mapRule(state, id, (rule) => ({
    ...rule,
    conditions: rule.conditions.map((condition, i) =>
      i === index ? change(condition) : condition,
    ),
  }))
}

function move<T>(items: T[], index: number, direction: Direction): T[] {
  const target = direction === 'up' ? index - 1 : index + 1
  if (index < 0 || target < 0 || target >= items.length) return items
  const next = [...items]
  ;[next[index], next[target]] = [next[target]!, next[index]!]
  return next
}

function mapQuestion(
  state: DraftState,
  id: string,
  change: (question: DraftQuestion) => DraftQuestion,
): DraftState {
  return { ...state, questions: state.questions.map((q) => (q.id === id ? change(q) : q)) }
}

export function fromVersion(version: SurveyVersion): DraftState {
  return {
    questions: version.questions.map((q) => ({
      id: q.key,
      key: q.key,
      type: q.type,
      text: q.text,
      required: q.required,
      options: q.options?.map((o) => ({ key: o.key, label: o.label })),
      minSelections: q.minSelections,
      maxSelections: q.maxSelections,
      maxLength: q.maxLength,
      multiline: q.multiline,
      min: q.min,
      max: q.max,
      integerOnly: q.integerOnly,
      minLabel: q.minLabel,
      maxLabel: q.maxLabel,
    })),
    rules: version.rules,
  }
}

export function draftReducer(state: DraftState, action: DraftAction): DraftState {
  switch (action.type) {
    case 'loaded':
      return fromVersion(action.version)
    case 'addQuestion':
      return { ...state, questions: [...state.questions, newQuestion(action.questionType)] }
    case 'removeQuestion': {
      const removed = state.questions.find((q) => q.id === action.id)
      if (!removed) return state
      return {
        questions: state.questions.filter((q) => q.id !== action.id),
        rules: withoutKey(state.rules, removed.key),
      }
    }
    case 'moveQuestion': {
      const moved = move(
        state.questions,
        state.questions.findIndex((q) => q.id === action.id),
        action.direction,
      )
      return moved === state.questions ? state : { ...state, questions: moved }
    }
    case 'updateQuestion':
      return mapQuestion(state, action.id, (q) => ({ ...q, ...action.patch }))
    case 'changeType': {
      const current = state.questions.find((q) => q.id === action.id)
      if (!current || current.type === action.questionType) return state
      const next = retyped(current, action.questionType)
      return {
        questions: state.questions.map((q) => (q.id === action.id ? next : q)),
        rules: retargeted(state.rules, current.key, next.key),
      }
    }
    case 'addOption':
      return mapQuestion(state, action.id, (q) => ({
        ...q,
        options: [...(q.options ?? []), newOption()],
      }))
    case 'removeOption':
      return mapQuestion(state, action.id, (q) => ({
        ...q,
        options: (q.options ?? []).filter((o) => o.key !== action.optionKey),
      }))
    case 'moveOption':
      return mapQuestion(state, action.id, (q) => {
        const options = q.options ?? []
        const moved = move(
          options,
          options.findIndex((o) => o.key === action.optionKey),
          action.direction,
        )
        return moved === options ? q : { ...q, options: moved }
      })
    case 'updateOption':
      return mapQuestion(state, action.id, (q) => ({
        ...q,
        options: (q.options ?? []).map((o) =>
          o.key === action.optionKey ? { ...o, label: action.label } : o,
        ),
      }))
    case 'addRule': {
      const question = state.questions.find((q) => q.id === action.id)
      const source = sourcesFor(state.questions, action.id)[0]
      if (!question || !source) return state
      if (state.rules.some((rule) => rule.targetQuestionKey === question.key)) return state
      return {
        ...state,
        rules: [
          ...state.rules,
          { targetQuestionKey: question.key, match: 'ALL', conditions: [defaultCondition(source)] },
        ],
      }
    }
    case 'removeRule':
      return mapRule(state, action.id, () => null)
    case 'setRuleMatch':
      return mapRule(state, action.id, (rule) => ({ ...rule, match: action.match }))
    case 'addCondition': {
      const source = sourcesFor(state.questions, action.id)[0]
      if (!source) return state
      return mapRule(state, action.id, (rule) => ({
        ...rule,
        conditions: [...rule.conditions, defaultCondition(source)],
      }))
    }
    case 'removeCondition':
      // A rule without conditions cannot exist: removing the last one removes the rule.
      return mapRule(state, action.id, (rule) =>
        rule.conditions.length <= 1
          ? null
          : { ...rule, conditions: rule.conditions.filter((_, i) => i !== action.index) },
      )
    case 'setConditionSource': {
      const source = state.questions.find((q) => q.key === action.sourceKey)
      if (!source) return state
      return mapCondition(state, action.id, action.index, () => defaultCondition(source))
    }
    case 'setConditionOperator':
      return mapCondition(state, action.id, action.index, (condition) => ({
        sourceQuestionKey: condition.sourceQuestionKey,
        operator: action.operator,
        // The value survives only when the new operator takes the same kind of value.
        ...(shapeOf(condition.operator) === shapeOf(action.operator) && condition.value
          ? { value: condition.value }
          : {}),
      }))
    case 'setConditionValue':
      return mapCondition(state, action.id, action.index, (condition) => ({
        sourceQuestionKey: condition.sourceQuestionKey,
        operator: condition.operator,
        ...(action.value ? { value: action.value } : {}),
      }))
  }
}

/** The body of `PUT /surveys/{id}/draft`: only the fields that apply to each question's type travel. */
export function toDraftDefinition(state: DraftState): DraftDefinition {
  return {
    questions: state.questions.map((q) => {
      // The two flags are always present because the contract gives them a default.
      const input: QuestionInput = {
        key: q.key,
        type: q.type,
        text: q.text,
        required: q.required,
        multiline: q.type === 'TEXT' && Boolean(q.multiline),
        integerOnly: q.type === 'NUMBER' && Boolean(q.integerOnly),
      }
      if (isChoice(q.type)) {
        input.options = (q.options ?? []).map((o) => ({ key: o.key, label: o.label }))
      }
      if (q.type === 'MULTIPLE_CHOICE') {
        if (q.minSelections !== undefined) input.minSelections = q.minSelections
        if (q.maxSelections !== undefined) input.maxSelections = q.maxSelections
      }
      if (q.type === 'TEXT') {
        if (q.maxLength !== undefined) input.maxLength = q.maxLength
      }
      if (q.type === 'NUMBER') {
        if (q.min !== undefined) input.min = q.min
        if (q.max !== undefined) input.max = q.max
      }
      if (q.type === 'SCALE') {
        if (q.min !== undefined) input.min = q.min
        if (q.max !== undefined) input.max = q.max
        if (q.minLabel) input.minLabel = q.minLabel
        if (q.maxLabel) input.maxLabel = q.maxLabel
      }
      return input
    }),
    rules: state.rules.map((rule) => ({
      targetQuestionKey: rule.targetQuestionKey,
      match: rule.match,
      conditions: rule.conditions.map((condition) => {
        const value = compactValue(condition.value)
        return {
          sourceQuestionKey: condition.sourceQuestionKey,
          operator: condition.operator,
          ...(value ? { value } : {}),
        }
      }),
    })),
  }
}

function compactValue(value: ConditionValue | undefined): ConditionValue | undefined {
  if (!value) return undefined
  const compact: ConditionValue = {}
  if (value.optionKey !== undefined) compact.optionKey = value.optionKey
  if (value.optionKeys !== undefined && value.optionKeys.length > 0) {
    compact.optionKeys = value.optionKeys
  }
  if (value.number !== undefined) compact.number = value.number
  if (value.min !== undefined) compact.min = value.min
  if (value.max !== undefined) compact.max = value.max
  return Object.keys(compact).length > 0 ? compact : undefined
}
