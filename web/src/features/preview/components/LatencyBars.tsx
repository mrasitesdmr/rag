import { formatDuration } from '../../../shared/format/formatDuration.ts'
import { latencyRows } from '../model/latencyRows.ts'
import type { DoneEvent } from '../model/types.ts'
import styles from './LatencyBars.module.css'
import card from './sideCard.module.css'

const MIN_BAR_PERCENT = 1
const FULL_PERCENT = 100

type LatencyBarsProps = { done: DoneEvent }

export function LatencyBars({ done }: LatencyBarsProps) {
  return (
    <>
      <h3 className={`${card.title} ${card.spacedTitle}`}>
        {`Süre · ${formatDuration(done.latencyMs)}`}
        {done.hostLabel === null || done.hostLabel === '' ? null : (
          <>
            {' '}
            <span className={`${card.sub} ${card.titleSub}`}>{`· ${done.hostLabel}`}</span>
          </>
        )}
      </h3>
      <div className={styles.rows}>
        {latencyRows(done).map((row) => (
          <div key={row.label} className={styles.row}>
            <span>{row.label}</span>
            <i
              className={row.isModel ? `${styles.bar} ${styles.model}` : styles.bar}
              style={{ width: `${barPercent(row.ms, done.latencyMs)}%` }}
            />
            <span className={styles.value}>{formatDuration(row.ms)}</span>
          </div>
        ))}
      </div>
    </>
  )
}

function barPercent(ms: number, totalMs: number): number {
  if (totalMs <= 0) {
    return MIN_BAR_PERCENT
  }
  return Math.min(FULL_PERCENT, Math.max(MIN_BAR_PERCENT, (ms / totalMs) * FULL_PERCENT))
}
