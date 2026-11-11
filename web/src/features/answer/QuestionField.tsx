import {
  Checkbox,
  FormControl,
  FormControlLabel,
  FormGroup,
  FormHelperText,
  FormLabel,
  Radio,
  RadioGroup,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import { FormattedMessage, useIntl } from 'react-intl'
import type { Question } from '../../api/types'
import type { AnswerValue, QuestionError } from './answers'
import { errorMessage } from './errors'

interface Props {
  question: Question
  value: AnswerValue | undefined
  errors: QuestionError[]
  missing: boolean
  onChange: (value: AnswerValue) => void
}

/** One question of the survey, rendered by type with its hints and its errors. */
export function QuestionField({ question, value, errors, missing, onChange }: Props) {
  const intl = useIntl()
  const errorId = `${question.key}-error`
  const hintId = `${question.key}-hint`
  const messages = [
    ...errors.map((error) => errorMessage(intl, error)),
    ...(missing && errors.length === 0
      ? [
          intl.formatMessage({
            id: 'error.MISSING_REQUIRED_ANSWER',
            defaultMessage: 'This question is required.',
          }),
        ]
      : []),
  ]
  const hint = hintFor(question, intl)
  const hasError = messages.length > 0
  const describedBy =
    [hint ? hintId : null, hasError ? errorId : null].filter(Boolean).join(' ') || undefined

  const label = (
    <>
      {question.text}
      {question.required && (
        <Typography component="span" color="text.secondary" sx={{ ml: 1 }}>
          <FormattedMessage id="answer.required" defaultMessage="(required)" />
        </Typography>
      )}
    </>
  )

  return (
    <FormControl
      component="fieldset"
      fullWidth
      error={hasError}
      sx={{ mb: 3 }}
      aria-describedby={describedBy}
    >
      <FormLabel component="legend" sx={{ mb: 1 }}>
        {label}
      </FormLabel>
      {question.type === 'SINGLE_CHOICE' && (
        <RadioGroup
          value={value?.optionKey ?? ''}
          onChange={(event) => onChange({ optionKey: event.target.value })}
        >
          {question.options?.map((option) => (
            <FormControlLabel
              key={option.key}
              value={option.key}
              control={<Radio />}
              label={option.label}
            />
          ))}
        </RadioGroup>
      )}
      {question.type === 'MULTIPLE_CHOICE' && (
        <FormGroup>
          {question.options?.map((option) => {
            const selected = value?.optionKeys ?? []
            const checked = selected.includes(option.key)
            return (
              <FormControlLabel
                key={option.key}
                control={
                  <Checkbox
                    checked={checked}
                    onChange={() =>
                      onChange({
                        optionKeys: checked
                          ? selected.filter((k) => k !== option.key)
                          : [...selected, option.key],
                      })
                    }
                  />
                }
                label={option.label}
              />
            )
          })}
        </FormGroup>
      )}
      {question.type === 'TEXT' && (
        <TextField
          value={value?.text ?? ''}
          onChange={(event) => onChange({ text: event.target.value })}
          multiline={question.multiline ?? false}
          minRows={question.multiline ? 3 : undefined}
          slotProps={{
            htmlInput: { maxLength: question.maxLength ?? 500, 'aria-label': question.text },
          }}
          error={hasError}
        />
      )}
      {question.type === 'NUMBER' && (
        <TextField
          type="number"
          value={value?.number ?? ''}
          onChange={(event) =>
            onChange({ number: event.target.value === '' ? undefined : Number(event.target.value) })
          }
          slotProps={{
            htmlInput: {
              min: question.min,
              max: question.max,
              step: question.integerOnly ? 1 : 'any',
              'aria-label': question.text,
            },
          }}
          error={hasError}
          sx={{ maxWidth: 240 }}
        />
      )}
      {question.type === 'SCALE' && (
        <>
          <ToggleButtonGroup
            exclusive
            value={value?.number ?? null}
            onChange={(_, next: number | null) => {
              if (next !== null) onChange({ number: next })
            }}
            aria-label={question.text}
          >
            {scaleValues(question).map((n) => (
              <ToggleButton key={n} value={n} aria-label={String(n)}>
                {n}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
          {(question.minLabel || question.maxLabel) && (
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
              {question.minLabel ?? ''} · {question.maxLabel ?? ''}
            </Typography>
          )}
        </>
      )}
      {hint && !hasError && <FormHelperText id={hintId}>{hint}</FormHelperText>}
      {hasError && (
        <FormHelperText id={errorId} role="alert">
          {messages.join(' ')}
        </FormHelperText>
      )}
    </FormControl>
  )
}

function scaleValues(question: Question): number[] {
  const min = question.min ?? 1
  const max = question.max ?? min
  const values: number[] = []
  for (let n = min; n <= max; n += 1) values.push(n)
  return values
}

function hintFor(question: Question, intl: ReturnType<typeof useIntl>): string | null {
  switch (question.type) {
    case 'MULTIPLE_CHOICE': {
      const min = question.minSelections ?? 1
      const max = question.maxSelections ?? question.options?.length ?? 0
      return intl.formatMessage(
        { id: 'answer.hint.selections', defaultMessage: 'Choose between {min} and {max}.' },
        { min, max },
      )
    }
    case 'TEXT':
      return intl.formatMessage(
        { id: 'answer.hint.maxLength', defaultMessage: 'Up to {max} characters.' },
        { max: question.maxLength ?? 500 },
      )
    case 'NUMBER': {
      const parts: string[] = []
      if (question.min !== undefined && question.max !== undefined) {
        parts.push(
          intl.formatMessage(
            { id: 'answer.hint.range', defaultMessage: 'Between {min} and {max}.' },
            { min: question.min, max: question.max },
          ),
        )
      } else if (question.min !== undefined) {
        parts.push(
          intl.formatMessage(
            { id: 'answer.hint.min', defaultMessage: 'At least {min}.' },
            { min: question.min },
          ),
        )
      } else if (question.max !== undefined) {
        parts.push(
          intl.formatMessage(
            { id: 'answer.hint.max', defaultMessage: 'At most {max}.' },
            { max: question.max },
          ),
        )
      }
      if (question.integerOnly)
        parts.push(
          intl.formatMessage({ id: 'answer.hint.integer', defaultMessage: 'Whole numbers only.' }),
        )
      return parts.length > 0 ? parts.join(' ') : null
    }
    default:
      return null
  }
}
