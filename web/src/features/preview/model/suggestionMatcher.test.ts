import { describe, expect, it } from 'vitest'
import { ANNUAL_FEE, FORGOT_PASSWORD, LOST_CARD, MINIMUM_PAYMENT, suggestionSet } from '../../../test/suggestionFixtures.ts'
import { createSuggestionMatcher } from './suggestionMatcher.ts'
import { listQuestionTexts } from './suggestions.ts'

describe('createSuggestionMatcher', () => {
  const matcher = createSuggestionMatcher(listQuestionTexts(suggestionSet()))

  it('givenRareAndCommonStem_whenScore_thenRareStemWeighsMore', () => {
    // ln(5/2) + ln(5/4) for the question with both stems, ln(5/4) for the ones with "kart" only
    expect(matcher.score('şifre kart', FORGOT_PASSWORD)).toBeCloseTo(1.139, 3)
    expect(matcher.score('şifre kart', LOST_CARD)).toBeCloseTo(0.223, 3)
    expect(matcher.score('şifre kart', ANNUAL_FEE)).toBeCloseTo(0.223, 3)
    expect(matcher.score('şifre kart', MINIMUM_PAYMENT)).toBe(0)
  })

  it.each(['asgari', 'ASGARİ', 'asgarisi'])('givenCaseOrSuffix_whenScore_thenOnlyMinimumPaymentMatches(%s)', (input) => {
    expect(matcher.score(input, MINIMUM_PAYMENT)).toBeGreaterThan(0)
    expect(matcher.score(input, LOST_CARD)).toBe(0)
    expect(matcher.score(input, FORGOT_PASSWORD)).toBe(0)
    expect(matcher.score(input, ANNUAL_FEE)).toBe(0)
  })

  it('givenOnlyStopWords_whenScore_thenZeroForEveryQuestion', () => {
    for (const text of listQuestionTexts(suggestionSet())) {
      expect(matcher.score('ne kaç', text)).toBe(0)
    }
  })

  it('givenThreeLetterPrefix_whenScore_thenOnlyPrefixBonusOnMatchingStarts', () => {
    expect(matcher.score('kar', LOST_CARD)).toBe(3)
    expect(matcher.score('kar', FORGOT_PASSWORD)).toBe(3)
    expect(matcher.score('kar', MINIMUM_PAYMENT)).toBe(0)
  })

  it('givenTwoLetterNonStopWord_whenScore_thenTooShortToMatch', () => {
    expect(matcher.score('ka', ANNUAL_FEE)).toBe(3)
    expect(matcher.score('ka', MINIMUM_PAYMENT)).toBe(0)
  })

  it('givenBlankInput_whenScore_thenZero', () => {
    expect(matcher.score('   ', LOST_CARD)).toBe(0)
  })

  it('givenEmptyPool_whenScore_thenNoThrowAndFiniteScore', () => {
    expect(Number.isFinite(createSuggestionMatcher([]).score('kart', LOST_CARD))).toBe(true)
  })
})
