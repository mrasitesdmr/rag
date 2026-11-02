import { describe, expect, it } from 'vitest'
import { normalizeSuggestionText } from './suggestionText.ts'

describe('normalizeSuggestionText', () => {
  it.each([
    ['  KARTIMI  kaybettim, ne yapmalıyım?', 'kartımı kaybettim ne yapmalıyım'],
    ['İŞLEM', 'işlem'],
    ['ISPARTA', 'ısparta'],
    ['Kart-aidatı!!', 'kart aidatı'],
    ['   ', ''],
  ])('givenTurkishText_whenNormalize_thenSameAsBackend(%j)', (input, expected) => {
    expect(normalizeSuggestionText(input)).toBe(expected)
  })
})
