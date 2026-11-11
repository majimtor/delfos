import { describe, expect, it } from 'vitest'
import en from './en.json'
import es from './es.json'
import { initialLocale } from './locale'

describe('initialLocale', () => {
  it('prefers a stored choice', () => {
    expect(initialLocale('en-US', 'es')).toBe('es')
  })
  it('falls back to Spanish unless the browser prefers English', () => {
    expect(initialLocale('es-ES', null)).toBe('es')
    expect(initialLocale('fr-FR', null)).toBe('es')
    expect(initialLocale('en-GB', null)).toBe('en')
  })
})

describe('catalogs', () => {
  it('have the same keys in both languages', () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(es).sort())
  })
})
