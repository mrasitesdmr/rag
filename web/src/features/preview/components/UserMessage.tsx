import { formatTime } from '../../../shared/format/formatDateTime.ts'
import message from './message.module.css'

type UserMessageProps = { question: string; askedAt: string }

export function UserMessage({ question, askedAt }: UserMessageProps) {
  return (
    <div className={`${message.message} ${message.user}`} data-message="question">
      <div className={message.bubble}>{question}</div>
      <span className={message.time}>{formatTime(askedAt)}</span>
    </div>
  )
}
