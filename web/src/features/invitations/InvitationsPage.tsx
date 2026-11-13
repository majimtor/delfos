import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  List,
  ListItem,
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
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { FormattedDate, FormattedMessage, useIntl } from 'react-intl'
import { Link, useParams } from 'react-router'
import { api } from '../../api/client'
import type { Schemas } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'

type Invitation = Schemas['Invitation']
type CreatedInvitation = Schemas['CreatedInvitation']

const MAX_PER_BATCH = 200

/** The single-use invitations of a survey: created in batches whose links are shown once, listed with their state, revoked one by one. */
export function InvitationsPage() {
  const intl = useIntl()
  const queryClient = useQueryClient()
  const surveyId = useParams().surveyId ?? ''
  const [count, setCount] = useState(5)
  const [created, setCreated] = useState<CreatedInvitation[]>([])
  const [failure, setFailure] = useState<'create' | 'revoke' | null>(null)
  const [copied, setCopied] = useState<string | null>(null)

  const survey = useQuery({
    queryKey: ['survey', surveyId],
    queryFn: async () => {
      const { data, response } = await api.GET('/surveys/{surveyId}', {
        params: { path: { surveyId } },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
  })
  const invitationsKey = ['invitations', surveyId] as const
  const invitations = useInfiniteQuery({
    queryKey: invitationsKey,
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, response } = await api.GET('/surveys/{surveyId}/invitations', {
        params: { path: { surveyId }, query: { cursor: pageParam, limit: 50 } },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    getNextPageParam: (last) => last.nextCursor,
  })
  usePageTitle(
    survey.data
      ? intl.formatMessage(
          { id: 'invitations.pageTitle', defaultMessage: 'Invitations of {title}' },
          { title: survey.data.title },
        )
      : null,
  )

  const create = useMutation({
    mutationFn: async (howMany: number) => {
      const { data, response } = await api.POST('/surveys/{surveyId}/invitations', {
        params: { path: { surveyId } },
        body: { count: howMany },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data.items
    },
    onMutate: () => setFailure(null),
    onSuccess: async (items) => {
      setCreated(items)
      await queryClient.invalidateQueries({ queryKey: invitationsKey })
    },
    onError: () => setFailure('create'),
  })
  const revoke = useMutation({
    mutationFn: async (invitationId: string) => {
      const { response } = await api.DELETE('/surveys/{surveyId}/invitations/{invitationId}', {
        params: { path: { surveyId, invitationId } },
      })
      if (!response.ok) throw new Error(`unexpected status ${response.status}`)
    },
    onMutate: () => setFailure(null),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: invitationsKey }),
    onError: () => setFailure('revoke'),
  })

  if (survey.isPending || invitations.isPending) {
    return (
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
    )
  }
  if (survey.isError || invitations.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage
          id="invitations.loadFailed"
          defaultMessage="The invitations could not be loaded."
        />
      </Alert>
    )
  }

  const linkOf = (token: string) =>
    `${window.location.origin}/s/${survey.data.publicId}?invitation=${encodeURIComponent(token)}`
  const copy = async (text: string, what: string) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(what)
    } catch {
      setCopied(null)
    }
  }
  const items: Invitation[] = invitations.data.pages.flatMap((page) => page.items)
  const invited = survey.data.respondentMode === 'INVITED'
  const canCreate = invited && survey.data.status !== 'CLOSED'

  return (
    <Stack spacing={3}>
      <Box>
        <Typography
          variant="overline"
          component={Link}
          to="/surveys"
          sx={{ textDecoration: 'none' }}
        >
          <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
        </Typography>
        <Typography variant="h4" component="h1">
          <FormattedMessage id="invitations.title" defaultMessage="Invitations" />
        </Typography>
        <Typography color="text.secondary" lang={survey.data.language}>
          {survey.data.title}
        </Typography>
      </Box>
      {!invited && (
        <Alert severity="info">
          <FormattedMessage
            id="invitations.anonymous"
            defaultMessage="This survey is answered by anyone with the link; invitations apply to surveys in invited mode."
          />
        </Alert>
      )}
      {invited && survey.data.status === 'CLOSED' && (
        <Alert severity="info">
          <FormattedMessage
            id="invitations.closed"
            defaultMessage="The survey is closed: its invitations can be listed, but no new ones created."
          />
        </Alert>
      )}
      {failure && (
        <Alert severity="error" role="alert" onClose={() => setFailure(null)}>
          {failure === 'create' ? (
            <FormattedMessage
              id="invitations.createFailed"
              defaultMessage="The invitations could not be created."
            />
          ) : (
            <FormattedMessage
              id="invitations.revokeFailed"
              defaultMessage="The invitation could not be revoked."
            />
          )}
        </Alert>
      )}
      {canCreate && (
        <Stack
          component="form"
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: { sm: 'flex-start' } }}
          onSubmit={(event) => {
            event.preventDefault()
            create.mutate(count)
          }}
        >
          <TextField
            label={intl.formatMessage({ id: 'invitations.count', defaultMessage: 'How many' })}
            type="number"
            size="small"
            value={count}
            onChange={(event) =>
              setCount(Math.min(MAX_PER_BATCH, Math.max(1, Number(event.target.value) || 1)))
            }
            slotProps={{ htmlInput: { min: 1, max: MAX_PER_BATCH } }}
            sx={{ width: 140 }}
          />
          <Button type="submit" variant="contained" disabled={create.isPending}>
            <FormattedMessage id="invitations.create" defaultMessage="Create invitations" />
          </Button>
        </Stack>
      )}
      {created.length > 0 && (
        <Paper
          variant="outlined"
          component="section"
          aria-labelledby="new-links"
          sx={{ p: 2 }}
          role="status"
        >
          <Stack spacing={1}>
            <Typography id="new-links" variant="subtitle1" component="h2">
              <FormattedMessage
                id="invitations.newLinks"
                defaultMessage="New links, shown once: copy them now."
              />
            </Typography>
            <List dense disablePadding>
              {created.map((item, index) => (
                <ListItem
                  key={item.id}
                  disableGutters
                  secondaryAction={
                    <IconButton
                      size="small"
                      onClick={() => void copy(linkOf(item.token), item.id)}
                      aria-label={intl.formatMessage(
                        { id: 'invitations.copy', defaultMessage: 'Copy link {number}' },
                        { number: index + 1 },
                      )}
                    >
                      <ContentCopyIcon fontSize="small" />
                    </IconButton>
                  }
                >
                  <Typography component="code" sx={{ fontSize: '0.85rem', wordBreak: 'break-all' }}>
                    {linkOf(item.token)}
                  </Typography>
                  {copied === item.id && (
                    <Typography component="span" variant="caption" sx={{ ml: 1 }}>
                      <FormattedMessage id="invitations.copied" defaultMessage="Copied." />
                    </Typography>
                  )}
                </ListItem>
              ))}
            </List>
            <Button
              size="small"
              onClick={() => void copy(created.map((item) => linkOf(item.token)).join('\n'), 'all')}
              sx={{ alignSelf: 'flex-start' }}
            >
              <FormattedMessage id="invitations.copyAll" defaultMessage="Copy all the links" />
              {copied === 'all' && (
                <>
                  {' '}
                  <FormattedMessage id="invitations.copied" defaultMessage="Copied." />
                </>
              )}
            </Button>
          </Stack>
        </Paper>
      )}
      {items.length === 0 ? (
        <Typography color="text.secondary">
          <FormattedMessage id="invitations.empty" defaultMessage="No invitations yet." />
        </Typography>
      ) : (
        <Table size="small" aria-label="invitations">
          <TableHead>
            <TableRow>
              <TableCell>
                <FormattedMessage id="invitations.column.created" defaultMessage="Created" />
              </TableCell>
              <TableCell>
                <FormattedMessage id="invitations.column.status" defaultMessage="Status" />
              </TableCell>
              <TableCell>
                <FormattedMessage id="invitations.column.used" defaultMessage="Used at" />
              </TableCell>
              <TableCell>
                <FormattedMessage id="invitations.column.actions" defaultMessage="Actions" />
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((invitation) => (
              <TableRow key={invitation.id}>
                <TableCell>
                  <FormattedDate
                    value={invitation.createdAt}
                    dateStyle="medium"
                    timeStyle="short"
                  />
                </TableCell>
                <TableCell>
                  <StatusChip status={invitation.status} />
                </TableCell>
                <TableCell>
                  {invitation.usedAt && (
                    <FormattedDate value={invitation.usedAt} dateStyle="medium" timeStyle="short" />
                  )}
                </TableCell>
                <TableCell>
                  {invitation.status === 'PENDING' && (
                    <Button
                      size="small"
                      onClick={() => revoke.mutate(invitation.id)}
                      disabled={revoke.isPending}
                    >
                      <FormattedMessage id="invitations.revoke" defaultMessage="Revoke" />
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      {invitations.hasNextPage && (
        <Button
          onClick={() => void invitations.fetchNextPage()}
          disabled={invitations.isFetchingNextPage}
          sx={{ alignSelf: 'flex-start' }}
        >
          <FormattedMessage id="surveys.loadMore" defaultMessage="Load more" />
        </Button>
      )}
    </Stack>
  )
}

function StatusChip({ status }: { status: Invitation['status'] }) {
  const label =
    status === 'PENDING' ? (
      <FormattedMessage id="invitations.status.PENDING" defaultMessage="Pending" />
    ) : status === 'USED' ? (
      <FormattedMessage id="invitations.status.USED" defaultMessage="Used" />
    ) : (
      <FormattedMessage id="invitations.status.REVOKED" defaultMessage="Revoked" />
    )
  const color = status === 'USED' ? 'success' : status === 'PENDING' ? 'warning' : 'default'
  return <Chip size="small" color={color} label={label} />
}
