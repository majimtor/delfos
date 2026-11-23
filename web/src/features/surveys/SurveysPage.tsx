import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined'
import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import MailOutlinedIcon from '@mui/icons-material/MailOutlined'
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined'
import PollOutlinedIcon from '@mui/icons-material/PollOutlined'
import PublishOutlinedIcon from '@mui/icons-material/PublishOutlined'
import {
  Alert,
  Button,
  Chip,
  IconButton,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { FormattedDate, FormattedMessage, defineMessages, useIntl } from 'react-intl'
import { Link, useNavigate } from 'react-router'
import { api } from '../../api/client'
import type { Problem, Schemas } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'
import { EmptyState } from '../../components/EmptyState'
import { PageSkeleton } from '../../components/PageSkeleton'

type Survey = Schemas['Survey']

const SURVEYS_QUERY_KEY = ['surveys'] as const

const failureMessages = defineMessages({
  noQuestions: {
    id: 'surveys.error.noQuestions',
    defaultMessage: 'The draft has no questions; it cannot be published.',
  },
  invalidDraft: {
    id: 'surveys.error.invalidDraft',
    defaultMessage: 'The draft has errors; fix them before publishing.',
  },
  state: {
    id: 'surveys.error.state',
    defaultMessage: 'The survey does not allow that action in its current state.',
  },
  generic: { id: 'surveys.error.generic', defaultMessage: 'The action could not be completed.' },
})

const actionMessages = defineMessages({
  edit: { id: 'surveys.action.edit', defaultMessage: 'Edit the draft' },
  publish: { id: 'surveys.action.publish', defaultMessage: 'Publish' },
  newVersion: { id: 'surveys.action.newVersion', defaultMessage: 'New version' },
  close: { id: 'surveys.action.close', defaultMessage: 'Close' },
  results: { id: 'surveys.action.results', defaultMessage: 'Results' },
  invitations: { id: 'surveys.action.invitations', defaultMessage: 'Invitations' },
  answerLink: { id: 'surveys.action.answerLink', defaultMessage: 'Public link' },
})

type Failure = keyof typeof failureMessages

export function SurveysPage() {
  const intl = useIntl()
  usePageTitle(intl.formatMessage({ id: 'surveys.title', defaultMessage: 'My surveys' }))
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const theme = useTheme()
  // Below the medium breakpoint the table has no room for its five columns: one card per survey.
  const compact = useMediaQuery(theme.breakpoints.down('md'))
  const [failure, setFailure] = useState<Failure | null>(null)
  const surveys = useInfiniteQuery({
    queryKey: SURVEYS_QUERY_KEY,
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, response } = await api.GET('/surveys', {
        params: { query: { cursor: pageParam, limit: 20 } },
      })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    getNextPageParam: (last) => last.nextCursor,
  })

  const act = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'publish' | 'close' }) => {
      const { data, error, response } = await api.POST(`/surveys/{surveyId}/${action}`, {
        params: { path: { surveyId: id } },
      })
      if (data) return data
      throw new ActionError(error ?? null, response.status)
    },
    onMutate: () => setFailure(null),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: SURVEYS_QUERY_KEY }),
    onError: (error) => setFailure(error instanceof ActionError ? error.failure : 'generic'),
  })

  // A published survey is edited through a new draft copied from the published version.
  const newVersion = useMutation({
    mutationFn: async (id: string) => {
      const { data, error, response } = await api.POST('/surveys/{surveyId}/draft', {
        params: { path: { surveyId: id } },
      })
      if (data) return id
      throw new ActionError(error ?? null, response.status)
    },
    onMutate: () => setFailure(null),
    onSuccess: async (id) => {
      await queryClient.invalidateQueries({ queryKey: SURVEYS_QUERY_KEY })
      navigate(`/surveys/${id}/draft`)
    },
    onError: (error) => setFailure(error instanceof ActionError ? error.failure : 'generic'),
  })

  if (surveys.isPending) {
    return (
      <PageSkeleton
        label={intl.formatMessage({ id: 'surveys.loading', defaultMessage: 'Loading the surveys' })}
        rows={4}
      />
    )
  }
  if (surveys.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage
          id="surveys.loadFailed"
          defaultMessage="The surveys could not be loaded."
        />
      </Alert>
    )
  }
  const items: Survey[] = surveys.data.pages.flatMap((page) => page.items)
  const busy = act.isPending || newVersion.isPending
  const actionsOf = (survey: Survey, variant: 'icons' | 'text') => (
    <SurveyActions
      survey={survey}
      variant={variant}
      busy={busy}
      onPublish={() => act.mutate({ id: survey.id, action: 'publish' })}
      onClose={() => act.mutate({ id: survey.id, action: 'close' })}
      onNewVersion={() => newVersion.mutate(survey.id)}
    />
  )
  const newSurvey = (
    <Button variant="contained" component={Link} to="/surveys/new">
      <FormattedMessage id="surveys.action.new" defaultMessage="New survey" />
    </Button>
  )

  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ justifyContent: 'space-between', alignItems: { sm: 'center' } }}
      >
        <Typography variant="h4" component="h1">
          <FormattedMessage id="surveys.title" defaultMessage="My surveys" />
        </Typography>
        {items.length > 0 && newSurvey}
      </Stack>
      {failure && (
        <Alert severity="error" role="alert" onClose={() => setFailure(null)}>
          <FormattedMessage {...failureMessages[failure]} />
        </Alert>
      )}
      {items.length === 0 ? (
        <EmptyState
          icon={<InboxOutlinedIcon />}
          title={<FormattedMessage id="surveys.empty" defaultMessage="You have no surveys yet." />}
          description={
            <FormattedMessage
              id="surveys.emptyHint"
              defaultMessage="Create the first one and share its public link, or invite the people who should answer."
            />
          }
          action={newSurvey}
        />
      ) : compact ? (
        <Stack
          component="ul"
          spacing={2}
          aria-label="surveys"
          sx={{ listStyle: 'none', m: 0, p: 0 }}
        >
          {items.map((survey) => (
            <Paper
              component="li"
              key={survey.id}
              variant="outlined"
              sx={{ p: 2, display: 'grid', gap: 1 }}
            >
              <Stack
                direction="row"
                spacing={1.5}
                sx={{ alignItems: 'center', justifyContent: 'space-between' }}
              >
                <Typography variant="h6" component="h2" lang={survey.language} sx={{ minWidth: 0 }}>
                  {survey.title}
                </Typography>
                <StatusChip survey={survey} />
              </Stack>
              <Typography variant="body2" color="textSecondary">
                <FormattedMessage
                  id="surveys.card.meta"
                  defaultMessage="{count, plural, one {# response} other {# responses}} · updated {date}"
                  values={{
                    count: survey.responseCount,
                    date: (
                      <FormattedDate
                        value={survey.updatedAt}
                        dateStyle="medium"
                        timeStyle="short"
                      />
                    ),
                  }}
                />
              </Typography>
              {actionsOf(survey, 'text')}
            </Paper>
          ))}
        </Stack>
      ) : (
        <TableContainer>
          <Table size="small" aria-label="surveys">
            <TableHead>
              <TableRow>
                <TableCell>
                  <FormattedMessage id="surveys.column.title" defaultMessage="Title" />
                </TableCell>
                <TableCell>
                  <FormattedMessage id="surveys.column.status" defaultMessage="Status" />
                </TableCell>
                <TableCell align="right">
                  <FormattedMessage id="surveys.column.responses" defaultMessage="Responses" />
                </TableCell>
                <TableCell>
                  <FormattedMessage id="surveys.column.updated" defaultMessage="Updated" />
                </TableCell>
                <TableCell>
                  <FormattedMessage id="surveys.column.actions" defaultMessage="Actions" />
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((survey) => (
                <TableRow key={survey.id}>
                  <TableCell lang={survey.language}>{survey.title}</TableCell>
                  <TableCell>
                    <StatusChip survey={survey} />
                  </TableCell>
                  <TableCell align="right">{survey.responseCount}</TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>
                    <FormattedDate value={survey.updatedAt} dateStyle="medium" timeStyle="short" />
                  </TableCell>
                  <TableCell sx={{ whiteSpace: 'nowrap', py: 0.5 }}>
                    {actionsOf(survey, 'icons')}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      {surveys.hasNextPage && (
        <Button
          onClick={() => void surveys.fetchNextPage()}
          disabled={surveys.isFetchingNextPage}
          sx={{ alignSelf: 'flex-start' }}
        >
          <FormattedMessage id="surveys.loadMore" defaultMessage="Load more" />
        </Button>
      )}
    </Stack>
  )
}

/**
 * The actions a survey allows in its state. In the table they are icon buttons with a tooltip and
 * an accessible name, so a row stays one line; on a card they are text buttons, since a touch
 * screen shows no tooltip.
 */
function SurveyActions({
  survey,
  variant,
  busy,
  onPublish,
  onClose,
  onNewVersion,
}: {
  survey: Survey
  variant: 'icons' | 'text'
  busy: boolean
  onPublish: () => void
  onClose: () => void
  onNewVersion: () => void
}) {
  const intl = useIntl()
  const draftOpen = survey.hasDraft && survey.status !== 'CLOSED'
  const actions: {
    key: keyof typeof actionMessages
    icon: ReactNode
    to?: string
    onClick?: () => void
    external?: boolean
  }[] = []
  if (draftOpen)
    actions.push({
      key: 'edit',
      icon: <EditOutlinedIcon fontSize="small" />,
      to: `/surveys/${survey.id}/draft`,
    })
  if (draftOpen)
    actions.push({
      key: 'publish',
      icon: <PublishOutlinedIcon fontSize="small" />,
      onClick: onPublish,
    })
  if (survey.status === 'PUBLISHED' && !survey.hasDraft) {
    actions.push({
      key: 'newVersion',
      icon: <ContentCopyOutlinedIcon fontSize="small" />,
      onClick: onNewVersion,
    })
  }
  if (survey.status === 'PUBLISHED')
    actions.push({ key: 'close', icon: <LockOutlinedIcon fontSize="small" />, onClick: onClose })
  if (survey.publishedVersionNumber !== undefined) {
    actions.push({
      key: 'results',
      icon: <PollOutlinedIcon fontSize="small" />,
      to: `/surveys/${survey.id}/versions/${survey.publishedVersionNumber}/results`,
    })
  }
  if (survey.respondentMode === 'INVITED') {
    actions.push({
      key: 'invitations',
      icon: <MailOutlinedIcon fontSize="small" />,
      to: `/surveys/${survey.id}/invitations`,
    })
  }
  if (survey.status !== 'DRAFT') {
    actions.push({
      key: 'answerLink',
      icon: <OpenInNewOutlinedIcon fontSize="small" />,
      to: `/s/${survey.publicId}`,
      external: true,
    })
  }
  return (
    <Stack
      direction="row"
      spacing={variant === 'icons' ? 0 : 1}
      useFlexGap
      sx={{ flexWrap: 'wrap' }}
    >
      {actions.map(({ key, icon, to, onClick, external }) => {
        const label = intl.formatMessage(actionMessages[key])
        const linkProps = to
          ? { component: Link, to, ...(external ? { target: '_blank', rel: 'noopener' } : {}) }
          : {}
        if (variant === 'icons') {
          return (
            <Tooltip key={key} title={label}>
              <IconButton
                aria-label={label}
                size="small"
                color="primary"
                onClick={onClick}
                disabled={Boolean(onClick) && busy}
                {...linkProps}
              >
                {icon}
              </IconButton>
            </Tooltip>
          )
        }
        return (
          <Button
            key={key}
            size="small"
            startIcon={icon}
            onClick={onClick}
            disabled={Boolean(onClick) && busy}
            {...linkProps}
          >
            {label}
          </Button>
        )
      })}
    </Stack>
  )
}

function StatusChip({ survey }: { survey: Survey }) {
  const label =
    survey.status === 'DRAFT' ? (
      <FormattedMessage id="surveys.status.DRAFT" defaultMessage="Draft" />
    ) : survey.status === 'PUBLISHED' ? (
      <FormattedMessage id="surveys.status.PUBLISHED" defaultMessage="Published" />
    ) : (
      <FormattedMessage id="surveys.status.CLOSED" defaultMessage="Closed" />
    )
  const color =
    survey.status === 'PUBLISHED' ? 'success' : survey.status === 'CLOSED' ? 'default' : 'warning'
  return <Chip size="small" color={color} label={label} />
}

class ActionError extends Error {
  readonly failure: Failure

  constructor(problem: Problem | null, status: number) {
    super(problem?.code ?? String(status))
    const noQuestions = problem?.errors?.some((e) => e.code === 'NO_QUESTIONS') ?? false
    this.failure =
      problem?.code === 'VALIDATION_ERROR' && noQuestions
        ? 'noQuestions'
        : problem?.code === 'VALIDATION_ERROR'
          ? 'invalidDraft'
          : problem?.code === 'INVALID_STATE_TRANSITION' || problem?.code === 'NO_DRAFT'
            ? 'state'
            : 'generic'
  }
}
