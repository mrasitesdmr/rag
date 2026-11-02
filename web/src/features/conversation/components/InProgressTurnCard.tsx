import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import { TURN_STATUS_LABELS, turnStatusTone } from '../model/turnStatus.ts'
import type { InProgressTurnResponse } from '../model/types.ts'
import styles from './turnCard.module.css'

type InProgressTurnCardProps = { turn: InProgressTurnResponse }

export function InProgressTurnCard({ turn }: InProgressTurnCardProps) {
  return (
    <article className={styles.turnCard}>
      <div className={styles.question}>{`${turn.turnNo}. ${turn.question}`}</div>
      <div className={styles.answer}>…</div>
      <div className={styles.numbers}>
        <span>
          <StatusBadge tone={turnStatusTone('IN_PROGRESS')}>{TURN_STATUS_LABELS.IN_PROGRESS}</StatusBadge>
        </span>
      </div>
    </article>
  )
}
