// The interface language before the application runs: Chrome decides whether to offer a
// translation when parsing ends, and assistive technology reads the attribute from the first
// paint. The same rule as initialLocale in src/i18n/locale.ts; a test keeps them in step. A file of
// its own, not an inline script, so the content security policy can allow scripts from this origin only.
;(function () {
  let stored = null
  try {
    stored = localStorage.getItem('delfos.locale')
  } catch {
    // Without storage the preference is simply not read.
  }
  document.documentElement.lang =
    stored === 'es' || stored === 'en'
      ? stored
      : navigator.language.toLowerCase().startsWith('en')
        ? 'en'
        : 'es'
})()
