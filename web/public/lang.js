// What the document needs before the application runs, in a file of its own so the content
// security policy can allow scripts from this origin only.
//
// The interface language: Chrome decides whether to offer a translation when parsing ends, and
// assistive technology reads the attribute from the first paint. The same rule as initialLocale in
// src/i18n/locale.ts; a test keeps them in step.
//
// The colour scheme: the visitor's choice (src/app/colorScheme.ts keeps the key and the attribute),
// or the system's when there is none, so the first paint already has the right background and the
// theme takes over without a flash.
;(function () {
  let locale = null
  let mode = null
  try {
    locale = localStorage.getItem('delfos.locale')
    mode = localStorage.getItem('delfos.mode')
  } catch {
    // Without storage the preferences are simply not read.
  }
  document.documentElement.lang =
    locale === 'es' || locale === 'en'
      ? locale
      : navigator.language.toLowerCase().startsWith('en')
        ? 'en'
        : 'es'
  const systemDark =
    typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: dark)').matches
  const dark = mode === 'dark' || (mode !== 'light' && systemDark)
  document.documentElement.setAttribute('data-delfos-scheme', dark ? 'dark' : 'light')
})()
