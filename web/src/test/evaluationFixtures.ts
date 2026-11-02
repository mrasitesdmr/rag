import { http, HttpResponse } from 'msw'
import type {
  EvalOverviewResponse,
  EvalQuestionResultResponse,
  EvalRunDetailResponse,
  EvalRunResponse,
  GoldenSetSummaryResponse,
} from '../features/evaluation/model/types.ts'
import { pageOf } from './fixtures.ts'
import { server } from './server.ts'

export function goldenSet(overrides: Partial<GoldenSetSummaryResponse> = {}): GoldenSetSummaryResponse {
  return { version: 'rehber-golden-v1', questionCount: 100, loadedAt: '2026-11-02T09:00:00Z', ...overrides }
}

export function evalRun(overrides: Partial<EvalRunResponse> = {}): EvalRunResponse {
  return {
    evalRunId: 7,
    status: 'DONE',
    goldenSetVersion: 'rehber-golden-v1',
    retrievalStrategy: 'RERANK',
    candidates: 20,
    topK: 5,
    sourceVersions: '2026.10',
    questionCount: 100,
    measuredCount: 100,
    unmatchedCount: 0,
    processedCount: 100,
    hitCount: 98,
    missedCount: 2,
    recallAtK: 0.98,
    mrr: 0.9125,
    rerankFallbackCount: 0,
    errorCode: null,
    startedAt: '2026-11-02T10:00:00Z',
    finishedAt: '2026-11-02T10:01:42Z',
    durationMs: 102300,
    ...overrides,
  }
}

export function runningRun(overrides: Partial<EvalRunResponse> = {}): EvalRunResponse {
  return evalRun({
    status: 'RUNNING',
    processedCount: 0,
    hitCount: null,
    missedCount: null,
    recallAtK: null,
    mrr: null,
    finishedAt: null,
    durationMs: null,
    ...overrides,
  })
}

export function failedRun(overrides: Partial<EvalRunResponse> = {}): EvalRunResponse {
  return evalRun({
    status: 'FAILED',
    errorCode: 'EVAL_RUN_INTERRUPTED',
    hitCount: null,
    missedCount: null,
    recallAtK: null,
    mrr: null,
    ...overrides,
  })
}

export function missedQuestion(
  position: number,
  overrides: Partial<EvalQuestionResultResponse> = {},
): EvalQuestionResultResponse {
  return {
    position,
    category: 'Kartlar',
    text: `Soru ${position}?`,
    vectorRank: 3,
    promptPosition: 1,
    ...overrides,
  }
}

export function doneRunDetail(
  overrides: Partial<EvalRunDetailResponse> = {},
): EvalRunDetailResponse {
  return { ...evalRun(), missedAndMovedQuestions: [], ...overrides }
}

export function overview(overrides: Partial<EvalOverviewResponse> = {}): EvalOverviewResponse {
  return { goldenSet: goldenSet(), runningRun: null, lastFinishedRun: null, lastDoneRun: null, ...overrides }
}

export type EvalRequestLog = { overview: number; runs: URL[]; starts: number }

export function serveEvaluation(
  overviews: EvalOverviewResponse[],
  runs: EvalRunResponse[] = [],
  options: { totalElements?: number; start?: Response } = {},
): EvalRequestLog {
  const log: EvalRequestLog = { overview: 0, runs: [], starts: 0 }
  server.use(
    http.get('/api/assistant/evals/overview', () => {
      const reply = overviews[Math.min(log.overview, overviews.length - 1)]
      log.overview += 1
      return HttpResponse.json(reply)
    }),
    http.get('/api/assistant/evals', ({ request }) => {
      const url = new URL(request.url)
      log.runs.push(url)
      const page = Number(url.searchParams.get('page') ?? 0)
      const size = Number(url.searchParams.get('size') ?? 20)
      return HttpResponse.json(
        pageOf(runs.slice(page * size, (page + 1) * size), { page, size, totalElements: options.totalElements ?? runs.length }),
      )
    }),
    http.post('/api/assistant/evals', () => {
      log.starts += 1
      return options.start ?? HttpResponse.json(runningRun({ measuredCount: 3 }), { status: 202 })
    }),
  )
  return log
}
