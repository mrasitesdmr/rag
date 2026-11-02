import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { isClientError } from '../../../shared/api/ApiError.ts'
import type { PageParams, PageResponse } from '../../../shared/api/pagination.ts'
import { conversationKeys } from '../../../shared/conversation/conversationKeys.ts'
import { CONVERSATION_POLL_INTERVAL_MS } from '../model/conversationRules.ts'
import type { ConversationSummaryResponse } from '../model/types.ts'
import { getConversationDetail, listConversations } from './conversationApi.ts'

const COUNT_PAGE_PARAMS: PageParams = { page: 0, size: 1 }

export function useConversationsQuery(params: PageParams) {
  return useQuery({
    queryKey: conversationKeys.list(params),
    queryFn: () => listConversations(params),
    placeholderData: keepPreviousData,
    refetchInterval: (query) => pollWhile(query, hasInProgressConversation),
  })
}

export function useConversationCountQuery() {
  return useQuery({
    queryKey: conversationKeys.count,
    queryFn: () => listConversations(COUNT_PAGE_PARAMS),
    select: (page) => page.totalElements,
  })
}

export function useConversationDetailQuery(conversationId: number) {
  return useQuery({
    queryKey: conversationKeys.detail(conversationId),
    queryFn: () => getConversationDetail(conversationId),
    refetchInterval: (query) => pollWhile(query, (detail) => detail.inProgressTurn !== null),
  })
}

type PolledQuery<T> = { state: { data: T | undefined; error: unknown } }

function pollWhile<T>(query: PolledQuery<T>, isActive: (data: T) => boolean): number | false {
  const data = query.state.data
  if (isClientError(query.state.error) || data === undefined) {
    return false
  }
  return isActive(data) ? CONVERSATION_POLL_INTERVAL_MS : false
}

function hasInProgressConversation(page: PageResponse<ConversationSummaryResponse>): boolean {
  return page.content.some((conversation) => conversation.lastTurnStatus === 'IN_PROGRESS')
}
