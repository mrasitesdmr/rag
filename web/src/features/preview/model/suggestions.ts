import type { SuggestionMatcher } from './suggestionMatcher.ts'
import { normalizeSuggestionText } from './suggestionText.ts'
import type { PreviewTurn, SuggestedQuestionResponse, SuggestionItem, SuggestionSetResponse } from './types.ts'

export const CHIP_MAX_COUNT = 2
export const AUTOCOMPLETE_MAX_ITEMS = 7

export function listQuestionTexts(set: SuggestionSetResponse): string[] {
  return listQuestions(set).map((question) => question.text)
}

export function followUpsFor(set: SuggestionSetResponse, turns: PreviewTurn[]): string[] {
  const lastTurn = turns.at(-1)
  if (lastTurn === undefined || lastTurn.phase !== 'done') {
    return []
  }
  const lastQuestion = normalizeSuggestionText(lastTurn.question)
  const match = listQuestions(set).find((question) => normalizeSuggestionText(question.text) === lastQuestion)
  return match?.followUps ?? []
}

export function suggestionChips(set: SuggestionSetResponse, turns: PreviewTurn[]): SuggestionItem[] {
  const asked = new Set(turns.map((turn) => normalizeSuggestionText(turn.question)))
  const isUnasked = (text: string) => !asked.has(normalizeSuggestionText(text))
  const chips: SuggestionItem[] = []
  const followUp = followUpsFor(set, turns).find(isUnasked)
  if (followUp !== undefined) {
    chips.push({ text: followUp, kind: 'followUp' })
  }
  const questions = listQuestions(set).filter((question) => isUnasked(question.text))
  const ordered = [
    ...questions.filter((question) => question.starter),
    ...questions.filter((question) => !question.starter),
  ]
  for (const question of ordered) {
    if (chips.length >= CHIP_MAX_COUNT) {
      break
    }
    chips.push({ text: question.text, kind: 'question' })
  }
  return chips.slice(0, CHIP_MAX_COUNT)
}

export function autocompleteItems(
  set: SuggestionSetResponse,
  turns: PreviewTurn[],
  input: string,
  matcher: SuggestionMatcher,
): SuggestionItem[] {
  if (normalizeSuggestionText(input) === '') {
    return []
  }
  const followUps = followUpsFor(set, turns).filter((text) => matcher.score(input, text) > 0)
  const followUpKeys = new Set(followUps.map(normalizeSuggestionText))
  const questions = listQuestionTexts(set)
    .map((text, index) => ({ text, index, score: matcher.score(input, text) }))
    .filter((candidate) => candidate.score > 0 && !followUpKeys.has(normalizeSuggestionText(candidate.text)))
    .sort((left, right) => right.score - left.score || left.index - right.index)
  const items: SuggestionItem[] = [
    ...followUps.map((text): SuggestionItem => ({ text, kind: 'followUp' })),
    ...questions.map((candidate): SuggestionItem => ({ text: candidate.text, kind: 'question' })),
  ]
  return items.slice(0, AUTOCOMPLETE_MAX_ITEMS)
}

export function autocompleteOptionId(listId: string, index: number): string {
  return `${listId}-option-${index}`
}

function listQuestions(set: SuggestionSetResponse): SuggestedQuestionResponse[] {
  return set.categories.flatMap((category) => category.questions)
}
