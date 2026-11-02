import { describe, expect, it } from 'vitest'
import { GUIDE_NAME, liveConfig } from '../../../test/fixtures.ts'
import type { LiveConfigResponse } from '../../../shared/liveConfig/types.ts'
import { liveConfigItems } from './liveConfigItems.ts'
import type { LiveConfigItem } from './liveConfigItems.ts'

function item(config: LiveConfigResponse, label: string): LiveConfigItem {
  const found = liveConfigItems(config).find((candidate) => candidate.label === label)
  if (found === undefined) {
    throw new Error(`missing item ${label}`)
  }
  return found
}

describe('liveConfigItems', () => {
  it('givenDefaultConfig_whenLiveConfigItems_thenFiveLabItemsInOrderAllNormal', () => {
    const items = liveConfigItems(liveConfig())

    expect(items).toEqual([
      { label: 'Model', value: 'qwen3.5:9b', tone: 'normal' },
      { label: 'Embedding', value: 'bge-m3', tone: 'normal' },
      { label: 'Retrieval', value: 'rerank 20→5', tone: 'normal' },
      { label: 'Soru yeniden yazma', value: 'açık', tone: 'normal' },
      { label: 'Bilgi kaynakları', value: 'Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10', tone: 'normal' },
    ])
  })

  it.each([
    ['LLM_ONLY mode', { generationMode: 'LLM_ONLY', rerankerStatus: 'NOT_USED' }, 'kapalı (Sadece LLM)', 'danger'],
    ['reranker down', { rerankerStatus: 'DOWN' }, 'reranker DOWN', 'warning'],
    ['reranker unknown', { rerankerStatus: 'UNKNOWN' }, 'rerank 20→5', 'normal'],
    ['top-k strategy', { retrievalStrategy: 'TOP_K', rerankerStatus: 'NOT_USED' }, 'top-5', 'normal'],
    ['top-k with other number', { retrievalStrategy: 'TOP_K', topK: 3, rerankerStatus: 'NOT_USED' }, 'top-3', 'normal'],
    ['other candidates', { candidates: 30, topK: 3 }, 'rerank 30→3', 'normal'],
    ['LLM_ONLY beats down', { generationMode: 'LLM_ONLY', rerankerStatus: 'DOWN' }, 'kapalı (Sadece LLM)', 'danger'],
    ['top-k beats down', { retrievalStrategy: 'TOP_K', rerankerStatus: 'DOWN' }, 'top-5', 'normal'],
  ] as const)('givenRetrievalSetup_whenLiveConfigItems_thenRetrievalValueAndTone %s', (_name, overrides, value, tone) => {
    const retrieval = item(liveConfig(overrides), 'Retrieval')

    expect(retrieval).toEqual({ label: 'Retrieval', value, tone })
  })

  it.each([
    [true, 'açık', 'normal'],
    [false, 'kapalı', 'warning'],
  ] as const)('givenQueryRewrite%s_whenLiveConfigItems_thenValueAndTone', (enabled, value, tone) => {
    expect(item(liveConfig({ queryRewriteEnabled: enabled }), 'Soru yeniden yazma')).toEqual({
      label: 'Soru yeniden yazma',
      value,
      tone,
    })
  })

  it('givenEmbeddingNull_whenLiveConfigItems_thenDashAndStillNormal', () => {
    expect(item(liveConfig({ embeddingModel: null, embeddingDimensions: null }), 'Embedding')).toEqual({
      label: 'Embedding',
      value: '—',
      tone: 'normal',
    })
  })

  it('givenOtherChatModel_whenLiveConfigItems_thenModelFromConfig', () => {
    expect(item(liveConfig({ chatModel: 'llama3:8b' }), 'Model').value).toBe('llama3:8b')
  })

  it.each([
    [
      'three documents',
      {
        status: 'READY',
        searchableSourceCount: 3,
        sources: [
          { sourceId: 10001, name: GUIDE_NAME, version: '2026.10' },
          { sourceId: 10002, name: 'İkinci', version: 'v1' },
          { sourceId: 10003, name: 'Üçüncü', version: 'v1' },
        ],
      },
      '3 belge',
      'normal',
    ],
    ['no documents', { status: 'EMPTY', searchableSourceCount: 0, sources: [] }, 'belge yok', 'warning'],
    ['retrieval-api down', { status: 'UNAVAILABLE', searchableSourceCount: null, sources: [] }, 'ulaşılamıyor', 'warning'],
    [
      'one document with 45 character name',
      {
        status: 'READY',
        searchableSourceCount: 1,
        sources: [{ sourceId: 10001, name: 'x'.repeat(45), version: '2026.10' }],
      },
      `${'x'.repeat(39)}… · 2026.10`,
      'normal',
    ],
    [
      'one document with exactly 40 character name',
      {
        status: 'READY',
        searchableSourceCount: 1,
        sources: [{ sourceId: 10001, name: 'y'.repeat(40), version: 'v2' }],
      },
      `${'y'.repeat(40)} · v2`,
      'normal',
    ],
  ] as const)('givenKnowledgeBase_whenLiveConfigItems_thenKnowledgeSourcesValueAndTone %s', (_name, knowledgeBase, value, tone) => {
    const knowledge = item(liveConfig({ knowledgeBase: { ...knowledgeBase, sources: [...knowledgeBase.sources] } }), 'Bilgi kaynakları')

    expect(knowledge).toEqual({ label: 'Bilgi kaynakları', value, tone })
  })

  it('givenRetrievalApiDown_whenLiveConfigItems_thenEmbeddingDashAndKnowledgeWarningButOtherValuesStay', () => {
    const items = liveConfigItems(
      liveConfig({
        embeddingModel: null,
        embeddingDimensions: null,
        knowledgeBase: { status: 'UNAVAILABLE', searchableSourceCount: null, sources: [] },
      }),
    )

    expect(items.map((entry) => `${entry.label} ${entry.value}`)).toEqual([
      'Model qwen3.5:9b',
      'Embedding —',
      'Retrieval rerank 20→5',
      'Soru yeniden yazma açık',
      'Bilgi kaynakları ulaşılamıyor',
    ])
  })
})
