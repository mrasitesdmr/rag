import type {
  ConversationDetailResponse,
  ConversationSummaryResponse,
  HandoffResponse,
  TurnChunkResponse,
  TurnResponse,
} from '../features/conversation/model/types.ts'

export const CREATED_AT = '2026-11-02T10:31:05Z'
export const LOST_CARD_QUESTION = 'Kartımı kaybettim, ne yapmalıyım?'
export const FOLLOW_UP_QUESTION = 'Yenisi kaç günde gelir?'
export const REWRITTEN_QUERY = 'Kaybolan kartın yenisi kaç günde gelir?'

export function conversationSummary(overrides: Partial<ConversationSummaryResponse> = {}): ConversationSummaryResponse {
  return {
    conversationId: 12,
    channel: 'PREVIEW',
    createdAt: CREATED_AT,
    turnCount: 2,
    firstQuestion: LOST_CARD_QUESTION,
    lastTurnStatus: 'GROUNDED',
    handedOff: false,
    ...overrides,
  }
}

export function turnChunk(n: number, overrides: Partial<TurnChunkResponse> = {}): TurnChunkResponse {
  const vectorRanks = [7, 1, 3, 2, 9]
  return {
    n,
    chunkId: 150 + n,
    sourceId: 10001,
    version: '2026.10',
    chunkNo: 30 + n,
    pageNumber: 18 + n,
    heading: n === 1 ? '2.16 Kayıp ya da çalıntı bildirimi' : `Başlık ${n}`,
    vectorRank: vectorRanks[n - 1] ?? n,
    distance: n === 1 ? 0.3124 : 0.35,
    rerankScore: n === 1 ? 5.36 : 1.2,
    containsAnswer: false,
    ...overrides,
  }
}

export function promptChunks(overrides: Partial<TurnChunkResponse> = {}): TurnChunkResponse[] {
  return [1, 2, 3, 4, 5].map((n) => turnChunk(n, overrides))
}

export function turn(overrides: Partial<TurnResponse> = {}): TurnResponse {
  return {
    turnId: 31,
    turnNo: 1,
    status: 'GROUNDED',
    question: LOST_CARD_QUESTION,
    searchQuery: LOST_CARD_QUESTION,
    answer: 'Kartınızı hemen 7/24 hattını arayarak kapatın [1].',
    generationMode: 'RAG',
    retrievalStrategy: 'RERANK',
    candidates: 20,
    topK: 5,
    cited: [1],
    promptTokens: 1234,
    completionTokens: 87,
    latencyMs: 3420,
    errorCode: null,
    helpful: false,
    createdAt: CREATED_AT,
    completedAt: CREATED_AT,
    contextChunks: promptChunks(),
    ...overrides,
  }
}

export function followUpTurn(overrides: Partial<TurnResponse> = {}): TurnResponse {
  return turn({
    turnId: 32,
    turnNo: 2,
    question: FOLLOW_UP_QUESTION,
    searchQuery: REWRITTEN_QUERY,
    answer: 'Yeni kart 5 iş gününde adresinize gelir [1].',
    helpful: null,
    ...overrides,
  })
}

export function handoff(overrides: Partial<HandoffResponse> = {}): HandoffResponse {
  return { conversationId: 12, turnId: 31, reason: 'NO_ANSWER', queue: 'bireysel', createdAt: CREATED_AT, ...overrides }
}

export function conversationDetail(overrides: Partial<ConversationDetailResponse> = {}): ConversationDetailResponse {
  return {
    conversationId: 12,
    channel: 'PREVIEW',
    createdAt: CREATED_AT,
    turns: [turn(), followUpTurn()],
    inProgressTurn: null,
    handoff: null,
    ...overrides,
  }
}
