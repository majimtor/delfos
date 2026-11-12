import { expect, type APIRequestContext } from '@playwright/test'

// What both media scripts share: where the locally seeded API is, the demo password and the
// reset that puts the demo data back to its seeded state, so that every run starts from the
// same place.
export const api = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080'
export const demoPassword = 'delfos-demo'
const resetToken = process.env.E2E_RESET_TOKEN ?? 'e2e-reset-token'

export async function resetDemo(request: APIRequestContext) {
  const reset = await request.post(`${api}/api/v1/demo/reset`, {
    headers: { Authorization: `Bearer ${resetToken}` },
  })
  expect(reset.status(), 'the local API must run with the demo seed and its reset token').toBe(204)
}
