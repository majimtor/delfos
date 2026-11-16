/**
 * The keep-alive of the API: requests its liveness probe, which never touches the database, so
 * the free instance stays awake during the day (plan, section 12).
 */

type Fetch = (url: string, init?: RequestInit) => Promise<Response>

/** Attempts per run, thirty seconds apart, as the former workflow retried. */
export const ATTEMPTS = 3
export const RETRY_DELAY_MS = 30_000
/** A sleeping instance answers its first request after about a hundred seconds (plan, section 12). */
export const ATTEMPT_TIMEOUT_MS = 150_000

const wait = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

/** Requests the probe until it answers with success; throws the last failure when no attempt did. */
export async function requestLiveness(
  url: string,
  fetchFn: Fetch = fetch,
  sleep: (ms: number) => Promise<void> = wait,
): Promise<Response> {
  let failure: unknown
  for (let attempt = 1; attempt <= ATTEMPTS; attempt++) {
    try {
      const response = await fetchFn(url, { signal: AbortSignal.timeout(ATTEMPT_TIMEOUT_MS) })
      if (response.ok) return response
      failure = new Error(`The liveness probe answered ${response.status}`)
    } catch (error) {
      failure = error
    }
    if (attempt < ATTEMPTS) await sleep(RETRY_DELAY_MS)
  }
  throw failure
}
