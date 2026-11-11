import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Schemas } from '../../api/types'

export type VersionResults = Schemas['VersionResults']
export type ComparableResults = Schemas['ComparableResults']
export type QuestionResult = Schemas['QuestionResult']
export type Scope = 'version' | 'comparable'

export interface Results {
  responseCount: number
  computedAt: string
  questions: QuestionResult[]
  versionNumbers: number[]
  /** The validator of this representation, sent back on the next poll of the same query. */
  etag: string | null
}

/**
 * Polls the results and sends back the `ETag` of the data cached for this very query, so the
 * server answers `304` and the data on screen stays until something changes. The validator lives
 * with the data of its key, never in a shared place: a change of version or scope starts without
 * one and can never take one representation for another.
 */
export function useResults(
  surveyId: string,
  versionNumber: number,
  scope: Scope,
  pollIntervalMs: number,
) {
  const queryClient = useQueryClient()
  const queryKey = ['results', surveyId, scope, versionNumber] as const
  return useQuery({
    queryKey,
    refetchInterval: pollIntervalMs,
    queryFn: async (): Promise<Results> => {
      const previous = queryClient.getQueryData<Results>(queryKey)
      const headers = previous?.etag ? { 'If-None-Match': previous.etag } : undefined
      const { data, response } =
        scope === 'version'
          ? await api.GET('/surveys/{surveyId}/versions/{versionNumber}/results', {
              params: { path: { surveyId, versionNumber } },
              headers,
            })
          : await api.GET('/surveys/{surveyId}/results', {
              params: { path: { surveyId }, query: { scope: 'comparable' } },
              headers,
            })
      if (response.status === 304 && previous) return previous
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return {
        responseCount: data.responseCount,
        computedAt: data.computedAt,
        questions: data.questions,
        versionNumbers: 'versionNumbers' in data ? data.versionNumbers : [versionNumber],
        etag: response.headers.get('ETag'),
      }
    },
  })
}
