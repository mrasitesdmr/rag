import { request, withQuery } from '../../../shared/api/httpClient.ts'
import type { PageParams, PageResponse } from '../../../shared/api/pagination.ts'
import { WORKSPACE_API_PATH } from '../../../shared/api/workspace.ts'
import type {
  ChunkSummaryResponse,
  CreateSourceInput,
  IngestionJobResponse,
  ListChunksParams,
  SourceResponse,
  WorkspaceResponse,
} from '../model/types.ts'

const SOURCES_PATH = `${WORKSPACE_API_PATH}/sources`

export function getWorkspace(): Promise<WorkspaceResponse> {
  return request<WorkspaceResponse>(WORKSPACE_API_PATH)
}

export function listSources(params: PageParams): Promise<PageResponse<SourceResponse>> {
  return request<PageResponse<SourceResponse>>(withQuery(SOURCES_PATH, params))
}

export function getSource(sourceId: number): Promise<SourceResponse> {
  return request<SourceResponse>(`${SOURCES_PATH}/${sourceId}`)
}

export function createSource(input: CreateSourceInput): Promise<IngestionJobResponse> {
  const body = new FormData()
  if (input.file !== null) {
    body.append('file', input.file)
  }
  if (input.name !== '') {
    body.append('name', input.name)
  }
  body.append('version', input.version)
  return request<IngestionJobResponse>(SOURCES_PATH, { method: 'POST', body })
}

export function retryIngestion(sourceId: number): Promise<IngestionJobResponse> {
  return request<IngestionJobResponse>(`${SOURCES_PATH}/${sourceId}/ingestion-jobs`, { method: 'POST' })
}

export function listIngestionJobs(sourceId: number, params: PageParams): Promise<PageResponse<IngestionJobResponse>> {
  return request<PageResponse<IngestionJobResponse>>(withQuery(`${SOURCES_PATH}/${sourceId}/ingestion-jobs`, params))
}

export function getIngestionJob(jobId: number): Promise<IngestionJobResponse> {
  return request<IngestionJobResponse>(`${WORKSPACE_API_PATH}/ingestion-jobs/${jobId}`)
}

export function listChunks(sourceId: number, params: ListChunksParams): Promise<PageResponse<ChunkSummaryResponse>> {
  const query = params.query.trim()
  return request<PageResponse<ChunkSummaryResponse>>(
    withQuery(`${SOURCES_PATH}/${sourceId}/chunks`, {
      query: query === '' ? undefined : query,
      page: params.page,
      size: params.size,
    }),
  )
}

export function sourceFileUrl(sourceId: number): string {
  return `${SOURCES_PATH}/${sourceId}/file`
}
