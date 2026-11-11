import { createContext, useContext } from 'react'

export type Locale = 'es' | 'en'

export interface LocaleContextValue {
  locale: Locale
  setLocale: (locale: Locale) => void
}

export const LocaleContext = createContext<LocaleContextValue>({
  locale: 'es',
  setLocale: () => {},
})

export const LOCALE_STORAGE_KEY = 'delfos.locale'

/** Spanish first; English when the browser prefers it or the visitor chose it before. */
export function initialLocale(navigatorLanguage: string, stored: string | null): Locale {
  if (stored === 'es' || stored === 'en') return stored
  return navigatorLanguage.toLowerCase().startsWith('en') ? 'en' : 'es'
}

export function useLocale(): LocaleContextValue {
  return useContext(LocaleContext)
}
