import { describe, expect, it } from 'vitest'
import {
  draftReducer,
  fromVersion,
  newKey,
  toDraftDefinition,
  type DraftState,
  type SurveyVersion,
} from './draft'

const version: SurveyVersion = {
  number: 1,
  status: 'DRAFT',
  createdAt: '2026-01-01T00:00:00Z',
  questions: [
    {
      key: 'books',
      type: 'NUMBER',
      text: 'How many books?',
      required: true,
      min: 0,
      max: 200,
      integerOnly: true,
      multiline: false,
    },
    {
      key: 'formats',
      type: 'MULTIPLE_CHOICE',
      text: 'Which formats?',
      required: false,
      multiline: false,
      integerOnly: false,
      options: [
        { key: 'paper', label: 'Paper' },
        { key: 'digital', label: 'Digital' },
      ],
    },
    {
      key: 'recommendation',
      type: 'TEXT',
      text: 'Recommend a book.',
      required: false,
      multiline: false,
      integerOnly: false,
      maxLength: 150,
    },
  ],
  rules: [
    {
      targetQuestionKey: 'recommendation',
      match: 'ALL',
      conditions: [{ sourceQuestionKey: 'books', operator: 'GT', value: { number: 0 } }],
    },
  ],
}

const loaded = (): DraftState => fromVersion(version)

describe('draftReducer', () => {
  it('generates keys in the shape the server assigns', () => {
    expect(newKey('q')).toMatch(/^q-[A-Za-z0-9]{8}$/)
    expect(newKey('o')).toMatch(/^o-[A-Za-z0-9]{8}$/)
    expect(newKey('q')).not.toBe(newKey('q'))
  })

  it('adds a question with the defaults of its type, at the end', () => {
    const state = draftReducer(loaded(), { type: 'addQuestion', questionType: 'SINGLE_CHOICE' })
    const added = state.questions[3]!
    expect(added.type).toBe('SINGLE_CHOICE')
    expect(added.text).toBe('')
    expect(added.required).toBe(false)
    expect(added.options).toHaveLength(2)
    expect(added.id).toBe(added.key)
    const scale = draftReducer(state, { type: 'addQuestion', questionType: 'SCALE' }).questions[4]!
    expect(scale.max).toBe(5)
    expect(scale.options).toBeUndefined()
  })

  it('removes a question together with the rules that mention it', () => {
    const withoutBooks = draftReducer(loaded(), { type: 'removeQuestion', id: 'books' })
    expect(withoutBooks.questions.map((q) => q.key)).toEqual(['formats', 'recommendation'])
    expect(withoutBooks.rules).toEqual([])
    const withoutFormats = draftReducer(loaded(), { type: 'removeQuestion', id: 'formats' })
    expect(withoutFormats.rules).toHaveLength(1)
  })

  it('moves a question one place and ignores a move past the ends', () => {
    const down = draftReducer(loaded(), { type: 'moveQuestion', id: 'books', direction: 'down' })
    expect(down.questions.map((q) => q.key)).toEqual(['formats', 'books', 'recommendation'])
    const up = draftReducer(down, { type: 'moveQuestion', id: 'books', direction: 'up' })
    expect(up.questions.map((q) => q.key)).toEqual(['books', 'formats', 'recommendation'])
    expect(draftReducer(up, { type: 'moveQuestion', id: 'books', direction: 'up' })).toBe(up)
  })

  it('updates the fields of a question in place', () => {
    const state = draftReducer(loaded(), {
      type: 'updateQuestion',
      id: 'books',
      patch: { text: 'How many books did you read?', required: false, max: undefined },
    })
    expect(state.questions[0]).toMatchObject({
      key: 'books',
      text: 'How many books did you read?',
      required: false,
      min: 0,
    })
    expect(state.questions[0]!.max).toBeUndefined()
  })

  it('gives a question a new key when its type changes, keeps the options between choice types and drops its rules', () => {
    const toText = draftReducer(loaded(), { type: 'changeType', id: 'books', questionType: 'TEXT' })
    const retyped = toText.questions[0]!
    expect(retyped.id).toBe('books')
    expect(retyped.key).not.toBe('books')
    expect(retyped.type).toBe('TEXT')
    expect(retyped.text).toBe('How many books?')
    expect(retyped.required).toBe(true)
    expect(retyped.min).toBeUndefined()
    expect(toText.rules).toEqual([])

    const toSingle = draftReducer(loaded(), {
      type: 'changeType',
      id: 'formats',
      questionType: 'SINGLE_CHOICE',
    })
    expect(toSingle.questions[1]!.options?.map((o) => o.key)).toEqual(['paper', 'digital'])
    expect(toSingle.questions[1]!.key).not.toBe('formats')

    expect(
      draftReducer(loaded(), { type: 'changeType', id: 'books', questionType: 'NUMBER' }),
    ).toEqual(loaded())
  })

  it('adds, edits, moves and removes options', () => {
    let state = draftReducer(loaded(), { type: 'addOption', id: 'formats' })
    expect(state.questions[1]!.options).toHaveLength(3)
    const added = state.questions[1]!.options![2]!
    state = draftReducer(state, {
      type: 'updateOption',
      id: 'formats',
      optionKey: added.key,
      label: 'Audio',
    })
    expect(state.questions[1]!.options![2]!.label).toBe('Audio')
    state = draftReducer(state, {
      type: 'moveOption',
      id: 'formats',
      optionKey: added.key,
      direction: 'up',
    })
    expect(state.questions[1]!.options!.map((o) => o.label)).toEqual(['Paper', 'Audio', 'Digital'])
    state = draftReducer(state, { type: 'removeOption', id: 'formats', optionKey: 'paper' })
    expect(state.questions[1]!.options!.map((o) => o.label)).toEqual(['Audio', 'Digital'])
  })
})

describe('toDraftDefinition', () => {
  it('sends only the fields that apply to each type and keeps the rules', () => {
    let state = loaded()
    state = draftReducer(state, {
      type: 'updateQuestion',
      id: 'recommendation',
      patch: { multiline: false, min: 3 },
    })
    state = draftReducer(state, { type: 'addQuestion', questionType: 'SCALE' })
    const scale = state.questions[3]!
    state = draftReducer(state, {
      type: 'updateQuestion',
      id: scale.id,
      patch: { text: 'Rate it', minLabel: '', maxLabel: 'Great' },
    })
    const definition = toDraftDefinition(state)
    expect(definition.rules).toEqual(version.rules)
    expect(definition.questions[0]).toEqual({
      key: 'books',
      type: 'NUMBER',
      text: 'How many books?',
      required: true,
      multiline: false,
      integerOnly: true,
      min: 0,
      max: 200,
    })
    expect(definition.questions[1]).toEqual({
      key: 'formats',
      type: 'MULTIPLE_CHOICE',
      text: 'Which formats?',
      required: false,
      multiline: false,
      integerOnly: false,
      options: [
        { key: 'paper', label: 'Paper' },
        { key: 'digital', label: 'Digital' },
      ],
    })
    expect(definition.questions[2]).toEqual({
      key: 'recommendation',
      type: 'TEXT',
      text: 'Recommend a book.',
      required: false,
      multiline: false,
      integerOnly: false,
      maxLength: 150,
    })
    expect(definition.questions[3]).toEqual({
      key: scale.key,
      type: 'SCALE',
      text: 'Rate it',
      required: false,
      multiline: false,
      integerOnly: false,
      max: 5,
      maxLabel: 'Great',
    })
  })
})
