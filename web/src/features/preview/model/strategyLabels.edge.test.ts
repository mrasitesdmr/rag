import { describe, expect, it } from 'vitest'
import { sourcesEvent } from '../../../test/previewFixtures.ts'
import { strategyLabel } from './strategyLabels.ts'

describe('strategyLabel with missing numbers', () => {
  it('givenTopKStrategyWithoutTopK_whenLabelled_thenGenericTopK', () => {
    expect(strategyLabel(sourcesEvent({ retrievalStrategy: 'TOP_K', topK: null }))).toBe('top-k')
  })

  it.each([
    [null, 5],
    [20, null],
    [null, null],
  ])('givenRerankWithCandidates%sAndTopK%s_whenLabelled_thenPlainRerank', (candidates, topK) => {
    expect(strategyLabel(sourcesEvent({ retrievalStrategy: 'RERANK', candidates, topK }))).toBe('rerank')
  })
})
