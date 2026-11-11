import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { ResultsPage } from './ResultsPage'

let responseCount = 3
let etagsReceived: Array<string | null> = []

function versionResults() {
  return {
    surveyId: 'a',
    versionNumber: 1,
    responseCount,
    computedAt: '2026-01-01T10:00:00Z',
    questions: [
      {
        key: 'colour',
        text: 'Color',
        type: 'SINGLE_CHOICE',
        answerCount: responseCount,
        options: [
          { key: 'red', label: 'Rojo', count: 2 },
          { key: 'blue', label: 'Azul', count: 1 },
        ],
      },
      {
        key: 'age',
        text: 'Edad',
        type: 'NUMBER',
        answerCount: 3,
        number: { count: 3, mean: 30.5, min: 20, max: 40 },
      },
      {
        key: 'mood',
        text: 'Ánimo',
        type: 'SCALE',
        answerCount: 3,
        number: { count: 3, mean: 4, min: 3, max: 5 },
        distribution: [
          { value: 1, count: 0 },
          { value: 2, count: 0 },
          { value: 3, count: 1 },
          { value: 4, count: 1 },
          { value: 5, count: 1 },
        ],
      },
      { key: 'note', text: 'Nota', type: 'TEXT', answerCount: 2 },
    ],
  }
}

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  const json = (status: number, body: unknown, headers: Record<string, string> = {}) =>
    new Response(status === 304 ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json', ...headers },
    })
  if (url.pathname.endsWith('/surveys/a')) {
    return json(200, {
      id: 'a',
      publicId: 'pub-aaaaaaaa',
      title: 'Colores',
      language: 'es',
      status: 'PUBLISHED',
      respondentMode: 'ANONYMOUS',
      responseCount,
      hasDraft: false,
      publishedVersionNumber: 1,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    })
  }
  if (url.pathname.endsWith('/versions/1/results')) {
    const etag = `"v1-${responseCount}"`
    etagsReceived.push(request.headers.get('If-None-Match'))
    if (request.headers.get('If-None-Match') === etag) return json(304, null, { ETag: etag })
    return json(200, versionResults(), { ETag: etag })
  }
  if (url.pathname.endsWith('/surveys/a/results')) {
    const base = versionResults()
    return json(
      200,
      {
        ...base,
        versionNumbers: [1, 2],
        questions: base.questions.map((q, i) => ({
          ...q,
          comparable: i !== 0,
          versionNumbers: i === 0 ? [1, 2] : [1],
        })),
      },
      { ETag: '"all-3"' },
    )
  }
  if (url.pathname.endsWith('/answers')) {
    return json(200, {
      items: [
        {
          responseId: 'r1',
          submittedAt: '2026-01-01T09:00:00Z',
          answer: { questionKey: 'note', text: 'Primera nota' },
        },
        {
          responseId: 'r2',
          submittedAt: '2026-01-01T09:30:00Z',
          answer: { questionKey: 'note', text: 'Segunda nota' },
        },
      ],
    })
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/surveys/a/versions/1/results']}>
          <Routes>
            <Route
              path="/surveys/:surveyId/versions/:versionNumber/results"
              element={<ResultsPage pollIntervalMs={50} />}
            />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

describe('ResultsPage', () => {
  beforeEach(() => {
    responseCount = 3
    etagsReceived = []
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows the aggregates per type, polls with the ETag and updates when responses arrive', async () => {
    renderPage()
    await screen.findByText('Colores')
    expect(screen.getByRole('status').textContent).toContain('3 respuestas')
    const colour = screen.getByRole('region', { name: 'Color' })
    expect(within(colour).getByText('Rojo')).toBeTruthy()
    expect(within(colour).getByText(/2 · 67 %/)).toBeTruthy()
    const age = screen.getByRole('region', { name: 'Edad' })
    expect(within(age).getByText('30,5')).toBeTruthy()
    const mood = screen.getByRole('region', { name: 'Ánimo' })
    expect(within(mood).getByLabelText('5')).toBeTruthy()

    await waitFor(() =>
      expect(etagsReceived.filter((e) => e === '"v1-3"').length).toBeGreaterThan(0),
    )
    expect(screen.getByRole('status').textContent).toContain('3 respuestas')

    responseCount = 4
    await waitFor(() => expect(screen.getByRole('status').textContent).toContain('4 respuestas'))
  })

  it('lists the text answers on demand', async () => {
    renderPage()
    await screen.findByText('Colores')
    fireEvent.click(screen.getByRole('button', { name: 'Ver las contestaciones' }))
    await waitFor(() => expect(screen.getByText(/Segunda nota/)).toBeTruthy())
  })

  it('aggregates across versions and marks the question that changed', async () => {
    renderPage()
    await screen.findByText('Colores')
    fireEvent.click(screen.getByRole('button', { name: 'Todas las versiones' }))
    await waitFor(() =>
      expect(screen.getByText(/Esta pregunta cambió entre versiones/)).toBeTruthy(),
    )
    expect(screen.getByText('Versiones 1, 2')).toBeTruthy()
  })
})
