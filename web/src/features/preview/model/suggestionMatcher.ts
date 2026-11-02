import { normalizeSuggestionText } from './suggestionText.ts'

export const STEM_LENGTH = 4
export const MIN_TOKEN_LENGTH = 3
export const PREFIX_BONUS = 3
export const STOP_WORDS: ReadonlySet<string> = new Set([
  'ne',
  'mi',
  'mı',
  'mu',
  'mü',
  've',
  'ile',
  'bir',
  'için',
  'gibi',
  'kaç',
  'nasıl',
  'neden',
  'var',
  'yok',
  'olur',
  'ben',
  'bana',
  'benim',
  'miyim',
  'mıyım',
  'muyum',
  'müyüm',
  'ama',
  'daha',
  'çok',
  'şu',
  'bu',
  'o',
])

export type SuggestionMatcher = { score: (input: string, text: string) => number }

export function createSuggestionMatcher(pool: string[]): SuggestionMatcher {
  const documentFrequency = new Map<string, number>()
  for (const text of pool) {
    for (const stem of stemsOf(normalizeSuggestionText(text))) {
      documentFrequency.set(stem, (documentFrequency.get(stem) ?? 0) + 1)
    }
  }

  function inverseDocumentFrequency(stem: string): number {
    return Math.log((pool.length + 1) / ((documentFrequency.get(stem) ?? 0) + 1))
  }

  function score(input: string, text: string): number {
    const normalizedInput = normalizeSuggestionText(input)
    if (normalizedInput === '') {
      return 0
    }
    const inputStems = new Set(
      normalizedInput
        .split(' ')
        .filter((word) => word.length >= MIN_TOKEN_LENGTH && !STOP_WORDS.has(word))
        .map(stemOf),
    )
    const normalizedText = normalizeSuggestionText(text)
    const textStems = stemsOf(normalizedText)
    let total = 0
    for (const stem of inputStems) {
      if (textStems.has(stem)) {
        total += inverseDocumentFrequency(stem)
      }
    }
    return normalizedText.startsWith(normalizedInput) ? total + PREFIX_BONUS : total
  }

  return { score }
}

function stemOf(word: string): string {
  return Array.from(word).slice(0, STEM_LENGTH).join('')
}

function stemsOf(normalizedText: string): Set<string> {
  if (normalizedText === '') {
    return new Set()
  }
  return new Set(normalizedText.split(' ').map(stemOf))
}
