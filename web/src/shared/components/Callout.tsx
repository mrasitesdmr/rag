import type { ReactNode } from 'react'
import styles from './Callout.module.css'

type CalloutTone = 'info' | 'warning'

type CalloutProps = { tone?: CalloutTone; children: ReactNode }

export function Callout({ tone = 'info', children }: CalloutProps) {
  if (tone === 'warning') {
    return <div className={`${styles.callout} ${styles.warning}`}>{children}</div>
  }
  return (
    <div className={styles.callout}>
      <svg
        className={styles.icon}
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9" />
        <path d="M12 8v5M12 16h.01" />
      </svg>
      <span>{children}</span>
    </div>
  )
}
