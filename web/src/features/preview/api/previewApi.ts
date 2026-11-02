import { postEventStream } from '../../../shared/api/eventStream.ts'
import { request } from '../../../shared/api/httpClient.ts'
import { toAnswerStreamEvent } from '../model/answerStreamEvents.ts'
import type {
  AnswerStreamEvent,
  ConversationResponse,
  CreateConversationRequest,
  SendMessageRequest,
  SuggestionSetResponse,
} from '../model/types.ts'

const CONVERSATIONS_PATH = '/api/assistant/conversations'

export function createConversation(): Promise<ConversationResponse> {
  const body: CreateConversationRequest = { channel: 'PREVIEW' }
  return request<ConversationResponse>(CONVERSATIONS_PATH, { method: 'POST', body: JSON.stringify(body) })
}

export async function* sendMessage(
  conversationId: number,
  body: SendMessageRequest,
  signal: AbortSignal,
): AsyncGenerator<AnswerStreamEvent> {
  for await (const event of postEventStream(`${CONVERSATIONS_PATH}/${conversationId}/messages`, body, signal)) {
    const answerEvent = toAnswerStreamEvent(event)
    if (answerEvent !== null) {
      yield answerEvent
    }
  }
}

const SUGGESTIONS_PATH = '/api/assistant/suggestions'

export function getSuggestions(): Promise<SuggestionSetResponse> {
  return request<SuggestionSetResponse>(SUGGESTIONS_PATH)
}
