import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { FormattedMessage, FormattedTime, useIntl } from 'react-intl'
import { Link, useParams } from 'react-router'
import { api, apiBaseUrl } from '../../api/client'
import { usePageTitle } from '../../app/usePageTitle'
import { QuestionResultView } from './QuestionResultView'
import { useResults, type Scope } from './useResults'

export const RESULTS_POLL_INTERVAL_MS = 5_000

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
  const results = useResults(surveyId, versionNumber, scope, pollIntervalMs)
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
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
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
        <Typography
          variant="overline"
          component={Link}
          to="/surveys"
          sx={{ textDecoration: 'none' }}
        >
          <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
        </Typography>
        <Typography variant="h4" component="h1" lang={survey.data.language}>
          {survey.data.title}
        </Typography>
        <Typography color="text.secondary" role="status" aria-live="polite">
          <FormattedMessage
            id="results.summary"
            defaultMessage="{count, plural, one {# response} other {# responses}} · updated at {time}"
            values={{
              count: results.data.responseCount,
              time: <FormattedTime value={results.data.computedAt} timeStyle="medium" />,
            }}
          />
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
      <Button
        component="a"
        href={`${apiBaseUrl}/api/v1/surveys/${surveyId}/versions/${versionNumber}/export.csv`}
        variant="outlined"
        size="small"
        sx={{ alignSelf: 'flex-start' }}
      >
        <FormattedMessage
          id="results.export"
          defaultMessage="Download the responses of version {number} (CSV)"
          values={{ number: versionNumber }}
        />
      </Button>
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
