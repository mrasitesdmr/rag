import type { PageParams } from '../api/pagination.ts'

const CONVERSATIONS_KEY = ['conversations'] as const

export const conversationKeys = {
  all: CONVERSATIONS_KEY,
  list: (params: PageParams) => [...CONVERSATIONS_KEY, 'list', params] as const,
  count: [...CONVERSATIONS_KEY, 'count'] as const,
  detail: (conversationId: number) => [...CONVERSATIONS_KEY, 'detail', conversationId] as const,
}
