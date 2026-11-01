import type { MouseEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { messageFor } from '../../../shared/api/errorMessages.ts'
import button from '../../../shared/components/button.module.css'
import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import table from '../../../shared/components/table.module.css'
import { formatDateTime } from '../../../shared/format/formatDateTime.ts'
import { formatInteger } from '../../../shared/format/formatNumber.ts'
import { sourceStatus } from '../model/ingestionRules.ts'
import type { IngestionJobResponse, SourceResponse } from '../model/types.ts'
import styles from './SourceTable.module.css'

type SourceTableProps = {
  sources: SourceResponse[]
  jobsById: Map<number, IngestionJobResponse>
  onRetry: (sourceId: number) => void
  retryingSourceId: number | null
}

export function SourceTable({ sources, jobsById, onRetry, retryingSourceId }: SourceTableProps) {
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>Kaynak</th>
            <th>Tür</th>
            <th>Sürüm</th>
            <th>Parça</th>
            <th>Durum</th>
            <th>Son senkron</th>
          </tr>
        </thead>
        <tbody>
          {sources.map((source) => (
            <SourceRow
              key={source.sourceId}
              source={source}
              job={jobsById.get(source.latestJob.jobId) ?? source.latestJob}
              onRetry={onRetry}
              isRetrying={retryingSourceId === source.sourceId}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

type SourceRowProps = {
  source: SourceResponse
  job: IngestionJobResponse
  onRetry: (sourceId: number) => void
  isRetrying: boolean
}

function SourceRow({ source, job, onRetry, isRetrying }: SourceRowProps) {
  const navigate = useNavigate()
  const detailPath = `/kaynaklar/${source.sourceId}`

  function handleRowClick(event: MouseEvent<HTMLTableRowElement>) {
    if (event.target instanceof Element && event.target.closest('a, button') !== null) {
      return
    }
    void navigate(detailPath)
  }

  return (
    // The name link is the keyboard path to the detail; a click on the rest of the row is a mouse shortcut like the lab.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <tr className={table.clickable} onClick={handleRowClick}>
      <td>
        <Link to={detailPath} className={styles.nameLink}>
          <b>{source.name}</b>
        </Link>
        <small>{source.fileName}</small>
      </td>
      <td>{source.pageCount === null ? source.kind : `${source.kind} · ${source.pageCount} sayfa`}</td>
      <td className={table.num}>{source.version}</td>
      <td className={table.num}>{source.chunkCount === null ? '–' : formatInteger(source.chunkCount)}</td>
      <td>
        <SourceStatusCell source={source} job={job} onRetry={onRetry} isRetrying={isRetrying} />
      </td>
      <td className={table.num}>{source.syncedAt === null ? '–' : formatDateTime(source.syncedAt)}</td>
    </tr>
  )
}

function SourceStatusCell({ source, job, onRetry, isRetrying }: SourceRowProps) {
  const status = sourceStatus(source, job)
  if (status === 'RUNNING') {
    return <StatusBadge tone="run">{job.stage}</StatusBadge>
  }
  if (status === 'READY') {
    return <StatusBadge tone="ok">Hazır</StatusBadge>
  }
  return (
    <div className={styles.statusCell}>
      <StatusBadge tone="bad">Başarısız</StatusBadge>
      <small className={styles.failureReason}>{messageFor(job.errorCode ?? '')}</small>
      <button
        type="button"
        className={`${button.button} ${button.outline}`}
        disabled={isRetrying}
        onClick={() => onRetry(source.sourceId)}
      >
        Yeniden dene
      </button>
    </div>
  )
}
