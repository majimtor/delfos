import { describe, expect, it } from 'vitest'
import html from '../../index.html?raw'
import inlineScript from '../../public/lang.js?raw'
import { MODE_STORAGE_KEY, SCHEME_ATTRIBUTE } from '../app/colorScheme'
import { LOCALE_STORAGE_KEY, initialLocale } from './locale'

// The script index.html loads first, before the application, must choose the language as
// initialLocale does and the colour scheme as the theme will.

function languageChosenBeforeTheApp(navigatorLanguage: string, stored: string | null): string {
  Object.defineProperty(window.navigator, 'language', {
    value: navigatorLanguage,
    configurable: true,
  })
  window.localStorage.clear()
  if (stored !== null) window.localStorage.setItem(LOCALE_STORAGE_KEY, stored)
  document.documentElement.lang = ''
  new Function(inlineScript)()
  return document.documentElement.lang
}

function schemeChosenBeforeTheApp(
  stored: string | null,
  systemDark: boolean | null,
): string | null {
  window.localStorage.clear()
  if (stored !== null) window.localStorage.setItem(MODE_STORAGE_KEY, stored)
  // jsdom has no matchMedia; a browser answers the dark-scheme query, an old one may lack it.
  Object.defineProperty(window, 'matchMedia', {
    value:
      systemDark === null
        ? undefined
        : (query: string) => ({ matches: systemDark && query.includes('dark'), media: query }),
    configurable: true,
  })
  document.documentElement.removeAttribute(SCHEME_ATTRIBUTE)
  new Function(inlineScript)()
  return document.documentElement.getAttribute(SCHEME_ATTRIBUTE)
}

describe('the document language before the application runs', () => {
  it('is set by the script index.html loads before the application', () => {
    expect(html).toContain('<script src="/lang.js"></script>')
    expect(inlineScript).toContain('document.documentElement.lang')
  })

  it.each([
    ['en-US', null],
    ['en-GB', null],
    ['es-ES', null],
    ['fr-FR', null],
    ['en-US', 'es'],
    ['es-ES', 'en'],
    ['es-ES', 'other'],
  ])('agrees with initialLocale for %s with %s stored', (navigatorLanguage, stored) => {
    expect(languageChosenBeforeTheApp(navigatorLanguage, stored)).toBe(
      initialLocale(navigatorLanguage, stored),
    )
  })
})

describe('the colour scheme before the application runs', () => {
  it('reads the key and writes the attribute the theme uses', () => {
    expect(inlineScript).toContain(`'${MODE_STORAGE_KEY}'`)
    expect(inlineScript).toContain(`'${SCHEME_ATTRIBUTE}'`)
  })

  it.each([
    ['dark', false, 'dark'],
    ['light', true, 'light'],
    ['system', true, 'dark'],
    ['system', false, 'light'],
    [null, true, 'dark'],
    [null, false, 'light'],
    [null, null, 'light'],
  ])('with %s stored and a dark device %s chooses %s', (stored, systemDark, expected) => {
    expect(schemeChosenBeforeTheApp(stored, systemDark)).toBe(expected)
  })
})
