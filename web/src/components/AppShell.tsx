import {
  AppBar,
  Box,
  Button,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Toolbar,
  Typography,
} from '@mui/material'
import { useEffect, useRef } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { FormattedMessage, useIntl } from 'react-intl'
import { useLogout, useSession } from '../features/auth/session'
import { useLocale, type Locale } from '../i18n/locale'

export function AppShell() {
  const { locale, setLocale } = useLocale()
  const intl = useIntl()
  const session = useSession()
  const logout = useLogout()
  const navigate = useNavigate()
  const location = useLocation()
  const main = useRef<HTMLDivElement>(null)
  const firstRender = useRef(true)
  const languageLabel = intl.formatMessage({ id: 'app.language', defaultMessage: 'Language' })

  // A single-page application moves the content, not the focus: after each navigation the focus
  // goes to the main region, so keyboard and screen-reader users start from the new page.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false
      return
    }
    main.current?.focus()
  }, [location.pathname])

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <a href="#main" className="skip-link">
        <FormattedMessage id="app.skipToContent" defaultMessage="Skip to content" />
      </a>
      <AppBar position="static" color="default" elevation={0} component="header">
        <Toolbar sx={{ gap: 2 }}>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{ flexGrow: 1, color: 'inherit', textDecoration: 'none' }}
          >
            <FormattedMessage id="app.title" defaultMessage="Delfos" />
          </Typography>
          {session.data && (
            <>
              <Button component={Link} to="/surveys" color="inherit">
                <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
              </Button>
              {session.data.isAdmin && (
                <Button component={Link} to="/accounts" color="inherit">
                  <FormattedMessage id="app.nav.accounts" defaultMessage="Accounts" />
                </Button>
              )}
              <Typography variant="body2" color="text.secondary">
                {session.data.username}
              </Typography>
              <Button
                color="inherit"
                onClick={() =>
                  logout.mutate(undefined, { onSettled: () => navigate('/', { replace: true }) })
                }
                disabled={logout.isPending}
              >
                <FormattedMessage id="app.nav.logout" defaultMessage="Log out" />
              </Button>
            </>
          )}
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel id="language-label">{languageLabel}</InputLabel>
            <Select
              labelId="language-label"
              label={languageLabel}
              value={locale}
              onChange={(event) => setLocale(event.target.value as Locale)}
            >
              <MenuItem value="es">
                <FormattedMessage id="app.language.es" defaultMessage="Español" />
              </MenuItem>
              <MenuItem value="en">
                <FormattedMessage id="app.language.en" defaultMessage="English" />
              </MenuItem>
            </Select>
          </FormControl>
        </Toolbar>
      </AppBar>
      <Container
        component="main"
        id="main"
        ref={main}
        tabIndex={-1}
        maxWidth="md"
        sx={{ py: 4, flexGrow: 1, '&:focus': { outline: 'none' } }}
      >
        <Outlet />
      </Container>
    </Box>
  )
}
