import { describe, expect, it } from 'vitest'
import {
  ANNUAL_FEE,
  FOLLOW_UP_FREEZE,
  FOLLOW_UP_NEW_CARD,
  FORGOT_PASSWORD,
  LOST_CARD,
  MINIMUM_PAYMENT,
  suggestionSet,
  turn,
} from '../../../test/suggestionFixtures.ts'
import { createSuggestionMatcher } from './suggestionMatcher.ts'
import { autocompleteItems, followUpsFor, listQuestionTexts, suggestionChips } from './suggestions.ts'
import type { SuggestionSetResponse } from './types.ts'

const q = (text: string) => ({ text, kind: 'question' as const })
const f = (text: string) => ({ text, kind: 'followUp' as const })

describe('suggestionChips', () => {
  const set = suggestionSet()

  it.each([
    ['noTurns_twoStarters', [], [q(LOST_CARD), q(MINIMUM_PAYMENT)]],
    ['lastAnsweredListQuestion_followUpFirst', [turn(LOST_CARD)], [f(FOLLOW_UP_NEW_CARD), q(MINIMUM_PAYMENT)]],
    [
      'lastTurnIsFollowUp_noFollowUpAndNextNonStarter',
      [turn(LOST_CARD), turn(FOLLOW_UP_NEW_CARD)],
      [q(MINIMUM_PAYMENT), q(FORGOT_PASSWORD)],
    ],
    [
      'askedFollowUpSkipped',
      [turn(LOST_CARD, 'done', 'a'), turn(FOLLOW_UP_NEW_CARD, 'done', 'b'), turn(LOST_CARD, 'done', 'c')],
      [f(FOLLOW_UP_FREEZE), q(MINIMUM_PAYMENT)],
    ],
    ['lastTurnFailed_noFollowUp', [turn(LOST_CARD, 'failed')], [q(MINIMUM_PAYMENT), q(FORGOT_PASSWORD)]],
    ['lastTurnStreaming_noFollowUp', [turn(LOST_CARD, 'streaming')], [q(MINIMUM_PAYMENT), q(FORGOT_PASSWORD)]],
    [
      'differentSpellingStillMatchesListQuestion',
      [turn('kartımı KAYBETTİM ne yapmalıyım')],
      [f(FOLLOW_UP_NEW_CARD), q(MINIMUM_PAYMENT)],
    ],
  ])('given%s', (_name, turns, expected) => {
    expect(suggestionChips(set, turns)).toEqual(expected)
  })

  it('givenEveryQuestionAsked_whenChips_thenEmpty', () => {
    const turns = listQuestionTexts(set).map((text) => turn(text))

    expect(suggestionChips(set, turns)).toEqual([])
  })

  it('givenNoStarters_whenChips_thenFirstQuestionsInListOrder', () => {
    const noStarters: SuggestionSetResponse = {
      ...set,
      categories: set.categories.map((c) => ({ ...c, questions: c.questions.map((x) => ({ ...x, starter: false })) })),
    }
    expect(suggestionChips(noStarters, [])).toEqual([q(LOST_CARD), q(FORGOT_PASSWORD)])
  })
})

describe('followUpsFor', () => {
  it('givenTurnInProgress_whenFollowUps_thenEmpty', () => {
    expect(followUpsFor(suggestionSet(), [turn(LOST_CARD, 'waiting')])).toEqual([])
  })

  it('givenUnlistedLastQuestion_whenFollowUps_thenEmpty', () => {
    expect(followUpsFor(suggestionSet(), [turn('Başka bir soru')])).toEqual([])
  })
})

describe('autocompleteItems', () => {
  function kredit(count: number): SuggestionSetResponse {
    const questions = Array.from({ length: count }, (_, i) => ({
      text: `Kredi sorusu ${i}`,
      starter: false,
      followUps: [],
    }))
    return { questionCount: count, followUpCount: 0, categories: [{ name: 'Krediler', questions }] }
  }

  it('givenTenMatchingQuestions_whenAutocomplete_thenMaxSevenInListOrder', () => {
    const set = kredit(10)
    const matcher = createSuggestionMatcher(listQuestionTexts(set))

    const items = autocompleteItems(set, [], 'kredi', matcher)

    expect(items.map((item) => item.text)).toEqual(Array.from({ length: 7 }, (_, i) => `Kredi sorusu ${i}`))
  })

  it('givenAnsweredLostCard_whenTypingKart_thenFollowUpFirstThenQuestions', () => {
    const set = suggestionSet()
    const matcher = createSuggestionMatcher(listQuestionTexts(set))

    const items = autocompleteItems(set, [turn(LOST_CARD)], 'kart', matcher)

    expect(items[0]).toEqual(f(FOLLOW_UP_FREEZE))
    expect(items.slice(1).every((item) => item.kind === 'question')).toBe(true)
    expect(items.map((item) => item.text)).toContain(LOST_CARD)
    expect(items.map((item) => item.text)).not.toContain(FOLLOW_UP_NEW_CARD)
  })

  it('givenAskedQuestion_whenTypingItsWords_thenStillListed', () => {
    const set = suggestionSet()
    const matcher = createSuggestionMatcher(listQuestionTexts(set))

    const items = autocompleteItems(set, [turn(MINIMUM_PAYMENT)], 'asgari', matcher)

    expect(items).toEqual([q(MINIMUM_PAYMENT)])
  })

  it('givenHigherScoreLater_whenAutocomplete_thenSortedByScoreDescending', () => {
    const set = suggestionSet()
    const matcher = createSuggestionMatcher(listQuestionTexts(set))

    const items = autocompleteItems(set, [], 'şifre kart', matcher)

    expect(items.map((item) => item.text)).toEqual([FORGOT_PASSWORD, LOST_CARD, ANNUAL_FEE])
  })

  it.each(['', '   ', '!!!'])('givenBlankInput_whenAutocomplete_thenEmpty(%j)', (input) => {
    const set = suggestionSet()
    expect(autocompleteItems(set, [], input, createSuggestionMatcher(listQuestionTexts(set)))).toEqual([])
  })

  it('givenNoMatch_whenAutocomplete_thenEmpty', () => {
    const set = suggestionSet()
    expect(autocompleteItems(set, [], 'döviz', createSuggestionMatcher(listQuestionTexts(set)))).toEqual([])
  })
})
