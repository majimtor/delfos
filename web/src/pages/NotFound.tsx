import { Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'
import { FormattedMessage, useIntl } from 'react-intl'
import { usePageTitle } from '../app/usePageTitle'

export function NotFound() {
  const intl = useIntl()
  usePageTitle(
    intl.formatMessage({ id: 'notFound.title', defaultMessage: 'This page does not exist' }),
  )
  return (
    <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
      <Typography variant="h5" component="h1">
        <FormattedMessage id="notFound.title" defaultMessage="This page does not exist" />
      </Typography>
      <Button component={Link} to="/">
        <FormattedMessage id="notFound.home" defaultMessage="Back to the start" />
      </Button>
    </Stack>
  )
}
