import { http, HttpResponse } from 'msw'
import { chunkDetail } from './fixtures.ts'
import { sseResponse } from './sse.ts'
import type { SseEvent } from './sse.ts'
import { server } from './server.ts'

export type PreviewRequestLog = {
  conversationBodies: unknown[]
  messages: { conversationId: number; body: unknown; accept: string | null }[]
}

type MessageReply = (request: Request) => Response | Promise<Response>

export function servePreview(reply: SseEvent[] | MessageReply, conversationIds: number[] = [1, 2, 3]): PreviewRequestLog {
  const log: PreviewRequestLog = { conversationBodies: [], messages: [] }
  let created = 0
  server.use(
    http.post('/api/assistant/conversations', async ({ request }) => {
      log.conversationBodies.push(await request.json())
      const conversationId = conversationIds[Math.min(created, conversationIds.length - 1)] ?? 1
      created += 1
      return HttpResponse.json(
        { conversationId, channel: 'PREVIEW', createdAt: '2026-11-01T10:00:00Z' },
        { status: 201, headers: { Location: `/api/assistant/conversations/${conversationId}` } },
      )
    }),
    http.post('/api/assistant/conversations/:conversationId/messages', async ({ request, params }) => {
      log.messages.push({
        conversationId: Number(params.conversationId),
        body: await request.clone().json(),
        accept: request.headers.get('Accept'),
      })
      return typeof reply === 'function' ? reply(request) : sseResponse(reply)
    }),
  )
  return log
}

export function serveChunkDetails() {
  const requested: number[] = []
  server.use(
    http.get('/api/workspaces/42/chunks/:chunkId', ({ params }) => {
      const chunkId = Number(params.chunkId)
      requested.push(chunkId)
      return HttpResponse.json(
        chunkId === 151
          ? chunkDetail()
          : chunkDetail({ chunkId, heading: `Parça ${chunkId} başlığı`, chunkNo: chunkId - 120 }),
      )
    }),
  )
  return requested
}
