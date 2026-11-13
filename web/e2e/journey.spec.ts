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

async function login(page: Page, username: string, password = demoPassword) {
  await page.goto('/login')
  await page.getByLabel('Usuario').fill(username)
  await page.getByLabel('Contraseña').fill(password)
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

test('a manager creates a survey, publishes it with a rule and then a second version', async ({
  page,
}) => {
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
  await second.getByLabel('Texto de la pregunta').fill('¿Qué mejorarías primero?')
  await expect(first.getByText(/Siempre visible/)).toBeVisible()

  // The second question depends on the first: shown only when the answer is "Mejorable".
  await second.getByLabel(/Mostrar solo cuando/).check()
  const condition = second.getByRole('group', { name: 'Condición 1' })
  await expect(condition.getByRole('combobox', { name: 'Depende de la pregunta' })).toContainText(
    '1. ¿Cómo valoras el ambiente del equipo?',
  )
  await expect(condition.getByRole('combobox', { name: 'Operador' })).toContainText('es')
  // The value is chosen with the keyboard: the select opens with Enter and the option with Enter.
  const option = condition.getByRole('combobox', { name: 'Opción' })
  await option.focus()
  await page.keyboard.press('Enter')
  await page.getByRole('option', { name: 'Mejorable' }).focus()
  await page.keyboard.press('Enter')
  await expect(option).toContainText('Mejorable')
  await expectAccessible(page, 'the editor')

  await page.getByRole('button', { name: 'Guardar el borrador' }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Borrador guardado.' })).toBeVisible()
  await page.getByRole('button', { name: 'Guardar y publicar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()
  const row = page
    .getByRole('table', { name: 'surveys' })
    .getByRole('row', { name: /Clima del equipo/ })
  await expect(row).toContainText('Publicada')

  // The rule drives the public page: the second question appears only with "Mejorable".
  const publicLink = await row.getByRole('link', { name: 'Enlace público' }).getAttribute('href')
  await page.goto(publicLink!)
  await expect(page.getByRole('heading', { name: 'Clima del equipo' })).toBeVisible()
  // A scale question is a fieldset; its row of buttons is a second group with the same name.
  const dependent = page.locator('fieldset').filter({ hasText: '¿Qué mejorarías primero?' })
  await expect(dependent).toBeHidden()
  await page.getByRole('radio', { name: 'Mejorable' }).check()
  await expect(dependent).toBeVisible()
  await page.getByRole('radio', { name: 'Bueno' }).check()
  await expect(dependent).toBeHidden()

  // A new version: the draft copies the published questions and is published as version 2.
  await page.goto('/surveys')
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()
  await row.getByRole('button', { name: 'Nueva versión' }).click()
  await expect(page.getByRole('heading', { name: 'Clima del equipo' })).toBeVisible()
  await expect(page.getByText(/Borrador de la versión 2/)).toBeVisible()
  await expect(page.getByText(/La versión 1 está publicada/)).toBeVisible()
  await expect(page.getByRole('region', { name: 'Pregunta 2' })).toBeVisible()
  const details = page.getByRole('region', { name: 'Título y descripción' })
  await details.getByLabel('Título').fill('Clima del equipo (2026)')
  await page.getByRole('button', { name: 'Añadir una pregunta' }).click()
  await page.getByRole('menuitem', { name: 'Texto' }).click()
  await expect(page.locator('[role="menu"]')).toHaveCount(0)
  await page
    .getByRole('region', { name: 'Pregunta 3' })
    .getByLabel('Texto de la pregunta')
    .fill('¿Algo más que contarnos?')
  await page.getByRole('button', { name: 'Guardar y publicar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()
  const renamed = page
    .getByRole('table', { name: 'surveys' })
    .getByRole('row', { name: /Clima del equipo \(2026\)/ })
  await expect(renamed).toContainText('Publicada')
  await expect(renamed.getByRole('link', { name: 'Resultados' })).toHaveAttribute(
    'href',
    /\/versions\/2\/results$/,
  )
  await page.goto(publicLink!)
  await expect(page.getByRole('heading', { name: 'Clima del equipo (2026)' })).toBeVisible()
  await expect(page.getByRole('textbox', { name: '¿Algo más que contarnos?' })).toBeVisible()
})

test('a manager creates an invited survey and a respondent uses a single-use link once', async ({
  page,
}) => {
  await login(page, 'demo-manager-2')
  await page.getByRole('link', { name: 'Nueva encuesta' }).click()
  await page.getByLabel('Título').fill('Solo con invitación')
  await page.getByRole('combobox', { name: 'Quién puede responder' }).click()
  await page.getByRole('option', { name: 'Solo personas con invitación' }).click()
  await page.getByRole('button', { name: 'Crear y editar las preguntas' }).click()
  await expect(page.getByRole('heading', { name: 'Solo con invitación' })).toBeVisible()
  await page.getByRole('button', { name: 'Añadir una pregunta' }).click()
  await page.getByRole('menuitem', { name: 'Escala' }).click()
  await expect(page.locator('[role="menu"]')).toHaveCount(0)
  await page
    .getByRole('region', { name: 'Pregunta 1' })
    .getByLabel('Texto de la pregunta')
    .fill('¿Cómo valoras la sesión?')
  await page.getByRole('button', { name: 'Guardar y publicar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()

  const surveys = page.getByRole('table', { name: 'surveys' })
  await surveys
    .getByRole('row', { name: /Solo con invitación/ })
    .getByRole('link', { name: 'Invitaciones' })
    .click()
  await expect(page.getByRole('heading', { name: 'Invitaciones' })).toBeVisible()
  await expect(page.getByText('Todavía no hay invitaciones.')).toBeVisible()
  await page.getByLabel('Cuántas').fill('2')
  await page.getByRole('button', { name: 'Crear invitaciones' }).click()
  const links = page.getByRole('status').filter({ hasText: 'Enlaces nuevos' })
  await expect(links).toBeVisible()
  await expectAccessible(page, 'the invitations page')
  const link = (await links.locator('code').first().textContent()) ?? ''
  expect(link).toContain('?invitation=')
  const invitations = page.getByRole('table', { name: 'invitations' })
  await expect(invitations.getByText('Pendiente', { exact: true })).toHaveCount(2)

  // Without the token the survey stays closed to strangers; with it, it opens exactly once.
  await page.goto(link.split('?')[0] ?? link)
  await expect(page.getByText(/se responde por invitación/)).toBeVisible()
  await page.goto(link)
  await expect(page.getByRole('heading', { name: 'Solo con invitación' })).toBeVisible()
  await page
    .locator('fieldset')
    .filter({ hasText: '¿Cómo valoras la sesión?' })
    .getByRole('button', { name: '4', exact: true })
    .click()
  await page.getByRole('button', { name: 'Enviar mi respuesta' }).click()
  await expect(page.getByRole('heading', { name: 'Gracias' })).toBeVisible()
  await page.goto(link)
  await expect(page.getByText('Esta invitación ya se ha usado.')).toBeVisible()

  await page.goto('/surveys')
  await surveys
    .getByRole('row', { name: /Solo con invitación/ })
    .getByRole('link', { name: 'Invitaciones' })
    .click()
  await expect(invitations.getByText('Usada', { exact: true })).toHaveCount(1)
  await expect(invitations.getByText('Pendiente', { exact: true })).toHaveCount(1)
})

test('an administrator creates an account, which works until it is disabled', async ({ page }) => {
  await login(page, 'demo-admin')
  await page.getByRole('link', { name: 'Cuentas' }).click()
  await expect(page.getByRole('heading', { name: 'Cuentas' })).toBeVisible()
  const accounts = page.getByRole('table', { name: 'accounts' })
  await expect(accounts.getByRole('row', { name: /demo-admin/ })).toContainText('(tú)')
  await page.getByLabel('Usuario').fill('e2e-gestora')
  await page.getByLabel('Contraseña').fill('una-contraseña-larga')
  await page.getByRole('button', { name: 'Crear la cuenta' }).click()
  await expect(
    page.getByRole('status').filter({ hasText: 'Cuenta e2e-gestora creada.' }),
  ).toBeVisible()
  await expectAccessible(page, 'the accounts page')
  await expect(accounts.getByRole('row', { name: /e2e-gestora/ })).toContainText('Activa')

  await page.getByRole('button', { name: 'Salir' }).click()
  await login(page, 'e2e-gestora', 'una-contraseña-larga')
  await expect(page.getByText('Todavía no tienes encuestas.')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Cuentas' })).toHaveCount(0)
  await page.getByRole('button', { name: 'Salir' }).click()

  await login(page, 'demo-admin')
  await page.getByRole('link', { name: 'Cuentas' }).click()
  await page.getByRole('button', { name: 'Deshabilitar e2e-gestora' }).click()
  await expect(accounts.getByRole('row', { name: /e2e-gestora/ })).toContainText('Deshabilitada')
  await page.getByRole('button', { name: 'Salir' }).click()

  await page.goto('/login')
  await page.getByLabel('Usuario').fill('e2e-gestora')
  await page.getByLabel('Contraseña').fill('una-contraseña-larga')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('alert')).toContainText('Esta cuenta está deshabilitada.')
})
