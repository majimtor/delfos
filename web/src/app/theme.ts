import { createTheme } from '@mui/material'

/**
 * The default palette's warning orange does not reach the 4.5:1 contrast that WCAG AA asks of
 * white text on it (the status chip of a draft, for example); this darker orange reaches 5:1 and
 * keeps the rest of the palette.
 */
export const theme = createTheme({
  palette: {
    warning: { main: '#b45309' },
  },
})
