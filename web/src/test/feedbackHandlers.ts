import { http, HttpResponse } from 'msw'
import { server } from './server.ts'

export type FeedbackRequestLog = {
  feedback: { turnId: number; body: unknown }[]
  handoff: { conversationId: number; body: unknown }[]
}

type Reply = (request: Request) => Response | Promise<Response>

type FeedbackHandlerOptions = { feedbackReply?: Reply; handoffReply?: Reply }

export function handoffResponse(overrides: Record<string, unknown> = {}) {
  return HttpResponse.json(
    { conversationId: 1, turnId: 12, reason: 'NO_ANSWER', queue: 'bireysel', createdAt: '2026-11-02T10:31:05Z', ...overrides },
    { status: 202 },
  )
}

export function serveFeedback(options: FeedbackHandlerOptions = {}): FeedbackRequestLog {
  const log: FeedbackRequestLog = { feedback: [], handoff: [] }
  server.use(
    http.post('/api/assistant/turns/:turnId/feedback', async ({ request, params }) => {
      log.feedback.push({ turnId: Number(params.turnId), body: await request.clone().json() })
      return options.feedbackReply ? options.feedbackReply(request) : new HttpResponse(null, { status: 204 })
    }),
    http.post('/api/assistant/conversations/:conversationId/handoff', async ({ request, params }) => {
      log.handoff.push({ conversationId: Number(params.conversationId), body: await request.clone().json() })
      return options.handoffReply ? options.handoffReply(request) : handoffResponse()
    }),
  )
  return log
}
