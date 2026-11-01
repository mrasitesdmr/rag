import type { ReactNode } from 'react'
import styles from './Callout.module.css'

type CalloutProps = { children: ReactNode }

export function Callout({ children }: CalloutProps) {
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
