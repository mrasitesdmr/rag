import { useState } from 'react'
import type { FormEvent } from 'react'
import { canSendMessage, MESSAGE_MAX_LENGTH } from '../model/messageRules.ts'
import styles from './MessageComposer.module.css'

type MessageComposerProps = { isStreaming: boolean; onSend: (text: string) => void }

export function MessageComposer({ isStreaming, onSend }: MessageComposerProps) {
  const [value, setValue] = useState('')
  const canSend = !isStreaming && canSendMessage(value)

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canSend) {
      return
    }
    onSend(value)
    setValue('')
  }

  return (
    <form className={styles.composer} autoComplete="off" noValidate onSubmit={handleSubmit}>
      <input
        className={styles.input}
        name="text"
        aria-label="Soru"
        placeholder="Sorunu yaz…"
        maxLength={MESSAGE_MAX_LENGTH}
        value={value}
        disabled={isStreaming}
        onChange={(event) => setValue(event.target.value)}
      />
      <button type="submit" className={styles.send} aria-label="Gönder" disabled={!canSend}>
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          aria-hidden="true"
          focusable="false"
        >
          <path d="M5 12h14M13 6l6 6-6 6" />
        </svg>
      </button>
    </form>
  )
}
