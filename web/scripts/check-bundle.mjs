// The budget of the built web (ADR 0017): what a visitor downloads on a first visit must stay
// within these sizes, so a dependency or a font cannot grow the page unnoticed. Run after
// `vite build`; it reads dist/index.html for the scripts a first visit loads (the entry and what
// it preloads; the manager's pages load later, each as its own chunk) and fails when a budget is
// exceeded. The total of every chunk is printed for information.
import { readFileSync, readdirSync, statSync } from 'node:fs'
import path from 'node:path'

const dist = 'dist'
const assets = path.join(dist, 'assets')
const html = readFileSync(path.join(dist, 'index.html'), 'utf8')
const firstVisit = [...html.matchAll(/(?:src|href)="\/(assets\/[^"]+\.js)"/g)].map((m) => m[1])
const size = (file) => statSync(path.join(dist, file)).size
const files = readdirSync(assets)
const total = (pattern) =>
  files
    .filter((f) => pattern.test(f))
    .reduce((sum, f) => sum + statSync(path.join(assets, f)).size, 0)

const budgets = [
  ['JavaScript of a first visit', firstVisit.reduce((sum, f) => sum + size(f), 0), 900_000],
  ['CSS', total(/\.css$/), 60_000],
  // The only font file a Spanish or English visitor downloads: the Latin subset, weight axis.
  ['Latin font', total(/latin-wght-normal.*\.woff2$/), 50_000],
]
let failed = false
for (const [name, bytes, budget] of budgets) {
  const ok = bytes <= budget
  if (!ok) failed = true
  console.log(`${ok ? 'within budget' : 'OVER BUDGET'}: ${name} ${bytes} bytes of ${budget}`)
}
console.log(
  `for information: every JavaScript chunk ${total(/\.js$/)} bytes in ${files.filter((f) => f.endsWith('.js')).length} files`,
)
process.exit(failed ? 1 : 0)
