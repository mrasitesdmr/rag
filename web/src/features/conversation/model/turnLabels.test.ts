import { describe, expect, it } from 'vitest'
import { followUpTurn, promptChunks, turn, turnChunk } from '../../../test/conversationFixtures.ts'
import {
  failureReasonLabel,
  generationLabel,
  guideVersionLabel,
  hasDistinctSearchQuery,
} from './turnLabels.ts'

describe('turnLabels', () => {
  it.each([
    [{ generationMode: 'LLM_ONLY', retrievalStrategy: 'NONE', candidates: null, topK: null }, 'Sadece LLM'],
    [{ retrievalStrategy: 'TOP_K', candidates: 5, topK: 5 }, 'RAG · top-5'],
    [{ retrievalStrategy: 'RERANK', candidates: 20, topK: 5 }, 'RAG · rerank 20→5'],
    [{ retrievalStrategy: 'RERANK_FALLBACK', candidates: 20, topK: 5 }, 'RAG · reranker DOWN'],
  ] as const)('givenGenerationSettings_whenLabelled_thenLabText %#', (overrides, label) => {
    expect(generationLabel(turn(overrides))).toBe(label)
  })

  it('givenChunksFromTwoVersions_whenGuideVersionLabelled_thenDistinctVersionsJoined', () => {
    const chunks = [turnChunk(1, { version: '2026.10' }), turnChunk(2, { version: '2026.09' }), turnChunk(3)]

    expect(guideVersionLabel(turn({ contextChunks: chunks }))).toBe('2026.10, 2026.09')
  })

  it('givenNoChunks_whenGuideVersionLabelled_thenNull', () => {
    expect(guideVersionLabel(turn({ contextChunks: [] }))).toBeNull()
    expect(guideVersionLabel(turn({ contextChunks: promptChunks() }))).toBe('2026.10')
  })

  it.each([
    ['CHAT_MODEL_UNAVAILABLE', 'dil modeline ulaşılamadı'],
    ['RETRIEVAL_UNAVAILABLE', 'bilgi bankasına ulaşılamadı'],
    ['STREAM_CANCELLED', 'cevap yarıda kesildi'],
    ['INTERNAL_ERROR', 'beklenmeyen hata'],
  ] as const)('givenErrorCode_whenFailureReasonLabelled_thenTurkishReason %s', (code, label) => {
    expect(failureReasonLabel(code)).toBe(label)
  })

  it('givenSearchQuery_whenCompared_thenDistinctOnlyWhenDifferentAndPresent', () => {
    expect(hasDistinctSearchQuery(followUpTurn())).toBe(true)
    expect(hasDistinctSearchQuery(turn())).toBe(false)
    expect(hasDistinctSearchQuery(turn({ searchQuery: null }))).toBe(false)
  })
})
