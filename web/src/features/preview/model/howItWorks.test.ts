import { describe, expect, it } from 'vitest'
import { liveConfig } from '../../../test/fixtures.ts'
import { howItWorksText } from './howItWorks.ts'

const NOTE = 'Cevaptaki numaralar rehberin parçalarını gösterir.'

describe('howItWorksText', () => {
  it.each([
    [
      'rerank',
      {},
      `Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. ${NOTE}`,
    ],
    [
      'rerank with unknown reranker status',
      { rerankerStatus: 'UNKNOWN' },
      `Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. ${NOTE}`,
    ],
    [
      'top-k',
      { retrievalStrategy: 'TOP_K', rerankerStatus: 'NOT_USED' },
      `Soru önce rehberde aranır (bge-m3 + pgvector), en yakın 5 parça qwen3.5:9b modeline gider. ${NOTE}`,
    ],
    [
      'rerank with reranker down',
      { rerankerStatus: 'DOWN' },
      `Soru önce rehberde aranır (bge-m3 + pgvector), en yakın 5 parça qwen3.5:9b modeline gider. ${NOTE}`,
    ],
    [
      'llm only',
      { generationMode: 'LLM_ONLY', rerankerStatus: 'NOT_USED' },
      'Soru önce rehberde aranır (bge-m3 + pgvector), bu modda arama kapalı; soru doğrudan modele gider.',
    ],
    [
      'other numbers and model',
      { candidates: 30, topK: 3, chatModel: 'llama3:8b' },
      `Soru önce rehberde aranır (bge-m3 + pgvector), 30 aday reranker ile puanlanıp en iyi 3 parça llama3:8b modeline gider. ${NOTE}`,
    ],
    [
      'top-k with other numbers and model',
      { retrievalStrategy: 'TOP_K', topK: 8, chatModel: 'llama3:8b', rerankerStatus: 'NOT_USED' },
      `Soru önce rehberde aranır (bge-m3 + pgvector), en yakın 8 parça llama3:8b modeline gider. ${NOTE}`,
    ],
    [
      'embedding unknown',
      { embeddingModel: null, embeddingDimensions: null },
      `Soru önce rehberde aranır (pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. ${NOTE}`,
    ],
    [
      'llm only with other embedding',
      { generationMode: 'LLM_ONLY', rerankerStatus: 'NOT_USED', embeddingModel: 'nomic-embed' },
      'Soru önce rehberde aranır (nomic-embed + pgvector), bu modda arama kapalı; soru doğrudan modele gider.',
    ],
  ] as const)('givenConfig_whenHowItWorksText_thenVariantFromConfig %s', (_name, overrides, expected) => {
    expect(howItWorksText(liveConfig(overrides))).toBe(expected)
  })
})
