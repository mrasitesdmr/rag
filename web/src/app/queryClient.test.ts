import { describe, expect, it } from 'vitest'
import { ApiError } from '../shared/api/ApiError.ts'
import { createQueryClient, shouldRetryQuery } from './queryClient.ts'

describe('shouldRetryQuery', () => {
  it.each([400, 404, 409])('given4xx_whenShouldRetryQuery_thenNoRetry %i', (status) => {
    expect(shouldRetryQuery(0, new ApiError(status, 'X'))).toBe(false)
  })

  it('given5xxOrNetwork_whenShouldRetryQuery_thenRetriesExactlyOnce', () => {
    expect(shouldRetryQuery(0, new ApiError(500, 'INTERNAL_ERROR'))).toBe(true)
    expect(shouldRetryQuery(0, new ApiError(0, 'NETWORK_ERROR'))).toBe(true)
    expect(shouldRetryQuery(1, new ApiError(500, 'INTERNAL_ERROR'))).toBe(false)
  })

  it('givenCreatedClient_whenMutationDefaults_thenMutationsAreNeverRetried', () => {
    expect(createQueryClient().getDefaultOptions().mutations?.retry).toBe(false)
  })
})
