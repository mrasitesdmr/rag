import type { ServerSentEvent } from '../../../shared/api/eventStream.ts'
import { RETRIEVAL_STRATEGIES } from './types.ts'
import type {
  AnswerStreamEvent,
  ContextChunk,
  DoneEvent,
  RetrievalStrategy,
  SourcesEvent,
  StreamErrorEvent,
  TokenEvent,
} from './types.ts'

type JsonObject = Record<string, unknown>

export function toAnswerStreamEvent(event: ServerSentEvent): AnswerStreamEvent | null {
  const data = parseJsonObject(event.data)
  if (data === null) {
    return null
  }
  switch (event.event) {
    case 'token':
      return isTokenEvent(data) ? { type: 'token', data } : null
    case 'sources':
      return isSourcesEvent(data) ? { type: 'sources', data } : null
    case 'done':
      return isDoneEvent(data) ? { type: 'done', data } : null
    case 'error':
      return isStreamErrorEvent(data) ? { type: 'error', data } : null
    default:
      return null
  }
}

function parseJsonObject(text: string): JsonObject | null {
  try {
    const value: unknown = JSON.parse(text)
    return isObject(value) ? value : null
  } catch {
    return null
  }
}

function isObject(value: unknown): value is JsonObject {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value)
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function isNullableInteger(value: unknown): value is number | null {
  return value === null || isInteger(value)
}

function isNullableString(value: unknown): value is string | null {
  return value === null || typeof value === 'string'
}

function isRetrievalStrategy(value: unknown): value is RetrievalStrategy {
  return typeof value === 'string' && (RETRIEVAL_STRATEGIES as readonly string[]).includes(value)
}

function isTokenEvent(data: JsonObject): data is TokenEvent {
  return typeof data.text === 'string'
}

function isContextChunk(value: unknown): value is ContextChunk {
  return (
    isObject(value) &&
    isInteger(value.n) &&
    isInteger(value.chunkId) &&
    isInteger(value.chunkNo) &&
    isInteger(value.pageNumber) &&
    typeof value.heading === 'string' &&
    isInteger(value.sourceId) &&
    typeof value.sourceName === 'string' &&
    typeof value.version === 'string' &&
    isInteger(value.vectorRank) &&
    isFiniteNumber(value.distance) &&
    (value.rerankScore === null || isFiniteNumber(value.rerankScore))
  )
}

function isSourcesEvent(data: JsonObject): data is SourcesEvent {
  return (
    isRetrievalStrategy(data.retrievalStrategy) &&
    isInteger(data.candidates) &&
    isInteger(data.topK) &&
    Array.isArray(data.contextChunks) &&
    data.contextChunks.every(isContextChunk) &&
    Array.isArray(data.citations) &&
    data.citations.every(isInteger)
  )
}

function isDoneEvent(data: JsonObject): data is DoneEvent {
  return (
    isInteger(data.turnId) &&
    typeof data.notFoundAnswer === 'boolean' &&
    typeof data.chatModel === 'string' &&
    isNullableInteger(data.promptTokens) &&
    isNullableInteger(data.completionTokens) &&
    isInteger(data.retrievalLatencyMs) &&
    isNullableInteger(data.rerankLatencyMs) &&
    isNullableInteger(data.generationLatencyMs) &&
    isNullableInteger(data.timeToFirstTokenMs) &&
    isInteger(data.latencyMs) &&
    isNullableString(data.hostLabel)
  )
}

function isStreamErrorEvent(data: JsonObject): data is StreamErrorEvent {
  return typeof data.errorCode === 'string' && data.errorCode !== ''
}
