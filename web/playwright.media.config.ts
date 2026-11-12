import { defineConfig, devices } from '@playwright/test'

// Produces the screenshots and the tour video that the README shows: the same stranger's visit as
// the end-to-end path, slowed down to be watched, in English and in a 16:9 frame. It runs by hand
// against the locally seeded API (see README, "Screenshots and video"), never in CI; its output
// is looked at before being committed.
const port = 5173

export default defineConfig({
  testDir: './media',
  outputDir: './test-results/media',
  timeout: 180_000,
  expect: { timeout: 15_000 },
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: `http://localhost:${port}`,
    // English, the language of the README the pictures illustrate.
    locale: 'en-US',
    video: { mode: 'on', size: { width: 1280, height: 720 } },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'], deviceScaleFactor: 2 } }],
  webServer: {
    command: `npm run preview -- --port ${port} --strictPort`,
    url: `http://localhost:${port}`,
    reuseExistingServer: true,
    timeout: 60_000,
  },
})
