import { useCallback, useRef, useState } from 'react'
import { errorCodeOf } from '../../../shared/api/ApiError.ts'
import { useToast } from '../../../shared/toast/useToast.ts'
import { useSubmitFeedbackMutation } from '../api/previewQueries.ts'
import { FEEDBACK_HELPFUL_TOAST, FEEDBACK_NOT_HELPFUL_TOAST } from '../model/feedbackMessages.ts'

export type TurnFeedbackState = { helpful: boolean | null; isSaving: boolean; errorCode: string | null }

export type FeedbackControls = {
  feedbackFor: (turnId: number) => TurnFeedbackState
  submitFeedback: (turnId: number, helpful: boolean) => void
}

const INITIAL_FEEDBACK: TurnFeedbackState = { helpful: null, isSaving: false, errorCode: null }

export function useTurnFeedback(): FeedbackControls {
  const { showToast } = useToast()
  const [feedbacks, setFeedbacks] = useState<Record<number, TurnFeedbackState>>({})
  const feedbacksRef = useRef<Record<number, TurnFeedbackState>>({})

  const updateFeedback = useCallback((turnId: number, update: (feedback: TurnFeedbackState) => TurnFeedbackState) => {
    const next = { ...feedbacksRef.current, [turnId]: update(feedbacksRef.current[turnId] ?? INITIAL_FEEDBACK) }
    feedbacksRef.current = next
    setFeedbacks(next)
  }, [])

  const mutation = useSubmitFeedbackMutation({
    onSuccess: ({ turnId, body }) => {
      updateFeedback(turnId, () => ({ helpful: body.helpful, isSaving: false, errorCode: null }))
      showToast(body.helpful ? FEEDBACK_HELPFUL_TOAST : FEEDBACK_NOT_HELPFUL_TOAST)
    },
    onError: (error, { turnId }) => {
      updateFeedback(turnId, (feedback) => ({ ...feedback, isSaving: false, errorCode: errorCodeOf(error) }))
    },
  })
  const { mutate } = mutation

  const feedbackFor = useCallback((turnId: number) => feedbacks[turnId] ?? INITIAL_FEEDBACK, [feedbacks])

  const submitFeedback = useCallback(
    (turnId: number, helpful: boolean) => {
      const current = feedbacksRef.current[turnId] ?? INITIAL_FEEDBACK
      if (current.isSaving || current.helpful === helpful) {
        return
      }
      updateFeedback(turnId, (feedback) => ({ ...feedback, isSaving: true, errorCode: null }))
      mutate({ turnId, body: { helpful } })
    },
    [mutate, updateFeedback],
  )

  return { feedbackFor, submitFeedback }
}
