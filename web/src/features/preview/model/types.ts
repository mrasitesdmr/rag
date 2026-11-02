import type { GenerationMode } from '../../../shared/liveConfig/types.ts'

export const CHANNELS = ['PREVIEW'] as const
export type Channel = (typeof CHANNELS)[number]

export type CreateConversationRequest = { channel: Channel }

export type ConversationResponse = {
  conversationId: number
  channel: Channel
  createdAt: string
}

export type SendMessageRequest = { text: string }

export const RETRIEVAL_STRATEGIES = ['TOP_K', 'RERANK', 'RERANK_FALLBACK', 'NONE'] as const
export type RetrievalStrategy = (typeof RETRIEVAL_STRATEGIES)[number]

export const QUERY_REWRITE_STATUSES = ['DISABLED', 'NO_HISTORY', 'REWRITTEN', 'FAILED', 'EMPTY'] as const
export type QueryRewriteStatus = (typeof QUERY_REWRITE_STATUSES)[number]

export type ContextChunk = {
  n: number
  chunkId: number
  chunkNo: number
  pageNumber: number
  heading: string
  sourceId: number
  sourceName: string
  version: string
  vectorRank: number
  distance: number
  rerankScore: number | null
  containsAnswer: boolean
}

export type TokenEvent = { text: string }

export type SourcesEvent = {
  generationMode: GenerationMode
  retrievalStrategy: RetrievalStrategy
  candidates: number | null
  topK: number | null
  searchQuery: string | null
  queryRewriteStatus: QueryRewriteStatus
  contextChunks: ContextChunk[]
  citations: number[]
}

export type DoneEvent = {
  turnId: number
  notFoundAnswer: boolean
  chatModel: string
  promptTokens: number | null
  completionTokens: number | null
  queryRewriteLatencyMs: number | null
  retrievalLatencyMs: number | null
  rerankLatencyMs: number | null
  generationLatencyMs: number | null
  timeToFirstTokenMs: number | null
  latencyMs: number
  hostLabel: string | null
}

export type StreamErrorEvent = { errorCode: string }

export type AnswerStreamEvent =
  | { type: 'token'; data: TokenEvent }
  | { type: 'sources'; data: SourcesEvent }
  | { type: 'done'; data: DoneEvent }
  | { type: 'error'; data: StreamErrorEvent }

export const TURN_PHASES = ['waiting', 'streaming', 'done', 'failed'] as const
export type TurnPhase = (typeof TURN_PHASES)[number]

export type PreviewTurn = {
  localId: string
  conversationId: number | null
  question: string
  askedAt: string
  answer: string
  phase: TurnPhase
  sources: SourcesEvent | null
  done: DoneEvent | null
  answeredAt: string | null
  errorCode: string | null
}

export type SuggestedQuestionResponse = {
  text: string
  starter: boolean
  followUps: string[]
}

export type SuggestionCategoryResponse = {
  name: string
  questions: SuggestedQuestionResponse[]
}

export type SuggestionSetResponse = {
  questionCount: number
  followUpCount: number
  categories: SuggestionCategoryResponse[]
}

export const SUGGESTION_KINDS = ['followUp', 'question'] as const
export type SuggestionKind = (typeof SUGGESTION_KINDS)[number]

export type SuggestionItem = { text: string; kind: SuggestionKind }

export type SubmitFeedbackRequest = { helpful: boolean }

export const HANDOFF_REASONS = ['NO_ANSWER'] as const
export type HandoffReason = (typeof HANDOFF_REASONS)[number]

export type RequestHandoffRequest = { turnId: number; reason: HandoffReason }

export type HandoffResponse = {
  conversationId: number
  turnId: number
  reason: HandoffReason
  queue: string
  createdAt: string
}

export type PreviewNavigationState = { question: string }
