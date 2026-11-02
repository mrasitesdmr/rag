import { request, withQuery } from '../../../shared/api/httpClient.ts'
import type { PageParams, PageResponse } from '../../../shared/api/pagination.ts'
import type { ConversationDetailResponse, ConversationSummaryResponse } from '../model/types.ts'

const CONVERSATIONS_PATH = '/api/assistant/conversations'

export function listConversations(params: PageParams): Promise<PageResponse<ConversationSummaryResponse>> {
  return request<PageResponse<ConversationSummaryResponse>>(withQuery(CONVERSATIONS_PATH, params))
}

export function getConversationDetail(conversationId: number): Promise<ConversationDetailResponse> {
  return request<ConversationDetailResponse>(`${CONVERSATIONS_PATH}/${conversationId}`)
}
