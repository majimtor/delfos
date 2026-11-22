// The budget of the built web (ADR 0017): what a visitor downloads on a first visit must stay
// within these sizes, so a dependency or a font cannot grow the page unnoticed. Run after
// `vite build`; it reads dist/assets and fails when a budget is exceeded.
import { readdirSync, statSync } from 'node:fs'
import path from 'node:path'

const assets = 'dist/assets'
const files = readdirSync(assets)
const total = (pattern) =>
  files
    .filter((f) => pattern.test(f))
    .reduce((sum, f) => sum + statSync(path.join(assets, f)).size, 0)

const budgets = [
  ['JavaScript', total(/\.js$/), 900_000],
  ['CSS', total(/\.css$/), 60_000],
  // The only font file a Spanish or English visitor downloads: the Latin subset, weight axis.
  ['Latin font', total(/latin-wght-normal.*\.woff2$/), 50_000],
]
let failed = false
for (const [name, size, budget] of budgets) {
  const ok = size <= budget
  if (!ok) failed = true
  console.log(`${ok ? 'within budget' : 'OVER BUDGET'}: ${name} ${size} bytes of ${budget}`)
}
process.exit(failed ? 1 : 0)
