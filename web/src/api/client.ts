import createClient from 'openapi-fetch'
import type { paths } from './schema'

/** Base URL of the API, without the `/api/v1` prefix; the contract's paths carry it. */
export const apiBaseUrl: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Name sent in the header that cookie-authenticated mutating requests must carry. */
export const clientHeader = { 'X-Delfos-Client': 'web' } as const

/** Typed client over the contract. Cookies travel with every request; the client header always. */
export const api = createClient<paths>({
  baseUrl: `${apiBaseUrl}/api/v1`,
  credentials: 'include',
  headers: clientHeader,
})
