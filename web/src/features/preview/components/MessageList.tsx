import { useLayoutEffect, useRef } from 'react'
import type { HandoffControls } from '../hooks/useHandoffRequests.ts'
import type { FeedbackControls } from '../hooks/useTurnFeedback.ts'
import type { PreviewTurn } from '../model/types.ts'
import { AnswerMessage } from './AnswerMessage.tsx'
import message from './message.module.css'
import styles from './MessageList.module.css'
import { UserMessage } from './UserMessage.tsx'

export const WELCOME_MESSAGE =
  "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılık sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplarım. Bakiyen gibi sana özel bilgileri göremem."

const QUESTION_SELECTOR = '[data-message="question"]'
const SCROLL_MARGIN_PX = 8
const CUT_TOLERANCE_PX = 2

type MessageListProps = {
  previousTurns: PreviewTurn[]
  turns: PreviewTurn[]
  conversationId: number | null
  feedback: FeedbackControls
  handoff: HandoffControls
  onCitationOpen: (chunkId: number) => void
}

export function MessageList({
  previousTurns,
  turns,
  conversationId,
  feedback,
  handoff,
  onCitationOpen,
}: MessageListProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const spacerRef = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    if (containerRef.current !== null && spacerRef.current !== null) {
      scrollToLatestQuestion(containerRef.current, spacerRef.current)
    }
  }, [previousTurns, turns])

  return (
    <div ref={containerRef} className={styles.messages} role="region" aria-label="Mesajlar" tabIndex={0}>
      <div className={`${message.message} ${message.assistant}`}>
        <div className={message.bubble}>{WELCOME_MESSAGE}</div>
      </div>
      {previousTurns.length > 0 ? (
        <>
          {previousTurns.map((turn) => (
            <TurnMessages
              key={turn.localId}
              turn={turn}
              feedback={feedback}
              handoff={handoff}
              onCitationOpen={onCitationOpen}
            />
          ))}
          <div className={styles.systemMessage}>
            {conversationId === null ? 'Yeni konuşma' : `Yeni konuşma · ${conversationId}`}
          </div>
        </>
      ) : null}
      {turns.map((turn) => (
        <TurnMessages
          key={turn.localId}
          turn={turn}
          feedback={feedback}
          handoff={handoff}
          onCitationOpen={onCitationOpen}
        />
      ))}
      <div ref={spacerRef} className={styles.spacer} aria-hidden="true" />
    </div>
  )
}

type TurnMessagesProps = {
  turn: PreviewTurn
  feedback: FeedbackControls
  handoff: HandoffControls
  onCitationOpen: (chunkId: number) => void
}

function TurnMessages({ turn, feedback, handoff, onCitationOpen }: TurnMessagesProps) {
  return (
    <>
      <UserMessage question={turn.question} askedAt={turn.askedAt} />
      <AnswerMessage turn={turn} feedback={feedback} handoff={handoff} onCitationOpen={onCitationOpen} />
    </>
  )
}

function scrollToLatestQuestion(container: HTMLElement, spacer: HTMLElement) {
  spacer.style.height = '0px'
  const maxScroll = container.scrollHeight - container.clientHeight
  if (maxScroll <= 0) {
    container.scrollTop = 0
    return
  }
  const questions = container.querySelectorAll<HTMLElement>(QUESTION_SELECTOR)
  const latestQuestion = questions.item(questions.length - 1) as HTMLElement | null
  const questionTop = latestQuestion === null ? maxScroll : latestQuestion.offsetTop - SCROLL_MARGIN_PX
  let top = Math.min(maxScroll, questionTop)
  const children = Array.from(container.children).filter(
    (child): child is HTMLElement => child instanceof HTMLElement && child !== spacer,
  )
  const cut = children.find((child) => child.offsetTop < top && child.offsetTop + child.offsetHeight > top)
  if (cut !== undefined) {
    const hiddenTop = cut.offsetTop + cut.offsetHeight - CUT_TOLERANCE_PX
    top = hiddenTop <= questionTop ? hiddenTop : Math.max(0, cut.offsetTop - SCROLL_MARGIN_PX)
  }
  if (top > maxScroll) {
    spacer.style.height = `${top - maxScroll}px`
  }
  container.scrollTop = top
}
