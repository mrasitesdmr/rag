import { describe, expect, it } from 'vitest'
import { failedSource, job, source } from '../../../test/fixtures.ts'
import { canRetry, isJobActive, sourceStatus } from './ingestionRules.ts'
import type { IngestionStage } from './types.ts'

describe('ingestion rules', () => {
  it.each<[IngestionStage, boolean]>([
    ['QUEUED', true],
    ['PARSING', true],
    ['CHUNKING', true],
    ['EMBEDDING', true],
    ['WRITING', true],
    ['DONE', false],
    ['FAILED', false],
  ])('givenStage_whenIsJobActive_thenOnlyUnfinishedStagesAreActive %s', (stage, expected) => {
    expect(isJobActive(job({ stage }))).toBe(expected)
  })

  it.each<IngestionStage>(['QUEUED', 'PARSING', 'CHUNKING', 'EMBEDDING', 'WRITING'])(
    'givenRunningJob_whenSourceStatus_thenRunningEvenWithActiveVersion %s',
    (stage) => {
      expect(sourceStatus(source(), job({ stage }))).toBe('RUNNING')
    },
  )

  it('givenDoneJob_whenSourceStatus_thenReady', () => {
    expect(sourceStatus(source(), job({ stage: 'DONE' }))).toBe('READY')
  })

  it('givenFailedFirstJobWithoutVersion_whenSourceStatus_thenFailedAndRetryable', () => {
    const failed = failedSource()

    expect(sourceStatus(failed, failed.latestJob)).toBe('FAILED')
    expect(canRetry(failed, failed.latestJob)).toBe(true)
  })

  it('givenFailedJobButCompletedVersionExists_whenSourceStatus_thenReadyAndNotRetryable', () => {
    const withVersion = source({ latestJob: job({ stage: 'FAILED', errorCode: 'EMBEDDING_UNAVAILABLE' }) })

    expect(sourceStatus(withVersion, withVersion.latestJob)).toBe('READY')
    expect(canRetry(withVersion, withVersion.latestJob)).toBe(false)
  })

  it('givenReadyAndRunningSources_whenCanRetry_thenFalse', () => {
    expect(canRetry(source(), source().latestJob)).toBe(false)
    expect(canRetry(failedSource(), job({ stage: 'QUEUED' }))).toBe(false)
  })
})
