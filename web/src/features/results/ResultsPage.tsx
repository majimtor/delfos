import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import {
  Alert,
  Box,
  Button,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { FormattedMessage, FormattedTime, useIntl } from 'react-intl'
import { useParams } from 'react-router'
import { api, apiBaseUrl } from '../../api/client'
import { usePageTitle } from '../../app/usePageTitle'
import { PageSkeleton } from '../../components/PageSkeleton'
import { ParentLink } from '../../components/ParentLink'
import { QuestionResultView } from './QuestionResultView'
import { useResults, type Scope } from './useResults'
import { useResultsEvents } from './useResultsEvents'

export const RESULTS_POLL_INTERVAL_MS = 5_000
/** While the event stream is open, polling only guards against a missed event. */
export const RESULTS_FALLBACK_POLL_INTERVAL_MS = 60_000

export function ResultsPage({
  pollIntervalMs = RESULTS_POLL_INTERVAL_MS,
}: {
  pollIntervalMs?: number
}) {
  const params = useParams()
  const surveyId = params.surveyId ?? ''
  const versionNumber = Number(params.versionNumber)
  const [scope, setScope] = useState<Scope>('version')
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
  const live = useResultsEvents(surveyId)
  const results = useResults(
    surveyId,
    versionNumber,
    scope,
    live ? Math.max(pollIntervalMs, RESULTS_FALLBACK_POLL_INTERVAL_MS) : pollIntervalMs,
  )
  const intl = useIntl()
  usePageTitle(
    survey.data
      ? intl.formatMessage(
          { id: 'results.pageTitle', defaultMessage: 'Results of {title}' },
          { title: survey.data.title },
        )
      : null,
  )

  if (survey.isPending || results.isPending) {
    return (
      <PageSkeleton
        label={intl.formatMessage({ id: 'results.loading', defaultMessage: 'Loading the results' })}
        rows={4}
      />
    )
  }
  if (survey.isError || results.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage
          id="results.loadFailed"
          defaultMessage="The results could not be loaded."
        />
      </Alert>
    )
  }

  return (
    <Stack spacing={3}>
      <Box>
        <ParentLink to="/surveys">
          <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
        </ParentLink>
        <Typography variant="h4" component="h1" lang={survey.data.language}>
          {survey.data.title}
        </Typography>
        <Typography color="textSecondary" role="status" aria-live="polite">
          <FormattedMessage
            id="results.summary"
            defaultMessage="{count, plural, one {# response} other {# responses}} · updated at {time}"
            values={{
              count: results.data.responseCount,
              time: <FormattedTime value={results.data.computedAt} timeStyle="medium" />,
            }}
          />
          {live && (
            <>
              {' · '}
              <span className="live-dot" aria-hidden="true" />
              <FormattedMessage id="results.live" defaultMessage="live" />
            </>
          )}
        </Typography>
      </Box>
      <ToggleButtonGroup
        exclusive
        value={scope}
        onChange={(_, next: Scope | null) => next && setScope(next)}
        size="small"
        aria-label="scope"
      >
        <ToggleButton value="version">
          <FormattedMessage
            id="results.scope.version"
            defaultMessage="Version {number}"
            values={{ number: versionNumber }}
          />
        </ToggleButton>
        <ToggleButton value="comparable">
          <FormattedMessage id="results.scope.comparable" defaultMessage="All versions" />
        </ToggleButton>
      </ToggleButtonGroup>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ alignItems: 'flex-start' }}>
        <Button
          component="a"
          href={`${apiBaseUrl}/api/v1/surveys/${surveyId}/versions/${versionNumber}/export.csv`}
          variant="outlined"
          size="small"
          startIcon={<FileDownloadOutlinedIcon />}
          aria-label={intl.formatMessage(
            {
              id: 'results.export',
              defaultMessage: 'CSV, download the responses of version {number}',
            },
            { number: versionNumber },
          )}
        >
          CSV
        </Button>
        <Button
          component="a"
          href={`${apiBaseUrl}/api/v1/surveys/${surveyId}/versions/${versionNumber}/export.xlsx`}
          variant="outlined"
          size="small"
          startIcon={<FileDownloadOutlinedIcon />}
          aria-label={intl.formatMessage(
            {
              id: 'results.exportXlsx',
              defaultMessage: 'Excel, download the responses of version {number}',
            },
            { number: versionNumber },
          )}
        >
          Excel
        </Button>
      </Stack>
      {results.data.questions.map((question) => (
        <QuestionResultView
          key={question.key}
          result={question}
          surveyId={surveyId}
          versionNumber={versionNumber}
          language={survey.data.language}
          showVersions={scope === 'comparable'}
        />
      ))}
    </Stack>
  )
}
