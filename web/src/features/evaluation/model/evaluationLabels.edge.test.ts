import { describe, expect, it } from 'vitest'
import { evalRun } from '../../../test/evaluationFixtures.ts'
import { recallSubtitle, retrievalSubtitle, runFailureText } from './evaluationLabels.ts'

describe('evaluation subtitles', () => {
  it('givenNoUnmatchedQuestions_whenRecallSubtitle_thenNoSuffix', () => {
    expect(recallSubtitle(evalRun({ hitCount: 98, measuredCount: 100, unmatchedCount: 0 }))).toBe('98/100 soru')
  })

  it('givenUnmatchedQuestions_whenRecallSubtitle_thenSuffixWithCount', () => {
    expect(recallSubtitle(evalRun({ hitCount: 90, measuredCount: 95, unmatchedCount: 5 }))).toBe(
      '90/95 soru · 5 soru ölçülemedi',
    )
  })

  it('givenNoFallback_whenRetrievalSubtitle_thenGuideVersionOnly', () => {
    expect(retrievalSubtitle(evalRun({ sourceVersions: '2026.10', rerankFallbackCount: 0 }))).toBe('rehber 2026.10')
  })

  it('givenFallback_whenRetrievalSubtitle_thenRerankerDownCount', () => {
    expect(retrievalSubtitle(evalRun({ sourceVersions: '2026.10', rerankFallbackCount: 3 }))).toBe(
      'rehber 2026.10 · reranker DOWN: 3 soru',
    )
  })
})

describe('runFailureText', () => {
  it('givenNoErrorCode_whenRunFailureText_thenTreatedAsInternalError', () => {
    expect(runFailureText(null)).toBe('Son ölçüm tamamlanamadı: beklenmeyen bir hata oluştu.')
  })

  it('givenInterruptedRun_whenRunFailureText_thenRestartReason', () => {
    expect(runFailureText('EVAL_RUN_INTERRUPTED')).toBe('Son ölçüm tamamlanamadı: servis ölçüm sırasında yeniden başladı.')
  })
})
