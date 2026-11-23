import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined'
import SensorsOutlinedIcon from '@mui/icons-material/SensorsOutlined'
import { Box, Button, Paper, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'
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
    <Stack spacing={6}>
      <Stack spacing={3} sx={{ alignItems: 'flex-start', pt: { xs: 2, sm: 5 } }}>
        <Typography
          variant="h3"
          component="h1"
          sx={{
            fontWeight: 600,
            letterSpacing: '-0.02em',
            lineHeight: 1.15,
            fontSize: { xs: '1.9rem', sm: '2.4rem', md: '2.75rem' },
            maxWidth: '22ch',
            textWrap: 'balance',
          }}
        >
          <FormattedMessage
            id="landing.lead"
            defaultMessage="Surveys with conditional logic, versions and live results."
          />
        </Typography>
        <Typography
          variant="body1"
          color="textSecondary"
          sx={{ fontSize: '1.1rem', maxWidth: '58ch' }}
        >
          <FormattedMessage
            id="landing.intro"
            defaultMessage="Build questionnaires whose questions appear according to earlier answers, publish them in versions without losing what was collected, and watch the responses arrive as they are given."
          />
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignSelf: 'stretch' }}>
          <Button
            variant="contained"
            size="large"
            component={Link}
            to={`/s/${SAMPLE_SURVEY_PUBLIC_ID[locale]}`}
          >
            <FormattedMessage id="landing.answerSample" defaultMessage="Answer a sample survey" />
          </Button>
          <Button variant="outlined" size="large" component={Link} to="/login">
            <FormattedMessage id="landing.login" defaultMessage="Log in as a manager" />
          </Button>
        </Stack>
      </Stack>

      <Box
        component="ul"
        sx={{
          listStyle: 'none',
          m: 0,
          p: 0,
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: 'repeat(3, 1fr)' },
          border: 1,
          borderColor: 'divider',
          borderRadius: 2.5,
          overflow: 'hidden',
          bgcolor: 'background.paper',
        }}
      >
        <Trait
          icon={<AccountTreeOutlinedIcon fontSize="small" />}
          title={
            <FormattedMessage id="landing.trait.logic.title" defaultMessage="Conditional logic" />
          }
        >
          <FormattedMessage
            id="landing.trait.logic.body"
            defaultMessage="Each question is shown according to rules on the earlier ones; the server evaluates them and the web shows only what is visible."
          />
        </Trait>
        <Trait
          icon={<HistoryOutlinedIcon fontSize="small" />}
          title={<FormattedMessage id="landing.trait.versions.title" defaultMessage="Versions" />}
        >
          <FormattedMessage
            id="landing.trait.versions.body"
            defaultMessage="A new publication keeps the responses of the previous one; the results compare the versions that are comparable."
          />
        </Trait>
        <Trait
          icon={<SensorsOutlinedIcon fontSize="small" />}
          title={<FormattedMessage id="landing.trait.live.title" defaultMessage="Live results" />}
        >
          <FormattedMessage
            id="landing.trait.live.body"
            defaultMessage="Responses arrive as events while they are given; the export comes out as CSV and as a workbook."
          />
        </Trait>
      </Box>

      <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 } }}>
        <Typography variant="body2" color="textSecondary">
          <Typography
            component="span"
            variant="body2"
            sx={{ fontWeight: 500, color: 'text.primary' }}
          >
            <FormattedMessage id="landing.demo.title" defaultMessage="A public demo at no cost." />
          </Typography>{' '}
          <FormattedMessage
            id="landing.demo.body"
            defaultMessage="The accounts demo-manager, demo-manager-2 and demo-admin use the password delfos-demo; the data is reset every day, and the first visit after a quiet night can take up to two minutes while the API wakes."
          />
        </Typography>
      </Paper>
    </Stack>
  )
}

function Trait({
  icon,
  title,
  children,
}: {
  icon: ReactNode
  title: ReactNode
  children: ReactNode
}) {
  return (
    <Box
      component="li"
      sx={{
        p: 3,
        display: 'grid',
        gap: 1,
        alignContent: 'start',
        minWidth: 0,
        '& + &': {
          borderTop: { xs: 1, md: 0 },
          borderLeft: { xs: 0, md: 1 },
          borderColor: { xs: 'divider', md: 'divider' },
        },
      }}
    >
      <Box
        aria-hidden="true"
        sx={{
          width: 36,
          height: 36,
          borderRadius: 1.5,
          display: 'grid',
          placeItems: 'center',
          color: 'primary.main',
          backgroundColor: 'rgba(var(--delfos-palette-primary-mainChannel) / 0.12)',
        }}
      >
        {icon}
      </Box>
      <Typography variant="h6" component="h2">
        {title}
      </Typography>
      <Typography variant="body2" color="textSecondary">
        {children}
      </Typography>
    </Box>
  )
}
