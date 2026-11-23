import { Box, Button, Card, CardContent, Chip, Stack, Typography } from '@mui/material'
import { useInfiniteQuery } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
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
          <Typography variant="body2" color="textSecondary">
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
          <Typography variant="body2" color="textSecondary">
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
        {comparable && result.distribution && <Distribution distribution={result.distribution} />}
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

/** The share of the answers as a horizontal bar; the figures next to it carry the data, the bar repeats them. */
function Bar({ share }: { share: number }) {
  return (
    <Box
      aria-hidden="true"
      sx={{
        mt: 0.5,
        height: 8,
        borderRadius: 999,
        overflow: 'hidden',
        bgcolor: 'rgba(var(--delfos-palette-primary-mainChannel) / 0.15)',
      }}
    >
      <Box
        sx={{
          width: `${share * 100}%`,
          height: '100%',
          borderRadius: 999,
          bgcolor: 'primary.main',
        }}
      />
    </Box>
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
    <Stack spacing={1.25} sx={{ mt: 1.5 }}>
      {options.map((option) => {
        const share = total > 0 ? option.count / total : 0
        return (
          <Box key={option.key}>
            <Stack
              direction="row"
              sx={{ justifyContent: 'space-between', alignItems: 'baseline', gap: 1 }}
            >
              <Typography variant="body2" sx={{ minWidth: 0 }}>
                {option.label}
              </Typography>
              <Typography
                variant="body2"
                color="textSecondary"
                sx={{ fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}
              >
                {option.count} ·{' '}
                <FormattedNumber value={share} style="percent" maximumFractionDigits={0} />
              </Typography>
            </Stack>
            <Bar share={share} />
          </Box>
        )
      })}
    </Stack>
  )
}

/** One figure with its label above, as a tile; three of them describe a numeric question. */
function Stat({ label, value }: { label: ReactNode; value: ReactNode }) {
  return (
    <Box>
      <Typography variant="caption" component="div" color="textSecondary">
        {label}
      </Typography>
      <Typography variant="h6" component="div" sx={{ fontVariantNumeric: 'tabular-nums' }}>
        {value}
      </Typography>
    </Box>
  )
}

function NumberStats({ stats }: { stats: NonNullable<QuestionResult['number']> }) {
  return (
    <Box
      sx={{
        mt: 1.5,
        display: 'grid',
        gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
        gap: 2,
        maxWidth: 420,
      }}
    >
      <Stat
        label={<FormattedMessage id="results.mean" defaultMessage="Mean" />}
        value={<FormattedNumber value={stats.mean} maximumFractionDigits={2} />}
      />
      <Stat
        label={<FormattedMessage id="results.min" defaultMessage="Minimum" />}
        value={<FormattedNumber value={stats.min} maximumFractionDigits={2} />}
      />
      <Stat
        label={<FormattedMessage id="results.max" defaultMessage="Maximum" />}
        value={<FormattedNumber value={stats.max} maximumFractionDigits={2} />}
      />
    </Box>
  )
}

/**
 * The scale as a histogram: one column per value, scaled to the most chosen one, with the count
 * above and the value below. The picture is named by its figures, which is what assistive
 * technology reads; the labels inside it are decoration.
 */
function Distribution({
  distribution,
}: {
  distribution: NonNullable<QuestionResult['distribution']>
}) {
  const max = Math.max(1, ...distribution.map((entry) => entry.count))
  const summary = distribution.map((entry) => `${entry.value}: ${entry.count}`).join(', ')
  return (
    <Box
      role="img"
      aria-label={summary}
      sx={{
        mt: 2,
        display: 'grid',
        gridTemplateColumns: `repeat(${distribution.length}, minmax(0, 1fr))`,
        gap: 1,
        height: 150,
      }}
    >
      {distribution.map((entry) => (
        <Box
          key={entry.value}
          aria-hidden="true"
          sx={{
            display: 'grid',
            gridTemplateRows: 'auto 1fr auto',
            justifyItems: 'center',
            alignItems: 'end',
            gap: 0.5,
            minWidth: 0,
          }}
        >
          <Typography
            variant="caption"
            color="textSecondary"
            sx={{ fontVariantNumeric: 'tabular-nums' }}
          >
            {entry.count}
          </Typography>
          <Box
            sx={{
              width: '100%',
              maxWidth: 48,
              height: `${entry.count > 0 ? Math.max((entry.count / max) * 100, 4) : 2}%`,
              borderRadius: '4px 4px 0 0',
              bgcolor:
                entry.count > 0
                  ? 'primary.main'
                  : 'rgba(var(--delfos-palette-primary-mainChannel) / 0.15)',
            }}
          />
          <Typography variant="body2">{entry.value}</Typography>
        </Box>
      ))}
    </Box>
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
          <Typography component="span" variant="caption" color="textSecondary">
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
