import { queryOptions, useQueries, useQueryClient } from '@tanstack/react-query'
import { isClientError } from '../../../shared/api/ApiError.ts'
import { getIngestionJob } from '../api/sourceApi.ts'
import { invalidateSourceViews, sourceKeys } from '../api/sourceQueries.ts'
import { INGESTION_POLL_INTERVAL_MS, isJobActive } from '../model/ingestionRules.ts'
import type { IngestionJobResponse } from '../model/types.ts'

export function useIngestionJobsPolling(jobs: IngestionJobResponse[]): Map<number, IngestionJobResponse> {
  const queryClient = useQueryClient()
  const activeJobs = jobs.filter(isJobActive)

  async function fetchJob(jobId: number): Promise<IngestionJobResponse> {
    const next = await getIngestionJob(jobId)
    const previous = queryClient.getQueryData<IngestionJobResponse>(sourceKeys.job(jobId))
    if (!isJobActive(next) && (previous === undefined || isJobActive(previous))) {
      void invalidateSourceViews(queryClient)
    }
    return next
  }

  const results = useQueries({
    queries: activeJobs.map((job) =>
      queryOptions({
        queryKey: sourceKeys.job(job.jobId),
        queryFn: () => fetchJob(job.jobId),
        initialData: job,
        refetchInterval: (query) => {
          const { data, error } = query.state
          if (isClientError(error) || data === undefined || !isJobActive(data)) {
            return false
          }
          return INGESTION_POLL_INTERVAL_MS
        },
        refetchIntervalInBackground: true,
      }),
    ),
  })

  const jobsById = new Map<number, IngestionJobResponse>()
  for (const result of results) {
    if (result.data !== undefined) {
      jobsById.set(result.data.jobId, result.data)
    }
  }
  return jobsById
}
