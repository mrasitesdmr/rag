import { errorCodeOf } from '../api/ApiError.ts'
import button from '../components/button.module.css'
import { Dialog } from '../components/Dialog.tsx'
import { ErrorAlert } from '../components/ErrorAlert.tsx'
import styles from './ChunkDialog.module.css'
import { useChunkQuery } from './chunkQueries.ts'
import type { ChunkResponse } from './types.ts'

const EMBEDDING_FRACTION_DIGITS = 5
const LOADING_TITLE = 'Parça'

type ChunkDialogProps = { chunkId: number; onClose: () => void }

export function ChunkDialog({ chunkId, onClose }: ChunkDialogProps) {
  const chunkQuery = useChunkQuery(chunkId)
  const chunk = chunkQuery.data
  return (
    <Dialog title={chunk?.heading ?? LOADING_TITLE} onClose={onClose}>
      {chunk ? (
        <ChunkDetails chunk={chunk} />
      ) : chunkQuery.isError ? (
        <ErrorAlert errorCode={errorCodeOf(chunkQuery.error)} />
      ) : (
        <p className={styles.status}>Yükleniyor…</p>
      )}
      <div className={styles.actions}>
        <button type="button" className={`${button.button} ${button.outline}`} onClick={onClose}>
          Kapat
        </button>
      </div>
    </Dialog>
  )
}

function ChunkDetails({ chunk }: { chunk: ChunkResponse }) {
  return (
    <>
      <div className={styles.strip}>
        <span>
          chunk_id <b>{chunk.chunkId}</b>
        </span>
        <span>
          chunk_no <b>{chunk.chunkNo}</b>
        </span>
        <span>
          token <b>{chunk.tokenCount}</b>
        </span>
        <span>
          model <b>{chunk.embeddingModel}</b>
        </span>
        <span>
          kaynak <b>{`${chunk.sourceName} · ${chunk.version}`}</b>
        </span>
      </div>
      <div className={styles.text} tabIndex={0} role="region" aria-label="Parça metni">
        {chunk.content}
      </div>
      <p className={styles.vectors}>{embeddingText(chunk)}</p>
    </>
  )
}

function embeddingText(chunk: ChunkResponse): string {
  const values = chunk.embeddingPreview.map((value) => value.toFixed(EMBEDDING_FRACTION_DIGITS))
  const remaining = chunk.embeddingDimensions - chunk.embeddingPreview.length
  const items = remaining > 0 ? [...values, `… ${remaining} değer daha`] : values
  return `embedding [${items.join(', ')}]`
}
