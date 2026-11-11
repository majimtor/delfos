import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { afterEach, describe, expect, it } from 'vitest'
import es from '../i18n/es.json'
import { WaitForApi } from './WaitForApi'

function renderWithProbe(results: boolean[]) {
  const probe = async () => results.shift() ?? true
  return render(
    <IntlProvider locale="es" messages={es}>
      <WaitForApi probe={probe} intervalMs={5} budgetMs={10_000}>
        <p>ready content</p>
      </WaitForApi>
    </IntlProvider>,
  )
}

describe('WaitForApi', () => {
  afterEach(cleanup)

  it('shows the waiting screen until the API answers, then the content', async () => {
    renderWithProbe([false, false, true])
    expect(screen.getByRole('status').textContent).toContain('El servicio se está despertando')
    await waitFor(() => expect(screen.getByText('ready content')).toBeTruthy())
  })

  it('offers a retry when the budget is exhausted', async () => {
    const probe = async () => false
    render(
      <IntlProvider locale="es" messages={es}>
        <WaitForApi probe={probe} intervalMs={5} budgetMs={20}>
          <p>ready content</p>
        </WaitForApi>
      </IntlProvider>,
    )
    await waitFor(() => expect(screen.getByRole('button', { name: 'Reintentar' })).toBeTruthy())
    expect(screen.queryByText('ready content')).toBeNull()
  })
})
