import { Alert, Button, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { FormattedMessage, useIntl } from 'react-intl'
import { useNavigate } from 'react-router'
import { api } from '../../api/client'
import type { Schemas } from '../../api/types'
import { usePageTitle } from '../../app/usePageTitle'
import { useLocale, type Locale } from '../../i18n/locale'

interface Fields {
  title: string
  description: string
}

const MAX_TITLE = 200
const MAX_DESCRIPTION = 2000

/** Creates a survey with its title, description and language, then opens the editor of its first draft. */
export function NewSurveyPage() {
  const intl = useIntl()
  const navigate = useNavigate()
  const { locale } = useLocale()
  const [language, setLanguage] = useState<Locale>(locale)
  const form = useForm<Fields>({ defaultValues: { title: '', description: '' } })
  usePageTitle(intl.formatMessage({ id: 'newSurvey.title', defaultMessage: 'New survey' }))

  const create = useMutation({
    mutationFn: async (body: Schemas['CreateSurveyRequest']) => {
      const { data, response } = await api.POST('/surveys', { body })
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    onSuccess: (survey) => navigate(`/surveys/${survey.id}/draft`),
  })

  const submit = form.handleSubmit((fields) => {
    const description = fields.description.trim()
    create.mutate({
      title: fields.title.trim(),
      language,
      ...(description ? { description } : {}),
    })
  })
  const errors = form.formState.errors
  const required = <FormattedMessage id="editor.error.required" defaultMessage="Required." />

  return (
    <form noValidate onSubmit={(event) => void submit(event)}>
      <Stack spacing={3} sx={{ maxWidth: 640 }}>
        <Typography variant="h4" component="h1">
          <FormattedMessage id="newSurvey.title" defaultMessage="New survey" />
        </Typography>
        {create.isError && (
          <Alert severity="error" role="alert">
            <FormattedMessage
              id="newSurvey.failed"
              defaultMessage="The survey could not be created."
            />
          </Alert>
        )}
        <TextField
          label={intl.formatMessage({ id: 'survey.field.title', defaultMessage: 'Title' })}
          required
          error={Boolean(errors.title)}
          helperText={
            errors.title?.type === 'maxLength'
              ? intl.formatMessage(
                  { id: 'error.TOO_LONG', defaultMessage: 'Use at most {max} characters.' },
                  { max: MAX_TITLE },
                )
              : errors.title && required
          }
          {...form.register('title', {
            required: true,
            maxLength: MAX_TITLE,
            validate: (v) => v.trim() !== '',
          })}
        />
        <TextField
          label={intl.formatMessage({
            id: 'survey.field.description',
            defaultMessage: 'Description',
          })}
          multiline
          minRows={2}
          error={Boolean(errors.description)}
          helperText={
            errors.description &&
            intl.formatMessage(
              { id: 'error.TOO_LONG', defaultMessage: 'Use at most {max} characters.' },
              { max: MAX_DESCRIPTION },
            )
          }
          {...form.register('description', { maxLength: MAX_DESCRIPTION })}
        />
        <TextField
          select
          label={intl.formatMessage({
            id: 'newSurvey.field.language',
            defaultMessage: 'Language of the survey',
          })}
          value={language}
          onChange={(event) => setLanguage(event.target.value as Locale)}
          sx={{ maxWidth: 240 }}
        >
          <MenuItem value="es">
            <FormattedMessage id="app.language.es" defaultMessage="Español" />
          </MenuItem>
          <MenuItem value="en">
            <FormattedMessage id="app.language.en" defaultMessage="English" />
          </MenuItem>
        </TextField>
        <Button
          type="submit"
          variant="contained"
          disabled={create.isPending}
          sx={{ alignSelf: 'flex-start' }}
        >
          <FormattedMessage id="newSurvey.submit" defaultMessage="Create and edit the questions" />
        </Button>
      </Stack>
    </form>
  )
}
