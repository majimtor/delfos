import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { apiBaseUrl } from '../../api/client'

/**
 * Listens to the results events of a survey over an `EventSource` and drops the cached results
 * whenever one arrives, so the page reads them again at once. The browser reconnects by itself
 * when the stream ends or fails; while it is down, and wherever `EventSource` does not exist, the
 * page falls back to its polling. Returns whether the stream is open.
 */
export function useResultsEvents(surveyId: string): boolean {
  const queryClient = useQueryClient()
  const [live, setLive] = useState(false)

  useEffect(() => {
    if (!surveyId || typeof EventSource === 'undefined') return undefined
    const source = new EventSource(`${apiBaseUrl}/api/v1/surveys/${surveyId}/results/events`, {
      withCredentials: true,
    })
    source.onopen = () => setLive(true)
    source.onerror = () => setLive(false)
    source.addEventListener('results', () => {
      void queryClient.invalidateQueries({ queryKey: ['results', surveyId] })
    })
    return () => {
      source.close()
      setLive(false)
    }
  }, [surveyId, queryClient])

  return live
}
