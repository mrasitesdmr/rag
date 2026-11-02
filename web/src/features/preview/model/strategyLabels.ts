import { shortenText } from '../../../shared/format/formatText.ts'
import type { SourcesEvent } from './types.ts'

export const RERANKER_DOWN_LABEL = 'reranker DOWN · vector sırası'
export const QUERY_REWRITE_FAILED_LABEL = 'yeniden yazma başarısız'
export const SEARCH_QUERY_SHORT_LENGTH = 40

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

export function queryRewriteLabel(sources: SourcesEvent): string | null {
  switch (sources.queryRewriteStatus) {
    case 'REWRITTEN':
      return `arama: "${shortenText(sources.searchQuery, SEARCH_QUERY_SHORT_LENGTH)}"`
    case 'FAILED':
    case 'EMPTY':
      return QUERY_REWRITE_FAILED_LABEL
    case 'DISABLED':
    case 'NO_HISTORY':
      return null
  }
}
