import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined'
import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  FormGroup,
  FormHelperText,
  FormLabel,
  IconButton,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { FormattedMessage, useIntl, type IntlShape } from 'react-intl'
import type { DraftAction, DraftQuestion, VisibilityRule } from './draft'
import { draftErrorMessage } from './errors'
import {
  operatorsFor,
  shapeOf,
  sourcesFor,
  type Condition,
  type ConditionOperator,
  type ConditionValue,
  type MatchMode,
} from './rules'
import type { DraftError } from './validation'

interface Props {
  question: DraftQuestion
  questions: DraftQuestion[]
  rules: VisibilityRule[]
  errorsAt: (path: string, prefix?: boolean) => DraftError[]
  onEdit: (action: DraftAction) => void
}

function operatorLabel(intl: IntlShape, operator: ConditionOperator): string {
  switch (operator) {
    case 'EQUALS':
      return intl.formatMessage({ id: 'rules.operator.EQUALS', defaultMessage: 'is' })
    case 'NOT_EQUALS':
      return intl.formatMessage({ id: 'rules.operator.NOT_EQUALS', defaultMessage: 'is not' })
    case 'IN':
      return intl.formatMessage({ id: 'rules.operator.IN', defaultMessage: 'is one of' })
    case 'INCLUDES_ANY':
      return intl.formatMessage({
        id: 'rules.operator.INCLUDES_ANY',
        defaultMessage: 'includes any of',
      })
    case 'INCLUDES_ALL':
      return intl.formatMessage({
        id: 'rules.operator.INCLUDES_ALL',
        defaultMessage: 'includes all of',
      })
    case 'EXCLUDES':
      return intl.formatMessage({
        id: 'rules.operator.EXCLUDES',
        defaultMessage: 'includes none of',
      })
    case 'EQ':
      return intl.formatMessage({ id: 'rules.operator.EQ', defaultMessage: 'equals' })
    case 'NE':
      return intl.formatMessage({ id: 'rules.operator.NE', defaultMessage: 'does not equal' })
    case 'GT':
      return intl.formatMessage({ id: 'rules.operator.GT', defaultMessage: 'is greater than' })
    case 'GTE':
      return intl.formatMessage({ id: 'rules.operator.GTE', defaultMessage: 'is at least' })
    case 'LT':
      return intl.formatMessage({ id: 'rules.operator.LT', defaultMessage: 'is less than' })
    case 'LTE':
      return intl.formatMessage({ id: 'rules.operator.LTE', defaultMessage: 'is at most' })
    case 'BETWEEN':
      return intl.formatMessage({ id: 'rules.operator.BETWEEN', defaultMessage: 'is between' })
    case 'IS_ANSWERED':
      return intl.formatMessage({ id: 'rules.operator.IS_ANSWERED', defaultMessage: 'is answered' })
    case 'IS_NOT_ANSWERED':
      return intl.formatMessage({
        id: 'rules.operator.IS_NOT_ANSWERED',
        defaultMessage: 'is not answered',
      })
  }
}

/** The visibility rule of one question: shown always, or only when conditions on earlier questions hold. */
export function RuleEditor({ question, questions, rules, errorsAt, onEdit }: Props) {
  const intl = useIntl()
  const sources = sourcesFor(questions, question.id)
  const ruleIndex = rules.findIndex((rule) => rule.targetQuestionKey === question.key)
  const rule = ruleIndex >= 0 ? rules[ruleIndex] : undefined
  const rulePath = `rules[${ruleIndex}]`
  const id = question.id
  const targetErrors = errorsAt(`${rulePath}.targetQuestionKey`)
  const conditionsErrors = errorsAt(`${rulePath}.conditions`)

  return (
    <Box component="fieldset" sx={{ border: 0, p: 0, m: 0, minWidth: 0 }}>
      <Typography component="legend" variant="subtitle2" sx={{ mb: 1 }}>
        <FormattedMessage id="rules.legend" defaultMessage="Visibility" />
      </Typography>
      {sources.length === 0 ? (
        <Typography color="text.secondary" variant="body2">
          <FormattedMessage
            id="rules.always"
            defaultMessage="Always shown: there is no earlier question to depend on."
          />
        </Typography>
      ) : (
        <Stack spacing={2}>
          <FormControlLabel
            control={
              <Checkbox
                checked={Boolean(rule)}
                onChange={(event) =>
                  onEdit(
                    event.target.checked ? { type: 'addRule', id } : { type: 'removeRule', id },
                  )
                }
              />
            }
            label={intl.formatMessage({
              id: 'rules.enable',
              defaultMessage: 'Shown only when conditions on earlier questions hold',
            })}
          />
          {rule && (
            <Stack spacing={2} sx={{ pl: { sm: 4 } }}>
              {targetErrors.length > 0 && (
                <FormHelperText error>{draftErrorMessage(intl, targetErrors[0]!)}</FormHelperText>
              )}
              <TextField
                select
                size="small"
                label={intl.formatMessage({
                  id: 'rules.match',
                  defaultMessage: 'Conditions that must hold',
                })}
                value={rule.match}
                onChange={(event) =>
                  onEdit({ type: 'setRuleMatch', id, match: event.target.value as MatchMode })
                }
                sx={{ maxWidth: 280 }}
              >
                <MenuItem value="ALL">
                  <FormattedMessage id="rules.match.ALL" defaultMessage="all of them" />
                </MenuItem>
                <MenuItem value="ANY">
                  <FormattedMessage id="rules.match.ANY" defaultMessage="any of them" />
                </MenuItem>
              </TextField>
              {rule.conditions.map((condition, index) => (
                <ConditionRow
                  key={index}
                  condition={condition}
                  index={index}
                  path={`${rulePath}.conditions[${index}]`}
                  sources={sources}
                  questions={questions}
                  errorsAt={errorsAt}
                  onEdit={onEdit}
                  targetId={id}
                />
              ))}
              {conditionsErrors.length > 0 && (
                <FormHelperText error>
                  {draftErrorMessage(intl, conditionsErrors[0]!)}
                </FormHelperText>
              )}
              <Button
                size="small"
                onClick={() => onEdit({ type: 'addCondition', id })}
                sx={{ alignSelf: 'flex-start' }}
              >
                <FormattedMessage id="rules.addCondition" defaultMessage="Add a condition" />
              </Button>
            </Stack>
          )}
        </Stack>
      )}
    </Box>
  )
}

interface ConditionProps {
  condition: Condition
  index: number
  path: string
  sources: DraftQuestion[]
  questions: DraftQuestion[]
  targetId: string
  errorsAt: (path: string, prefix?: boolean) => DraftError[]
  onEdit: (action: DraftAction) => void
}

function ConditionRow({
  condition,
  index,
  path,
  sources,
  questions,
  targetId,
  errorsAt,
  onEdit,
}: ConditionProps) {
  const intl = useIntl()
  const number = index + 1
  const source = questions.find((q) => q.key === condition.sourceQuestionKey)
  // A source that moved after the target is kept in the list, so the choice stays visible with its error.
  const sourceChoices = source && !sources.includes(source) ? [...sources, source] : sources
  const operators = source ? operatorsFor(source.type) : []
  const operatorChoices = operators.includes(condition.operator)
    ? operators
    : [...operators, condition.operator]
  const helper = (field: string, prefix = false) => {
    const errors = errorsAt(`${path}.${field}`, prefix)
    return {
      error: errors.length > 0,
      helperText: errors.length > 0 ? draftErrorMessage(intl, errors[0]!) : undefined,
    }
  }
  const setValue = (value: ConditionValue | undefined) =>
    onEdit({ type: 'setConditionValue', id: targetId, index, value })
  const questionLabel = (q: DraftQuestion) =>
    `${questions.indexOf(q) + 1}. ${q.text.trim() || intl.formatMessage({ id: 'rules.untitled', defaultMessage: '(untitled)' })}`
  const conditionLabel = intl.formatMessage(
    { id: 'rules.condition', defaultMessage: 'Condition {number}' },
    { number },
  )

  return (
    <Stack
      role="group"
      aria-label={conditionLabel}
      direction={{ xs: 'column', md: 'row' }}
      spacing={1}
      useFlexGap
      sx={{ flexWrap: 'wrap', alignItems: { md: 'flex-start' } }}
    >
      <TextField
        select
        size="small"
        label={intl.formatMessage({ id: 'rules.source', defaultMessage: 'Depends on question' })}
        value={condition.sourceQuestionKey}
        onChange={(event) =>
          onEdit({ type: 'setConditionSource', id: targetId, index, sourceKey: event.target.value })
        }
        sx={{ minWidth: 220, flexGrow: 1 }}
        {...helper('sourceQuestionKey')}
      >
        {sourceChoices.map((q) => (
          <MenuItem key={q.key} value={q.key}>
            {questionLabel(q)}
          </MenuItem>
        ))}
      </TextField>
      <TextField
        select
        size="small"
        label={intl.formatMessage({ id: 'rules.operator', defaultMessage: 'Operator' })}
        value={condition.operator}
        onChange={(event) =>
          onEdit({
            type: 'setConditionOperator',
            id: targetId,
            index,
            operator: event.target.value as ConditionOperator,
          })
        }
        sx={{ minWidth: 180 }}
        {...helper('operator')}
      >
        {operatorChoices.map((operator) => (
          <MenuItem key={operator} value={operator}>
            {operatorLabel(intl, operator)}
          </MenuItem>
        ))}
      </TextField>
      <ValueControl
        condition={condition}
        source={source}
        setValue={setValue}
        {...helper('value', true)}
      />
      <IconButton
        size="small"
        onClick={() => onEdit({ type: 'removeCondition', id: targetId, index })}
        aria-label={intl.formatMessage(
          { id: 'rules.removeCondition', defaultMessage: 'Remove condition {number}' },
          { number },
        )}
        sx={{ alignSelf: { md: 'center' } }}
      >
        <DeleteOutlinedIcon fontSize="small" />
      </IconButton>
    </Stack>
  )
}

interface ValueProps {
  condition: Condition
  source: DraftQuestion | undefined
  setValue: (value: ConditionValue | undefined) => void
  error: boolean
  helperText: string | undefined
}

function ValueControl({ condition, source, setValue, error, helperText }: ValueProps) {
  const intl = useIntl()
  const value = condition.value
  const options = source?.options ?? []
  const optionText = (key: string) => options.find((o) => o.key === key)?.label || key
  const numberField = (label: string, field: 'number' | 'min' | 'max') => (
    <TextField
      label={label}
      type="number"
      size="small"
      value={value?.[field] ?? ''}
      onChange={(event) =>
        setValue({
          ...value,
          [field]: event.target.value === '' ? undefined : Number(event.target.value),
        })
      }
      slotProps={{ htmlInput: { step: 'any' } }}
      error={error}
      helperText={field !== 'max' ? helperText : undefined}
      sx={{ width: 140 }}
    />
  )
  switch (shapeOf(condition.operator)) {
    case 'none':
      return null
    case 'optionKey':
      return (
        <TextField
          select
          size="small"
          label={intl.formatMessage({ id: 'rules.option', defaultMessage: 'Option' })}
          value={value?.optionKey ?? ''}
          onChange={(event) => setValue({ optionKey: event.target.value })}
          error={error}
          helperText={helperText}
          sx={{ minWidth: 180 }}
        >
          {options.map((option) => (
            <MenuItem key={option.key} value={option.key}>
              {optionText(option.key)}
            </MenuItem>
          ))}
        </TextField>
      )
    case 'optionKeys': {
      const chosen = value?.optionKeys ?? []
      return (
        <FormGroup
          role="group"
          aria-label={intl.formatMessage({ id: 'rules.options', defaultMessage: 'Options' })}
        >
          <FormLabel component="span" sx={{ fontSize: '0.75rem' }} error={error}>
            <FormattedMessage id="rules.options" defaultMessage="Options" />
          </FormLabel>
          {options.map((option) => (
            <FormControlLabel
              key={option.key}
              control={
                <Checkbox
                  size="small"
                  checked={chosen.includes(option.key)}
                  onChange={(event) =>
                    setValue({
                      optionKeys: event.target.checked
                        ? [...chosen, option.key]
                        : chosen.filter((key) => key !== option.key),
                    })
                  }
                />
              }
              label={optionText(option.key)}
            />
          ))}
          {helperText && <FormHelperText error={error}>{helperText}</FormHelperText>}
        </FormGroup>
      )
    }
    case 'number':
      return numberField(
        intl.formatMessage({ id: 'rules.value', defaultMessage: 'Value' }),
        'number',
      )
    case 'range':
      return (
        <>
          {numberField(intl.formatMessage({ id: 'rules.from', defaultMessage: 'From' }), 'min')}
          {numberField(intl.formatMessage({ id: 'rules.to', defaultMessage: 'To' }), 'max')}
        </>
      )
  }
}
