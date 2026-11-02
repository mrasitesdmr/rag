import { describe, expect, it } from 'vitest'
import { doneEvent, sourcesEvent } from '../../../test/previewFixtures.ts'
import { canSendMessage, normalizeMessage } from './messageRules.ts'
import { latencyRows } from './latencyRows.ts'
import { queryRewriteLabel, strategyLabel } from './strategyLabels.ts'

describe('strategyLabel', () => {
  it.each([
    ['TOP_K', 'top-5'],
    ['RERANK', 'rerank 20→5'],
    ['RERANK_FALLBACK', 'reranker DOWN · vector sırası'],
  ] as const)('givenStrategy_whenStrategyLabel_thenLabText %s', (retrievalStrategy, expected) => {
    expect(strategyLabel(sourcesEvent({ retrievalStrategy }))).toBe(expected)
  })

  it('givenOtherCounts_whenStrategyLabel_thenUsesEventNumbers', () => {
    expect(strategyLabel(sourcesEvent({ retrievalStrategy: 'RERANK', candidates: 30, topK: 3 }))).toBe('rerank 30→3')
  })
})

describe('queryRewriteLabel', () => {
  it.each([
    ['NO_HISTORY', null],
    ['DISABLED', null],
    ['FAILED', 'yeniden yazma başarısız'],
    ['EMPTY', 'yeniden yazma başarısız'],
    ['REWRITTEN', 'arama: "Kaybolan kartın yenisi kaç günde gelir?"'],
  ] as const)('givenStatus_whenQueryRewriteLabel_thenLabelOnlyForRewrittenAndFailures %s', (queryRewriteStatus, expected) => {
    const sources = sourcesEvent({
      queryRewriteStatus,
      searchQuery: 'Kaybolan kartın yenisi kaç günde gelir?',
    })

    expect(queryRewriteLabel(sources)).toBe(expected)
  })

  it('givenSearchQueryOf45Characters_whenQueryRewriteLabel_thenShortenedTo39PlusEllipsis', () => {
    const searchQuery = 'a'.repeat(45)

    const label = queryRewriteLabel(sourcesEvent({ queryRewriteStatus: 'REWRITTEN', searchQuery }))

    expect(label).toBe(`arama: "${'a'.repeat(39)}…"`)
  })

  it('givenSearchQueryOfExactly40Characters_whenQueryRewriteLabel_thenNotShortened', () => {
    const searchQuery = 'b'.repeat(40)

    expect(queryRewriteLabel(sourcesEvent({ queryRewriteStatus: 'REWRITTEN', searchQuery }))).toBe(
      `arama: "${searchQuery}"`,
    )
  })
})

describe('latencyRows', () => {
  it('givenRewriteLatency_whenLatencyRows_thenRewriteRowFirst', () => {
    const rows = latencyRows(doneEvent({ queryRewriteLatencyMs: 812 }))

    expect(rows).toEqual([
      { label: 'yeniden yazma', ms: 812, isModel: false },
      { label: 'retrieval-api araması', ms: 84, isModel: false },
      { label: 'rerank', ms: 712, isModel: false },
      { label: 'qwen3.5 cevabı', ms: 3920, isModel: true },
    ])
  })

  it('givenNullRewriteLatency_whenLatencyRows_thenNoRewriteRow', () => {
    const rows = latencyRows(doneEvent({ queryRewriteLatencyMs: null }))

    expect(rows.map((row) => row.label)).not.toContain('yeniden yazma')
  })

  it('givenZeroRewriteLatency_whenLatencyRows_thenRewriteRowStillShown', () => {
    expect(latencyRows(doneEvent({ queryRewriteLatencyMs: 0 }))[0]).toEqual({
      label: 'yeniden yazma',
      ms: 0,
      isModel: false,
    })
  })

  it('givenRerankDone_whenLatencyRows_thenThreeRowsWithModelNameWithoutTag', () => {
    expect(latencyRows(doneEvent())).toEqual([
      { label: 'retrieval-api araması', ms: 84, isModel: false },
      { label: 'rerank', ms: 712, isModel: false },
      { label: 'qwen3.5 cevabı', ms: 3920, isModel: true },
    ])
  })

  it('givenTopKDone_whenLatencyRows_thenNoRerankRow', () => {
    const rows = latencyRows(doneEvent({ rerankLatencyMs: null }))

    expect(rows.map((row) => row.label)).toEqual(['retrieval-api araması', 'qwen3.5 cevabı'])
  })

  it('givenNoGeneration_whenLatencyRows_thenNoModelRow', () => {
    const rows = latencyRows(doneEvent({ generationLatencyMs: null, rerankLatencyMs: null }))

    expect(rows).toEqual([{ label: 'retrieval-api araması', ms: 84, isModel: false }])
  })

  it('givenFallbackWithFailedRerankAttempt_whenLatencyRows_thenRerankRowShowsSpentTime', () => {
    const rows = latencyRows(doneEvent({ rerankLatencyMs: 5003 }))

    expect(rows[1]).toEqual({ label: 'rerank', ms: 5003, isModel: false })
  })

  it('givenModelNameWithoutTag_whenLatencyRows_thenFullName', () => {
    expect(latencyRows(doneEvent({ chatModel: 'llama' }))[2]?.label).toBe('llama cevabı')
  })
})

describe('message rules', () => {
  it.each([
    ['', false],
    ['   ', false],
    ['\n\t ', false],
    ['a', true],
    ['  Kartımı kaybettim  ', true],
    ['a'.repeat(1000), true],
    ['a'.repeat(1001), false],
    [` ${'a'.repeat(1000)} `, true],
  ])('givenInput_whenCanSendMessage_thenAcceptsOneToThousandAfterTrim %#', (input, expected) => {
    expect(canSendMessage(input)).toBe(expected)
  })

  it('givenPaddedInput_whenNormalizeMessage_thenTrimmedAndTurkishCharactersKept', () => {
    expect(normalizeMessage('  Kartımı kaybettim, ne yapmalıyım?  ')).toBe('Kartımı kaybettim, ne yapmalıyım?')
  })
})
