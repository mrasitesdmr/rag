import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../../test/server.ts'
import { problem } from '../../test/server.ts'
import { ApiError, errorCodeOf, isApiErrorWithCode, isClientError } from './ApiError.ts'
import { request, withQuery } from './httpClient.ts'

async function failureOf(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise
  } catch (error) {
    if (error instanceof ApiError) {
      return error
    }
  }
  throw new Error('expected ApiError')
}

describe('withQuery', () => {
  it.each([
    [{ page: 0, size: 12 }, '/x?page=0&size=12'],
    [{ query: 'asgari ödeme', page: 1 }, '/x?query=asgari+%C3%B6deme&page=1'],
    [{ query: undefined, page: 2 }, '/x?page=2'],
    [{ query: null }, '/x'],
    [{}, '/x'],
  ])('givenParams_whenWithQuery_thenSkipsMissingAndEncodes %#', (params, expected) => {
    expect(withQuery('/x', params)).toBe(expected)
  })
})

describe('request', () => {
  it('givenProblemJson_whenRequest_thenThrowsApiErrorWithStatusCodeAndViolations', async () => {
    server.use(
      http.get('/api/x', () => problem(400, 'VALIDATION_FAILED', [{ field: 'version', message: 'must not be blank' }])),
    )

    const error = await failureOf(request('/api/x'))

    expect(error.status).toBe(400)
    expect(error.errorCode).toBe('VALIDATION_FAILED')
    expect(error.violations).toEqual([{ field: 'version', message: 'must not be blank' }])
  })

  it('givenMalformedViolations_whenRequest_thenKeepsOnlyWellFormedOnes', async () => {
    server.use(
      http.get('/api/x', () =>
        HttpResponse.json(
          { errorCode: 'VALIDATION_FAILED', violations: [{ field: 'a', message: 'ok' }, { field: 1 }, 'x', null] },
          { status: 400 },
        ),
      ),
    )

    const error = await failureOf(request('/api/x'))

    expect(error.violations).toEqual([{ field: 'a', message: 'ok' }])
  })

  it.each([
    ['html body', () => new HttpResponse('<html>502</html>', { status: 502, headers: { 'Content-Type': 'text/html' } })],
    ['json without errorCode', () => HttpResponse.json({ title: 'x' }, { status: 500 })],
    ['empty errorCode', () => HttpResponse.json({ errorCode: '' }, { status: 500 })],
    ['non-string errorCode', () => HttpResponse.json({ errorCode: 5 }, { status: 500 })],
  ])('givenErrorWithoutProblemShape_whenRequest_thenUnexpectedError %s', async (_name, respond) => {
    server.use(http.get('/api/x', respond))

    const error = await failureOf(request('/api/x'))

    expect(error.errorCode).toBe('UNEXPECTED_ERROR')
  })

  it('givenNetworkFailure_whenRequest_thenNetworkError', async () => {
    server.use(http.get('/api/x', () => HttpResponse.error()))

    const error = await failureOf(request('/api/x'))

    expect(error.errorCode).toBe('NETWORK_ERROR')
    expect(error.status).toBe(0)
  })

  it('givenOkWithInvalidJson_whenRequest_thenUnexpectedError', async () => {
    server.use(http.get('/api/x', () => new HttpResponse('not json', { status: 200 })))

    const error = await failureOf(request('/api/x'))

    expect(error.errorCode).toBe('UNEXPECTED_ERROR')
  })

  it('givenJsonString_whenPost_thenSendsJsonContentType', async () => {
    let contentType: string | null = null
    server.use(
      http.post('/api/x', ({ request: incoming }) => {
        contentType = incoming.headers.get('Content-Type')
        return HttpResponse.json({ ok: true })
      }),
    )

    await request('/api/x', { method: 'POST', body: '{}' })

    expect(contentType).toBe('application/json')
  })

  it('givenFormData_whenPost_thenBrowserSetsMultipartBoundaryNotJson', async () => {
    let contentType: string | null = null
    server.use(
      http.post('/api/x', ({ request: incoming }) => {
        contentType = incoming.headers.get('Content-Type')
        return HttpResponse.json({ ok: true })
      }),
    )
    const body = new FormData()
    body.append('version', '1')

    await request('/api/x', { method: 'POST', body })

    expect(contentType).toMatch(/^multipart\/form-data; boundary=/)
  })

  it('given204_whenRequest_thenResolvesUndefined', async () => {
    server.use(http.delete('/api/x', () => new HttpResponse(null, { status: 204 })))

    await expect(request('/api/x', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})

describe('ApiError helpers', () => {
  it.each([
    [new ApiError(400, 'A'), true],
    [new ApiError(404, 'A'), true],
    [new ApiError(499, 'A'), true],
    [new ApiError(500, 'A'), false],
    [new ApiError(0, 'NETWORK_ERROR'), false],
    [new Error('x'), false],
  ])('givenError_whenIsClientError_thenOnly4xxApiError %#', (error, expected) => {
    expect(isClientError(error)).toBe(expected)
  })

  it('givenApiError_whenCodeHelpers_thenMatchByCodeAndFallBackToUnexpected', () => {
    expect(isApiErrorWithCode(new ApiError(404, 'SOURCE_NOT_FOUND'), 'SOURCE_NOT_FOUND')).toBe(true)
    expect(isApiErrorWithCode(new ApiError(404, 'SOURCE_NOT_FOUND'), 'CHUNK_NOT_FOUND')).toBe(false)
    expect(isApiErrorWithCode(new Error('x'), 'SOURCE_NOT_FOUND')).toBe(false)
    expect(errorCodeOf(new ApiError(409, 'INGESTION_IN_PROGRESS'))).toBe('INGESTION_IN_PROGRESS')
    expect(errorCodeOf(new Error('x'))).toBe('UNEXPECTED_ERROR')
  })
})
