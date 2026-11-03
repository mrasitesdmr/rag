import { describe, expect, it } from 'vitest'
import { createSuggestionMatcher } from './suggestionMatcher.ts'

const STOP_WORDS_OF_MIN_LENGTH = [
  'ile', 'bir', 'için', 'gibi', 'kaç', 'nasıl', 'neden', 'var', 'yok', 'olur', 'ben', 'bana', 'benim',
  'miyim', 'mıyım', 'muyum', 'müyüm', 'ama', 'daha', 'çok',
]

describe('createSuggestionMatcher stop words', () => {
  it.each(STOP_WORDS_OF_MIN_LENGTH)('givenStopWordInTextAndInput_whenScore_thenItAddsNothing(%s)', (word) => {
    const text = `kart ${word}`
    const matcher = createSuggestionMatcher([text, 'limit artırma', 'hesap kapatma'])

    expect(matcher.score(word, text)).toBe(0)
  })

  it('givenThreeLetterNonStopWord_whenScore_thenCounted', () => {
    const matcher = createSuggestionMatcher(['kart atm', 'limit artırma', 'hesap kapatma'])

    expect(matcher.score('atm', 'kart atm')).toBeCloseTo(Math.log(4 / 2), 6)
  })

  it('givenTwoLetterNonStopWord_whenScore_thenIgnored', () => {
    const matcher = createSuggestionMatcher(['kart hb', 'limit artırma', 'hesap kapatma'])

    expect(matcher.score('hb', 'kart hb')).toBe(0)
  })

  it('givenEmptyText_whenScore_thenZero', () => {
    const matcher = createSuggestionMatcher(['', 'kart'])

    expect(matcher.score('kart', '')).toBe(0)
  })
})
