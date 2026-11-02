import { postEventStream } from '../../../shared/api/eventStream.ts'
import { request } from '../../../shared/api/httpClient.ts'
import { toAnswerStreamEvent } from '../model/answerStreamEvents.ts'
import type {
  AnswerStreamEvent,
  ConversationResponse,
  CreateConversationRequest,
  HandoffResponse,
  RequestHandoffRequest,
  SendMessageRequest,
  SubmitFeedbackRequest,
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

const TURNS_PATH = '/api/assistant/turns'

export function submitFeedback(turnId: number, body: SubmitFeedbackRequest): Promise<void> {
  return request<void>(`${TURNS_PATH}/${turnId}/feedback`, { method: 'POST', body: JSON.stringify(body) })
}

export function requestHandoff(conversationId: number, body: RequestHandoffRequest): Promise<HandoffResponse> {
  return request<HandoffResponse>(`${CONVERSATIONS_PATH}/${conversationId}/handoff`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
