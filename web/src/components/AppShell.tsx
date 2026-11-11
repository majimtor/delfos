import {
  AppBar,
  Box,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Toolbar,
  Typography,
} from '@mui/material'
import { Link, Outlet } from 'react-router'
import { FormattedMessage, useIntl } from 'react-intl'
import { useLocale, type Locale } from '../i18n/locale'

export function AppShell() {
  const { locale, setLocale } = useLocale()
  const intl = useIntl()
  const languageLabel = intl.formatMessage({ id: 'app.language', defaultMessage: 'Language' })
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="static" color="default" elevation={0} component="header">
        <Toolbar>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{ flexGrow: 1, color: 'inherit', textDecoration: 'none' }}
          >
            <FormattedMessage id="app.title" defaultMessage="Delfos" />
          </Typography>
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
      <Container component="main" maxWidth="md" sx={{ py: 4, flexGrow: 1 }}>
        <Outlet />
      </Container>
    </Box>
  )
}
