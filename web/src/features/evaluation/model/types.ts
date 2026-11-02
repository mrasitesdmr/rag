export type GoldenSetSummaryResponse = {
  version: string
  questionCount: number
  loadedAt: string
}

export const EVAL_RUN_STATUSES = ['RUNNING', 'DONE', 'FAILED'] as const
export type EvalRunStatus = (typeof EVAL_RUN_STATUSES)[number]

export const EVAL_RUN_ERROR_CODES = ['RETRIEVAL_UNAVAILABLE', 'EVAL_RUN_INTERRUPTED', 'INTERNAL_ERROR'] as const
export type EvalRunErrorCode = (typeof EVAL_RUN_ERROR_CODES)[number]

export const EVAL_RETRIEVAL_STRATEGIES = ['TOP_K', 'RERANK'] as const
export type EvalRetrievalStrategy = (typeof EVAL_RETRIEVAL_STRATEGIES)[number]

export type EvalRunResponse = {
  evalRunId: number
  status: EvalRunStatus
  goldenSetVersion: string
  retrievalStrategy: EvalRetrievalStrategy
  candidates: number
  topK: number
  sourceVersions: string
  questionCount: number
  measuredCount: number
  unmatchedCount: number
  processedCount: number
  hitCount: number | null
  missedCount: number | null
  recallAtK: number | null
  mrr: number | null
  rerankFallbackCount: number
  errorCode: EvalRunErrorCode | null
  startedAt: string
  finishedAt: string | null
  durationMs: number | null
}

export type EvalQuestionResultResponse = {
  position: number
  category: string
  text: string
  vectorRank: number | null
  promptPosition: number | null
}

export type EvalRunDetailResponse = EvalRunResponse & {
  missedAndMovedQuestions: EvalQuestionResultResponse[]
}

export type EvalOverviewResponse = {
  goldenSet: GoldenSetSummaryResponse | null
  runningRun: EvalRunResponse | null
  lastFinishedRun: EvalRunResponse | null
  lastDoneRun: EvalRunDetailResponse | null
}
