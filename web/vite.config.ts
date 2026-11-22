/// <reference types="vitest/config" />
import { readFileSync } from 'node:fs'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The version the footer shows is the one release-please keeps in version.txt at the root.
const version = readFileSync(new URL('../version.txt', import.meta.url), 'utf8').trim()

export default defineConfig({
  plugins: [react()],
  define: { __DELFOS_VERSION__: JSON.stringify(version) },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
