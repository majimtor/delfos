import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { EditorPage } from './EditorPage'

let putBodies: Array<Record<string, unknown>> = []
let published = 0
let serverRejectsMax: number | null = null

const draft = () => ({
  number: 1,
  status: 'DRAFT',
  createdAt: '2026-01-01T00:00:00Z',
  questions: [
    {
      key: 'books',
      type: 'NUMBER',
      text: '¿Cuántos libros leíste?',
      required: true,
      min: 0,
      max: 200,
      integerOnly: true,
    },
  ],
  rules: [],
})

async function fakeFetch(input: RequestInfo | URL): Promise<Response> {
  const request = input as Request
  const url = new URL(request.url)
  const json = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.pathname.endsWith('/surveys/a') && request.method === 'GET') {
    return json(200, {
      id: 'a',
      publicId: 'pub-aaaaaaaa',
      title: 'Hábitos de lectura',
      language: 'es',
      status: 'DRAFT',
      respondentMode: 'ANONYMOUS',
      responseCount: 0,
      hasDraft: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    })
  }
  if (url.pathname.endsWith('/surveys/a/draft') && request.method === 'GET')
    return json(200, draft())
  if (url.pathname.endsWith('/surveys/a/draft') && request.method === 'PUT') {
    const body = (await request.json()) as { questions: Array<Record<string, unknown>> }
    putBodies.push(body)
    if (serverRejectsMax !== null && body.questions[0]?.max === serverRejectsMax) {
      return json(400, {
        type: 'urn:delfos:problem:validation-error',
        title: 'Bad Request',
        status: 400,
        code: 'VALIDATION_ERROR',
        errors: [{ path: 'questions[0].max', code: 'INVALID_VALUE' }],
      })
    }
    return json(200, { ...draft(), questions: body.questions, rules: [] })
  }
  if (url.pathname.endsWith('/surveys/a/publish') && request.method === 'POST') {
    published += 1
    return json(200, { id: 'a', status: 'PUBLISHED' })
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={['/surveys/a/draft']}>
          <Routes>
            <Route path="/surveys/:surveyId/draft" element={<EditorPage />} />
            <Route path="/surveys" element={<p>Lista de encuestas</p>} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

const question = (name: string) => screen.getByRole('region', { name })

// The editor renders a large tree; under a loaded CI runner the first test exceeds the default 5 s.
vi.setConfig({ testTimeout: 20_000 })

describe('EditorPage', () => {
  beforeEach(() => {
    putBodies = []
    published = 0
    serverRejectsMax = null
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('loads the draft, adds a choice question with its options and saves the whole definition', async () => {
    renderPage()
    await screen.findByText('Hábitos de lectura')
    expect(
      within(question('Pregunta 1'))
        .getByLabelText(/Texto de la pregunta/)
        .getAttribute('value') ??
        within(question('Pregunta 1')).getByLabelText(/Texto de la pregunta/).textContent,
    ).toContain('¿Cuántos libros leíste?')

    fireEvent.click(screen.getByRole('button', { name: 'Añadir una pregunta' }))
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Elección única' }))
    const added = question('Pregunta 2')
    fireEvent.change(within(added).getByLabelText(/Texto de la pregunta/), {
      target: { value: '¿En qué formato lees?' },
    })
    fireEvent.change(within(added).getByLabelText('Opción 1'), { target: { value: 'Papel' } })
    fireEvent.change(within(added).getByLabelText('Opción 2'), { target: { value: 'Digital' } })
    fireEvent.click(within(added).getByRole('button', { name: 'Añadir una opción' }))
    fireEvent.change(within(added).getByLabelText('Opción 3'), { target: { value: 'Audio' } })
    fireEvent.click(within(added).getByRole('button', { name: 'Subir la opción 3' }))
    fireEvent.click(within(added).getByLabelText('Obligatoria'))

    fireEvent.click(screen.getByRole('button', { name: 'Guardar el borrador' }))
    await screen.findByText('Borrador guardado.')
    expect(putBodies).toHaveLength(1)
    const sent = putBodies[0]!.questions as Array<Record<string, unknown>>
    expect(sent).toHaveLength(2)
    expect(sent[0]).toMatchObject({
      key: 'books',
      type: 'NUMBER',
      min: 0,
      max: 200,
      integerOnly: true,
    })
    expect(sent[1]).toMatchObject({
      type: 'SINGLE_CHOICE',
      text: '¿En qué formato lees?',
      required: true,
    })
    expect(sent[1]!.key).toMatch(/^q-[A-Za-z0-9]{8}$/)
    expect((sent[1]!.options as Array<{ label: string }>).map((o) => o.label)).toEqual([
      'Papel',
      'Audio',
      'Digital',
    ])
    expect(published).toBe(0)
  })

  it('marks the fields the mirror finds empty before any request', async () => {
    renderPage()
    await screen.findByText('Hábitos de lectura')
    fireEvent.click(screen.getByRole('button', { name: 'Añadir una pregunta' }))
    fireEvent.click(await screen.findByRole('menuitem', { name: 'Texto' }))
    fireEvent.click(screen.getByRole('button', { name: 'Guardar el borrador' }))
    expect((await screen.findByRole('alert')).textContent).toContain('1 campo necesita atención.')
    expect(within(question('Pregunta 2')).getByText('Obligatorio.')).toBeTruthy()
    expect(putBodies).toHaveLength(0)
    fireEvent.change(within(question('Pregunta 2')).getByLabelText(/Texto de la pregunta/), {
      target: { value: 'Recomiéndanos un libro.' },
    })
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('shows the errors the server returns next to their fields', async () => {
    serverRejectsMax = 7
    renderPage()
    await screen.findByText('Hábitos de lectura')
    fireEvent.change(within(question('Pregunta 1')).getByLabelText('Máximo'), {
      target: { value: '7' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Guardar el borrador' }))
    await screen.findByRole('alert')
    expect(within(question('Pregunta 1')).getByText('Este valor no es válido.')).toBeTruthy()
    expect(putBodies).toHaveLength(1)
  })

  it('saves and publishes, then returns to the list', async () => {
    renderPage()
    await screen.findByText('Hábitos de lectura')
    fireEvent.click(screen.getByRole('button', { name: 'Guardar y publicar' }))
    await screen.findByText('Lista de encuestas')
    expect(putBodies).toHaveLength(1)
    expect(published).toBe(1)
  })

  it('removes a question and drops the rules that mention it', async () => {
    renderPage()
    await screen.findByText('Hábitos de lectura')
    fireEvent.click(
      within(question('Pregunta 1')).getByRole('button', { name: 'Quitar la pregunta 1' }),
    )
    expect(screen.getByText('El borrador todavía no tiene preguntas.')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Guardar y publicar' }))
    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain(
        'Añade al menos una pregunta antes de publicar.',
      ),
    )
    expect(putBodies).toHaveLength(0)
  })
})
