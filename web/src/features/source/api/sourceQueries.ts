import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QueryClient } from '@tanstack/react-query'
import type { PageParams, PageResponse } from '../../../shared/api/pagination.ts'
import { JOB_PAGE_SIZE } from '../model/chunkRules.ts'
import type { CreateSourceInput, IngestionJobResponse, ListChunksParams, SourceResponse } from '../model/types.ts'
import {
  createSource,
  getSource,
  getWorkspace,
  listChunks,
  listIngestionJobs,
  listSources,
  retryIngestion,
} from './sourceApi.ts'

export const workspaceKeys = {
  detail: () => ['workspace'] as const,
}

export const sourceKeys = {
  all: ['sources'] as const,
  lists: () => [...sourceKeys.all, 'list'] as const,
  list: (params: PageParams) => [...sourceKeys.lists(), params] as const,
  details: () => [...sourceKeys.all, 'detail'] as const,
  detail: (sourceId: number) => [...sourceKeys.details(), sourceId] as const,
  jobs: (sourceId: number) => [...sourceKeys.all, 'jobs', sourceId] as const,
  job: (jobId: number) => [...sourceKeys.all, 'job', jobId] as const,
  chunks: (sourceId: number, params: ListChunksParams) => [...sourceKeys.all, 'chunks', sourceId, params] as const,
}

const JOB_KEY_SEGMENT = 'job'

export function invalidateSourceViews(queryClient: QueryClient): Promise<void> {
  return queryClient.invalidateQueries({
    queryKey: sourceKeys.all,
    predicate: (query) => query.queryKey[1] !== JOB_KEY_SEGMENT,
  })
}

export function useWorkspaceQuery() {
  return useQuery({ queryKey: workspaceKeys.detail(), queryFn: getWorkspace })
}

export function useSourcesQuery(params: PageParams) {
  return useQuery({
    queryKey: sourceKeys.list(params),
    queryFn: () => listSources(params),
    placeholderData: keepPreviousData,
  })
}

export function useSourceQuery(sourceId: number) {
  return useQuery({ queryKey: sourceKeys.detail(sourceId), queryFn: () => getSource(sourceId) })
}

export function useIngestionJobsQuery(sourceId: number) {
  return useQuery({
    queryKey: sourceKeys.jobs(sourceId),
    queryFn: () => listIngestionJobs(sourceId, { page: 0, size: JOB_PAGE_SIZE }),
  })
}

export function useChunksQuery(sourceId: number, params: ListChunksParams) {
  return useQuery({
    queryKey: sourceKeys.chunks(sourceId, params),
    queryFn: () => listChunks(sourceId, params),
    placeholderData: keepPreviousData,
  })
}

export function useCreateSourceMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateSourceInput) => createSource(input),
    onSuccess: (job) => applyStartedJob(queryClient, job),
  })
}

export function useRetryIngestionMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sourceId: number) => retryIngestion(sourceId),
    onSuccess: (job) => applyStartedJob(queryClient, job),
    onError: () => invalidateSourceViews(queryClient),
  })
}

function applyStartedJob(queryClient: QueryClient, job: IngestionJobResponse): Promise<void> {
  queryClient.setQueryData(sourceKeys.job(job.jobId), job)
  queryClient.setQueriesData<PageResponse<SourceResponse>>({ queryKey: sourceKeys.lists() }, (page) =>
    page === undefined
      ? page
      : {
          ...page,
          content: page.content.map((source) =>
            source.sourceId === job.sourceId ? { ...source, latestJob: job } : source,
          ),
        },
  )
  queryClient.setQueryData<SourceResponse>(sourceKeys.detail(job.sourceId), (source) =>
    source === undefined ? source : { ...source, latestJob: job },
  )
  return invalidateSourceViews(queryClient)
}
