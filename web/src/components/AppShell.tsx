import {
  AppBar,
  Box,
  Button,
  Container,
  FormControl,
  IconButton,
  InputLabel,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Select,
  Toolbar,
  Typography,
  useColorScheme,
} from '@mui/material'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import LightModeIcon from '@mui/icons-material/LightMode'
import SettingsBrightnessIcon from '@mui/icons-material/SettingsBrightness'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { FormattedMessage, useIntl } from 'react-intl'
import { MODES, type Mode } from '../app/colorScheme'
import { useLogout, useSession } from '../features/auth/session'
import { useLocale, type Locale } from '../i18n/locale'
import { BrandMark } from './BrandMark'
import { SiteFooter } from './SiteFooter'

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
        <Toolbar sx={{ gap: 1.5 }}>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{
              flexGrow: 1,
              color: 'inherit',
              textDecoration: 'none',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 1,
            }}
          >
            <BrandMark />
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
          <SchemeMenu />
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
      <SiteFooter />
    </Box>
  )
}

/**
 * The colour scheme: the device's by default, or the one the visitor chooses, remembered per
 * browser. A menu of three radio items behind one button that names the current choice.
 */
function SchemeMenu() {
  const intl = useIntl()
  const { mode, setMode } = useColorScheme()
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const current: Mode = mode ?? 'system'
  const label = intl.formatMessage({ id: 'app.scheme', defaultMessage: 'Colour scheme' })
  const names: Record<Mode, string> = {
    system: intl.formatMessage({ id: 'app.scheme.system', defaultMessage: 'Same as the device' }),
    light: intl.formatMessage({ id: 'app.scheme.light', defaultMessage: 'Light' }),
    dark: intl.formatMessage({ id: 'app.scheme.dark', defaultMessage: 'Dark' }),
  }
  const icons: Record<Mode, ReactNode> = {
    system: <SettingsBrightnessIcon />,
    light: <LightModeIcon />,
    dark: <DarkModeIcon />,
  }
  const open = Boolean(anchor)
  return (
    <>
      <IconButton
        id="scheme-button"
        color="inherit"
        aria-label={`${label}: ${names[current]}`}
        aria-haspopup="menu"
        aria-controls={open ? 'scheme-menu' : undefined}
        aria-expanded={open ? 'true' : undefined}
        onClick={(event) => setAnchor(event.currentTarget)}
      >
        {icons[current]}
      </IconButton>
      <Menu
        id="scheme-menu"
        anchorEl={anchor}
        open={open}
        onClose={() => setAnchor(null)}
        slotProps={{ list: { 'aria-labelledby': 'scheme-button' } }}
      >
        {MODES.map((candidate) => (
          <MenuItem
            key={candidate}
            role="menuitemradio"
            aria-checked={candidate === current}
            selected={candidate === current}
            onClick={() => {
              setMode(candidate)
              setAnchor(null)
            }}
          >
            <ListItemIcon>{icons[candidate]}</ListItemIcon>
            <ListItemText>{names[candidate]}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  )
}
