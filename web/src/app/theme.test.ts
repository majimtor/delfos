import type { Palette } from '@mui/material'
import { describe, expect, it } from 'vitest'
import { theme as created } from './theme'

// The theme carries its two schemes at runtime; the library's type declares only the active one.
const theme = created as unknown as {
  colorSchemes: Record<'light' | 'dark', { palette: Palette }>
  colorSchemeSelector: string
}

// The tokens of ADR 0017 promise WCAG AA contrast for every text pair in both colour schemes;
// a change of a value that breaks the promise fails here before it reaches a page.

function luminance(hex: string): number {
  const channel = (value: number) => {
    const c = value / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  }
  const n = parseInt(hex.slice(1), 16)
  return 0.2126 * channel(n >> 16) + 0.7152 * channel((n >> 8) & 255) + 0.0722 * channel(n & 255)
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

describe.each(['light', 'dark'] as const)('the %s colour scheme', (scheme) => {
  const palette = theme.colorSchemes[scheme].palette
  const surfaces = [palette.background.default, palette.background.paper]

  it.each([
    ['primary text', palette.text.primary],
    ['secondary text', palette.text.secondary],
    ['primary colour', palette.primary.main],
    ['success', palette.success.main],
    ['error', palette.error.main],
    ['info', palette.info.main],
  ])('keeps %s readable on both surfaces', (_name, colour) => {
    for (const surface of surfaces) expect(contrast(colour, surface)).toBeGreaterThanOrEqual(4.5)
  })

  it.each([
    ['primary', palette.primary],
    ['success', palette.success],
    ['warning', palette.warning],
    ['error', palette.error],
    ['info', palette.info],
  ])('keeps the text on a %s control readable', (_name, colour) => {
    expect(contrast(colour.contrastText, colour.main)).toBeGreaterThanOrEqual(4.5)
  })
})

it('selects the scheme through the attribute the first-paint script sets', () => {
  expect(theme.colorSchemeSelector).toBe('data-delfos-scheme')
})
