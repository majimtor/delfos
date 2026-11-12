import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { IntlProvider } from 'react-intl'
import en from './en.json'
import es from './es.json'
import { LOCALE_STORAGE_KEY, LocaleContext, initialLocale, type Locale } from './locale'

const catalogs: Record<Locale, Record<string, string>> = { es, en }

function readStored(): string | null {
  try {
    return window.localStorage.getItem(LOCALE_STORAGE_KEY)
  } catch {
    return null
  }
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() =>
    initialLocale(navigator.language, readStored()),
  )
  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next)
    try {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, next)
    } catch {
      // A visitor without storage simply loses the preference on reload.
    }
  }, [])
  // The document's language follows the interface from the first render, not only after a change.
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  const value = useMemo(() => ({ locale, setLocale }), [locale, setLocale])
  return (
    <LocaleContext.Provider value={value}>
      <IntlProvider locale={locale} defaultLocale="es" messages={catalogs[locale]}>
        {children}
      </IntlProvider>
    </LocaleContext.Provider>
  )
}
