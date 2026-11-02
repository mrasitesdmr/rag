import type { MouseEvent } from 'react'
import table from '../../../shared/components/table.module.css'
import { formatDecimal } from '../../../shared/format/formatNumber.ts'
import { DISTANCE_FRACTION_DIGITS, RERANK_FRACTION_DIGITS } from '../model/conversationRules.ts'
import type { TurnChunkResponse } from '../model/types.ts'
import styles from './TurnChunkTable.module.css'

type TurnChunkTableProps = {
  chunks: TurnChunkResponse[]
  cited: number[]
  hasRerank: boolean
  onChunkOpen: (chunkId: number) => void
}

export function TurnChunkTable({ chunks, cited, hasRerank, onChunkOpen }: TurnChunkTableProps) {
  return (
    <div className={table.scroll}>
      <table className={table.table}>
        <thead>
          <tr>
            <th>Prompt</th>
            <th>Parça</th>
            <th>Vector #</th>
            <th>Mesafe</th>
            {hasRerank ? <th>Rerank</th> : null}
            <th>Atıf</th>
          </tr>
        </thead>
        <tbody>
          {chunks.map((chunk) => (
            <TurnChunkRow
              key={chunk.n}
              chunk={chunk}
              isCited={cited.includes(chunk.n)}
              hasRerank={hasRerank}
              onChunkOpen={onChunkOpen}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

type TurnChunkRowProps = {
  chunk: TurnChunkResponse
  isCited: boolean
  hasRerank: boolean
  onChunkOpen: (chunkId: number) => void
}

function TurnChunkRow({ chunk, isCited, hasRerank, onChunkOpen }: TurnChunkRowProps) {
  function handleRowClick(event: MouseEvent<HTMLTableRowElement>) {
    event.currentTarget.querySelector('button')?.focus()
    onChunkOpen(chunk.chunkId)
  }

  return (
    // The chunk button is the keyboard path; a click on the rest of the row opens the chunk like the lab.
    // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions
    <tr className={table.clickable} onClick={handleRowClick}>
      <td className={table.num}>{`[${chunk.n}]`}</td>
      <td className={table.text}>
        <button type="button" className={styles.openButton} title={chunk.heading}>
          {`s. ${chunk.pageNumber} · #${chunk.chunkNo}`}
        </button>
      </td>
      <td className={table.num}>{chunk.vectorRank}</td>
      <td className={table.num}>{formatDecimal(chunk.distance, DISTANCE_FRACTION_DIGITS)}</td>
      {hasRerank ? (
        <td className={table.num}>
          {chunk.rerankScore === null ? '–' : formatDecimal(chunk.rerankScore, RERANK_FRACTION_DIGITS)}
        </td>
      ) : null}
      <td>{isCited ? <span className={styles.used}>✓</span> : null}</td>
    </tr>
  )
}
