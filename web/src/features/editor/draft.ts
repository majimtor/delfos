import type { Schemas } from '../../api/types'

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
      return {
        questions: state.questions.map((q) =>
          q.id === action.id ? retyped(q, action.questionType) : q,
        ),
        rules: withoutKey(state.rules, current.key),
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
    rules: state.rules,
  }
}
