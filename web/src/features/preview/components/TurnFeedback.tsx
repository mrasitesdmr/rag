import { messageFor } from '../../../shared/api/errorMessages.ts'
import { formatTime } from '../../../shared/format/formatDateTime.ts'
import type { TurnFeedbackState } from '../hooks/useTurnFeedback.ts'
import styles from './feedbackRow.module.css'
import message from './message.module.css'

type TurnFeedbackProps = { state: TurnFeedbackState; answeredAt: string; onSubmit: (helpful: boolean) => void }

export function TurnFeedback({ state, answeredAt, onSubmit }: TurnFeedbackProps) {
  return (
    <>
      <div className={styles.row} role="group" aria-label="Geri bildirim">
        <span>Faydalı mı?</span>
        <FeedbackButton
          label="Evet"
          isSelected={state.helpful === true}
          isDisabled={state.isSaving}
          onClick={() => onSubmit(true)}
        />
        <FeedbackButton
          label="Hayır"
          isSelected={state.helpful === false}
          isDisabled={state.isSaving}
          onClick={() => onSubmit(false)}
        />
        <span className={message.time}>{formatTime(answeredAt)}</span>
      </div>
      {state.errorCode !== null ? (
        <p role="alert" className={styles.error}>
          {messageFor(state.errorCode)}
        </p>
      ) : null}
    </>
  )
}

type FeedbackButtonProps = { label: string; isSelected: boolean; isDisabled: boolean; onClick: () => void }

function FeedbackButton({ label, isSelected, isDisabled, onClick }: FeedbackButtonProps) {
  return (
    <button
      type="button"
      className={isSelected ? `${styles.button} ${styles.selected}` : styles.button}
      aria-pressed={isSelected}
      disabled={isDisabled}
      onClick={onClick}
    >
      {label}
    </button>
  )
}
