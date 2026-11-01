import type { MouseEvent } from 'react'
import table from '../../../shared/components/table.module.css'
import { collapseWhitespace, shortenText } from '../../../shared/format/formatText.ts'
import { CHUNK_HEADING_PREVIEW_LENGTH, CHUNK_PREVIEW_LENGTH } from '../model/chunkRules.ts'
import type { ChunkSummaryResponse } from '../model/types.ts'
import styles from './ChunkTable.module.css'

type ChunkTableProps = { chunks: ChunkSummaryResponse[]; onOpen: (chunkId: number) => void }

export function ChunkTable({ chunks, onOpen }: ChunkTableProps) {
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>#</th>
            <th>Sayfa · başlık</th>
            <th>İlk satır</th>
            <th>Token</th>
          </tr>
        </thead>
        <tbody>
          {chunks.length === 0 ? (
            <tr>
              <td colSpan={4} className={table.text}>
                Eşleşen parça yok.
              </td>
            </tr>
          ) : (
            chunks.map((chunk) => <ChunkRow key={chunk.chunkId} chunk={chunk} onOpen={onOpen} />)
          )}
        </tbody>
      </table>
    </div>
  )
}

type ChunkRowProps = { chunk: ChunkSummaryResponse; onOpen: (chunkId: number) => void }

function ChunkRow({ chunk, onOpen }: ChunkRowProps) {
  function handleRowClick(event: MouseEvent<HTMLTableRowElement>) {
    event.currentTarget.querySelector('button')?.focus()
    onOpen(chunk.chunkId)
  }

  return (
    // The heading button is the keyboard path; a click on the rest of the row opens the chunk like the lab.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <tr className={table.clickable} onClick={handleRowClick}>
      <td className={table.num}>{chunk.chunkNo}</td>
      <td>
        {/* oxlint-disable-next-line jsx-a11y/control-has-associated-label -- the label is the page and heading text below */}
        <button type="button" className={styles.openButton}>
          <b>{`s. ${chunk.pageNumber}`}</b>
          <small>{shortenText(chunk.heading, CHUNK_HEADING_PREVIEW_LENGTH)}</small>
        </button>
      </td>
      <td className={table.text}>{shortenText(collapseWhitespace(chunk.content), CHUNK_PREVIEW_LENGTH)}</td>
      <td className={table.num}>{chunk.tokenCount}</td>
    </tr>
  )
}
