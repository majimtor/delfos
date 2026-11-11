import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useRef } from 'react'
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
}

/**
 * Polls the results and sends the last `ETag` back, so the server answers `304` and the data on
 * screen stays until something changes.
 */
export function useResults(
  surveyId: string,
  versionNumber: number,
  scope: Scope,
  pollIntervalMs: number,
) {
  const queryClient = useQueryClient()
  const etagRef = useRef<string | null>(null)
  const queryKey = ['results', surveyId, scope, versionNumber] as const
  return useQuery({
    queryKey,
    refetchInterval: pollIntervalMs,
    queryFn: async (): Promise<Results> => {
      const headers = etagRef.current ? { 'If-None-Match': etagRef.current } : undefined
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
      if (response.status === 304) {
        const previous = queryClient.getQueryData<Results>(queryKey)
        if (previous) return previous
      }
      if (!data) throw new Error(`unexpected status ${response.status}`)
      etagRef.current = response.headers.get('ETag')
      return {
        responseCount: data.responseCount,
        computedAt: data.computedAt,
        questions: data.questions,
        versionNumbers: 'versionNumbers' in data ? data.versionNumbers : [versionNumber],
      }
    },
  })
}
