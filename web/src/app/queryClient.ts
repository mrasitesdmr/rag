import { QueryClient } from '@tanstack/react-query'
import { isClientError } from '../shared/api/ApiError.ts'

const QUERY_RETRY_COUNT = 1

export function shouldRetryQuery(failureCount: number, error: Error): boolean {
  if (isClientError(error)) {
    return false
  }
  return failureCount < QUERY_RETRY_COUNT
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: shouldRetryQuery },
      mutations: { retry: false },
    },
  })
}
