import type { IngestionJobResponse, SourceResponse } from './types.ts'

export const INGESTION_POLL_INTERVAL_MS = 1000

export type SourceStatus = 'RUNNING' | 'READY' | 'FAILED'

export function isJobActive(job: IngestionJobResponse): boolean {
  return job.stage !== 'DONE' && job.stage !== 'FAILED'
}

export function sourceStatus(source: SourceResponse, job: IngestionJobResponse): SourceStatus {
  if (isJobActive(job)) {
    return 'RUNNING'
  }
  if (source.chunkCount === null && job.stage === 'FAILED') {
    return 'FAILED'
  }
  return 'READY'
}

export function canRetry(source: SourceResponse, job: IngestionJobResponse): boolean {
  return sourceStatus(source, job) === 'FAILED'
}
