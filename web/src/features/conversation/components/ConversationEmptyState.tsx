import { useId } from 'react'
import { Link } from 'react-router'
import button from '../../../shared/components/button.module.css'
import { EMPTY_ACTION, EMPTY_TEXT, EMPTY_TITLE } from '../model/conversationTexts.ts'
import styles from './ConversationEmptyState.module.css'

export function ConversationEmptyState() {
  const titleId = useId()
  return (
    <section className={styles.emptyState} aria-labelledby={titleId}>
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
        <path d="M4 6h16M4 12h16M4 18h10" />
      </svg>
      <div className={styles.body}>
        <h2 id={titleId} className={styles.title}>
          {EMPTY_TITLE}
        </h2>
        <p className={styles.text}>{EMPTY_TEXT}</p>
        <Link to="/onizleme" className={button.button}>
          {EMPTY_ACTION}
        </Link>
      </div>
    </section>
  )
}
