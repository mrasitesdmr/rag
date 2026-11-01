import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import type { StatusTone } from '../../../shared/components/StatusBadge.tsx'
import table from '../../../shared/components/table.module.css'
import { formatDuration } from '../../../shared/format/formatDuration.ts'
import type { IngestionJobResponse, IngestionStage } from '../model/types.ts'

type IngestionJobTableProps = { jobs: IngestionJobResponse[] }

export function IngestionJobTable({ jobs }: IngestionJobTableProps) {
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>İş</th>
            <th>Dosya</th>
            <th>Sürüm</th>
            <th>Parça</th>
            <th>Süre</th>
            <th>Durum</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr key={job.jobId}>
              <td className={table.num}>{`#${job.jobId}`}</td>
              <td className={table.text}>{job.fileName}</td>
              <td className={table.num}>{job.version}</td>
              <td className={table.num}>{job.totalChunks ?? '–'}</td>
              <td className={table.num}>{durationText(job)}</td>
              <td>
                <StatusBadge tone={stageTone(job.stage)}>{job.stage}</StatusBadge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function durationText(job: IngestionJobResponse): string {
  if (job.startedAt === null || job.finishedAt === null) {
    return '–'
  }
  return formatDuration(Date.parse(job.finishedAt) - Date.parse(job.startedAt))
}

function stageTone(stage: IngestionStage): StatusTone {
  if (stage === 'DONE') {
    return 'ok'
  }
  if (stage === 'FAILED') {
    return 'bad'
  }
  return 'run'
}
