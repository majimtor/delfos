import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { NewSurveyPage } from './NewSurveyPage'

let bodies: Array<Record<string, unknown>> = []

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  if (url.pathname.endsWith('/surveys') && request.method === 'POST') {
    bodies.push((await request.json()) as Record<string, unknown>)
    return new Response(
      JSON.stringify({
        id: 'new-id',
        publicId: 'pub-new',
        title: 'x',
        language: 'es',
        status: 'DRAFT',
      }),
      {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      },
    )
  }
  return new Response(JSON.stringify({ code: 'NOT_FOUND' }), { status: 404 })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/surveys/new']}>
          <Routes>
            <Route path="/surveys/new" element={<NewSurveyPage />} />
            <Route path="/surveys/:surveyId/draft" element={<p>Editor de new-id</p>} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

describe('NewSurveyPage', () => {
  beforeEach(() => {
    bodies = []
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('requires a title, creates the survey and opens its editor', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('button', { name: 'Crear y editar las preguntas' }))
    expect(await screen.findByText('Obligatorio.')).toBeTruthy()
    expect(bodies).toHaveLength(0)

    fireEvent.change(screen.getByLabelText(/Título/), { target: { value: '  Clima laboral ' } })
    fireEvent.change(screen.getByLabelText(/Descripción/), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Crear y editar las preguntas' }))
    await screen.findByText('Editor de new-id')
    expect(bodies).toEqual([{ title: 'Clima laboral', language: 'es' }])
  })

  it('sends the invited mode when chosen', async () => {
    renderPage()
    fireEvent.change(screen.getByLabelText(/Título/), { target: { value: 'Solo invitados' } })
    fireEvent.mouseDown(screen.getByRole('combobox', { name: 'Quién puede responder' }))
    fireEvent.click(await screen.findByRole('option', { name: 'Solo personas con invitación' }))
    fireEvent.click(screen.getByRole('button', { name: 'Crear y editar las preguntas' }))
    await screen.findByText('Editor de new-id')
    expect(bodies).toEqual([{ title: 'Solo invitados', language: 'es', respondentMode: 'INVITED' }])
  })
})
