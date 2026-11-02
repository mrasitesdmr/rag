import { useMutation, useQuery } from '@tanstack/react-query'
import type { HandoffResponse, RequestHandoffRequest, SubmitFeedbackRequest } from '../model/types.ts'
import { getSuggestions, requestHandoff, submitFeedback } from './previewApi.ts'

export const previewKeys = {
  suggestions: ['preview', 'suggestions'] as const,
}

export function useSuggestionsQuery() {
  return useQuery({
    queryKey: previewKeys.suggestions,
    queryFn: getSuggestions,
  })
}

export type SubmitFeedbackVariables = { turnId: number; body: SubmitFeedbackRequest }

type SubmitFeedbackMutationOptions = {
  onSuccess: (variables: SubmitFeedbackVariables) => void
  onError: (error: unknown, variables: SubmitFeedbackVariables) => void
}

export function useSubmitFeedbackMutation({ onSuccess, onError }: SubmitFeedbackMutationOptions) {
  return useMutation({
    mutationFn: ({ turnId, body }: SubmitFeedbackVariables) => submitFeedback(turnId, body),
    onSuccess: (_data, variables) => onSuccess(variables),
    onError,
  })
}

export type RequestHandoffVariables = { conversationId: number; body: RequestHandoffRequest }

type RequestHandoffMutationOptions = {
  onSuccess: (handoff: HandoffResponse, variables: RequestHandoffVariables) => void
  onError: (error: unknown, variables: RequestHandoffVariables) => void
}

export function useRequestHandoffMutation({ onSuccess, onError }: RequestHandoffMutationOptions) {
  return useMutation({
    mutationFn: ({ conversationId, body }: RequestHandoffVariables) => requestHandoff(conversationId, body),
    onSuccess,
    onError,
  })
}
