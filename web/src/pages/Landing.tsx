import { Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

/** Public id of the sample survey of the demo seed. */
export const SAMPLE_SURVEY_PUBLIC_ID = 'demo-movilidad'

export function Landing() {
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
        <Button variant="contained" component={Link} to={`/s/${SAMPLE_SURVEY_PUBLIC_ID}`}>
          <FormattedMessage id="landing.answerSample" defaultMessage="Answer a sample survey" />
        </Button>
        <Button variant="outlined" component={Link} to="/login">
          <FormattedMessage id="landing.login" defaultMessage="Log in as a manager" />
        </Button>
      </Stack>
    </Stack>
  )
}
