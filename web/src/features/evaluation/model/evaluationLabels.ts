import type { LiveConfigResponse } from '../../../shared/liveConfig/types.ts'
import { RUN_BUTTON_IDLE, RUN_BUTTON_RUNNING, RUN_FAILURE_PREFIX, RUN_FAILURE_REASONS } from './evaluationTexts.ts'
import type { EvalRetrievalStrategy, EvalRunErrorCode, EvalRunResponse } from './types.ts'

export function evalRunLabel(evalRunId: number): string {
  return `ev_${evalRunId}`
}

export function strategyText(strategy: EvalRetrievalStrategy, candidates: number, topK: number): string {
  return strategy === 'RERANK' ? `rerank ${candidates}→${topK}` : `top-${topK}`
}

export function retrievalText(run: EvalRunResponse): string {
  const strategy = strategyText(run.retrievalStrategy, run.candidates, run.topK)
  return run.rerankFallbackCount > 0 ? `${strategy} · reranker DOWN (${run.rerankFallbackCount})` : strategy
}

export function vectorRankText(vectorRank: number | null, candidates: number): string {
  return vectorRank === null ? `${candidates}+` : String(vectorRank)
}

export function promptPositionText(promptPosition: number | null): string {
  return promptPosition === null ? 'yok' : `${promptPosition}. sıra`
}

export function runButtonText(liveConfig: LiveConfigResponse | undefined, runningRun: EvalRunResponse | null): string {
  if (runningRun !== null) {
    return `${RUN_BUTTON_RUNNING} · ${runningRun.processedCount}/${runningRun.measuredCount}`
  }
  if (liveConfig === undefined) {
    return RUN_BUTTON_IDLE
  }
  return `${RUN_BUTTON_IDLE} · ${strategyText(liveConfig.retrievalStrategy, liveConfig.candidates, liveConfig.topK)}`
}

export function recallSubtitle(run: EvalRunResponse): string {
  const measured = `${run.hitCount ?? 0}/${run.measuredCount} soru`
  return run.unmatchedCount > 0 ? `${measured} · ${run.unmatchedCount} soru ölçülemedi` : measured
}

export function retrievalSubtitle(run: EvalRunResponse): string {
  const guide = `rehber ${run.sourceVersions}`
  return run.rerankFallbackCount > 0 ? `${guide} · reranker DOWN: ${run.rerankFallbackCount} soru` : guide
}

export function runFailureText(errorCode: EvalRunErrorCode | null): string {
  return `${RUN_FAILURE_PREFIX}${RUN_FAILURE_REASONS[errorCode ?? 'INTERNAL_ERROR']}`
}
