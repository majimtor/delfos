// English lives in the code as each message's defaultMessage; en.json is derived from it with
// `npm run i18n:sync-en` and es.json is the translation. This check fails when en.json differs from
// the code, or when es.json lacks a message or keeps one the code no longer uses.
import { readFileSync } from 'node:fs'

const extracted = JSON.parse(readFileSync('build/messages.json', 'utf8'))
const en = JSON.parse(readFileSync('src/i18n/en.json', 'utf8'))
const es = JSON.parse(readFileSync('src/i18n/es.json', 'utf8'))
const ids = Object.keys(extracted).sort()
let failed = false

const enDrift = ids.filter((id) => en[id] !== extracted[id])
const enStale = Object.keys(en).filter((id) => !(id in extracted))
if (enDrift.length > 0 || enStale.length > 0) {
  failed = true
  console.error(
    `src/i18n/en.json differs from the code; run npm run i18n:sync-en (${enDrift.length} changed, ${enStale.length} unused)`,
  )
}
const esMissing = ids.filter((id) => !(id in es))
const esStale = Object.keys(es).filter((id) => !(id in extracted))
if (esMissing.length > 0) {
  failed = true
  console.error(`src/i18n/es.json: missing ${esMissing.length} message(s): ${esMissing.join(', ')}`)
}
if (esStale.length > 0) {
  failed = true
  console.error(`src/i18n/es.json: ${esStale.length} unused message(s): ${esStale.join(', ')}`)
}
console.log(
  `${ids.length} messages in code; en.json ${Object.keys(en).length}, es.json ${Object.keys(es).length}`,
)
process.exit(failed ? 1 : 0)
