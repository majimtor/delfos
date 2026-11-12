import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { FormattedDate, FormattedMessage, defineMessages, useIntl } from 'react-intl'
import { Link } from 'react-router'
import { api } from '../../api/client'
import type { Problem, Schemas } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'

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

type Failure = keyof typeof failureMessages

export function SurveysPage() {
  const intl = useIntl()
  usePageTitle(intl.formatMessage({ id: 'surveys.title', defaultMessage: 'My surveys' }))
  const queryClient = useQueryClient()
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

  if (surveys.isPending) {
    return (
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
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

  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        <FormattedMessage id="surveys.title" defaultMessage="My surveys" />
      </Typography>
      {failure && (
        <Alert severity="error" role="alert" onClose={() => setFailure(null)}>
          <FormattedMessage {...failureMessages[failure]} />
        </Alert>
      )}
      {items.length === 0 ? (
        <Typography color="text.secondary">
          <FormattedMessage id="surveys.empty" defaultMessage="You have no surveys yet." />
        </Typography>
      ) : (
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
                <TableCell>
                  <FormattedDate value={survey.updatedAt} dateStyle="medium" timeStyle="short" />
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    {survey.hasDraft && survey.status !== 'CLOSED' && (
                      <Button
                        size="small"
                        onClick={() => act.mutate({ id: survey.id, action: 'publish' })}
                        disabled={act.isPending}
                      >
                        <FormattedMessage id="surveys.action.publish" defaultMessage="Publish" />
                      </Button>
                    )}
                    {survey.status === 'PUBLISHED' && (
                      <Button
                        size="small"
                        onClick={() => act.mutate({ id: survey.id, action: 'close' })}
                        disabled={act.isPending}
                      >
                        <FormattedMessage id="surveys.action.close" defaultMessage="Close" />
                      </Button>
                    )}
                    {survey.publishedVersionNumber !== undefined && (
                      <Button
                        size="small"
                        component={Link}
                        to={`/surveys/${survey.id}/versions/${survey.publishedVersionNumber}/results`}
                      >
                        <FormattedMessage id="surveys.action.results" defaultMessage="Results" />
                      </Button>
                    )}
                    {survey.status !== 'DRAFT' && (
                      <Button
                        size="small"
                        component={Link}
                        to={`/s/${survey.publicId}`}
                        target="_blank"
                        rel="noopener"
                      >
                        <FormattedMessage
                          id="surveys.action.answerLink"
                          defaultMessage="Public link"
                        />
                      </Button>
                    )}
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
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
