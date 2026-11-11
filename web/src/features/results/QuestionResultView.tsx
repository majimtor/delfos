import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  LinearProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from '@mui/material'
import { useInfiniteQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { FormattedDate, FormattedMessage, FormattedNumber } from 'react-intl'
import { api } from '../../api/client'
import type { QuestionResult } from './useResults'

interface Props {
  result: QuestionResult
  surveyId: string
  versionNumber: number
  language: string
  showVersions: boolean
}

export function QuestionResultView({
  result,
  surveyId,
  versionNumber,
  language,
  showVersions,
}: Props) {
  const comparable = result.comparable !== false
  return (
    <Card variant="outlined" component="section" aria-labelledby={`q-${result.key}`}>
      <CardContent>
        <Stack
          direction="row"
          spacing={1}
          sx={{ alignItems: 'baseline', flexWrap: 'wrap', mb: 1 }}
          useFlexGap
        >
          <Typography
            variant="h6"
            component="h2"
            id={`q-${result.key}`}
            lang={language}
            sx={{ flexGrow: 1 }}
          >
            {result.text}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <FormattedMessage
              id="results.answerCount"
              defaultMessage="{count, plural, one {# answer} other {# answers}}"
              values={{ count: result.answerCount }}
            />
          </Typography>
          {showVersions && result.versionNumbers && (
            <Chip
              size="small"
              label={
                <FormattedMessage
                  id="results.versions"
                  defaultMessage="Versions {versions}"
                  values={{ versions: result.versionNumbers.join(', ') }}
                />
              }
            />
          )}
        </Stack>
        {!comparable && (
          <Typography variant="body2" color="text.secondary">
            <FormattedMessage
              id="results.notComparable"
              defaultMessage="This question changed between versions, so its answers are not aggregated."
            />
          </Typography>
        )}
        {comparable && result.options && (
          <OptionBars options={result.options} total={result.answerCount} />
        )}
        {comparable && result.number && <NumberStats stats={result.number} />}
        {comparable && result.distribution && (
          <Distribution distribution={result.distribution} total={result.answerCount} />
        )}
        {comparable && result.type === 'TEXT' && result.answerCount > 0 && (
          <TextAnswers
            surveyId={surveyId}
            versionNumber={versionNumber}
            questionKey={result.key}
            language={language}
          />
        )}
      </CardContent>
    </Card>
  )
}

function OptionBars({
  options,
  total,
}: {
  options: NonNullable<QuestionResult['options']>
  total: number
}) {
  return (
    <Stack spacing={1} sx={{ mt: 1 }}>
      {options.map((option) => {
        const share = total > 0 ? option.count / total : 0
        return (
          <Box key={option.key}>
            <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
              <Typography variant="body2">{option.label}</Typography>
              <Typography variant="body2" color="text.secondary">
                {option.count} ·{' '}
                <FormattedNumber value={share} style="percent" maximumFractionDigits={0} />
              </Typography>
            </Stack>
            <LinearProgress variant="determinate" value={share * 100} aria-label={option.label} />
          </Box>
        )
      })}
    </Stack>
  )
}

function NumberStats({ stats }: { stats: NonNullable<QuestionResult['number']> }) {
  return (
    <Table size="small" sx={{ maxWidth: 360, mt: 1 }}>
      <TableBody>
        <TableRow>
          <TableCell>
            <FormattedMessage id="results.mean" defaultMessage="Mean" />
          </TableCell>
          <TableCell align="right">
            <FormattedNumber value={stats.mean} maximumFractionDigits={2} />
          </TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            <FormattedMessage id="results.min" defaultMessage="Minimum" />
          </TableCell>
          <TableCell align="right">
            <FormattedNumber value={stats.min} maximumFractionDigits={2} />
          </TableCell>
        </TableRow>
        <TableRow>
          <TableCell>
            <FormattedMessage id="results.max" defaultMessage="Maximum" />
          </TableCell>
          <TableCell align="right">
            <FormattedNumber value={stats.max} maximumFractionDigits={2} />
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  )
}

function Distribution({
  distribution,
  total,
}: {
  distribution: NonNullable<QuestionResult['distribution']>
  total: number
}) {
  return (
    <Stack spacing={0.5} sx={{ mt: 1 }}>
      {distribution.map((entry) => {
        const share = total > 0 ? entry.count / total : 0
        return (
          <Stack key={entry.value} direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <Typography variant="body2" sx={{ width: 24, textAlign: 'right' }}>
              {entry.value}
            </Typography>
            <LinearProgress
              variant="determinate"
              value={share * 100}
              sx={{ flexGrow: 1 }}
              aria-label={String(entry.value)}
            />
            <Typography variant="body2" color="text.secondary" sx={{ width: 32 }}>
              {entry.count}
            </Typography>
          </Stack>
        )
      })}
    </Stack>
  )
}

function TextAnswers({
  surveyId,
  versionNumber,
  questionKey,
  language,
}: {
  surveyId: string
  versionNumber: number
  questionKey: string
  language: string
}) {
  const [open, setOpen] = useState(false)
  const answers = useInfiniteQuery({
    queryKey: ['answers', surveyId, versionNumber, questionKey],
    enabled: open,
    initialPageParam: undefined as string | undefined,
    queryFn: async ({ pageParam }) => {
      const { data, response } = await api.GET(
        '/surveys/{surveyId}/versions/{versionNumber}/answers',
        {
          params: {
            path: { surveyId, versionNumber },
            query: { questionKey, cursor: pageParam, limit: 20 },
          },
        },
      )
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    getNextPageParam: (last) => last.nextCursor,
  })
  if (!open) {
    return (
      <Button size="small" onClick={() => setOpen(true)} sx={{ mt: 1 }}>
        <FormattedMessage id="results.showAnswers" defaultMessage="Show the answers" />
      </Button>
    )
  }
  const items = answers.data?.pages.flatMap((page) => page.items) ?? []
  return (
    <Stack
      spacing={1}
      sx={{ mt: 1 }}
      component="ul"
      aria-label={questionKey}
      style={{ listStyle: 'none', paddingLeft: 0 }}
    >
      {items.map((item) => (
        <Typography key={item.responseId} component="li" variant="body2" lang={language}>
          {item.answer.text}{' '}
          <Typography component="span" variant="caption" color="text.secondary">
            <FormattedDate value={item.submittedAt} dateStyle="short" timeStyle="short" />
          </Typography>
        </Typography>
      ))}
      {answers.hasNextPage && (
        <Button
          size="small"
          onClick={() => void answers.fetchNextPage()}
          disabled={answers.isFetchingNextPage}
          sx={{ alignSelf: 'flex-start' }}
        >
          <FormattedMessage id="surveys.loadMore" defaultMessage="Load more" />
        </Button>
      )}
    </Stack>
  )
}
