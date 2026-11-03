import { describe, expect, it } from 'vitest'
import { createSuggestionMatcher } from './suggestionMatcher.ts'
import { autocompleteItems, autocompleteOptionId, suggestionChips } from './suggestions.ts'
import type { SuggestionSetResponse } from './types.ts'

function setOf(...questions: Array<[string, boolean]>): SuggestionSetResponse {
  return {
    questionCount: questions.length,
    followUpCount: 0,
    categories: [{ name: 'Kartlar', questions: questions.map(([text, starter]) => ({ text, starter, followUps: [] })) }],
  }
}

describe('suggestionChips limit and order', () => {
  it('givenThreeStarters_whenChips_thenOnlyFirstTwo', () => {
    const set = setOf(['Birinci soru', true], ['İkinci soru', true], ['Üçüncü soru', true])

    expect(suggestionChips(set, []).map((chip) => chip.text)).toEqual(['Birinci soru', 'İkinci soru'])
  })

  it('givenNonStarterBeforeStarter_whenChips_thenStarterFirst', () => {
    const set = setOf(['Sıradan soru', false], ['Başlangıç sorusu', true])

    expect(suggestionChips(set, []).map((chip) => chip.text)).toEqual(['Başlangıç sorusu', 'Sıradan soru'])
  })
})

describe('autocompleteItems ordering', () => {
  it('givenEqualScores_whenAutocomplete_thenListOrderKept', () => {
    const set = setOf(['Kart aidatı nedir', false], ['Kart limiti nedir', false])
    const matcher = createSuggestionMatcher(['Kart aidatı nedir', 'Kart limiti nedir', 'Hesap açmak'])

    expect(autocompleteItems(set, [], 'kart', matcher).map((item) => item.text)).toEqual([
      'Kart aidatı nedir',
      'Kart limiti nedir',
    ])
  })

  it('givenListId_whenOptionId_thenIdJoinsListAndIndex', () => {
    expect(autocompleteOptionId('lst', 3)).toBe('lst-option-3')
  })
})
