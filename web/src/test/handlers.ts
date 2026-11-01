import { http, HttpResponse } from 'msw'
import type { ChunkSummaryResponse, IngestionJobResponse, SourceResponse } from '../features/source/model/types.ts'
import { chunkSummary, job, pageOf, source } from './fixtures.ts'
import { server } from './server.ts'

export function sleep(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

export function deferred<T = void>() {
  let resolve: (value: T) => void = () => undefined
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

export type RequestLog = { urls: URL[] }

export function recordRequests(): RequestLog {
  const log: RequestLog = { urls: [] }
  server.events.on('request:start', ({ request }) => {
    log.urls.push(new URL(request.url))
  })
  return log
}

export function countOf(log: RequestLog, pathname: string): number {
  return log.urls.filter((url) => url.pathname === pathname).length
}

export function serveSourceList(sources: SourceResponse[]) {
  server.use(http.get('/api/workspaces/42/sources', () => HttpResponse.json(pageOf(sources))))
}

export function serveSourceListSequence(...responses: SourceResponse[][]) {
  let call = 0
  server.use(
    http.get('/api/workspaces/42/sources', () => {
      const content = responses[Math.min(call, responses.length - 1)] ?? []
      call += 1
      return HttpResponse.json(pageOf(content))
    }),
  )
}

export function serveJobSequence(jobId: number, ...jobs: IngestionJobResponse[]) {
  let call = 0
  server.use(
    http.get(`/api/workspaces/42/ingestion-jobs/${jobId}`, () => {
      const next = jobs[Math.min(call, jobs.length - 1)] ?? job()
      call += 1
      return HttpResponse.json(next)
    }),
  )
}

export type DetailOptions = {
  source?: SourceResponse
  jobs?: IngestionJobResponse[]
  totalChunks?: number
}

export function serveDetail(options: DetailOptions = {}) {
  const detail = options.source ?? source()
  const totalChunks = options.totalChunks ?? detail.chunkCount ?? 0
  server.use(
    http.get(`/api/workspaces/42/sources/${detail.sourceId}`, () => HttpResponse.json(detail)),
    http.get(`/api/workspaces/42/sources/${detail.sourceId}/ingestion-jobs`, () =>
      HttpResponse.json(pageOf(options.jobs ?? [detail.latestJob])),
    ),
    http.get(`/api/workspaces/42/sources/${detail.sourceId}/chunks`, ({ request }) => {
      const params = new URL(request.url).searchParams
      const page = Number(params.get('page') ?? 0)
      const size = Number(params.get('size') ?? 12)
      const rest = Math.max(0, Math.min(size, totalChunks - page * size))
      const content: ChunkSummaryResponse[] = Array.from({ length: rest }, (_, index) => chunkSummary(page * size + index))
      return HttpResponse.json(pageOf(content, { page, size, totalElements: totalChunks }))
    }),
  )
}
