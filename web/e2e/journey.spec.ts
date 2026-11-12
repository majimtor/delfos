import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

// The path a stranger takes through the demo, as the plan's definition of done describes it,
// against a locally seeded API. Every page visited is also checked by axe against WCAG 2.2 AA.
const api = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080'
const resetToken = process.env.E2E_RESET_TOKEN ?? 'e2e-reset-token'
const demoPassword = 'delfos-demo'

async function expectAccessible(page: Page, name: string) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze()
  const found = results.violations.map((v) => ({
    id: v.id,
    impact: v.impact,
    nodes: v.nodes.map((n) => ({ target: n.target, html: n.html.slice(0, 200) })),
  }))
  expect(found, `accessibility violations on ${name}`).toEqual([])
}

async function login(page: Page, username: string) {
  await page.goto('/login')
  await page.getByLabel('Usuario').fill(username)
  await page.getByLabel('Contraseña').fill(demoPassword)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()
}

test.beforeAll(async ({ request }) => {
  const reset = await request.post(`${api}/api/v1/demo/reset`, {
    headers: { Authorization: `Bearer ${resetToken}` },
  })
  expect(reset.status(), 'the local API must run with the demo seed and its reset token').toBe(204)
})

test('a stranger answers the sample survey, then manages it and watches the results move', async ({
  page,
  request,
}) => {
  await test.step('the landing page leads to the sample survey', async () => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Delfos' })).toBeVisible()
    await expect(page).toHaveTitle('Delfos')
    await expectAccessible(page, 'the landing page')
    // The first Tab stop skips the header, for keyboard users.
    await page.keyboard.press('Tab')
    await expect(page.getByRole('link', { name: 'Saltar al contenido' })).toBeFocused()
    await page.getByRole('link', { name: 'Responder una encuesta de ejemplo' }).click()
    await expect(page.getByRole('heading', { name: 'Movilidad urbana' })).toBeVisible()
    await expect(page).toHaveTitle('Movilidad urbana · Delfos')
    // After a navigation the focus starts from the main region, not from the top of the document.
    await expect(page.getByRole('main')).toBeFocused()
  })

  const licence = page.getByRole('group', { name: '¿Tienes carnet de conducir?' })
  await test.step('a later question appears and disappears as the answers change', async () => {
    await expect(licence).toBeHidden()
    await page.getByRole('radio', { name: 'En coche' }).check()
    await expect(licence).toBeVisible()
    await page.getByRole('radio', { name: 'En bicicleta' }).check()
    await expect(licence).toBeHidden()
    await expectAccessible(page, 'the answer page')
  })

  await test.step('the response is accepted', async () => {
    await page.getByRole('radio', { name: 'En transporte público' }).check()
    await page.getByRole('checkbox', { name: 'El tiempo' }).check()
    await page
      .getByRole('spinbutton', { name: '¿Cuántos kilómetros recorres a la semana?' })
      .fill('12')
    await page
      .getByRole('group', { name: '¿Cómo valoras tu forma de desplazarte?', exact: true })
      .getByRole('button', { name: '4', exact: true })
      .click()
    await licence.getByRole('radio', { name: 'Sí' }).check()
    await page.getByRole('spinbutton', { name: '¿Cuántos años llevas con carnet?' }).fill('3')
    await page.getByRole('button', { name: 'Enviar mi respuesta' }).click()
    await expect(page.getByRole('heading', { name: 'Gracias' })).toBeVisible()
  })

  await test.step('the manager logs in and sees the new response counted', async () => {
    await page.goto('/login')
    await expectAccessible(page, 'the login page')
    await login(page, 'demo-manager')
    await expect(page).toHaveTitle('Mis encuestas · Delfos')
    await expectAccessible(page, 'the survey list')
    const mobility = page.getByRole('table', { name: 'surveys' }).getByRole('row', {
      name: /Movilidad urbana/,
    })
    await expect(mobility.getByRole('cell').nth(2)).toHaveText('41')
  })

  await test.step('the draft is published and then closed', async () => {
    const reading = page.getByRole('table', { name: 'surveys' }).getByRole('row', {
      name: /Hábitos de lectura/,
    })
    await expect(reading).toContainText('Borrador')
    await reading.getByRole('button', { name: 'Publicar' }).click()
    await expect(reading).toContainText('Publicada')
    await reading.getByRole('button', { name: 'Cerrar' }).click()
    await expect(reading).toContainText('Cerrada')
  })

  await test.step('the results page updates when a response arrives', async () => {
    await page
      .getByRole('table', { name: 'surveys' })
      .getByRole('row', { name: /Movilidad urbana/ })
      .getByRole('link', { name: 'Resultados' })
      .click()
    await expect(page.getByRole('heading', { name: 'Movilidad urbana' })).toBeVisible()
    const summary = page.getByRole('status').filter({ hasText: 'respuestas' })
    await expect(summary).toContainText('41 respuestas')
    await expectAccessible(page, 'the results page')

    const submitted = await request.post(`${api}/api/v1/public/surveys/demo-movilidad/responses`, {
      data: {
        versionNumber: 1,
        answers: [
          { questionKey: 'transporte', optionKey: 'pie' },
          { questionKey: 'km', number: 3 },
          { questionKey: 'satisfaccion', number: 5 },
        ],
      },
    })
    expect(submitted.status()).toBe(201)
    await expect(summary).toContainText('42 respuestas', { timeout: 20_000 })
  })
})

test('a manager sees only their own surveys', async ({ page }) => {
  await login(page, 'demo-manager-2')
  const table = page.getByRole('table', { name: 'surveys' })
  await expect(table.getByRole('row')).toHaveCount(3)
  await expect(table).toContainText('Teletrabajo en el equipo')
  await expect(table).toContainText('Remote work in the team')
  await expect(table).not.toContainText('Movilidad urbana')
})

test('a manager creates a survey, edits its draft and publishes it', async ({ page }) => {
  await login(page, 'demo-manager-2')
  await page.getByRole('link', { name: 'Nueva encuesta' }).click()
  await expect(page.getByRole('heading', { name: 'Nueva encuesta' })).toBeVisible()
  await expectAccessible(page, 'the new survey page')
  await page.getByLabel('Título').fill('Clima del equipo')
  await page.getByRole('button', { name: 'Crear y editar las preguntas' }).click()
  await expect(page.getByRole('heading', { name: 'Clima del equipo' })).toBeVisible()
  await expect(page).toHaveTitle('Borrador de Clima del equipo · Delfos')
  await expect(page.getByText('El borrador todavía no tiene preguntas.')).toBeVisible()

  // An empty draft is refused before any request leaves the browser.
  await page.getByRole('button', { name: 'Guardar y publicar' }).click()
  await expect(page.getByRole('alert')).toContainText(
    'Añade al menos una pregunta antes de publicar.',
  )

  await page.getByRole('button', { name: 'Añadir una pregunta' }).click()
  await page.getByRole('menuitem', { name: 'Elección única' }).click()
  await expect(page.locator('[role="menu"]')).toHaveCount(0)
  const first = page.getByRole('region', { name: 'Pregunta 1' })
  await first.getByLabel('Texto de la pregunta').fill('¿Cómo valoras el ambiente del equipo?')
  await first.getByLabel('Opción 1', { exact: true }).fill('Bueno')
  await first.getByLabel('Opción 2', { exact: true }).fill('Mejorable')
  await page.getByRole('button', { name: 'Añadir una pregunta' }).click()
  await page.getByRole('menuitem', { name: 'Escala' }).click()
  // The menu fades out hidden from the accessibility tree and is removed afterwards; axe must not
  // measure the contrast of its items mid-fade, so wait for the removal, not for the hiding.
  await expect(page.locator('[role="menu"]')).toHaveCount(0)
  const second = page.getByRole('region', { name: 'Pregunta 2' })
  await second.getByLabel('Texto de la pregunta').fill('¿Recomendarías el equipo?')
  await expectAccessible(page, 'the editor')

  await page.getByRole('button', { name: 'Guardar el borrador' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Borrador guardado.' })).toBeVisible()
  await page.getByRole('button', { name: 'Guardar y publicar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()
  const row = page
    .getByRole('table', { name: 'surveys' })
    .getByRole('row', { name: /Clima del equipo/ })
  await expect(row).toContainText('Publicada')
})
