import type { ReactNode } from 'react'
import { useClock } from '../hooks/useClock.ts'
import styles from './PhoneFrame.module.css'

type PhoneFrameProps = {
  isStreaming: boolean
  onNewConversation: () => void
  children: ReactNode
}

export function PhoneFrame({ isStreaming, onNewConversation, children }: PhoneFrameProps) {
  const time = useClock()
  return (
    <div className={styles.phone}>
      <div className={styles.screen}>
        <div className={styles.statusBar} aria-hidden="true">
          <span>{time}</span>
          <span>
            5G
            <i className={styles.battery} />
          </span>
        </div>
        <div className={styles.appBar}>
          <span className={styles.avatar} aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 30 30" focusable="false">
              <circle className={styles.avatarCircle} cx="15" cy="15" r="11" />
              <rect className={styles.avatarBar} x="13" y="8" width="4" height="14" rx="1.3" />
            </svg>
          </span>
          <div>
            <b className={styles.assistantName}>Dijital Asistan</b>
            <small className={styles.assistantSub}>Raşit Mobil · 7/24</small>
          </div>
          <button
            type="button"
            className={styles.newConversation}
            disabled={isStreaming}
            onClick={onNewConversation}
          >
            Yeni konuşma
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
