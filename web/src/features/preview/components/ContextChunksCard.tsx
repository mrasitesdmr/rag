import { useId } from 'react'
import { formatDecimal } from '../../../shared/format/formatNumber.ts'
import { shortenText } from '../../../shared/format/formatText.ts'
import { strategyLabel } from '../model/strategyLabels.ts'
import type { ContextChunk, DoneEvent, SourcesEvent } from '../model/types.ts'
import styles from './ContextChunksCard.module.css'
import { LatencyBars } from './LatencyBars.tsx'
import card from './sideCard.module.css'

const QUESTION_SHORT_LENGTH = 60
const RERANK_SCORE_DIGITS = 2
const DISTANCE_DIGITS = 3

type ContextChunksCardProps = {
  question: string
  sources: SourcesEvent
  done: DoneEvent
  onChunkOpen: (chunkId: number) => void
}

export function ContextChunksCard({ question, sources, done, onChunkOpen }: ContextChunksCardProps) {
  const titleId = useId()
  return (
    <section className={card.card} aria-labelledby={titleId}>
      <h2 id={titleId} className={card.title}>
        Son cevapta modele giden parçalar
      </h2>
      <div className={card.sub}>{`"${shortenText(question, QUESTION_SHORT_LENGTH)}" · ${strategyLabel(sources)}`}</div>
      <div className={styles.list}>
        {sources.contextChunks.map((chunk) => (
          <button key={chunk.n} type="button" className={styles.chunk} onClick={() => onChunkOpen(chunk.chunkId)}>
            <span className={styles.number}>
              {`[${chunk.n}]`}
              {sources.citations.includes(chunk.n) ? <span className={styles.check}> ✓</span> : null}
            </span>
            <span className={styles.text}>{`s. ${chunk.pageNumber} · ${chunk.heading} · #${chunk.chunkNo}`}</span>
            <small className={styles.scores}>{scoreText(chunk, sources)}</small>
          </button>
        ))}
      </div>
      <div className={card.sub}>✓ atıf yapılan</div>
      <LatencyBars done={done} />
    </section>
  )
}

function scoreText(chunk: ContextChunk, sources: SourcesEvent): string {
  const distance = `d ${formatDecimal(chunk.distance, DISTANCE_DIGITS)}`
  if (sources.retrievalStrategy === 'RERANK' && chunk.rerankScore !== null) {
    return `rerank ${formatDecimal(chunk.rerankScore, RERANK_SCORE_DIGITS)} · ${distance}`
  }
  return distance
}
