import { describe, expect, it } from 'vitest'

/**
 * Material UI applies the colour of a Typography only in the form `textSecondary`: the dotted form
 * `text.secondary` (which Link and sx accept) is ignored in silence, and the text comes out in the
 * primary colour. This keeps the dotted form out of every Typography tag of the sources.
 */
const sources = import.meta.glob('../**/*.tsx', { query: '?raw', import: 'default', eager: true })

describe('the colour of a Typography', () => {
  it('is never asked for in the dotted form, which Typography ignores', () => {
    const offenders = Object.entries(sources).flatMap(([file, source]) => {
      const tags = (source as string).match(/<Typography\b[^>]*?>/gs) ?? []
      return tags.filter((tag) => /color="text\./.test(tag)).map(() => file)
    })
    expect(offenders).toEqual([])
  })
})
