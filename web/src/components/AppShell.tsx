import DarkModeIcon from '@mui/icons-material/DarkMode'
import LightModeIcon from '@mui/icons-material/LightMode'
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined'
import SettingsBrightnessIcon from '@mui/icons-material/SettingsBrightness'
import TranslateOutlinedIcon from '@mui/icons-material/TranslateOutlined'
import {
  AppBar,
  Box,
  Button,
  Container,
  Divider,
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
  useMediaQuery,
  useTheme,
} from '@mui/material'
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
  const theme = useTheme()
  // Below the medium breakpoint the bar keeps the mark and three icon buttons; the navigation and
  // the session go behind a menu.
  const compact = useMediaQuery(theme.breakpoints.down('md'))
  const main = useRef<HTMLDivElement>(null)
  const firstRender = useRef(true)
  const languageLabel = intl.formatMessage({ id: 'app.language', defaultMessage: 'Language' })
  const languages: Record<Locale, string> = {
    es: intl.formatMessage({ id: 'app.language.es', defaultMessage: 'Español' }),
    en: intl.formatMessage({ id: 'app.language.en', defaultMessage: 'English' }),
  }

  // A single-page application moves the content, not the focus: after each navigation the focus
  // goes to the main region, so keyboard and screen-reader users start from the new page.
  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false
      return
    }
    main.current?.focus()
  }, [location.pathname])

  const signOut = () =>
    logout.mutate(undefined, { onSettled: () => navigate('/', { replace: true }) })

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <a href="#main" className="skip-link">
        <FormattedMessage id="app.skipToContent" defaultMessage="Skip to content" />
      </a>
      <AppBar position="static" color="default" elevation={0} component="header">
        <Toolbar sx={{ gap: compact ? 0.5 : 1.5 }}>
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
          {!compact && session.data && (
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
              <Button color="inherit" onClick={signOut} disabled={logout.isPending}>
                <FormattedMessage id="app.nav.logout" defaultMessage="Log out" />
              </Button>
            </>
          )}
          {compact ? (
            <RadioMenu
              id="language"
              label={languageLabel}
              icon={<TranslateOutlinedIcon />}
              options={(['es', 'en'] as const).map((value) => ({ value, name: languages[value] }))}
              value={locale}
              onChange={setLocale}
            />
          ) : (
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel id="language-label">{languageLabel}</InputLabel>
              <Select
                labelId="language-label"
                label={languageLabel}
                value={locale}
                onChange={(event) => setLocale(event.target.value as Locale)}
              >
                <MenuItem value="es">{languages.es}</MenuItem>
                <MenuItem value="en">{languages.en}</MenuItem>
              </Select>
            </FormControl>
          )}
          <SchemeMenu />
          {compact && session.data && (
            <SessionMenu
              username={session.data.username}
              isAdmin={session.data.isAdmin}
              onSignOut={signOut}
              signingOut={logout.isPending}
            />
          )}
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

/** One icon button that opens a menu of radio items; the button names the current choice. */
function RadioMenu<T extends string>({
  id,
  label,
  icon,
  options,
  value,
  onChange,
}: {
  id: string
  label: string
  icon: ReactNode
  options: { value: T; name: string; icon?: ReactNode }[]
  value: T
  onChange: (value: T) => void
}) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const open = Boolean(anchor)
  const current = options.find((option) => option.value === value)
  return (
    <>
      <IconButton
        id={`${id}-button`}
        color="inherit"
        aria-label={`${label}: ${current?.name ?? value}`}
        aria-haspopup="menu"
        aria-controls={open ? `${id}-menu` : undefined}
        aria-expanded={open ? 'true' : undefined}
        onClick={(event) => setAnchor(event.currentTarget)}
      >
        {current?.icon ?? icon}
      </IconButton>
      <Menu
        id={`${id}-menu`}
        anchorEl={anchor}
        open={open}
        onClose={() => setAnchor(null)}
        slotProps={{ list: { 'aria-labelledby': `${id}-button` } }}
      >
        {options.map((option) => (
          <MenuItem
            key={option.value}
            role="menuitemradio"
            aria-checked={option.value === value}
            selected={option.value === value}
            onClick={() => {
              onChange(option.value)
              setAnchor(null)
            }}
          >
            {option.icon && <ListItemIcon>{option.icon}</ListItemIcon>}
            <ListItemText>{option.name}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  )
}

/**
 * The colour scheme: the device's by default, or the one the visitor chooses, remembered per
 * browser.
 */
function SchemeMenu() {
  const intl = useIntl()
  const { mode, setMode } = useColorScheme()
  const current: Mode = mode ?? 'system'
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
  return (
    <RadioMenu
      id="scheme"
      label={intl.formatMessage({ id: 'app.scheme', defaultMessage: 'Colour scheme' })}
      icon={icons[current]}
      options={MODES.map((value) => ({ value, name: names[value], icon: icons[value] }))}
      value={current}
      onChange={setMode}
    />
  )
}

/** On a narrow screen: the pages of the session and the way out, behind one button. */
function SessionMenu({
  username,
  isAdmin,
  onSignOut,
  signingOut,
}: {
  username: string
  isAdmin: boolean
  onSignOut: () => void
  signingOut: boolean
}) {
  const intl = useIntl()
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const open = Boolean(anchor)
  const close = () => setAnchor(null)
  return (
    <>
      <IconButton
        id="session-button"
        color="inherit"
        aria-label={intl.formatMessage({ id: 'app.nav.menu', defaultMessage: 'Menu' })}
        aria-haspopup="menu"
        aria-controls={open ? 'session-menu' : undefined}
        aria-expanded={open ? 'true' : undefined}
        onClick={(event) => setAnchor(event.currentTarget)}
      >
        <MenuOutlinedIcon />
      </IconButton>
      <Menu
        id="session-menu"
        anchorEl={anchor}
        open={open}
        onClose={close}
        slotProps={{ list: { 'aria-labelledby': 'session-button' } }}
      >
        <MenuItem disabled sx={{ opacity: '1 !important' }}>
          <ListItemText secondary={username}>
            <FormattedMessage id="app.nav.signedInAs" defaultMessage="Signed in as" />
          </ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem component={Link} to="/surveys" onClick={close}>
          <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
        </MenuItem>
        {isAdmin && (
          <MenuItem component={Link} to="/accounts" onClick={close}>
            <FormattedMessage id="app.nav.accounts" defaultMessage="Accounts" />
          </MenuItem>
        )}
        <Divider />
        <MenuItem
          onClick={() => {
            close()
            onSignOut()
          }}
          disabled={signingOut}
        >
          <FormattedMessage id="app.nav.logout" defaultMessage="Log out" />
        </MenuItem>
      </Menu>
    </>
  )
}
