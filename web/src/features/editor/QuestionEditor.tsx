import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward'
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward'
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined'
import {
  Button,
  Checkbox,
  FormControlLabel,
  FormHelperText,
  IconButton,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { FormattedMessage, useIntl } from 'react-intl'
import {
  QUESTION_TYPES,
  isChoice,
  type DraftAction,
  type DraftQuestion,
  type QuestionType,
  type QuestionPatch,
} from './draft'
import { draftErrorMessage } from './errors'
import { typeLabel } from './labels'
import type { DraftError } from './validation'

interface Props {
  question: DraftQuestion
  index: number
  count: number
  errorsAt: (path: string) => DraftError[]
  onEdit: (action: DraftAction) => void
}

/** One question of the draft with the fields its type has; every control is labelled for the keyboard. */
export function QuestionEditor({ question, index, count, errorsAt, onEdit }: Props) {
  const intl = useIntl()
  const number = index + 1
  const path = `questions[${index}]`
  const headingId = `${question.id}-heading`
  const helper = (field: string) => {
    const errors = errorsAt(`${path}.${field}`)
    return {
      error: errors.length > 0,
      helperText: errors.length > 0 ? draftErrorMessage(intl, errors[0]!) : undefined,
    }
  }
  const update = (patch: QuestionPatch) =>
    onEdit({ type: 'updateQuestion', id: question.id, patch })
  const numberField = (
    field: 'minSelections' | 'maxSelections' | 'maxLength' | 'min' | 'max',
    label: string,
  ) => (
    <TextField
      label={label}
      type="number"
      size="small"
      value={question[field] ?? ''}
      onChange={(event) =>
        update({ [field]: event.target.value === '' ? undefined : Number(event.target.value) })
      }
      slotProps={{ htmlInput: { step: 'any' } }}
      {...helper(field)}
    />
  )
  const optionsErrors = errorsAt(`${path}.options`)

  return (
    <Paper component="section" variant="outlined" aria-labelledby={headingId} sx={{ p: 2 }}>
      <Stack spacing={2}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Typography id={headingId} variant="h6" component="h2" sx={{ flexGrow: 1 }}>
            <FormattedMessage
              id="editor.question.heading"
              defaultMessage="Question {number}"
              values={{ number }}
            />
          </Typography>
          <IconButton
            size="small"
            disabled={index === 0}
            onClick={() => onEdit({ type: 'moveQuestion', id: question.id, direction: 'up' })}
            aria-label={intl.formatMessage(
              { id: 'editor.question.moveUp', defaultMessage: 'Move question {number} up' },
              { number },
            )}
          >
            <ArrowUpwardIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            disabled={index === count - 1}
            onClick={() => onEdit({ type: 'moveQuestion', id: question.id, direction: 'down' })}
            aria-label={intl.formatMessage(
              { id: 'editor.question.moveDown', defaultMessage: 'Move question {number} down' },
              { number },
            )}
          >
            <ArrowDownwardIcon fontSize="small" />
          </IconButton>
          <IconButton
            size="small"
            onClick={() => onEdit({ type: 'removeQuestion', id: question.id })}
            aria-label={intl.formatMessage(
              { id: 'editor.question.remove', defaultMessage: 'Remove question {number}' },
              { number },
            )}
          >
            <DeleteOutlinedIcon fontSize="small" />
          </IconButton>
        </Stack>
        <TextField
          label={intl.formatMessage({
            id: 'editor.question.text',
            defaultMessage: 'Question text',
          })}
          value={question.text}
          onChange={(event) => update({ text: event.target.value })}
          multiline
          required
          {...helper('text')}
        />
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: { sm: 'center' } }}
        >
          <TextField
            select
            label={intl.formatMessage({ id: 'editor.question.type', defaultMessage: 'Type' })}
            size="small"
            value={question.type}
            onChange={(event) =>
              onEdit({
                type: 'changeType',
                id: question.id,
                questionType: event.target.value as QuestionType,
              })
            }
            sx={{ minWidth: 200 }}
          >
            {QUESTION_TYPES.map((type) => (
              <MenuItem key={type} value={type}>
                {typeLabel(intl, type)}
              </MenuItem>
            ))}
          </TextField>
          <FormControlLabel
            control={
              <Checkbox
                checked={question.required}
                onChange={(event) => update({ required: event.target.checked })}
              />
            }
            label={intl.formatMessage({
              id: 'editor.question.required',
              defaultMessage: 'Required',
            })}
          />
        </Stack>
        {isChoice(question.type) && (
          <Stack
            spacing={1}
            role="group"
            aria-label={intl.formatMessage({ id: 'editor.options', defaultMessage: 'Options' })}
          >
            {(question.options ?? []).map((option, optionIndex) => {
              const optionNumber = optionIndex + 1
              return (
                <Stack
                  key={option.key}
                  direction="row"
                  spacing={1}
                  sx={{ alignItems: 'flex-start' }}
                >
                  <TextField
                    label={intl.formatMessage(
                      { id: 'editor.option.label', defaultMessage: 'Option {number}' },
                      { number: optionNumber },
                    )}
                    size="small"
                    value={option.label}
                    onChange={(event) =>
                      onEdit({
                        type: 'updateOption',
                        id: question.id,
                        optionKey: option.key,
                        label: event.target.value,
                      })
                    }
                    sx={{ flexGrow: 1 }}
                    {...helper(`options[${optionIndex}].label`)}
                  />
                  <IconButton
                    size="small"
                    disabled={optionIndex === 0}
                    onClick={() =>
                      onEdit({
                        type: 'moveOption',
                        id: question.id,
                        optionKey: option.key,
                        direction: 'up',
                      })
                    }
                    aria-label={intl.formatMessage(
                      { id: 'editor.option.moveUp', defaultMessage: 'Move option {number} up' },
                      { number: optionNumber },
                    )}
                  >
                    <ArrowUpwardIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    disabled={optionIndex === (question.options?.length ?? 0) - 1}
                    onClick={() =>
                      onEdit({
                        type: 'moveOption',
                        id: question.id,
                        optionKey: option.key,
                        direction: 'down',
                      })
                    }
                    aria-label={intl.formatMessage(
                      { id: 'editor.option.moveDown', defaultMessage: 'Move option {number} down' },
                      { number: optionNumber },
                    )}
                  >
                    <ArrowDownwardIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() =>
                      onEdit({ type: 'removeOption', id: question.id, optionKey: option.key })
                    }
                    aria-label={intl.formatMessage(
                      { id: 'editor.option.remove', defaultMessage: 'Remove option {number}' },
                      { number: optionNumber },
                    )}
                  >
                    <DeleteOutlinedIcon fontSize="small" />
                  </IconButton>
                </Stack>
              )
            })}
            {optionsErrors.length > 0 && (
              <FormHelperText error>{draftErrorMessage(intl, optionsErrors[0]!)}</FormHelperText>
            )}
            <Button
              size="small"
              onClick={() => onEdit({ type: 'addOption', id: question.id })}
              sx={{ alignSelf: 'flex-start' }}
            >
              <FormattedMessage id="editor.option.add" defaultMessage="Add an option" />
            </Button>
          </Stack>
        )}
        {question.type === 'MULTIPLE_CHOICE' && (
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            {numberField(
              'minSelections',
              intl.formatMessage({
                id: 'editor.question.minSelections',
                defaultMessage: 'Minimum selections',
              }),
            )}
            {numberField(
              'maxSelections',
              intl.formatMessage({
                id: 'editor.question.maxSelections',
                defaultMessage: 'Maximum selections',
              }),
            )}
          </Stack>
        )}
        {question.type === 'TEXT' && (
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ alignItems: { sm: 'center' } }}
          >
            {numberField(
              'maxLength',
              intl.formatMessage({
                id: 'editor.question.maxLength',
                defaultMessage: 'Maximum length',
              }),
            )}
            <FormControlLabel
              control={
                <Checkbox
                  checked={question.multiline ?? false}
                  onChange={(event) => update({ multiline: event.target.checked })}
                />
              }
              label={intl.formatMessage({
                id: 'editor.question.multiline',
                defaultMessage: 'Several lines',
              })}
            />
          </Stack>
        )}
        {question.type === 'NUMBER' && (
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            spacing={2}
            sx={{ alignItems: { sm: 'center' } }}
          >
            {numberField(
              'min',
              intl.formatMessage({ id: 'editor.question.min', defaultMessage: 'Minimum' }),
            )}
            {numberField(
              'max',
              intl.formatMessage({ id: 'editor.question.max', defaultMessage: 'Maximum' }),
            )}
            <FormControlLabel
              control={
                <Checkbox
                  checked={question.integerOnly ?? false}
                  onChange={(event) => update({ integerOnly: event.target.checked })}
                />
              }
              label={intl.formatMessage({
                id: 'editor.question.integerOnly',
                defaultMessage: 'Whole numbers only',
              })}
            />
          </Stack>
        )}
        {question.type === 'SCALE' && (
          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              {numberField(
                'min',
                intl.formatMessage({
                  id: 'editor.question.scaleMin',
                  defaultMessage: 'Lowest value (0 or 1)',
                }),
              )}
              {numberField(
                'max',
                intl.formatMessage({
                  id: 'editor.question.scaleMax',
                  defaultMessage: 'Highest value (3 to 10)',
                }),
              )}
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label={intl.formatMessage({
                  id: 'editor.question.minLabel',
                  defaultMessage: 'Label of the lowest value',
                })}
                size="small"
                value={question.minLabel ?? ''}
                onChange={(event) => update({ minLabel: event.target.value })}
                {...helper('minLabel')}
              />
              <TextField
                label={intl.formatMessage({
                  id: 'editor.question.maxLabel',
                  defaultMessage: 'Label of the highest value',
                })}
                size="small"
                value={question.maxLabel ?? ''}
                onChange={(event) => update({ maxLabel: event.target.value })}
                {...helper('maxLabel')}
              />
            </Stack>
          </Stack>
        )}
      </Stack>
    </Paper>
  )
}
