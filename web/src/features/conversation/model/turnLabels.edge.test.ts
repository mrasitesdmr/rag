import { describe, expect, it } from 'vitest'
import { turn } from '../../../test/conversationFixtures.ts'
import { generationLabel } from './turnLabels.ts'

describe('generationLabel without numbers', () => {
  it('givenRagWithoutRetrieval_whenLabelled_thenPlainRag', () => {
    expect(generationLabel(turn({ generationMode: 'RAG', retrievalStrategy: 'NONE', candidates: null, topK: null }))).toBe('RAG')
  })

  it('givenTopKWithoutNumber_whenLabelled_thenDanglingDash', () => {
    expect(generationLabel(turn({ generationMode: 'RAG', retrievalStrategy: 'TOP_K', candidates: null, topK: null }))).toBe('RAG · top-')
  })

  it('givenRerankWithoutNumbers_whenLabelled_thenArrowOnly', () => {
    expect(generationLabel(turn({ generationMode: 'RAG', retrievalStrategy: 'RERANK', candidates: null, topK: null }))).toBe('RAG · rerank →')
  })
})
