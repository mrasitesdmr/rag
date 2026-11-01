export const CHANNELS = ['PREVIEW'] as const
export type Channel = (typeof CHANNELS)[number]

export type CreateConversationRequest = { channel: Channel }

export type ConversationResponse = {
  conversationId: number
  channel: Channel
  createdAt: string
}

export type SendMessageRequest = { text: string }

export const RETRIEVAL_STRATEGIES = ['TOP_K', 'RERANK', 'RERANK_FALLBACK'] as const
export type RetrievalStrategy = (typeof RETRIEVAL_STRATEGIES)[number]

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
}

export type TokenEvent = { text: string }

export type SourcesEvent = {
  retrievalStrategy: RetrievalStrategy
  candidates: number
  topK: number
  contextChunks: ContextChunk[]
  citations: number[]
}

export type DoneEvent = {
  turnId: number
  notFoundAnswer: boolean
  chatModel: string
  promptTokens: number | null
  completionTokens: number | null
  retrievalLatencyMs: number
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
  question: string
  askedAt: string
  answer: string
  phase: TurnPhase
  sources: SourcesEvent | null
  done: DoneEvent | null
  errorCode: string | null
}
