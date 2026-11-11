import { apiBaseUrl } from './client'

export type HealthProbe = () => Promise<boolean>

/** One attempt to reach the API; false on any failure, including a timeout. */
export const probeHealth: HealthProbe = async () => {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), PROBE_TIMEOUT_MS)
  try {
    const response = await fetch(`${apiBaseUrl}/api/v1/health`, { signal: controller.signal })
    return response.ok
  } catch {
    return false
  } finally {
    clearTimeout(timer)
  }
}

const PROBE_TIMEOUT_MS = 8_000
