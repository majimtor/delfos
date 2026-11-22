import { createTheme } from '@mui/material'
import { SCHEME_ATTRIBUTE } from './colorScheme'

/**
 * The visual identity of ADR 0017 as MUI tokens: a petrol brand colour with cool neutrals, a
 * dark scheme with the same roles, IBM Plex Sans served with the web, flat surfaces with a
 * border instead of a shadow, sentence-case buttons and touch targets of 44 pixels. Every text
 * pair reaches the 4.5:1 that WCAG AA asks; theme.test.ts checks it. Keyboard focus keeps a
 * visible ring on every button-like control, where the ripple alone is easy to miss.
 */
export const theme = createTheme({
  cssVariables: { colorSchemeSelector: SCHEME_ATTRIBUTE, cssVarPrefix: 'delfos' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#0B5B6B', dark: '#084A57', light: '#2E7E8E', contrastText: '#FFFFFF' },
        secondary: { main: '#44585A', contrastText: '#FFFFFF' },
        background: { default: '#F4F7F7', paper: '#FFFFFF' },
        text: { primary: '#0F1F20', secondary: '#44585A' },
        divider: '#C9D6D6',
        success: { main: '#137A4B', contrastText: '#FFFFFF' },
        warning: { main: '#A95E0C', contrastText: '#FFFFFF' },
        error: { main: '#B42318', contrastText: '#FFFFFF' },
        info: { main: '#1E5FA6', contrastText: '#FFFFFF' },
      },
    },
    dark: {
      palette: {
        primary: { main: '#6CC5D6', dark: '#4FB3C6', light: '#8AD3E1', contrastText: '#0C1517' },
        secondary: { main: '#9DB2B3', contrastText: '#0C1517' },
        background: { default: '#0C1517', paper: '#122023' },
        text: { primary: '#E6EEEE', secondary: '#9DB2B3' },
        divider: '#2B4145',
        success: { main: '#5BD39A', contrastText: '#0C1517' },
        warning: { main: '#F2B35A', contrastText: '#0C1517' },
        error: { main: '#F28B82', contrastText: '#0C1517' },
        info: { main: '#7FB2F0', contrastText: '#0C1517' },
      },
    },
  },
  typography: {
    fontFamily:
      '"IBM Plex Sans Variable", "IBM Plex Sans", "Segoe UI", system-ui, -apple-system, sans-serif',
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
      letterSpacing: '-0.01em',
      lineHeight: 1.2,
      '@media (min-width:600px)': { fontSize: '1.75rem' },
    },
    h5: { fontSize: '1.25rem', fontWeight: 600, letterSpacing: '-0.01em', lineHeight: 1.3 },
    h6: { fontSize: '1.05rem', fontWeight: 600, lineHeight: 1.4 },
    subtitle1: { fontWeight: 500 },
    // The way back from a page to its list: a small link, not a label in capitals.
    overline: {
      textTransform: 'none',
      letterSpacing: 0,
      fontSize: '0.875rem',
      fontWeight: 500,
      lineHeight: 1.6,
    },
    button: { textTransform: 'none', fontWeight: 500 },
  },
  shape: { borderRadius: 6 },
  components: {
    MuiButtonBase: {
      styleOverrides: {
        root: ({ theme }) => ({
          '&.Mui-focusVisible': {
            outline: `3px solid ${(theme.vars ?? theme).palette.primary.main}`,
            outlineOffset: 2,
          },
        }),
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { minHeight: 44, paddingInline: 18 },
        sizeSmall: { minHeight: 36, paddingInline: 12 },
      },
    },
    MuiIconButton: {
      styleOverrides: { root: { minWidth: 44, minHeight: 44 } },
    },
    MuiToggleButton: {
      styleOverrides: {
        root: ({ theme }) => ({
          textTransform: 'none',
          fontWeight: 500,
          minWidth: 44,
          minHeight: 44,
          color: (theme.vars ?? theme).palette.text.primary,
          '&.Mui-selected': {
            backgroundColor: (theme.vars ?? theme).palette.primary.main,
            color: (theme.vars ?? theme).palette.primary.contrastText,
            '&:hover': { backgroundColor: (theme.vars ?? theme).palette.primary.dark },
          },
        }),
      },
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 500 } },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        // Safari repaints the notch of an outlined field wrongly while its legend animates: the
        // gap stays open after the label returns, or never opens and the floating label crosses
        // the border, above all on multiline fields (WebKit bug 304070; MUI issues 44988 and
        // 46891, where the maintainers recommend this). The notch opens and closes at once.
        notchedOutline: { '& legend': { transition: 'unset' } },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        colorDefault: ({ theme }) => ({
          backgroundColor: (theme.vars ?? theme).palette.background.paper,
          color: (theme.vars ?? theme).palette.text.primary,
          borderBottom: `1px solid ${(theme.vars ?? theme).palette.divider}`,
        }),
      },
    },
    MuiPaper: {
      // No overlay gradient on dark surfaces: the palette already gives each surface its tone.
      styleOverrides: { root: { backgroundImage: 'none' } },
    },
    MuiTableCell: {
      styleOverrides: { head: { fontWeight: 600 } },
    },
    MuiLinearProgress: {
      styleOverrides: { root: { height: 8, borderRadius: 999 }, bar: { borderRadius: 999 } },
    },
  },
})
