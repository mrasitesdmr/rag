import type { Channel, TurnErrorCode, TurnResponse } from './types.ts'

const CHANNEL_LABELS: Record<Channel, string> = {
  PREVIEW: 'Önizleme',
}

const FAILURE_REASON_LABELS: Record<TurnErrorCode, string> = {
  CHAT_MODEL_UNAVAILABLE: 'dil modeline ulaşılamadı',
  RETRIEVAL_UNAVAILABLE: 'bilgi bankasına ulaşılamadı',
  STREAM_CANCELLED: 'cevap yarıda kesildi',
  INTERNAL_ERROR: 'beklenmeyen hata',
}

const LLM_ONLY_LABEL = 'Sadece LLM'

export function channelLabel(channel: Channel): string {
  return CHANNEL_LABELS[channel]
}

export function generationLabel(turn: TurnResponse): string {
  if (turn.generationMode === 'LLM_ONLY') {
    return LLM_ONLY_LABEL
  }
  switch (turn.retrievalStrategy) {
    case 'TOP_K':
      return `RAG · top-${turn.topK ?? ''}`
    case 'RERANK':
      return `RAG · rerank ${turn.candidates ?? ''}→${turn.topK ?? ''}`
    case 'RERANK_FALLBACK':
      return 'RAG · reranker DOWN'
    case 'NONE':
      return 'RAG'
  }
}

export function guideVersionLabel(turn: TurnResponse): string | null {
  const versions = [...new Set(turn.contextChunks.map((chunk) => chunk.version))]
  return versions.length === 0 ? null : versions.join(', ')
}

export function failureReasonLabel(errorCode: TurnErrorCode): string {
  return FAILURE_REASON_LABELS[errorCode]
}

export function hasDistinctSearchQuery(turn: TurnResponse): boolean {
  return turn.searchQuery !== null && turn.searchQuery !== turn.question
}
