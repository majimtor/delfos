import { Alert, Button, Stack, TextField, Typography } from '@mui/material'
import { useForm } from 'react-hook-form'
import { FormattedMessage, useIntl } from 'react-intl'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { LoginError, useLogin, useSession } from './session'

interface Credentials {
  username: string
  password: string
}

export function LoginPage() {
  const intl = useIntl()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const session = useSession()
  const login = useLogin()
  const form = useForm<Credentials>({ defaultValues: { username: '', password: '' } })
  const next = params.get('next') ?? '/surveys'

  if (session.data) return <Navigate to={next} replace />

  const submit = form.handleSubmit(async (credentials) => {
    try {
      await login.mutateAsync(credentials)
      navigate(next, { replace: true })
    } catch {
      // The mutation keeps the error; it is rendered below.
    }
  })

  const failure =
    login.error instanceof LoginError ? login.error.code : login.error ? 'INTERNAL_ERROR' : null
  const usernameLabel = intl.formatMessage({ id: 'login.username', defaultMessage: 'Username' })
  const passwordLabel = intl.formatMessage({ id: 'login.password', defaultMessage: 'Password' })

  return (
    <form noValidate onSubmit={(event) => void submit(event)}>
      <Stack spacing={3} sx={{ maxWidth: 420 }}>
        <Typography variant="h4" component="h1">
          <FormattedMessage id="login.title" defaultMessage="Log in" />
        </Typography>
        <Typography color="text.secondary">
          <FormattedMessage
            id="login.demoHint"
            defaultMessage="Demo accounts: demo-manager, demo-manager-2 and demo-admin, all with the password delfos-demo."
          />
        </Typography>
        {failure && (
          <Alert severity="error" role="alert">
            {failure === 'INVALID_CREDENTIALS' && (
              <FormattedMessage
                id="login.error.INVALID_CREDENTIALS"
                defaultMessage="The username or the password is not right."
              />
            )}
            {failure === 'ACCOUNT_DISABLED' && (
              <FormattedMessage
                id="login.error.ACCOUNT_DISABLED"
                defaultMessage="This account is disabled."
              />
            )}
            {failure === 'RATE_LIMITED' && (
              <FormattedMessage
                id="login.error.RATE_LIMITED"
                defaultMessage="Too many attempts. Wait a minute and try again."
              />
            )}
            {failure !== 'INVALID_CREDENTIALS' &&
              failure !== 'ACCOUNT_DISABLED' &&
              failure !== 'RATE_LIMITED' && (
                <FormattedMessage
                  id="login.error.generic"
                  defaultMessage="We could not log you in. Try again."
                />
              )}
          </Alert>
        )}
        <TextField
          label={usernameLabel}
          autoComplete="username"
          error={Boolean(form.formState.errors.username)}
          helperText={
            form.formState.errors.username && (
              <FormattedMessage id="login.required" defaultMessage="Required." />
            )
          }
          {...form.register('username', { required: true })}
        />
        <TextField
          label={passwordLabel}
          type="password"
          autoComplete="current-password"
          error={Boolean(form.formState.errors.password)}
          helperText={
            form.formState.errors.password && (
              <FormattedMessage id="login.required" defaultMessage="Required." />
            )
          }
          {...form.register('password', { required: true })}
        />
        <Button type="submit" variant="contained" disabled={login.isPending}>
          <FormattedMessage id="login.submit" defaultMessage="Enter" />
        </Button>
      </Stack>
    </form>
  )
}
