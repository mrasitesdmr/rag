import type { PreviewNavigationState } from './types.ts'

export function previewNavigationStateOf(state: unknown): PreviewNavigationState | null {
  if (typeof state !== 'object' || state === null || !('question' in state)) {
    return null
  }
  const question: unknown = state.question
  return typeof question === 'string' ? { question } : null
}
