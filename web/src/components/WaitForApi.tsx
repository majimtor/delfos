import { Box, Button, CircularProgress, Container, Typography } from '@mui/material'
import { useEffect, useState, type ReactNode } from 'react'
import { FormattedMessage } from 'react-intl'
import { probeHealth, type HealthProbe } from '../api/health'

type Status = 'probing' | 'ready' | 'failed'

interface Props {
  children: ReactNode
  probe?: HealthProbe
  /** Seconds between attempts and overall budget; small in tests. */
  intervalMs?: number
  budgetMs?: number
}

/**
 * Shows an honest waiting screen until the API answers. The demo sleeps after fifteen minutes
 * without traffic and takes up to a minute to wake, which this screen explains instead of
 * letting the first request fail silently.
 */
export function WaitForApi({
  children,
  probe = probeHealth,
  intervalMs = 3_000,
  budgetMs = 120_000,
}: Props) {
  const [status, setStatus] = useState<Status>('probing')
  const [startedAt, setStartedAt] = useState(() => Date.now())
  const [elapsed, setElapsed] = useState(0)

  useEffect(() => {
    if (status !== 'probing') return
    let cancelled = false
    const attempt = async () => {
      const ok = await probe()
      if (cancelled) return
      if (ok) {
        setStatus('ready')
        return
      }
      const waited = Date.now() - startedAt
      setElapsed(Math.round(waited / 1000))
      if (waited >= budgetMs) {
        setStatus('failed')
        return
      }
      timer = setTimeout(attempt, intervalMs)
    }
    let timer: ReturnType<typeof setTimeout> | undefined
    void attempt()
    return () => {
      cancelled = true
      if (timer !== undefined) clearTimeout(timer)
    }
  }, [status, startedAt, probe, intervalMs, budgetMs])

  if (status === 'ready') return <>{children}</>

  return (
    <Container maxWidth="sm" sx={{ py: 8, textAlign: 'center' }} role="status" aria-live="polite">
      <Typography variant="h5" component="h1" gutterBottom>
        {status === 'failed' ? (
          <FormattedMessage id="wait.failed" defaultMessage="We could not reach the service." />
        ) : (
          <FormattedMessage id="wait.title" defaultMessage="The service is waking up" />
        )}
      </Typography>
      {status === 'probing' && (
        <>
          <Box sx={{ my: 3 }}>
            <CircularProgress aria-hidden />
          </Box>
          <Typography>
            <FormattedMessage
              id="wait.body"
              defaultMessage="The demo sleeps when nobody uses it and takes up to a minute to start. Please wait."
            />
          </Typography>
          {elapsed > 0 && (
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              <FormattedMessage
                id="wait.elapsed"
                defaultMessage="Waiting for {seconds} seconds."
                values={{ seconds: elapsed }}
              />
            </Typography>
          )}
        </>
      )}
      {status === 'failed' && (
        <Button
          variant="contained"
          sx={{ mt: 2 }}
          onClick={() => {
            setStartedAt(Date.now())
            setElapsed(0)
            setStatus('probing')
          }}
        >
          <FormattedMessage id="wait.retry" defaultMessage="Retry" />
        </Button>
      )}
    </Container>
  )
}
