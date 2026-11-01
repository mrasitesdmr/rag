import { useState } from 'react'
import { errorCodeOf } from '../../../shared/api/ApiError.ts'
import button from '../../../shared/components/button.module.css'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import { Pagination } from '../../../shared/components/Pagination.tsx'
import { useRetryIngestionMutation, useSourcesQuery, useWorkspaceQuery } from '../api/sourceQueries.ts'
import { AddSourceDialog } from '../components/AddSourceDialog.tsx'
import { SourceEmptyState } from '../components/SourceEmptyState.tsx'
import { SourceTable } from '../components/SourceTable.tsx'
import { WorkspaceCallout } from '../components/WorkspaceCallout.tsx'
import { useIngestionJobsPolling } from '../hooks/useIngestionJobsPolling.ts'
import { SOURCE_PAGE_SIZE } from '../model/chunkRules.ts'
import styles from './SourcePages.module.css'

const EMBEDDING_MODEL_FALLBACK = 'embedding modeliyle'

export function SourceListPage() {
  const [page, setPage] = useState(0)
  const [isAddDialogOpen, setAddDialogOpen] = useState(false)
  const workspaceQuery = useWorkspaceQuery()
  const sourcesQuery = useSourcesQuery({ page, size: SOURCE_PAGE_SIZE })
  const retryIngestion = useRetryIngestionMutation()
  const sources = sourcesQuery.data?.content ?? []
  const jobsById = useIngestionJobsPolling(sources.map((source) => source.latestJob))
  const embeddingPhrase = workspaceQuery.data ? `${workspaceQuery.data.embeddingModel} ile` : EMBEDDING_MODEL_FALLBACK

  function openAddDialog() {
    setAddDialogOpen(true)
  }

  function handleRetry(sourceId: number) {
    retryIngestion.mutate(sourceId)
  }

  return (
    <>
      <PageHeader
        section="Bilgi kaynakları"
        title="Bilgi kaynakları"
        description={`Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça ${embeddingPhrase} embed edilip pgvector'e yazılır.`}
        actions={
          <button type="button" className={button.button} onClick={openAddDialog}>
            Belge ekle
          </button>
        }
      />
      {retryIngestion.isError ? <ErrorAlert errorCode={errorCodeOf(retryIngestion.error)} /> : null}
      {sourcesQuery.data === undefined ? (
        sourcesQuery.isError ? (
          <ErrorAlert errorCode={errorCodeOf(sourcesQuery.error)} />
        ) : (
          <p className={styles.status}>Yükleniyor…</p>
        )
      ) : sourcesQuery.data.totalElements === 0 ? (
        <SourceEmptyState onAdd={openAddDialog} />
      ) : (
        <>
          <SourceTable
            sources={sources}
            jobsById={jobsById}
            onRetry={handleRetry}
            retryingSourceId={retryIngestion.isPending ? retryIngestion.variables : null}
          />
          {sourcesQuery.data.totalPages > 1 ? (
            <Pagination
              label="Kaynak sayfaları"
              page={page}
              totalPages={sourcesQuery.data.totalPages}
              onPageChange={setPage}
            />
          ) : null}
        </>
      )}
      {workspaceQuery.data ? <WorkspaceCallout workspace={workspaceQuery.data} /> : null}
      {workspaceQuery.isError ? <ErrorAlert errorCode={errorCodeOf(workspaceQuery.error)} /> : null}
      {isAddDialogOpen ? <AddSourceDialog onClose={() => setAddDialogOpen(false)} /> : null}
    </>
  )
}
