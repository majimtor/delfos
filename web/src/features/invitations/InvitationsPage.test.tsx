import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { InvitationsPage } from './InvitationsPage'

let invitations: Array<Record<string, unknown>> = []
let createBodies: Array<Record<string, unknown>> = []
let revoked: string[] = []
let surveyMode = 'INVITED'
let written: string[] = []

function reset() {
  invitations = [
    { id: 'i1', status: 'USED', createdAt: '2026-01-01T10:00:00Z', usedAt: '2026-01-02T09:30:00Z' },
    { id: 'i2', status: 'PENDING', createdAt: '2026-01-01T10:00:00Z' },
  ]
  createBodies = []
  revoked = []
  surveyMode = 'INVITED'
  written = []
}

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  const json = (status: number, body: unknown) =>
    new Response(status === 204 ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.pathname.endsWith('/surveys/a') && request.method === 'GET') {
    return json(200, {
      id: 'a',
      publicId: 'pub-aaaaaaaa',
      title: 'Valoración del taller',
      language: 'es',
      status: 'PUBLISHED',
      respondentMode: surveyMode,
      responseCount: 1,
      hasDraft: false,
      publishedVersionNumber: 1,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    })
  }
  if (url.pathname.endsWith('/surveys/a/invitations') && request.method === 'GET') {
    return json(200, { items: invitations })
  }
  if (url.pathname.endsWith('/surveys/a/invitations') && request.method === 'POST') {
    const body = (await request.json()) as Record<string, unknown>
    createBodies.push(body)
    const items = Array.from({ length: body.count as number }, (_, i) => ({
      id: `new${i + 1}`,
      token: `tok${i + 1}`,
      createdAt: '2026-01-03T00:00:00Z',
    }))
    invitations = [
      ...items.map((item) => ({ id: item.id, status: 'PENDING', createdAt: item.createdAt })),
      ...invitations,
    ]
    return json(201, { items })
  }
  const revoke = /\/surveys\/a\/invitations\/([a-z0-9]+)$/.exec(url.pathname)
  if (revoke && request.method === 'DELETE') {
    revoked.push(revoke[1]!)
    invitations = invitations.map((i) => (i.id === revoke[1] ? { ...i, status: 'REVOKED' } : i))
    return json(204, null)
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/surveys/a/invitations']}>
          <Routes>
            <Route path="/surveys/:surveyId/invitations" element={<InvitationsPage />} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

describe('InvitationsPage', () => {
  beforeEach(() => {
    reset()
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
    vi.stubGlobal('navigator', {
      ...navigator,
      clipboard: { writeText: vi.fn(async (text: string) => void written.push(text)) },
    })
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('lists the invitations with their state, creates a batch whose links can be copied, and revokes one', async () => {
    renderPage()
    await screen.findByText('Valoración del taller')
    const table = screen.getByRole('table', { name: 'invitations' })
    expect(within(table).getByText('Usada')).toBeTruthy()
    expect(within(table).getByText('Pendiente')).toBeTruthy()
    expect(within(table).getAllByRole('button', { name: 'Revocar' })).toHaveLength(1)

    fireEvent.change(screen.getByLabelText('Cuántas'), { target: { value: '2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear invitaciones' }))
    const links = await screen.findByRole('status')
    expect(createBodies).toEqual([{ count: 2 }])
    expect(within(links).getByText(/\/s\/pub-aaaaaaaa\?invitation=tok1$/)).toBeTruthy()
    expect(within(links).getByText(/invitation=tok2$/)).toBeTruthy()
    fireEvent.click(within(links).getByRole('button', { name: 'Copiar el enlace 1' }))
    await waitFor(() => expect(written).toEqual([expect.stringMatching(/invitation=tok1$/)]))
    fireEvent.click(within(links).getByRole('button', { name: /Copiar todos los enlaces/ }))
    await waitFor(() => expect(written).toHaveLength(2))
    expect(written[1]!.split('\n')).toHaveLength(2)
    await waitFor(() =>
      expect(
        within(screen.getByRole('table', { name: 'invitations' })).getAllByText('Pendiente'),
      ).toHaveLength(3),
    )

    fireEvent.click(
      within(screen.getByRole('table', { name: 'invitations' })).getAllByRole('button', {
        name: 'Revocar',
      })[2]!,
    )
    await waitFor(() => expect(revoked).toEqual(['i2']))
    await waitFor(() =>
      expect(
        within(screen.getByRole('table', { name: 'invitations' })).getByText('Revocada'),
      ).toBeTruthy(),
    )
  })

  it('explains that an anonymous survey takes no invitations', async () => {
    surveyMode = 'ANONYMOUS'
    renderPage()
    await screen.findByText(/Esta encuesta la responde cualquiera con el enlace/)
    expect(screen.queryByRole('button', { name: 'Crear invitaciones' })).toBeNull()
  })
})
