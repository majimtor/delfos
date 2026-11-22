import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

// The manager's pages at phone width: the bar keeps the mark and three icon buttons, the session
// lives behind a menu, and the surveys are cards instead of a table. Every page visited is
// checked by axe against WCAG 2.2 AA, as the desktop path is.
const demoPassword = 'delfos-demo'

async function expectAccessible(page: Page, name: string) {
  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
    .analyze()
  expect(
    results.violations.map((v) => ({ id: v.id, nodes: v.nodes.map((n) => n.target) })),
    `accessibility violations on ${name}`,
  ).toEqual([])
}

test('a manager uses the surveys list and the menu on a phone', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toContainText('lógica condicional')
  await expectAccessible(page, 'the landing page on a phone')
  // No horizontal scroll at this width.
  expect(
    await page.evaluate(
      'document.documentElement.scrollWidth <= document.documentElement.clientWidth',
    ),
  ).toBe(true)

  await page.goto('/login')
  await page.getByLabel('Usuario').fill('demo-manager')
  await page.getByLabel('Contraseña').fill(demoPassword)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('heading', { name: 'Mis encuestas' })).toBeVisible()

  // Cards, not a table, each with its state and its actions as text buttons.
  const list = page.getByRole('list', { name: 'surveys' })
  await expect(list).toBeVisible()
  const mobility = list.getByRole('listitem').filter({ hasText: 'Movilidad urbana' })
  await expect(mobility).toContainText('Publicada')
  await expect(mobility.getByRole('link', { name: 'Resultados' })).toBeVisible()
  await expectAccessible(page, 'the surveys list on a phone')

  // The session menu holds the navigation and the way out.
  await page.getByRole('button', { name: 'Menú' }).click()
  const menu = page.getByRole('menu')
  await expect(menu).toContainText('demo-manager')
  await expect(menu.getByRole('menuitem', { name: 'Cuentas' })).toHaveCount(0)
  await menu.getByRole('menuitem', { name: 'Salir' }).click()
  await expect(page.getByRole('heading', { level: 1 })).toContainText('lógica condicional')
})
