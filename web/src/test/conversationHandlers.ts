import { http, HttpResponse } from 'msw'
import type { ConversationDetailResponse, ConversationSummaryResponse } from '../features/conversation/model/types.ts'
import { pageOf } from './fixtures.ts'
import { server } from './server.ts'

const LIST_PATH = '/api/assistant/conversations'

function summariesResponse(request: Request, rows: ConversationSummaryResponse[]) {
  const params = new URL(request.url).searchParams
  const size = Number(params.get('size') ?? 20)
  const page = Number(params.get('page') ?? 0)
  return HttpResponse.json(pageOf(rows.slice(page * size, (page + 1) * size), { page, size, totalElements: rows.length }))
}

export function serveConversations(rows: ConversationSummaryResponse[]) {
  server.use(http.get(LIST_PATH, ({ request }) => summariesResponse(request, rows)))
}

export function serveConversationSequence(...responses: ConversationSummaryResponse[][]) {
  let call = 0
  server.use(
    http.get(LIST_PATH, ({ request }) => {
      const isNavCounter = new URL(request.url).searchParams.get('size') === '1'
      const rows = responses[Math.min(call, responses.length - 1)] ?? []
      call += isNavCounter ? 0 : 1
      return summariesResponse(request, rows)
    }),
  )
}

export function serveDetailSequence(conversationId: number, ...responses: ConversationDetailResponse[]) {
  let call = 0
  server.use(
    http.get(`${LIST_PATH}/${conversationId}`, () => {
      const detail = responses[Math.min(call, responses.length - 1)]
      call += 1
      return HttpResponse.json(detail)
    }),
  )
}
