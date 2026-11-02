import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { conversationDetail } from './conversationFixtures.ts'
import { liveConfig, pageOf, workspace } from './fixtures.ts'

type TestViolation = { field: string; message: string }

export function problem(status: number, errorCode: string, violations?: TestViolation[]) {
  return HttpResponse.json(
    { type: 'about:blank', title: errorCode, status, errorCode, ...(violations ? { violations } : {}) },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export const defaultHandlers = [
  http.get('/api/assistant/live-config', () => HttpResponse.json(liveConfig())),
  http.get('/api/assistant/suggestions', () => HttpResponse.json({ questionCount: 0, followUpCount: 0, categories: [] })),
  http.get('/api/assistant/conversations', ({ request }) => {
    const size = Number(new URL(request.url).searchParams.get('size') ?? 20)
    return HttpResponse.json(pageOf([], { size }))
  }),
  http.get('/api/assistant/conversations/:conversationId', ({ params }) =>
    HttpResponse.json(conversationDetail({ conversationId: Number(params.conversationId) })),
  ),
  http.get('/api/workspaces/42', () => HttpResponse.json(workspace())),
  http.get('/api/workspaces/42/sources', () => HttpResponse.json(pageOf([]))),
]

export const server = setupServer(...defaultHandlers)
