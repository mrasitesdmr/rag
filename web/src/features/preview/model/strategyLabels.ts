import { shortenText } from '../../../shared/format/formatText.ts'
import type { SourcesEvent } from './types.ts'

export const RERANKER_DOWN_LABEL = 'reranker DOWN · vector sırası'
export const RETRIEVAL_OFF_LABEL = 'kapalı (Sadece LLM)'
export const QUERY_REWRITE_FAILED_LABEL = 'yeniden yazma başarısız'
export const SEARCH_QUERY_SHORT_LENGTH = 40

export function strategyLabel(sources: SourcesEvent): string {
  switch (sources.retrievalStrategy) {
    case 'TOP_K':
      return sources.topK === null ? 'top-k' : `top-${sources.topK}`
    case 'RERANK':
      return sources.candidates === null || sources.topK === null
        ? 'rerank'
        : `rerank ${sources.candidates}→${sources.topK}`
    case 'RERANK_FALLBACK':
      return RERANKER_DOWN_LABEL
    case 'NONE':
      return RETRIEVAL_OFF_LABEL
  }
}

export function queryRewriteLabel(sources: SourcesEvent): string | null {
  switch (sources.queryRewriteStatus) {
    case 'REWRITTEN':
      return sources.searchQuery === null
        ? null
        : `arama: "${shortenText(sources.searchQuery, SEARCH_QUERY_SHORT_LENGTH)}"`
    case 'FAILED':
    case 'EMPTY':
      return QUERY_REWRITE_FAILED_LABEL
    case 'DISABLED':
    case 'NO_HISTORY':
      return null
  }
}
