import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useCallback, useMemo, useState } from 'react'
import { FormattedMessage } from 'react-intl'
import { useParams, useSearchParams } from 'react-router'
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

type Refusal = 'INVITATION_REQUIRED' | 'INVITATION_INVALID' | 'INVITATION_USED'

const REFUSALS: Refusal[] = ['INVITATION_REQUIRED', 'INVITATION_INVALID', 'INVITATION_USED']

export function AnswerPage() {
  const { publicId = '' } = useParams()
  const [params] = useSearchParams()
  // The invitation travels in the link the respondent received; an anonymous survey ignores it.
  const invitation = params.get('invitation') ?? undefined
  const survey = useQuery({
    queryKey: ['public-survey', publicId, invitation],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/public/surveys/{publicId}', {
        params: { path: { publicId }, query: invitation ? { invitation } : undefined },
      })
      if (response.status === 404) return null
      if (error && REFUSALS.includes(error.code as Refusal)) return error.code as Refusal
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
  })
  usePageTitle(typeof survey.data === 'object' ? survey.data?.title : undefined)

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
  if (typeof survey.data === 'string') return <InvitationRefused code={survey.data} />
  return <SurveyForm publicId={publicId} survey={survey.data} invitation={invitation} />
}

function InvitationRefused({ code }: { code: Refusal }) {
  return (
    <Alert severity={code === 'INVITATION_REQUIRED' ? 'info' : 'warning'}>
      {code === 'INVITATION_REQUIRED' && (
        <FormattedMessage
          id="answer.invitation.required"
          defaultMessage="This survey is answered by invitation: open the link you received."
        />
      )}
      {code === 'INVITATION_INVALID' && (
        <FormattedMessage
          id="answer.invitation.invalid"
          defaultMessage="This invitation is not valid for this survey."
        />
      )}
      {code === 'INVITATION_USED' && (
        <FormattedMessage
          id="answer.invitation.used"
          defaultMessage="This invitation has already been used."
        />
      )}
    </Alert>
  )
}

interface FormProps {
  publicId: string
  survey: PublicSurvey
  invitation: string | undefined
}

function SurveyForm({ publicId, survey, invitation }: FormProps) {
  const questions: Question[] = useMemo(() => survey.version.questions, [survey])
  const [answers, setAnswers] = useState<Answers>({})
  const [submitting, setSubmitting] = useState(false)
  const [submitErrors, setSubmitErrors] = useState<QuestionError[]>([])
  const [outcome, setOutcome] = useState<'idle' | 'done' | 'closed' | 'used' | 'failed'>('idle')
  const evaluation = useEvaluation(publicId, survey.version.number, questions, answers, invitation)

  const onChange = useCallback((key: string, value: AnswerValue) => {
    setSubmitErrors([])
    setAnswers((previous) => ({ ...previous, [key]: value }))
  }, [])

  const submit = async () => {
    setSubmitting(true)
    const sent = answersToSend(questions, answers, evaluation.visible)
    const { data, error, response } = await api.POST('/public/surveys/{publicId}/responses', {
      params: { path: { publicId }, query: invitation ? { invitation } : undefined },
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
    if (error?.code === 'INVITATION_USED') {
      setOutcome('used')
      return
    }
    setOutcome(response.status === 409 ? 'closed' : 'failed')
  }

  if (outcome === 'used') return <InvitationRefused code="INVITATION_USED" />

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
