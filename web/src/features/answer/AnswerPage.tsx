import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useCallback, useMemo, useState } from 'react'
import { FormattedMessage } from 'react-intl'
import { useParams } from 'react-router'
import { api } from '../../api/client'
import { usePageTitle } from '../../app/usePageTitle'
import type { PublicSurvey, Question } from '../../api/types'
import {
  answersToSend,
  attributeErrors,
  type Answers,
  type AnswerValue,
  type QuestionError,
} from './answers'
import { QuestionField } from './QuestionField'
import { useEvaluation } from './useEvaluation'

export function AnswerPage() {
  const { publicId = '' } = useParams()
  const survey = useQuery({
    queryKey: ['public-survey', publicId],
    queryFn: async () => {
      const { data, response } = await api.GET('/public/surveys/{publicId}', {
        params: { path: { publicId } },
      })
      if (response.status === 404) return null
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
  })
  usePageTitle(survey.data?.title)

  if (survey.isPending) {
    return (
      <Box role="status" aria-live="polite" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
        <Typography sx={{ mt: 2 }}>
          <FormattedMessage id="answer.loading" defaultMessage="Loading the survey" />
        </Typography>
      </Box>
    )
  }
  if (survey.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage id="answer.failed" defaultMessage="The survey could not be loaded." />
      </Alert>
    )
  }
  if (survey.data === null) {
    return (
      <Alert severity="warning">
        <FormattedMessage id="answer.notFound" defaultMessage="This survey does not exist." />
      </Alert>
    )
  }
  return <SurveyForm publicId={publicId} survey={survey.data} />
}

function SurveyForm({ publicId, survey }: { publicId: string; survey: PublicSurvey }) {
  const questions: Question[] = useMemo(() => survey.version.questions, [survey])
  const [answers, setAnswers] = useState<Answers>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitErrors, setSubmitErrors] = useState<QuestionError[]>([])
  const [outcome, setOutcome] = useState<'idle' | 'done' | 'closed' | 'failed'>('idle')
  const evaluation = useEvaluation(publicId, survey.version.number, questions, answers)

  const onChange = useCallback((key: string, value: AnswerValue) => {
    setSubmitErrors([])
    setAnswers((previous) => ({ ...previous, [key]: value }))
  }, [])

  const submit = async () => {
    setSubmitting(true)
    const sent = answersToSend(questions, answers, evaluation.visible)
    const { data, error, response } = await api.POST('/public/surveys/{publicId}/responses', {
      params: { path: { publicId } },
      body: { versionNumber: survey.version.number, answers: sent },
    })
    setSubmitting(false)
    if (data) {
      setOutcome('done')
      return
    }
    if (response.status === 400 && error?.errors) {
      setSubmitErrors(attributeErrors(error.errors, sent))
      return
    }
    setOutcome(response.status === 409 ? 'closed' : 'failed')
  }

  if (outcome === 'done') {
    return (
      <Stack spacing={2} role="status">
        <Typography variant="h5" component="h1">
          <FormattedMessage id="answer.thanks.title" defaultMessage="Thank you" />
        </Typography>
        <Typography>
          <FormattedMessage
            id="answer.thanks.body"
            defaultMessage="Your response has been recorded."
          />
        </Typography>
      </Stack>
    )
  }

  const errorsByKey = new Map<string, QuestionError[]>()
  for (const error of [...evaluation.errors, ...submitErrors]) {
    errorsByKey.set(error.questionKey, [...(errorsByKey.get(error.questionKey) ?? []), error])
  }
  const missingFromSubmit = new Set(
    submitErrors.filter((e) => e.code === 'MISSING_REQUIRED_ANSWER').map((e) => e.questionKey),
  )
  const closed = !survey.acceptingResponses || outcome === 'closed'

  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        void submit()
      }}
    >
      <Typography variant="h4" component="h1" gutterBottom lang={survey.language}>
        {survey.title}
      </Typography>
      {survey.description && (
        <Typography color="text.secondary" sx={{ mb: 3 }} lang={survey.language}>
          {survey.description}
        </Typography>
      )}
      {closed && (
        <Alert severity="info" sx={{ mb: 3 }}>
          <FormattedMessage
            id="answer.closed"
            defaultMessage="This survey is no longer accepting responses."
          />
        </Alert>
      )}
      {outcome === 'failed' && (
        <Alert severity="error" sx={{ mb: 3 }}>
          <FormattedMessage
            id="answer.submitFailed"
            defaultMessage="The response could not be sent. Try again."
          />
        </Alert>
      )}
      <Box lang={survey.language}>
        {questions
          .filter((question) => evaluation.visible.has(question.key))
          .map((question) => (
            <QuestionField
              key={question.key}
              question={question}
              value={answers[question.key]}
              errors={errorsByKey.get(question.key) ?? []}
              missing={missingFromSubmit.has(question.key)}
              onChange={(value) => onChange(question.key, value)}
            />
          ))}
      </Box>
      <Button
        type="submit"
        variant="contained"
        disabled={closed || submitting || evaluation.pending}
      >
        {submitting ? (
          <FormattedMessage id="answer.submitting" defaultMessage="Sending" />
        ) : (
          <FormattedMessage id="answer.submit" defaultMessage="Send my response" />
        )}
      </Button>
    </form>
  )
}
