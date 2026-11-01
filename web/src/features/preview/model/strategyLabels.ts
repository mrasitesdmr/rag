import type { SourcesEvent } from './types.ts'

export const RERANKER_DOWN_LABEL = 'reranker DOWN · vector sırası'

export function strategyLabel(sources: SourcesEvent): string {
  switch (sources.retrievalStrategy) {
    case 'TOP_K':
      return `top-${sources.topK}`
    case 'RERANK':
      return `rerank ${sources.candidates}→${sources.topK}`
    case 'RERANK_FALLBACK':
      return RERANKER_DOWN_LABEL
  }
}
