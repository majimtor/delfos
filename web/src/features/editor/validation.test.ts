import { describe, expect, it } from 'vitest'
import type { DraftQuestion, DraftState } from './draft'
import { validateDraft } from './validation'

const question = (overrides: Partial<DraftQuestion>): DraftQuestion => ({
  id: 'q',
  key: 'q',
  type: 'TEXT',
  text: 'Fine',
  required: false,
  ...overrides,
})

const state = (...questions: DraftQuestion[]): DraftState => ({ questions, rules: [] })

describe('validateDraft', () => {
  it('accepts a valid draft and asks for a question only when publishing', () => {
    expect(validateDraft(state(question({})))).toEqual([])
    expect(validateDraft(state())).toEqual([])
    expect(validateDraft(state(), 'publish')).toEqual([{ path: 'questions', code: 'NO_QUESTIONS' }])
  })

  it('requires the text of every question and bounds its length', () => {
    expect(validateDraft(state(question({ text: '  ' })))).toEqual([
      { path: 'questions[0].text', code: 'REQUIRED' },
    ])
    expect(validateDraft(state(question({ text: 'x'.repeat(1001) })))).toEqual([
      { path: 'questions[0].text', code: 'TOO_LONG', params: { max: 1000 } },
    ])
  })

  it('checks the options of the choice types and the selection bounds of a multiple choice', () => {
    const one = question({
      id: 'c',
      key: 'c',
      type: 'SINGLE_CHOICE',
      options: [{ key: 'a', label: 'A' }],
    })
    expect(validateDraft(state(one))).toEqual([
      { path: 'questions[0].options', code: 'TOO_FEW_ITEMS', params: { min: 2 } },
    ])
    const blank = question({
      type: 'SINGLE_CHOICE',
      options: [
        { key: 'a', label: 'A' },
        { key: 'b', label: ' ' },
      ],
    })
    expect(validateDraft(state(blank))).toEqual([
      { path: 'questions[0].options[1].label', code: 'REQUIRED' },
    ])
    const options = [
      { key: 'a', label: 'A' },
      { key: 'b', label: 'B' },
    ]
    expect(
      validateDraft(state(question({ type: 'MULTIPLE_CHOICE', options, minSelections: 3 }))),
    ).toEqual([{ path: 'questions[0].minSelections', code: 'INVALID_VALUE' }])
    expect(
      validateDraft(state(question({ type: 'MULTIPLE_CHOICE', options, maxSelections: 3 }))),
    ).toEqual([{ path: 'questions[0].maxSelections', code: 'INVALID_VALUE' }])
    expect(
      validateDraft(
        state(question({ type: 'MULTIPLE_CHOICE', options, minSelections: 1, maxSelections: 2 })),
      ),
    ).toEqual([])
  })

  it('checks the bounds of text, number and scale questions', () => {
    expect(validateDraft(state(question({ maxLength: 0 })))).toEqual([
      { path: 'questions[0].maxLength', code: 'OUT_OF_RANGE', params: { min: 1, max: 2000 } },
    ])
    expect(validateDraft(state(question({ type: 'NUMBER', min: 10, max: 5 })))).toEqual([
      { path: 'questions[0].max', code: 'INVALID_VALUE' },
    ])
    expect(validateDraft(state(question({ type: 'SCALE' })))).toEqual([
      { path: 'questions[0].max', code: 'REQUIRED' },
    ])
    expect(validateDraft(state(question({ type: 'SCALE', max: 11, min: 2 })))).toEqual([
      { path: 'questions[0].max', code: 'OUT_OF_RANGE', params: { min: 3, max: 10 } },
      { path: 'questions[0].min', code: 'OUT_OF_RANGE', params: { min: 0, max: 1 } },
    ])
    expect(validateDraft(state(question({ type: 'SCALE', max: 7, min: 0 })))).toEqual([])
  })
})
