import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { expect, test } from '@playwright/test'
import { demoPassword, resetDemo } from './demo.ts'

// The screenshots of the README, in docs/media, from the seeded state of the demo. They are
// taken without recording because a full-page capture resizes the viewport for an instant,
// which a video would show.
test.use({ video: 'off' })

test.beforeAll(async ({ request }) => resetDemo(request))

test('landing, answer page, survey list and results', async ({ page }, testInfo) => {
  const mediaDir = path.resolve(testInfo.file, '../../../docs/media')
  await mkdir(mediaDir, { recursive: true })
  const shot = (name: string, fullPage = false) =>
    page.screenshot({ path: path.join(mediaDir, `${name}.png`), fullPage })

  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Delfos' })).toBeVisible()
  await shot('landing')

  await page.getByRole('link', { name: 'Answer a sample survey' }).click()
  await expect(page.getByRole('heading', { name: 'Urban mobility' })).toBeVisible()
  await page.getByRole('radio', { name: 'By car' }).check()
  await expect(page.getByRole('group', { name: 'Do you have a driving licence?' })).toBeVisible()
  await shot('answer', true)

  await page.goto('/login')
  await page.getByLabel('Username').fill('demo-manager')
  await page.getByLabel('Password').fill(demoPassword)
  await page.getByRole('button', { name: 'Enter' }).click()
  await expect(page.getByRole('heading', { name: 'My surveys' })).toBeVisible()
  const table = page.getByRole('table', { name: 'surveys' })
  await expect(table.getByRole('row', { name: /Reading habits/ })).toContainText('Draft')
  await shot('surveys', true)

  await table
    .getByRole('row', { name: /Urban mobility/ })
    .getByRole('link', { name: 'Results' })
    .click()
  await expect(page.getByRole('heading', { name: 'Urban mobility' })).toBeVisible()
  await expect(page.getByRole('status').filter({ hasText: 'responses' })).toContainText(
    '20 responses',
  )
  await shot('results')
})
