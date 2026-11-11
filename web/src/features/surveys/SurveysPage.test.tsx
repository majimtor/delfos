import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { SurveysPage } from './SurveysPage'

const base = {
  language: 'es',
  respondentMode: 'ANONYMOUS',
  responseCount: 0,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
}

let surveys: Array<Record<string, unknown>> = []

function reset() {
  surveys = [
    {
      ...base,
      id: 'a',
      publicId: 'pub-aaaaaaaa',
      title: 'Primer borrador',
      status: 'DRAFT',
      hasDraft: true,
    },
    {
      ...base,
      id: 'b',
      publicId: 'pub-bbbbbbbb',
      title: 'Ya publicada',
      status: 'PUBLISHED',
      hasDraft: false,
      publishedVersionNumber: 1,
      responseCount: 7,
    },
    {
      ...base,
      id: 'c',
      publicId: 'pub-cccccccc',
      title: 'Sin preguntas',
      status: 'DRAFT',
      hasDraft: true,
    },
  ]
}

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  const json = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.pathname.endsWith('/surveys') && request.method === 'GET') {
    const cursor = url.searchParams.get('cursor')
    return cursor
      ? json(200, { items: surveys.slice(2) })
      : json(200, { items: surveys.slice(0, 2), nextCursor: 'next' })
  }
  const publish = /\/surveys\/([a-z])\/publish$/.exec(url.pathname)
  if (publish && request.method === 'POST') {
    const id = publish[1]
    if (id === 'c') {
      return json(400, {
        type: 'urn:delfos:problem:validation-error',
        title: 'Bad Request',
        status: 400,
        code: 'VALIDATION_ERROR',
        errors: [{ path: 'questions', code: 'NO_QUESTIONS' }],
      })
    }
    surveys = surveys.map((s) =>
      s.id === id ? { ...s, status: 'PUBLISHED', hasDraft: false, publishedVersionNumber: 1 } : s,
    )
    return json(
      200,
      surveys.find((s) => s.id === id),
    )
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter>
          <SurveysPage />
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

const row = (title: string) => screen.getByText(title).closest('tr')!

describe('SurveysPage', () => {
  beforeEach(() => {
    reset()
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('lists surveys with their status and actions, loads more and publishes a draft', async () => {
    renderPage()
    await screen.findByText('Primer borrador')
    const draft = row('Primer borrador')
    expect(within(draft).getByText('Borrador')).toBeTruthy()
    expect(within(draft).getByRole('button', { name: 'Publicar' })).toBeTruthy()
    const published = row('Ya publicada')
    expect(within(published).getByRole('button', { name: 'Cerrar' })).toBeTruthy()
    expect(within(published).getByRole('link', { name: 'Resultados' }).getAttribute('href')).toBe(
      '/surveys/b/versions/1/results',
    )
    expect(within(published).getByText('7')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: 'Cargar más' }))
    await waitFor(() => expect(screen.getByText('Sin preguntas')).toBeTruthy())

    fireEvent.click(within(draft).getByRole('button', { name: 'Publicar' }))
    await waitFor(() =>
      expect(within(row('Primer borrador')).getByRole('button', { name: 'Cerrar' })).toBeTruthy(),
    )
  })

  it('explains why an empty draft cannot be published', async () => {
    renderPage()
    await screen.findByText('Primer borrador')
    fireEvent.click(screen.getByRole('button', { name: 'Cargar más' }))
    await screen.findByText('Sin preguntas')
    fireEvent.click(within(row('Sin preguntas')).getByRole('button', { name: 'Publicar' }))
    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain('El borrador no tiene preguntas'),
    )
  })
})
