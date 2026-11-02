import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../../../shared/api/ApiError.ts'
import { isAbortError, STREAM_INTERRUPTED } from '../../../shared/api/eventStream.ts'
import { liveConfigKeys } from '../../../shared/liveConfig/liveConfigQueries.ts'
import { createConversation, sendMessage } from '../api/previewApi.ts'
import { canSendMessage, normalizeMessage } from '../model/messageRules.ts'
import type { AnswerStreamEvent, PreviewTurn } from '../model/types.ts'

const CONVERSATION_NOT_FOUND = 'CONVERSATION_NOT_FOUND'
const UNEXPECTED_ERROR = 'UNEXPECTED_ERROR'

export type AnswerStream = {
  conversationId: number | null
  turns: PreviewTurn[]
  previousTurns: PreviewTurn[]
  isStreaming: boolean
  sendQuestion: (text: string) => void
  startNewConversation: () => void
  askInNewConversation: (text: string) => void
}

export function useAnswerStream(): AnswerStream {
  const queryClient = useQueryClient()
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [turns, setTurns] = useState<PreviewTurn[]>([])
  const [previousTurns, setPreviousTurns] = useState<PreviewTurn[]>([])
  const conversationIdRef = useRef<number | null>(null)
  const controllerRef = useRef<AbortController | null>(null)
  const turnCounterRef = useRef(0)

  useEffect(() => {
    return () => {
      controllerRef.current?.abort()
      controllerRef.current = null
    }
  }, [])

  const updateTurn = useCallback((localId: string, update: (turn: PreviewTurn) => PreviewTurn) => {
    setTurns((current) => current.map((turn) => (turn.localId === localId ? update(turn) : turn)))
  }, [])

  const refreshLiveConfig = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: liveConfigKeys.all })
  }, [queryClient])

  const rememberConversation = useCallback((nextConversationId: number | null) => {
    conversationIdRef.current = nextConversationId
    setConversationId(nextConversationId)
  }, [])

  const sendQuestion = useCallback(
    (text: string) => {
      if (!canSendMessage(text) || controllerRef.current !== null) {
        return
      }
      const question = normalizeMessage(text)
      const controller = new AbortController()
      controllerRef.current = controller
      turnCounterRef.current += 1
      const localId = `turn-${turnCounterRef.current}`
      setTurns((current) => [...current, newTurn(localId, conversationIdRef.current, question)])

      async function streamAnswer() {
        let isFinished = false
        try {
          let activeConversationId = conversationIdRef.current
          if (activeConversationId === null) {
            const conversation = await createConversation()
            if (controller.signal.aborted) {
              return
            }
            const createdConversationId = conversation.conversationId
            activeConversationId = createdConversationId
            rememberConversation(createdConversationId)
            updateTurn(localId, (turn) => ({ ...turn, conversationId: createdConversationId }))
          }
          for await (const event of sendMessage(activeConversationId, { text: question }, controller.signal)) {
            updateTurn(localId, (turn) => applyEvent(turn, event))
            if (event.type === 'done' || event.type === 'error') {
              isFinished = true
              refreshLiveConfig()
              break
            }
          }
          if (!isFinished) {
            updateTurn(localId, (turn) => failTurn(turn, STREAM_INTERRUPTED))
          }
        } catch (error) {
          if (isAbortError(error) || controller.signal.aborted) {
            return
          }
          const errorCode = error instanceof ApiError ? error.errorCode : UNEXPECTED_ERROR
          if (!(error instanceof ApiError)) {
            console.error('Answer stream failed', error instanceof Error ? error.name : typeof error)
          }
          if (errorCode === CONVERSATION_NOT_FOUND) {
            rememberConversation(null)
          }
          if (error instanceof ApiError) {
            refreshLiveConfig()
          }
          updateTurn(localId, (turn) => failTurn(turn, errorCode))
        } finally {
          if (controllerRef.current === controller) {
            controllerRef.current = null
          }
        }
      }

      void streamAnswer()
    },
    [refreshLiveConfig, rememberConversation, updateTurn],
  )

  const isStreaming = turns.some((turn) => turn.phase === 'waiting' || turn.phase === 'streaming')

  const startNewConversation = useCallback(() => {
    if (controllerRef.current !== null) {
      return
    }
    if (turns.length > 0) {
      setPreviousTurns(turns)
    }
    setTurns([])
    rememberConversation(null)
  }, [rememberConversation, turns])

  const askInNewConversation = useCallback(
    (text: string) => {
      if (controllerRef.current !== null || !canSendMessage(text)) {
        return
      }
      if (turns.length > 0) {
        startNewConversation()
      }
      sendQuestion(text)
    },
    [sendQuestion, startNewConversation, turns.length],
  )

  return {
    conversationId,
    turns,
    previousTurns,
    isStreaming,
    sendQuestion,
    startNewConversation,
    askInNewConversation,
  }
}

function newTurn(localId: string, conversationId: number | null, question: string): PreviewTurn {
  return {
    localId,
    conversationId,
    question,
    askedAt: new Date().toISOString(),
    answer: '',
    phase: 'waiting',
    sources: null,
    done: null,
    answeredAt: null,
    errorCode: null,
  }
}

function applyEvent(turn: PreviewTurn, event: AnswerStreamEvent): PreviewTurn {
  switch (event.type) {
    case 'token':
      return { ...turn, answer: turn.answer + event.data.text, phase: 'streaming' }
    case 'sources':
      return { ...turn, sources: event.data }
    case 'done':
      return { ...turn, done: event.data, phase: 'done', answeredAt: new Date().toISOString() }
    case 'error':
      return failTurn(turn, event.data.errorCode)
  }
}

function failTurn(turn: PreviewTurn, errorCode: string): PreviewTurn {
  return { ...turn, phase: 'failed', errorCode }
}
