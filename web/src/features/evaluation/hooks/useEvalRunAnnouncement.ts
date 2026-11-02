import { useState } from 'react'
import { RUN_FAILED_ANNOUNCEMENT, RUN_FINISHED_ANNOUNCEMENT, RUN_STARTED_ANNOUNCEMENT } from '../model/evaluationTexts.ts'
import type { EvalOverviewResponse } from '../model/types.ts'

export function useEvalRunAnnouncement(overview: EvalOverviewResponse | undefined): string {
  const runningRunId = overview?.runningRun?.evalRunId ?? null
  const [observedRunId, setObservedRunId] = useState<number | null>(null)
  const [announcement, setAnnouncement] = useState('')

  if (overview !== undefined && runningRunId !== observedRunId) {
    setObservedRunId(runningRunId)
    if (runningRunId !== null) {
      setAnnouncement(RUN_STARTED_ANNOUNCEMENT)
    } else if (observedRunId !== null) {
      const finishedRun = overview.lastFinishedRun
      const hasFailed = finishedRun !== null && finishedRun.evalRunId === observedRunId && finishedRun.status === 'FAILED'
      setAnnouncement(hasFailed ? RUN_FAILED_ANNOUNCEMENT : RUN_FINISHED_ANNOUNCEMENT)
    }
  }

  return announcement
}
