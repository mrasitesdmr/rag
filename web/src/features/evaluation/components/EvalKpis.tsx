import { formatDecimal } from '../../../shared/format/formatNumber.ts'
import { recallSubtitle, retrievalSubtitle, strategyText } from '../model/evaluationLabels.ts'
import { KPI_FRACTION_DIGITS, RECALL_PASS_THRESHOLD } from '../model/evaluationRules.ts'
import {
  KPI_MISSED_LABEL,
  KPI_MISSED_SUBTITLE,
  KPI_MRR_LABEL,
  KPI_MRR_SUBTITLE,
  KPI_RECALL_LABEL,
  KPI_RETRIEVAL_LABEL,
} from '../model/evaluationTexts.ts'
import type { EvalRunResponse } from '../model/types.ts'
import styles from './EvalKpis.module.css'

type KpiTone = 'good' | 'bad' | 'plain' | 'strategy'

type EvalKpisProps = { run: EvalRunResponse }

export function EvalKpis({ run }: EvalKpisProps) {
  const recallAtK = run.recallAtK ?? 0
  const missedCount = run.missedCount ?? 0
  return (
    <div className={styles.container}>
      <div className={styles.kpis}>
        <Kpi
          label={KPI_RECALL_LABEL}
          value={formatDecimal(recallAtK, KPI_FRACTION_DIGITS)}
          tone={recallAtK >= RECALL_PASS_THRESHOLD ? 'good' : 'bad'}
          subtitle={recallSubtitle(run)}
        />
        <Kpi
          label={KPI_MRR_LABEL}
          value={formatDecimal(run.mrr ?? 0, KPI_FRACTION_DIGITS)}
          tone="plain"
          subtitle={KPI_MRR_SUBTITLE}
        />
        <Kpi
          label={KPI_MISSED_LABEL}
          value={String(missedCount)}
          tone={missedCount > 0 ? 'bad' : 'good'}
          subtitle={KPI_MISSED_SUBTITLE}
        />
        <Kpi
          label={KPI_RETRIEVAL_LABEL}
          value={strategyText(run.retrievalStrategy, run.candidates, run.topK)}
          tone="strategy"
          subtitle={retrievalSubtitle(run)}
        />
      </div>
    </div>
  )
}

type KpiProps = { label: string; value: string; tone: KpiTone; subtitle: string }

function Kpi({ label, value, tone, subtitle }: KpiProps) {
  return (
    <div className={styles.kpi}>
      <span className={styles.label}>{label}</span>
      <b className={`${styles.value} ${styles[tone]}`}>{value}</b>
      <small className={styles.subtitle}>{subtitle}</small>
    </div>
  )
}
