import { http, HttpResponse } from 'msw'
import type { LiveConfigResponse } from '../shared/liveConfig/types.ts'
import { server } from './server.ts'

export type LiveConfigLog = { requests: number }

export function serveLiveConfig(replies: (LiveConfigResponse | Response)[]): LiveConfigLog {
  const log: LiveConfigLog = { requests: 0 }
  server.use(
    http.get('/api/assistant/live-config', () => {
      const reply = replies[Math.min(log.requests, replies.length - 1)]
      log.requests += 1
      if (reply === undefined) {
        throw new Error('no live-config reply configured')
      }
      return reply instanceof Response ? reply : HttpResponse.json(reply)
    }),
  )
  return log
}
