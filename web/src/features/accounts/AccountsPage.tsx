import {
  Alert,
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  FormControlLabel,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { FormattedDate, FormattedMessage, useIntl } from 'react-intl'
import { api } from '../../api/client'
import type { Problem, Schemas } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'
import { useSession } from '../auth/session'

type Account = Schemas['Account']

interface Fields {
  username: string
  password: string
  isAdmin: boolean
}

const USERNAME = /^[a-z0-9][a-z0-9._-]{2,63}$/
const MIN_PASSWORD = 12

const ACCOUNTS_QUERY_KEY = ['accounts'] as const

class CreateError extends Error {
  readonly problem: Problem | null

  constructor(problem: Problem | null, status: number) {
    super(problem?.code ?? String(status))
    this.problem = problem
  }
}

/** The accounts of the managers, for an administrator: create them, see them, disable them. */
export function AccountsPage() {
  const intl = useIntl()
  const session = useSession()
  const queryClient = useQueryClient()
  const [created, setCreated] = useState<string | null>(null)
  const [disableFailed, setDisableFailed] = useState(false)
  const form = useForm<Fields>({ defaultValues: { username: '', password: '', isAdmin: false } })
  usePageTitle(intl.formatMessage({ id: 'accounts.title', defaultMessage: 'Accounts' }))

  const isAdmin = session.data?.isAdmin ?? false
  const accounts = useInfiniteQuery({
    queryKey: ACCOUNTS_QUERY_KEY,
    enabled: isAdmin,
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, response } = await api.GET('/accounts', {
        params: { query: { cursor: pageParam, limit: 50 } },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    getNextPageParam: (last) => last.nextCursor,
  })

  const create = useMutation({
    mutationFn: async (fields: Fields) => {
      const { data, error, response } = await api.POST('/accounts', {
        body: { username: fields.username, password: fields.password, isAdmin: fields.isAdmin },
      })
      if (data) return data
      throw new CreateError(error ?? null, response.status)
    },
    onSuccess: async (account) => {
      setCreated(account.username)
      form.reset()
      await queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY })
    },
  })
  const disable = useMutation({
    mutationFn: async (accountId: string) => {
      const { data, response } = await api.POST('/accounts/{accountId}/disable', {
        params: { path: { accountId } },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    onMutate: () => setDisableFailed(false),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ACCOUNTS_QUERY_KEY }),
    onError: () => setDisableFailed(true),
  })

  if (!isAdmin) {
    return (
      <Alert severity="info">
        <FormattedMessage
          id="accounts.adminOnly"
          defaultMessage="Only an administrator manages accounts."
        />
      </Alert>
    )
  }
  if (accounts.isPending) {
    return (
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
    )
  }
  if (accounts.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage
          id="accounts.loadFailed"
          defaultMessage="The accounts could not be loaded."
        />
      </Alert>
    )
  }

  const items: Account[] = accounts.data.pages.flatMap((page) => page.items)
  const errors = form.formState.errors
  const taken =
    create.error instanceof CreateError && create.error.problem?.code === 'USERNAME_TAKEN'
  const submit = form.handleSubmit((fields) => {
    setCreated(null)
    create.mutate({ ...fields, username: fields.username.trim() })
  })

  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        <FormattedMessage id="accounts.title" defaultMessage="Accounts" />
      </Typography>
      <Paper
        variant="outlined"
        component="form"
        noValidate
        aria-labelledby="new-account"
        onSubmit={(event) => void submit(event)}
        sx={{ p: 2 }}
      >
        <Stack spacing={2} sx={{ maxWidth: 480 }}>
          <Typography id="new-account" variant="h6" component="h2">
            <FormattedMessage id="accounts.new.title" defaultMessage="New account" />
          </Typography>
          {created && (
            <Alert severity="success" role="status" onClose={() => setCreated(null)}>
              <FormattedMessage
                id="accounts.new.created"
                defaultMessage="Account {username} created."
                values={{ username: created }}
              />
            </Alert>
          )}
          {create.isError && (
            <Alert severity="error" role="alert">
              {taken ? (
                <FormattedMessage
                  id="accounts.new.taken"
                  defaultMessage="That username is already taken."
                />
              ) : (
                <FormattedMessage
                  id="accounts.new.failed"
                  defaultMessage="The account could not be created."
                />
              )}
            </Alert>
          )}
          <TextField
            label={intl.formatMessage({ id: 'login.username', defaultMessage: 'Username' })}
            autoComplete="off"
            required
            error={Boolean(errors.username)}
            helperText={intl.formatMessage({
              id: 'accounts.new.usernameHint',
              defaultMessage: '3 to 64 lowercase letters, digits, dots, hyphens or underscores.',
            })}
            {...form.register('username', { validate: (v) => USERNAME.test(v.trim()) })}
          />
          <TextField
            label={intl.formatMessage({ id: 'login.password', defaultMessage: 'Password' })}
            type="password"
            autoComplete="new-password"
            required
            error={Boolean(errors.password)}
            helperText={intl.formatMessage(
              { id: 'accounts.new.passwordHint', defaultMessage: 'At least {min} characters.' },
              { min: MIN_PASSWORD },
            )}
            {...form.register('password', { minLength: MIN_PASSWORD, required: true })}
          />
          <FormControlLabel
            control={<Checkbox {...form.register('isAdmin')} />}
            label={intl.formatMessage({
              id: 'accounts.role.ADMIN',
              defaultMessage: 'Administrator',
            })}
          />
          <Button
            type="submit"
            variant="contained"
            disabled={create.isPending}
            sx={{ alignSelf: 'flex-start' }}
          >
            <FormattedMessage id="accounts.new.submit" defaultMessage="Create the account" />
          </Button>
        </Stack>
      </Paper>
      {disableFailed && (
        <Alert severity="error" role="alert" onClose={() => setDisableFailed(false)}>
          <FormattedMessage
            id="accounts.disableFailed"
            defaultMessage="The account could not be disabled."
          />
        </Alert>
      )}
      <Table size="small" aria-label="accounts">
        <TableHead>
          <TableRow>
            <TableCell>
              <FormattedMessage id="accounts.column.username" defaultMessage="Username" />
            </TableCell>
            <TableCell>
              <FormattedMessage id="accounts.column.role" defaultMessage="Role" />
            </TableCell>
            <TableCell>
              <FormattedMessage id="accounts.column.status" defaultMessage="Status" />
            </TableCell>
            <TableCell>
              <FormattedMessage id="accounts.column.created" defaultMessage="Created" />
            </TableCell>
            <TableCell>
              <FormattedMessage id="accounts.column.actions" defaultMessage="Actions" />
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {items.map((account) => {
            const self = account.id === session.data?.accountId
            return (
              <TableRow key={account.id}>
                <TableCell>
                  {account.username}
                  {self && (
                    <>
                      {' '}
                      <Typography component="span" variant="body2" color="text.secondary">
                        <FormattedMessage id="accounts.you" defaultMessage="(you)" />
                      </Typography>
                    </>
                  )}
                </TableCell>
                <TableCell>
                  {account.isAdmin ? (
                    <FormattedMessage id="accounts.role.ADMIN" defaultMessage="Administrator" />
                  ) : (
                    <FormattedMessage id="accounts.role.MANAGER" defaultMessage="Manager" />
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    color={account.status === 'ACTIVE' ? 'success' : 'default'}
                    label={
                      account.status === 'ACTIVE' ? (
                        <FormattedMessage id="accounts.status.ACTIVE" defaultMessage="Active" />
                      ) : (
                        <FormattedMessage id="accounts.status.DISABLED" defaultMessage="Disabled" />
                      )
                    }
                  />
                </TableCell>
                <TableCell>
                  <FormattedDate value={account.createdAt} dateStyle="medium" timeStyle="short" />
                </TableCell>
                <TableCell>
                  {account.status === 'ACTIVE' && !self && (
                    <Button
                      size="small"
                      onClick={() => disable.mutate(account.id)}
                      disabled={disable.isPending}
                      aria-label={intl.formatMessage(
                        { id: 'accounts.disableNamed', defaultMessage: 'Disable {username}' },
                        { username: account.username },
                      )}
                    >
                      <FormattedMessage id="accounts.disable" defaultMessage="Disable" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
      {accounts.hasNextPage && (
        <Button
          onClick={() => void accounts.fetchNextPage()}
          disabled={accounts.isFetchingNextPage}
          sx={{ alignSelf: 'flex-start' }}
        >
          <FormattedMessage id="surveys.loadMore" defaultMessage="Load more" />
        </Button>
      )}
    </Stack>
  )
}
