import { createTheme } from '@mui/material'

/**
 * Two accessibility adjustments over the default theme: the warning orange does not reach the
 * 4.5:1 contrast that WCAG AA asks of white text on it (the status chip of a draft, for example),
 * so a darker one that reaches 5:1 replaces it; and keyboard focus gets a visible ring on every
 * button-like control, where the default ripple alone is easy to miss.
 */
export const theme = createTheme({
  palette: {
    warning: { main: '#b45309' },
  },
  components: {
    MuiButtonBase: {
      styleOverrides: {
        root: ({ theme }) => ({
          '&.Mui-focusVisible': {
            outline: `3px solid ${theme.palette.primary.main}`,
            outlineOffset: 2,
          },
        }),
      },
    },
  },
})
