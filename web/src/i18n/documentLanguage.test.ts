import { describe, expect, it } from 'vitest'
import html from '../../index.html?raw'
import inlineScript from '../../public/lang.js?raw'
import { LOCALE_STORAGE_KEY, initialLocale } from './locale'

// The script index.html loads first, before the application, must choose as initialLocale does.

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
