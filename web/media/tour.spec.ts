import { mkdir } from 'node:fs/promises'
import path from 'node:path'
import { expect, test, type Page } from '@playwright/test'

// The stranger's visit of the end-to-end path, paused between steps so that the recording can be
// followed, against a locally seeded API. It writes the screenshots to docs/media and the video
// to test-results/media; both are looked at before being committed or published.
const api = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080'
const resetToken = process.env.E2E_RESET_TOKEN ?? 'e2e-reset-token'
const demoPassword = 'delfos-demo'
const beat = 2_000

const settle = (page: Page, ms = beat) => page.waitForTimeout(ms)

test.beforeAll(async ({ request }) => {
  const reset = await request.post(`${api}/api/v1/demo/reset`, {
    headers: { Authorization: `Bearer ${resetToken}` },
  })
  expect(reset.status(), 'the local API must run with the demo seed and its reset token').toBe(204)
})

test('the tour: answer the sample survey, manage it and watch the results move', async ({
  page,
  request,
}, testInfo) => {
  const webDir = path.resolve(testInfo.file, '../..')
  const mediaDir = path.resolve(webDir, '../docs/media')
  await mkdir(mediaDir, { recursive: true })
  const shot = (name: string, fullPage = false) =>
    page.screenshot({ path: path.join(mediaDir, `${name}.png`), fullPage })

  await test.step('landing', async () => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Delfos' })).toBeVisible()
    await settle(page, 3_000)
    await shot('landing')
    await settle(page)
    await page.getByRole('link', { name: 'Answer a sample survey' }).click()
    await expect(page.getByRole('heading', { name: 'Urban mobility' })).toBeVisible()
    await settle(page)
  })

  const licence = page.getByRole('group', { name: 'Do you have a driving licence?' })
  await test.step('a later question appears and disappears as the answers change', async () => {
    await page.getByRole('radio', { name: 'By car' }).check()
    await expect(licence).toBeVisible()
    await settle(page)
    await shot('answer', true)
    await settle(page)
    await page.getByRole('radio', { name: 'By bicycle' }).check()
    await expect(licence).toBeHidden()
    await settle(page)
    await page.getByRole('radio', { name: 'By public transport' }).check()
    await expect(licence).toBeVisible()
    await settle(page)
  })

  await test.step('the rest of the answers', async () => {
    await page.getByRole('checkbox', { name: 'The time' }).check()
    await settle(page, 700)
    await page.getByRole('checkbox', { name: 'Health' }).check()
    await settle(page)
    await page
      .getByRole('spinbutton', { name: 'How many kilometres do you travel a week?' })
      .pressSequentially('12', { delay: 150 })
    await settle(page)
    await page
      .getByRole('group', { name: 'How do you rate the way you travel?', exact: true })
      .getByRole('button', { name: '4', exact: true })
      .click()
    await settle(page)
    await page
      .getByRole('textbox', { name: 'Anything you would like to add?' })
      .pressSequentially('Fewer transfers would help.', { delay: 45 })
    await settle(page)
    await licence.getByRole('radio', { name: 'Yes' }).check()
    await settle(page)
    await page
      .getByRole('spinbutton', { name: 'How many years have you had it?' })
      .pressSequentially('3', { delay: 150 })
    await settle(page)
    await page
      .getByRole('group', { name: 'How likely are you to switch to a bicycle this year?' })
      .getByRole('button', { name: '7', exact: true })
      .click()
    await settle(page)
    await page.getByRole('button', { name: 'Send my response' }).click()
    await expect(page.getByRole('heading', { name: 'Thank you' })).toBeVisible()
    await settle(page, 2_500)
  })

  await test.step('the manager logs in', async () => {
    await page.goto('/login')
    await settle(page)
    await page.getByLabel('Username').pressSequentially('demo-manager', { delay: 60 })
    await page.getByLabel('Password').pressSequentially(demoPassword, { delay: 60 })
    await settle(page, 700)
    await page.getByRole('button', { name: 'Enter' }).click()
    await expect(page.getByRole('heading', { name: 'My surveys' })).toBeVisible()
    await settle(page, 2_000)
    await shot('surveys', true)
  })

  const table = page.getByRole('table', { name: 'surveys' })
  await test.step('the draft is published and then closed', async () => {
    const reading = table.getByRole('row', { name: /Reading habits/ })
    await expect(reading).toContainText('Draft')
    await reading.getByRole('button', { name: 'Publish' }).click()
    await expect(reading).toContainText('Published')
    await settle(page, 2_000)
    await reading.getByRole('button', { name: 'Close' }).click()
    await expect(reading).toContainText('Closed')
    await settle(page, 2_000)
  })

  await test.step('the results page updates when a response arrives', async () => {
    await table
      .getByRole('row', { name: /Urban mobility/ })
      .getByRole('link', { name: 'Results' })
      .click()
    await expect(page.getByRole('heading', { name: 'Urban mobility' })).toBeVisible()
    const summary = page.getByRole('status').filter({ hasText: 'responses' })
    await expect(summary).toContainText('21 responses')
    await settle(page, 2_500)
    await shot('results')
    const submitted = await request.post(`${api}/api/v1/public/surveys/demo-mobility/responses`, {
      data: {
        versionNumber: 1,
        answers: [
          { questionKey: 'transport', optionKey: 'foot' },
          { questionKey: 'km', number: 3 },
          { questionKey: 'satisfaction', number: 5 },
        ],
      },
    })
    expect(submitted.status()).toBe(201)
    await expect(summary).toContainText('22 responses', { timeout: 20_000 })
    await settle(page, 4_000)
  })

  // The recording is complete only once the page is closed.
  const video = page.video()
  await page.close()
  await video?.saveAs(path.resolve(webDir, 'test-results/media/tour.webm'))
})
