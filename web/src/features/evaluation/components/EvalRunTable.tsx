import table from '../../../shared/components/table.module.css'
import { formatListTime } from '../../../shared/format/formatDateTime.ts'
import { formatDecimal } from '../../../shared/format/formatNumber.ts'
import { evalRunLabel, retrievalText } from '../model/evaluationLabels.ts'
import { RUN_TABLE_FRACTION_DIGITS } from '../model/evaluationRules.ts'
import { RUN_ROW_EMPTY_METRIC, RUN_ROW_FAILED, RUN_ROW_RUNNING } from '../model/evaluationTexts.ts'
import type { EvalRunResponse } from '../model/types.ts'

type EvalRunTableProps = { runs: EvalRunResponse[]; now: Date }

export function EvalRunTable({ runs, now }: EvalRunTableProps) {
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>Çalıştırma</th>
            <th>Retrieval</th>
            <th>Rehber</th>
            <th>recall@5</th>
            <th>MRR</th>
            <th>Saat</th>
          </tr>
        </thead>
        <tbody>
          {runs.map((run) => (
            <tr key={run.evalRunId}>
              <td className={table.num}>{evalRunLabel(run.evalRunId)}</td>
              <td>{retrievalText(run)}</td>
              <td className={table.num}>{run.sourceVersions}</td>
              <td className={table.num}>{recallCell(run)}</td>
              <td className={table.num}>{metricCell(run.status === 'DONE' ? run.mrr : null)}</td>
              <td className={table.num}>{formatListTime(run.startedAt, now)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function recallCell(run: EvalRunResponse): string {
  switch (run.status) {
    case 'RUNNING':
      return RUN_ROW_RUNNING
    case 'FAILED':
      return RUN_ROW_FAILED
    case 'DONE':
      return metricCell(run.recallAtK)
  }
}

function metricCell(value: number | null): string {
  return value === null ? RUN_ROW_EMPTY_METRIC : formatDecimal(value, RUN_TABLE_FRACTION_DIGITS)
}
