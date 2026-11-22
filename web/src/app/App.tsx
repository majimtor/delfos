import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { WaitForApi } from '../components/WaitForApi'
import { LocaleProvider } from '../i18n/LocaleProvider'
import { MODE_STORAGE_KEY } from './colorScheme'
import { router } from './router'
import { theme } from './theme'

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } },
})

export function App() {
  return (
    <LocaleProvider>
      <ThemeProvider
        theme={theme}
        defaultMode="system"
        modeStorageKey={MODE_STORAGE_KEY}
        disableTransitionOnChange
      >
        <CssBaseline enableColorScheme />
        <QueryClientProvider client={queryClient}>
          <WaitForApi>
            <RouterProvider router={router} />
          </WaitForApi>
        </QueryClientProvider>
      </ThemeProvider>
    </LocaleProvider>
  )
}
