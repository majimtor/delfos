import { describe, expect, it, vi } from 'vitest'
import worker from './index'
import { ATTEMPTS, requestLiveness } from './liveness'

type Fetch = (url: string) => Promise<Response>

const url = 'https://api.example/actuator/health/liveness'
const noWait = async () => {}

describe('the keep-alive of the API', () => {
  it('requests the liveness probe once when it answers', async () => {
    const fetchFn = vi.fn<Fetch>(async () => new Response('UP', { status: 200 }))
    const response = await requestLiveness(url, fetchFn, noWait)
    expect(response.ok).toBe(true)
    expect(fetchFn).toHaveBeenCalledTimes(1)
    expect(fetchFn.mock.calls[0][0]).toBe(url)
  })

  it('retries after a failed request and after an answer that is not a success', async () => {
    const fetchFn = vi
      .fn<Fetch>()
      .mockRejectedValueOnce(new Error('connection reset'))
      .mockResolvedValueOnce(new Response('down', { status: 503 }))
      .mockResolvedValueOnce(new Response('UP', { status: 200 }))
    const sleep = vi.fn(async () => {})
    await requestLiveness(url, fetchFn, sleep)
    expect(fetchFn).toHaveBeenCalledTimes(3)
    expect(sleep).toHaveBeenCalledTimes(2)
  })

  it('gives up after the last attempt with the last failure', async () => {
    const fetchFn = vi.fn(async () => {
      throw new Error('unreachable')
    })
    await expect(requestLiveness(url, fetchFn, noWait)).rejects.toThrow('unreachable')
    expect(fetchFn).toHaveBeenCalledTimes(ATTEMPTS)
  })

  it('runs on the schedule against the probe of the environment', async () => {
    const fetchFn = vi.fn<Fetch>(async () => new Response('UP', { status: 200 }))
    vi.stubGlobal('fetch', fetchFn)
    try {
      await worker.scheduled(
        { cron: '*/10 6-23 * * *', scheduledTime: 0 },
        { API_LIVENESS_URL: url, ASSETS: { fetch: async () => new Response() } },
      )
    } finally {
      vi.unstubAllGlobals()
    }
    expect(fetchFn.mock.calls[0][0]).toBe(url)
  })

  it('hands the requests that reach the script to the assets', async () => {
    const assets = { fetch: vi.fn(async () => new Response('the page')) }
    const request = new Request('https://delfos.example/s/abc')
    const response = await worker.fetch(request, { API_LIVENESS_URL: url, ASSETS: assets })
    expect(await response.text()).toBe('the page')
    expect(assets.fetch).toHaveBeenCalledWith(request)
  })
})
