import { http, HttpResponse } from 'msw'
import type { PreviewTurn, SuggestionSetResponse, TurnPhase } from '../features/preview/model/types.ts'
import { server } from './server.ts'

export const LOST_CARD = 'Kartımı kaybettim, ne yapmalıyım?'
export const FOLLOW_UP_NEW_CARD = 'Yenisi kaç günde gelir?'
export const FOLLOW_UP_FREEZE = 'Kartımı nasıl dondururum?'
export const FORGOT_PASSWORD = 'Kart şifremi unuttum'
export const ANNUAL_FEE = 'Kart aidatı ne kadar?'
export const MINIMUM_PAYMENT = 'Asgari ödeme tutarı nedir?'

export function suggestionSet(): SuggestionSetResponse {
  return {
    questionCount: 4,
    followUpCount: 2,
    categories: [
      {
        name: 'Kartlar',
        questions: [
          { text: LOST_CARD, starter: true, followUps: [FOLLOW_UP_NEW_CARD, FOLLOW_UP_FREEZE] },
          { text: FORGOT_PASSWORD, starter: false, followUps: [] },
          { text: ANNUAL_FEE, starter: false, followUps: [] },
        ],
      },
      { name: 'Krediler', questions: [{ text: MINIMUM_PAYMENT, starter: true, followUps: [] }] },
    ],
  }
}

export function emptySuggestionSet(): SuggestionSetResponse {
  return { questionCount: 0, followUpCount: 0, categories: [] }
}

export function serveSuggestions(set: SuggestionSetResponse): void {
  server.use(http.get('/api/assistant/suggestions', () => HttpResponse.json(set)))
}

export function turn(question: string, phase: TurnPhase = 'done', localId = question): PreviewTurn {
  return {
    localId,
    question,
    askedAt: '2026-11-01T10:00:00Z',
    answer: 'Cevap',
    phase,
    sources: null,
    done: null,
    errorCode: phase === 'failed' ? 'CHAT_MODEL_UNAVAILABLE' : null,
  }
}
