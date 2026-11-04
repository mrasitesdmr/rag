import type { GenerationMode } from '../../../shared/liveConfig/types.ts'

export const CHANNELS = ['PREVIEW'] as const
export type Channel = (typeof CHANNELS)[number]

export const TURN_STATUSES = [
  'IN_PROGRESS',
  'INCOMPLETE',
  'UNGROUNDED',
  'SMALL_TALK',
  'DONT_KNOW',
  'MISSED',
  'GENERATION_ERROR',
  'WRONG',
  'GROUNDED',
] as const
export type TurnStatus = (typeof TURN_STATUSES)[number]

export const RETRIEVAL_STRATEGIES = ['TOP_K', 'RERANK', 'RERANK_FALLBACK', 'NONE'] as const
export type RetrievalStrategy = (typeof RETRIEVAL_STRATEGIES)[number]

export const TURN_ERROR_CODES = [
  'RETRIEVAL_UNAVAILABLE',
  'CHAT_MODEL_UNAVAILABLE',
  'STREAM_CANCELLED',
  'INTERNAL_ERROR',
] as const
export type TurnErrorCode = (typeof TURN_ERROR_CODES)[number]

export const HANDOFF_REASONS = ['NO_ANSWER'] as const
export type HandoffReason = (typeof HANDOFF_REASONS)[number]

export type ConversationSummaryResponse = {
  conversationId: number
  channel: Channel
  createdAt: string
  turnCount: number
  firstQuestion: string
  lastTurnStatus: TurnStatus
  handedOff: boolean
}

export type HandoffResponse = {
  conversationId: number
  turnId: number
  reason: HandoffReason
  queue: string
  createdAt: string
}

export type InProgressTurnResponse = {
  turnNo: number
  question: string
  startedAt: string
}

export type TurnChunkResponse = {
  n: number
  chunkId: number
  sourceId: number
  version: string
  chunkNo: number
  pageNumber: number
  heading: string
  vectorRank: number
  distance: number
  rerankScore: number | null
  containsAnswer: boolean
}

export type TurnResponse = {
  turnId: number
  turnNo: number
  status: TurnStatus
  question: string
  searchQuery: string | null
  answer: string | null
  generationMode: GenerationMode
  retrievalStrategy: RetrievalStrategy
  candidates: number | null
  topK: number | null
  cited: number[]
  promptTokens: number | null
  completionTokens: number | null
  latencyMs: number
  errorCode: TurnErrorCode | null
  helpful: boolean | null
  createdAt: string
  completedAt: string
  contextChunks: TurnChunkResponse[]
}

export type ConversationDetailResponse = {
  conversationId: number
  channel: Channel
  createdAt: string
  turns: TurnResponse[]
  inProgressTurn: InProgressTurnResponse | null
  handoff: HandoffResponse | null
}
