import { GUIDE_NAME } from './fixtures.ts'
import type { SseEvent } from './sse.ts'
import type { ContextChunk, DoneEvent, SourcesEvent } from '../features/preview/model/types.ts'

export function contextChunk(n: number, overrides: Partial<ContextChunk> = {}): ContextChunk {
  return {
    n,
    chunkId: 150 + n,
    chunkNo: 30 + n,
    pageNumber: 18 + n,
    heading: `Başlık ${n}`,
    sourceId: 10001,
    sourceName: GUIDE_NAME,
    version: '2026.10',
    vectorRank: n,
    distance: 0.3124,
    rerankScore: 5.359,
    ...overrides,
  }
}

export function lostCardChunks(): ContextChunk[] {
  return [
    contextChunk(1, { heading: '2.16 Kayıp ya da çalıntı bildirimi', vectorRank: 3 }),
    contextChunk(2, { distance: 0.341, rerankScore: 2.1 }),
    contextChunk(3, { distance: 0.352, rerankScore: -1.5 }),
    contextChunk(4, { distance: 0.36, rerankScore: -4 }),
    contextChunk(5, { distance: 0.37, rerankScore: -11.04 }),
  ]
}

export function sourcesEvent(overrides: Partial<SourcesEvent> = {}): SourcesEvent {
  return {
    retrievalStrategy: 'RERANK',
    candidates: 20,
    topK: 5,
    searchQuery: 'Kartımı kaybettim, ne yapmalıyım?',
    queryRewriteStatus: 'NO_HISTORY',
    contextChunks: lostCardChunks(),
    citations: [1],
    ...overrides,
  }
}

export function doneEvent(overrides: Partial<DoneEvent> = {}): DoneEvent {
  return {
    turnId: 12,
    notFoundAnswer: false,
    chatModel: 'qwen3.5:9b',
    promptTokens: 2154,
    completionTokens: 61,
    queryRewriteLatencyMs: null,
    retrievalLatencyMs: 84,
    rerankLatencyMs: 712,
    generationLatencyMs: 3920,
    timeToFirstTokenMs: 900,
    latencyMs: 4730,
    hostLabel: 'Apple M5 · 16 GB',
    ...overrides,
  }
}

export function token(text: string): SseEvent {
  return { event: 'token', data: { text } }
}

export function sources(overrides: Partial<SourcesEvent> = {}): SseEvent {
  return { event: 'sources', data: sourcesEvent(overrides) }
}

export function done(overrides: Partial<DoneEvent> = {}): SseEvent {
  return { event: 'done', data: doneEvent(overrides) }
}

export function streamError(errorCode: string): SseEvent {
  return { event: 'error', data: { errorCode } }
}

export function lostCardAnswer(): SseEvent[] {
  return [token('Kartını '), token('hemen kapat [1].'), sources(), done()]
}
