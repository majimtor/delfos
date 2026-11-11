import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { Problem, Schemas } from '../../api/types'

export type Me = Schemas['Me']

export const ME_QUERY_KEY = ['me'] as const

/** The manager behind the session cookie, or null when there is none. */
export function useSession() {
  return useQuery({
    queryKey: ME_QUERY_KEY,
    queryFn: async (): Promise<Me | null> => {
      const { data, response } = await api.GET('/auth/me')
      if (response.status === 401) return null
      if (!data) throw new Error(`unexpected status ${response.status}`)
      return data
    },
    staleTime: 60_000,
  })
}

export class LoginError extends Error {
  readonly code: Problem['code'] | 'UNREACHABLE'

  constructor(code: Problem['code'] | 'UNREACHABLE') {
    super(code)
    this.code = code
  }
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (credentials: { username: string; password: string }) => {
      const { data, error, response } = await api.POST('/auth/sessions', {
        body: { ...credentials, transport: 'COOKIE' },
      })
      if (data) return data
      throw new LoginError(
        error?.code ?? (response.status === 0 ? 'UNREACHABLE' : 'INTERNAL_ERROR'),
      )
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ME_QUERY_KEY }),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      await api.DELETE('/auth/sessions/current')
    },
    onSettled: () => {
      queryClient.setQueryData(ME_QUERY_KEY, null)
      return queryClient.invalidateQueries()
    },
  })
}
