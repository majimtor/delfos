import { Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

export function NotFound() {
  return (
    <Stack spacing={2} alignItems="flex-start">
      <Typography variant="h5" component="h1">
        <FormattedMessage id="notFound.title" defaultMessage="This page does not exist" />
      </Typography>
      <Button component={Link} to="/">
        <FormattedMessage id="notFound.home" defaultMessage="Back to the start" />
      </Button>
    </Stack>
  )
}
