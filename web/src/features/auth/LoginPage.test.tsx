import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { LoginPage } from './LoginPage'

let loggedIn = false

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = request.url
  const json = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.endsWith('/auth/me')) {
    return loggedIn
      ? json(200, {
          accountId: '1',
          username: 'demo-manager',
          isAdmin: false,
          sessionExpiresAt: '2030-01-01T00:00:00Z',
        })
      : json(401, {
          type: 'urn:delfos:problem:unauthenticated',
          title: 'Unauthorized',
          status: 401,
          code: 'UNAUTHENTICATED',
        })
  }
  if (url.endsWith('/auth/sessions') && request.method === 'POST') {
    const body = JSON.parse(await request.text()) as {
      username: string
      password: string
      transport: string
    }
    expect(body.transport).toBe('COOKIE')
    expect(request.headers.get('X-Delfos-Client')).toBe('web')
    if (body.password !== 'delfos-demo') {
      return json(401, {
        type: 'urn:delfos:problem:invalid-credentials',
        title: 'Unauthorized',
        status: 401,
        code: 'INVALID_CREDENTIALS',
      })
    }
    loggedIn = true
    return json(201, {
      accountId: '1',
      username: body.username,
      isAdmin: false,
      expiresAt: '2030-01-01T00:00:00Z',
    })
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderLogin() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/surveys" element={<p>surveys page</p>} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    loggedIn = false
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows the credentials error and then enters with the right password', async () => {
    renderLogin()
    const username = await screen.findByLabelText('Usuario')
    fireEvent.change(username, { target: { value: 'demo-manager' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain(
        'El usuario o la contraseña no son correctos.',
      ),
    )

    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'delfos-demo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    await waitFor(() => expect(screen.getByText('surveys page')).toBeTruthy())
  })

  it('requires both fields before calling the API', async () => {
    renderLogin()
    fireEvent.click(await screen.findByRole('button', { name: 'Entrar' }))
    await waitFor(() => expect(screen.getAllByText('Obligatorio.')).toHaveLength(2))
    expect(loggedIn).toBe(false)
  })
})
