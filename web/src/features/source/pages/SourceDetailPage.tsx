import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { errorCodeOf, isApiErrorWithCode } from '../../../shared/api/ApiError.ts'
import { messageFor } from '../../../shared/api/errorMessages.ts'
import { ChunkDialog } from '../../../shared/chunk/ChunkDialog.tsx'
import button from '../../../shared/components/button.module.css'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import { Pagination } from '../../../shared/components/Pagination.tsx'
import { formatDateTime } from '../../../shared/format/formatDateTime.ts'
import { formatInteger } from '../../../shared/format/formatNumber.ts'
import { sourceFileUrl } from '../api/sourceApi.ts'
import { useChunksQuery, useIngestionJobsQuery, useSourceQuery, useWorkspaceQuery } from '../api/sourceQueries.ts'
import { ChunkTable } from '../components/ChunkTable.tsx'
import { IngestionJobTable } from '../components/IngestionJobTable.tsx'
import { SourceMetaGrid } from '../components/SourceMetaGrid.tsx'
import { useDebouncedValue } from '../hooks/useDebouncedValue.ts'
import { useIngestionJobsPolling } from '../hooks/useIngestionJobsPolling.ts'
import { CHUNK_FILTER_DEBOUNCE_MS, CHUNK_FILTER_MAX_LENGTH, CHUNK_PAGE_SIZE } from '../model/chunkRules.ts'
import type { IngestionJobResponse, SourceResponse } from '../model/types.ts'
import styles from './SourcePages.module.css'

const SOURCE_ID_PATTERN = /^\d+$/

export function SourceDetailPage() {
  const { sourceId } = useParams()
  if (sourceId === undefined || !SOURCE_ID_PATTERN.test(sourceId)) {
    return <SourceNotFound />
  }
  const id = Number(sourceId)
  return <SourceDetail key={id} sourceId={id} />
}

function SourceNotFound() {
  return (
    <div className={styles.notFound}>
      <title>Kaynak bulunamadı · Raşit Bankası Dijital Asistan Yönetimi</title>
      <h1>{messageFor('SOURCE_NOT_FOUND')}</h1>
      <Link to="/kaynaklar">Bilgi kaynaklarına dön</Link>
    </div>
  )
}

function SourceDetail({ sourceId }: { sourceId: number }) {
  const sourceQuery = useSourceQuery(sourceId)
  const workspaceQuery = useWorkspaceQuery()
  const source = sourceQuery.data
  const jobsById = useIngestionJobsPolling(source ? [source.latestJob] : [])

  if (source === undefined) {
    if (isApiErrorWithCode(sourceQuery.error, 'SOURCE_NOT_FOUND')) {
      return <SourceNotFound />
    }
    if (sourceQuery.isError) {
      return <ErrorAlert errorCode={errorCodeOf(sourceQuery.error)} />
    }
    return <p className={styles.status}>Yükleniyor…</p>
  }

  const syncedText = source.syncedAt === null ? '–' : formatDateTime(source.syncedAt)
  return (
    <>
      <PageHeader
        section={source.name}
        title={source.name}
        parent={{ label: 'Bilgi kaynakları', to: '/kaynaklar' }}
        description={`${source.fileName} · ${formatInteger(source.fileSizeBytes)} bayt · sürüm ${source.version} · son senkron ${syncedText}`}
        actions={
          <a
            className={`${button.button} ${button.outline}`}
            href={sourceFileUrl(source.sourceId)}
            target="_blank"
            rel="noopener noreferrer"
          >
            PDF'i aç
          </a>
        }
      />
      <SourceMetaGrid source={source} workspace={workspaceQuery.data} />
      <IngestionJobsSection sourceId={source.sourceId} jobsById={jobsById} />
      <ChunksSection source={source} />
    </>
  )
}

type IngestionJobsSectionProps = {
  sourceId: number
  jobsById: Map<number, IngestionJobResponse>
}

function IngestionJobsSection({ sourceId, jobsById }: IngestionJobsSectionProps) {
  const jobsQuery = useIngestionJobsQuery(sourceId)
  const jobs = (jobsQuery.data?.content ?? []).map((job) => jobsById.get(job.jobId) ?? job)
  return (
    <>
      <div className={styles.section}>
        <h2>İçe alma işleri</h2>
      </div>
      {jobsQuery.data ? (
        <IngestionJobTable jobs={jobs} />
      ) : jobsQuery.isError ? (
        <ErrorAlert errorCode={errorCodeOf(jobsQuery.error)} />
      ) : (
        <p className={styles.status}>Yükleniyor…</p>
      )}
    </>
  )
}

type ChunkPaging = { query: string; page: number }

function ChunksSection({ source }: { source: SourceResponse }) {
  const [filterText, setFilterText] = useState('')
  const [paging, setPaging] = useState<ChunkPaging>({ query: '', page: 0 })
  const [selectedChunkId, setSelectedChunkId] = useState<number | null>(null)
  const query = useDebouncedValue(filterText.trim(), CHUNK_FILTER_DEBOUNCE_MS)
  const page = paging.query === query ? paging.page : 0
  const chunksQuery = useChunksQuery(source.sourceId, { query, page, size: CHUNK_PAGE_SIZE })
  const chunkPage = chunksQuery.data

  return (
    <>
      <div className={styles.section}>
        <h2>Parçalar</h2>
        {chunkPage ? <span>{`${chunkPage.totalElements} / ${source.chunkCount ?? 0}`}</span> : null}
      </div>
      <div className={styles.filterBox}>
        <svg
          className={styles.filterIcon}
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          aria-hidden="true"
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.5-3.5" />
        </svg>
        <input
          type="search"
          value={filterText}
          placeholder="Parçalarda ara (başlık ya da metin)"
          aria-label="Parçalarda ara"
          maxLength={CHUNK_FILTER_MAX_LENGTH}
          onChange={(event) => setFilterText(event.target.value)}
        />
      </div>
      {chunkPage ? (
        <>
          <ChunkTable chunks={chunkPage.content} onOpen={setSelectedChunkId} />
          <Pagination
            label="Parça sayfaları"
            page={page}
            totalPages={chunkPage.totalPages}
            onPageChange={(nextPage) => setPaging({ query, page: nextPage })}
          />
        </>
      ) : chunksQuery.isError ? (
        <ErrorAlert errorCode={errorCodeOf(chunksQuery.error)} />
      ) : (
        <p className={styles.status}>Yükleniyor…</p>
      )}
      {selectedChunkId === null ? null : (
        <ChunkDialog chunkId={selectedChunkId} onClose={() => setSelectedChunkId(null)} />
      )}
    </>
  )
}
