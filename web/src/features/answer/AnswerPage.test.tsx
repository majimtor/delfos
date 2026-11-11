import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { IntlProvider } from 'react-intl'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import es from '../../i18n/es.json'
import { AnswerPage } from './AnswerPage'

/** A fake of the three public operations over one branching survey, enough to drive the page. */
const survey = {
  publicId: 'demo-test',
  title: 'Coches',
  language: 'es',
  status: 'PUBLISHED',
  acceptingResponses: true,
  version: {
    number: 1,
    status: 'PUBLISHED',
    createdAt: '2026-01-01T00:00:00Z',
    questions: [
      {
        key: 'owns',
        type: 'SINGLE_CHOICE',
        text: '¿Tienes coche?',
        required: true,
        options: [
          { key: 'yes', label: 'Sí' },
          { key: 'no', label: 'No' },
        ],
      },
      {
        key: 'fuel',
        type: 'SINGLE_CHOICE',
        text: '¿Qué combustible?',
        required: false,
        options: [
          { key: 'petrol', label: 'Gasolina' },
          { key: 'electric', label: 'Eléctrico' },
        ],
      },
      { key: 'note', type: 'TEXT', text: 'Comentario', required: false, maxLength: 5 },
    ],
    rules: [
      {
        targetQuestionKey: 'fuel',
        match: 'ALL',
        conditions: [
          { sourceQuestionKey: 'owns', operator: 'EQUALS', value: { optionKey: 'yes' } },
        ],
      },
    ],
  },
}

type Sent = { questionKey: string; optionKey?: string; text?: string }

function evaluate(answers: Sent[]) {
  const owns = answers.find((a) => a.questionKey === 'owns')?.optionKey
  const visible = owns === 'yes' ? ['owns', 'fuel', 'note'] : ['owns', 'note']
  const errors = answers.flatMap((a, i) =>
    a.questionKey === 'note' && (a.text ?? '').length > 5
      ? [{ path: `answers[${i}].text`, code: 'TOO_LONG', params: { max: 5 } }]
      : [],
  )
  const missing = owns ? [] : ['owns']
  return { visibleQuestionKeys: visible, errors, missingRequiredQuestionKeys: missing }
}

let submitted: Sent[][] = []

async function fakeFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const url =
    typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url
  const rawBody = input instanceof Request ? await input.text() : String(init?.body ?? '')
  const json = (status: number, body: unknown) =>
    new Response(JSON.stringify(body), {
      status,
      headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
    })
  if (url.endsWith('/public/surveys/demo-test')) return json(200, survey)
  if (url.endsWith('/public/surveys/missing')) {
    return json(404, {
      type: 'urn:delfos:problem:not-found',
      title: 'Not Found',
      status: 404,
      code: 'NOT_FOUND',
    })
  }
  const body = JSON.parse(rawBody || '{"answers":[]}') as { answers: Sent[] }
  if (url.endsWith('/evaluations')) return json(200, evaluate(body.answers))
  if (url.endsWith('/responses')) {
    const result = evaluate(body.answers)
    const errors = [
      ...result.errors,
      ...result.missingRequiredQuestionKeys.map((k) => ({
        path: 'answers',
        code: 'MISSING_REQUIRED_ANSWER',
        params: { questionKey: k },
      })),
    ]
    if (errors.length > 0) {
      return json(400, {
        type: 'urn:delfos:problem:validation-error',
        title: 'Bad Request',
        status: 400,
        code: 'VALIDATION_ERROR',
        errors,
      })
    }
    submitted.push(body.answers)
    return json(201, {
      responseId: '11111111-1111-1111-1111-111111111111',
      submittedAt: '2026-01-01T00:00:00Z',
    })
  }
  return json(404, { code: 'NOT_FOUND' })
}

function renderPage(publicId: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <IntlProvider locale="es" messages={es}>
        <MemoryRouter initialEntries={[`/s/${publicId}`]}>
          <Routes>
            <Route path="/s/:publicId" element={<AnswerPage />} />
          </Routes>
        </MemoryRouter>
      </IntlProvider>
    </QueryClientProvider>,
  )
}

const question = (name: RegExp) => screen.getByRole('group', { name })
const maybeQuestion = (name: RegExp) => screen.queryByRole('group', { name })

async function readySubmitButton() {
  const button = await screen.findByRole('button', { name: 'Enviar mi respuesta' })
  await waitFor(() => expect(button.hasAttribute('disabled')).toBe(false))
  return button
}

describe('AnswerPage', () => {
  beforeEach(() => {
    submitted = []
    vi.stubGlobal('fetch', vi.fn(fakeFetch))
  })
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows a not-found message for an unknown survey', async () => {
    renderPage('missing')
    await waitFor(() => expect(screen.getByText('Esta encuesta no existe.')).toBeTruthy())
  })

  it('reveals a question when its rule holds and sends only visible answers', async () => {
    renderPage('demo-test')
    await waitFor(() => expect(question(/Tienes coche/)).toBeTruthy())
    expect(maybeQuestion(/combustible/)).toBeNull()

    fireEvent.click(screen.getByLabelText('Sí'))
    await waitFor(() => expect(question(/combustible/)).toBeTruthy())
    fireEvent.click(screen.getByLabelText('Eléctrico'))

    fireEvent.click(screen.getByLabelText('No'))
    await waitFor(() => expect(maybeQuestion(/combustible/)).toBeNull())

    fireEvent.click(await readySubmitButton())
    await waitFor(() => expect(screen.getByText('Gracias')).toBeTruthy())
    expect(submitted).toHaveLength(1)
    expect(submitted[0].map((a) => a.questionKey)).toEqual(['owns'])
  })

  it('shows the required error from the server when submitting without an answer', async () => {
    renderPage('demo-test')
    await waitFor(() => expect(question(/Tienes coche/)).toBeTruthy())
    fireEvent.click(await readySubmitButton())
    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain('Esta pregunta es obligatoria.'),
    )
    expect(submitted).toHaveLength(0)
  })

  it('shows a translated field error from the evaluation', async () => {
    renderPage('demo-test')
    await waitFor(() => expect(question(/Comentario/)).toBeTruthy())
    fireEvent.change(screen.getByLabelText('Comentario'), { target: { value: 'demasiado largo' } })
    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain('Usa como máximo 5 caracteres.'),
    )
  })
})
