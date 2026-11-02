import { messageFor } from '../../../shared/api/errorMessages.ts'
import type { HandoffState } from '../hooks/useHandoffRequests.ts'
import styles from './feedbackRow.module.css'

type HandoffActionProps = { state: HandoffState; onRequest: () => void }

export function HandoffAction({ state, onRequest }: HandoffActionProps) {
  return (
    <>
      <div className={styles.row}>
        {state.isQueued ? (
          <span>Temsilci kuyruğundasın</span>
        ) : (
          <button
            type="button"
            className={`${styles.button} ${styles.handoff}`}
            disabled={state.isRequesting}
            onClick={onRequest}
          >
            Temsilciye bağlan
          </button>
        )}
      </div>
      {state.errorCode !== null ? (
        <p role="alert" className={styles.error}>
          {messageFor(state.errorCode)}
        </p>
      ) : null}
    </>
  )
}
