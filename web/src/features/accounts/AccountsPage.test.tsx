import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { AccountsPage } from './AccountsPage'

let isAdmin = true
let accounts: Array<Record<string, unknown>> = []
let createBodies: Array<Record<string, unknown>> = []
let disabled: string[] = []

function reset() {
  isAdmin = true
  accounts = [
    {
      id: 'me',
      username: 'demo-admin',
      isAdmin: true,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'm1',
      username: 'demo-manager',
      isAdmin: false,
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    },
    {
      id: 'm2',
      username: 'old-manager',
      isAdmin: false,
      status: 'DISABLED',
      createdAt: '2025-12-01T00:00:00Z',
    },
  ]
  createBodies = []
  disabled = []
}

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  const json = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.pathname.endsWith('/auth/me')) {
    return json(200, {
      accountId: 'me',
      username: 'demo-admin',
      isAdmin,
      sessionExpiresAt: '2026-01-02T00:00:00Z',
    })
  }
  if (url.pathname.endsWith('/accounts') && request.method === 'GET')
    return json(200, { items: accounts })
  if (url.pathname.endsWith('/accounts') && request.method === 'POST') {
    const body = (await request.json()) as Record<string, unknown>
    createBodies.push(body)
    if (body.username === 'demo-manager') {
      return json(409, {
        type: 'urn:delfos:problem:username-taken',
        title: 'Conflict',
        status: 409,
        code: 'USERNAME_TAKEN',
      })
    }
    const account = {
      id: `new-${createBodies.length}`,
      username: body.username,
      isAdmin: body.isAdmin,
      status: 'ACTIVE',
      createdAt: '2026-01-03T00:00:00Z',
    }
    accounts = [account, ...accounts]
    return json(201, account)
  }
  const disable = /\/accounts\/([a-z0-9-]+)\/disable$/.exec(url.pathname)
  if (disable && request.method === 'POST') {
    disabled.push(disable[1]!)
    accounts = accounts.map((a) => (a.id === disable[1] ? { ...a, status: 'DISABLED' } : a))
    return json(
      200,
      accounts.find((a) => a.id === disable[1]),
    )
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/accounts']}>
          <AccountsPage />
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

const row = (username: string) => screen.getByText(username).closest('tr')!

describe('AccountsPage', () => {
  beforeEach(() => {
    reset()
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('lists the accounts, creates one with the mirror checks and the taken-username answer, and disables another', async () => {
    renderPage()
    await screen.findByText('demo-manager')
    expect(within(row('demo-admin')).getByText('(tú)')).toBeTruthy()
    expect(within(row('demo-admin')).queryByRole('button', { name: /Deshabilitar/ })).toBeNull()
    expect(within(row('old-manager')).getByText('Deshabilitada')).toBeTruthy()
    expect(within(row('old-manager')).queryByRole('button', { name: /Deshabilitar/ })).toBeNull()

    fireEvent.change(screen.getByLabelText(/Usuario/), { target: { value: 'Bad Name' } })
    fireEvent.change(screen.getByLabelText(/Contraseña/), { target: { value: 'short' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear la cuenta' }))
    await waitFor(() =>
      expect(screen.getByLabelText(/Usuario/).getAttribute('aria-invalid')).toBe('true'),
    )
    expect(createBodies).toHaveLength(0)

    fireEvent.change(screen.getByLabelText(/Usuario/), { target: { value: 'demo-manager' } })
    fireEvent.change(screen.getByLabelText(/Contraseña/), {
      target: { value: 'a-long-enough-password' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Crear la cuenta' }))
    await screen.findByText('Ese usuario ya existe.')

    fireEvent.change(screen.getByLabelText(/Usuario/), { target: { value: 'nueva-gestora' } })
    fireEvent.change(screen.getByLabelText(/Contraseña/), {
      target: { value: 'a-long-enough-password' },
    })
    fireEvent.click(screen.getByLabelText('Administrador'))
    fireEvent.click(screen.getByRole('button', { name: 'Crear la cuenta' }))
    await screen.findByText('Cuenta nueva-gestora creada.')
    expect(createBodies[1]).toEqual({
      username: 'nueva-gestora',
      password: 'a-long-enough-password',
      isAdmin: true,
    })
    await waitFor(() =>
      expect(within(row('nueva-gestora')).getByText('Administrador')).toBeTruthy(),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Deshabilitar demo-manager' }))
    await waitFor(() => expect(disabled).toEqual(['m1']))
    await waitFor(() => expect(within(row('demo-manager')).getByText('Deshabilitada')).toBeTruthy())
  })

  it('tells a manager that accounts are for administrators', async () => {
    isAdmin = false
    renderPage()
    await screen.findByText('Solo un administrador gestiona las cuentas.')
    expect(screen.queryByRole('table')).toBeNull()
  })
})
