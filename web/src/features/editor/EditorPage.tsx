import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Menu,
  MenuItem,
  Stack,
  Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useReducer, useState } from 'react'
import { FormattedMessage, useIntl } from 'react-intl'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../../api/client'
import type { FieldError, Problem, Schemas, SurveyVersion } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'
import {
  QUESTION_TYPES,
  draftReducer,
  fromVersion,
  toDraftDefinition,
  type DraftAction,
  type DraftDefinition,
} from './draft'
import { draftErrorMessage } from './errors'
import { typeLabel } from './labels'
import { QuestionEditor } from './QuestionEditor'
import { validateDraft, type DraftError } from './validation'

class RequestFailed extends Error {
  readonly status: number
  readonly problem: Problem | null

  constructor(status: number, problem: Problem | null) {
    super(`request failed with ${status}`)
    this.status = status
    this.problem = problem
  }
}

/** Loads the survey and its draft, then hands them to the editor; the states before that are shown here. */
export function EditorPage() {
  const intl = useIntl()
  const surveyId = useParams().surveyId ?? ''
  const survey = useQuery({
    queryKey: ['survey', surveyId],
    queryFn: async () => {
      const { data, response } = await api.GET('/surveys/{surveyId}', {
        params: { path: { surveyId } },
      })
      if (!data) throw new RequestFailed(response.status, null)
      return data
    },
  })
  const draft = useQuery({
    queryKey: ['draft', surveyId],
    queryFn: async () => {
      const { data, error, response } = await api.GET('/surveys/{surveyId}/draft', {
        params: { path: { surveyId } },
      })
      if (!data) throw new RequestFailed(response.status, error ?? null)
      return data
    },
    retry: false,
  })
  usePageTitle(
    survey.data
      ? intl.formatMessage(
          { id: 'editor.pageTitle', defaultMessage: 'Draft of {title}' },
          { title: survey.data.title },
        )
      : null,
  )

  if (survey.isPending || draft.isPending) {
    return (
      <Box role="status" sx={{ py: 4, textAlign: 'center' }}>
        <CircularProgress aria-hidden />
      </Box>
    )
  }
  if (
    draft.isError &&
    draft.error instanceof RequestFailed &&
    draft.error.problem?.code === 'NO_DRAFT'
  ) {
    return (
      <Alert severity="info" action={<BackToSurveys />}>
        <FormattedMessage id="editor.noDraft" defaultMessage="This survey has no draft to edit." />
      </Alert>
    )
  }
  if (survey.isError || draft.isError) {
    return (
      <Alert severity="error">
        <FormattedMessage id="editor.loadFailed" defaultMessage="The draft could not be loaded." />
      </Alert>
    )
  }
  return <DraftEditor surveyId={surveyId} survey={survey.data} draft={draft.data} />
}

type Notice = 'saved' | 'saveFailed' | 'state' | null

interface EditorProps {
  surveyId: string
  survey: Schemas['Survey']
  draft: SurveyVersion
}

/** The questions and options of the draft, saved as a whole with every error shown in place. */
function DraftEditor({ surveyId, survey, draft }: EditorProps) {
  const intl = useIntl()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [state, dispatch] = useReducer(draftReducer, draft, fromVersion)
  const [attempted, setAttempted] = useState<'save' | 'publish' | null>(null)
  const [serverErrors, setServerErrors] = useState<FieldError[] | null>(null)
  const [notice, setNotice] = useState<Notice>(null)
  const [addMenu, setAddMenu] = useState<HTMLElement | null>(null)

  const save = useMutation({
    mutationFn: async (definition: DraftDefinition): Promise<SurveyVersion> => {
      const { data, error, response } = await api.PUT('/surveys/{surveyId}/draft', {
        params: { path: { surveyId } },
        body: definition,
      })
      if (data) return data
      throw new RequestFailed(response.status, error ?? null)
    },
  })
  const publish = useMutation({
    mutationFn: async () => {
      const { data, error, response } = await api.POST('/surveys/{surveyId}/publish', {
        params: { path: { surveyId } },
      })
      if (data) return data
      throw new RequestFailed(response.status, error ?? null)
    },
  })

  const edit = (action: DraftAction) => {
    setServerErrors(null)
    setNotice(null)
    dispatch(action)
  }
  const errors: DraftError[] = serverErrors ?? (attempted ? validateDraft(state, attempted) : [])
  const errorsAt = (path: string, prefix = false) =>
    errors.filter((error) => (prefix ? error.path.startsWith(path) : error.path === path))
  const topErrors = errors.filter((error) => error.path === 'questions')

  const submit = async (andPublish: boolean) => {
    const mode = andPublish ? 'publish' : 'save'
    setAttempted(mode)
    setNotice(null)
    setServerErrors(null)
    if (validateDraft(state, mode).length > 0) return
    try {
      const saved = await save.mutateAsync(toDraftDefinition(state))
      dispatch({ type: 'loaded', version: saved })
      await queryClient.invalidateQueries({ queryKey: ['surveys'] })
      if (!andPublish) {
        setNotice('saved')
        return
      }
      await publish.mutateAsync()
      navigate('/surveys')
    } catch (failure) {
      if (failure instanceof RequestFailed && failure.problem?.code === 'VALIDATION_ERROR') {
        setServerErrors(failure.problem.errors ?? [])
      } else if (failure instanceof RequestFailed && failure.status === 409) {
        setNotice('state')
      } else {
        setNotice('saveFailed')
      }
    }
  }

  const busy = save.isPending || publish.isPending
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
        <Typography variant="h4" component="h1" lang={survey.language}>
          {survey.title}
        </Typography>
        <Typography color="text.secondary">
          <FormattedMessage
            id="editor.draftNumber"
            defaultMessage="Draft of version {number}"
            values={{ number: draft.number }}
          />
        </Typography>
      </Box>
      {notice && (
        <Alert
          severity={notice === 'saved' ? 'success' : 'error'}
          role={notice === 'saved' ? 'status' : 'alert'}
          onClose={() => setNotice(null)}
        >
          {notice === 'saved' && (
            <FormattedMessage id="editor.saved" defaultMessage="Draft saved." />
          )}
          {notice === 'saveFailed' && (
            <FormattedMessage
              id="editor.saveFailed"
              defaultMessage="The draft could not be saved."
            />
          )}
          {notice === 'state' && (
            <FormattedMessage
              id="surveys.error.state"
              defaultMessage="The survey does not allow that action in its current state."
            />
          )}
        </Alert>
      )}
      {errors.length > 0 && (
        <Alert severity="error" role="alert">
          {topErrors.length > 0
            ? draftErrorMessage(intl, topErrors[0]!)
            : intl.formatMessage(
                {
                  id: 'editor.fixErrors',
                  defaultMessage:
                    '{count, plural, one {# field needs} other {# fields need}} attention.',
                },
                { count: errors.length },
              )}
        </Alert>
      )}
      {state.questions.length === 0 && (
        <Typography color="text.secondary">
          <FormattedMessage id="editor.empty" defaultMessage="The draft has no questions yet." />
        </Typography>
      )}
      {state.questions.map((question, index) => (
        <QuestionEditor
          key={question.id}
          question={question}
          questions={state.questions}
          rules={state.rules}
          index={index}
          count={state.questions.length}
          errorsAt={errorsAt}
          onEdit={edit}
        />
      ))}
      <Box>
        <Button
          variant="outlined"
          onClick={(event) => setAddMenu(event.currentTarget)}
          aria-haspopup="menu"
          aria-expanded={addMenu ? true : undefined}
        >
          <FormattedMessage id="editor.addQuestion" defaultMessage="Add a question" />
        </Button>
        <Menu anchorEl={addMenu} open={Boolean(addMenu)} onClose={() => setAddMenu(null)}>
          {QUESTION_TYPES.map((type) => (
            <MenuItem
              key={type}
              onClick={() => {
                setAddMenu(null)
                edit({ type: 'addQuestion', questionType: type })
              }}
            >
              {typeLabel(intl, type)}
            </MenuItem>
          ))}
        </Menu>
      </Box>
      <Stack direction="row" spacing={2}>
        <Button variant="contained" disabled={busy} onClick={() => void submit(false)}>
          <FormattedMessage id="editor.save" defaultMessage="Save the draft" />
        </Button>
        <Button variant="outlined" disabled={busy} onClick={() => void submit(true)}>
          <FormattedMessage id="editor.publish" defaultMessage="Save and publish" />
        </Button>
      </Stack>
    </Stack>
  )
}

function BackToSurveys() {
  return (
    <Button component={Link} to="/surveys" color="inherit" size="small">
      <FormattedMessage id="app.nav.surveys" defaultMessage="My surveys" />
    </Button>
  )
}
