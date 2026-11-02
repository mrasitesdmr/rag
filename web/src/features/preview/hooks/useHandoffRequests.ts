import { useCallback, useRef, useState } from 'react'
import { errorCodeOf } from '../../../shared/api/ApiError.ts'
import { useRequestHandoffMutation } from '../api/previewQueries.ts'

export type HandoffState = { isQueued: boolean; isRequesting: boolean; errorCode: string | null }

export type HandoffControls = {
  handoffFor: (conversationId: number) => HandoffState
  requestHandoff: (conversationId: number, turnId: number) => void
}

const INITIAL_HANDOFF: HandoffState = { isQueued: false, isRequesting: false, errorCode: null }

export function useHandoffRequests(): HandoffControls {
  const [handoffs, setHandoffs] = useState<Record<number, HandoffState>>({})
  const handoffsRef = useRef<Record<number, HandoffState>>({})

  const updateHandoff = useCallback((conversationId: number, update: (handoff: HandoffState) => HandoffState) => {
    const next = {
      ...handoffsRef.current,
      [conversationId]: update(handoffsRef.current[conversationId] ?? INITIAL_HANDOFF),
    }
    handoffsRef.current = next
    setHandoffs(next)
  }, [])

  const mutation = useRequestHandoffMutation({
    onSuccess: (_handoff, { conversationId }) => {
      updateHandoff(conversationId, () => ({ isQueued: true, isRequesting: false, errorCode: null }))
    },
    onError: (error, { conversationId }) => {
      updateHandoff(conversationId, () => ({ isQueued: false, isRequesting: false, errorCode: errorCodeOf(error) }))
    },
  })
  const { mutate } = mutation

  const handoffFor = useCallback((conversationId: number) => handoffs[conversationId] ?? INITIAL_HANDOFF, [handoffs])

  const requestHandoff = useCallback(
    (conversationId: number, turnId: number) => {
      const current = handoffsRef.current[conversationId] ?? INITIAL_HANDOFF
      if (current.isQueued || current.isRequesting) {
        return
      }
      updateHandoff(conversationId, (handoff) => ({ ...handoff, isRequesting: true, errorCode: null }))
      mutate({ conversationId, body: { reason: 'NO_ANSWER', turnId } })
    },
    [mutate, updateHandoff],
  )

  return { handoffFor, requestHandoff }
}
