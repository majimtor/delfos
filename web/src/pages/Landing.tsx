import { Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'
import { usePageTitle } from '../app/usePageTitle'
import { useLocale, type Locale } from '../i18n/locale'

/** Public ids of the sample surveys of the demo seed, one per interface language. */
export const SAMPLE_SURVEY_PUBLIC_ID: Record<Locale, string> = {
  es: 'demo-movilidad',
  en: 'demo-mobility',
}

export function Landing() {
  const { locale } = useLocale()
  usePageTitle(null)
  return (
    <Stack spacing={3} sx={{ alignItems: 'flex-start' }}>
      <Typography variant="h4" component="h1">
        <FormattedMessage id="app.title" defaultMessage="Delfos" />
      </Typography>
      <Typography variant="body1">
        <FormattedMessage
          id="landing.lead"
          defaultMessage="Surveys with conditional logic, versions and live results."
        />
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <Button variant="contained" component={Link} to={`/s/${SAMPLE_SURVEY_PUBLIC_ID[locale]}`}>
          <FormattedMessage id="landing.answerSample" defaultMessage="Answer a sample survey" />
        </Button>
        <Button variant="outlined" component={Link} to="/login">
          <FormattedMessage id="landing.login" defaultMessage="Log in as a manager" />
        </Button>
      </Stack>
    </Stack>
  )
}
