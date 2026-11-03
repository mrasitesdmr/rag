import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { evalRun, failedRun, overview, runningRun } from '../../../test/evaluationFixtures.ts'
import type { EvalOverviewResponse } from '../model/types.ts'
import { useEvalRunAnnouncement } from './useEvalRunAnnouncement.ts'

function renderAnnouncement(initial: EvalOverviewResponse | undefined) {
  return renderHook(({ current }) => useEvalRunAnnouncement(current), { initialProps: { current: initial } })
}

describe('useEvalRunAnnouncement', () => {
  it('givenNoOverviewYet_whenRendered_thenSilent', () => {
    expect(renderAnnouncement(undefined).result.current).toBe('')
  })

  it('givenIdleOverview_whenRendered_thenSilent', () => {
    expect(renderAnnouncement(overview()).result.current).toBe('')
  })

  it('givenRunningRunAppears_whenRerendered_thenStartedAnnouncement', () => {
    const view = renderAnnouncement(overview())

    view.rerender({ current: overview({ runningRun: runningRun({ evalRunId: 8 }) }) })

    expect(view.result.current).toBe('Ölçüm başladı.')
  })

  it('givenRunningRunAlreadyThereOnFirstLoad_whenRendered_thenStartedAnnouncement', () => {
    expect(renderAnnouncement(overview({ runningRun: runningRun({ evalRunId: 8 }) })).result.current).toBe('Ölçüm başladı.')
  })

  it('givenObservedRunFinishesDone_whenRerendered_thenFinishedAnnouncement', () => {
    const view = renderAnnouncement(overview({ runningRun: runningRun({ evalRunId: 8 }) }))

    view.rerender({ current: overview({ lastFinishedRun: evalRun({ evalRunId: 8, status: 'DONE' }) }) })

    expect(view.result.current).toBe('Ölçüm tamamlandı.')
  })

  it('givenObservedRunFails_whenRerendered_thenFailedAnnouncement', () => {
    const view = renderAnnouncement(overview({ runningRun: runningRun({ evalRunId: 8 }) }))

    view.rerender({ current: overview({ lastFinishedRun: failedRun({ evalRunId: 8 }) }) })

    expect(view.result.current).toBe('Ölçüm tamamlanamadı.')
  })

  it('givenEarlierFailedRunOtherThanObserved_whenObservedRunFinishes_thenFinishedAnnouncement', () => {
    const view = renderAnnouncement(overview({ runningRun: runningRun({ evalRunId: 8 }) }))

    view.rerender({ current: overview({ lastFinishedRun: failedRun({ evalRunId: 3 }) }) })

    expect(view.result.current).toBe('Ölçüm tamamlandı.')
  })

  it('givenAnnouncementShown_whenSameOverviewRerendered_thenAnnouncementKept', () => {
    const view = renderAnnouncement(overview({ runningRun: runningRun({ evalRunId: 8 }) }))

    view.rerender({ current: overview({ runningRun: runningRun({ evalRunId: 8, processedCount: 40 }) }) })

    expect(view.result.current).toBe('Ölçüm başladı.')
  })
})
