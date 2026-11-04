import { useId } from 'react'
import type { ChunkCitation } from '../../../shared/chunk/types.ts'
import { formatDecimal } from '../../../shared/format/formatNumber.ts'
import { shortenText } from '../../../shared/format/formatText.ts'
import { queryRewriteLabel, strategyLabel } from '../model/strategyLabels.ts'
import type { ContextChunk, DoneEvent, SourcesEvent } from '../model/types.ts'
import styles from './ContextChunksCard.module.css'
import { LatencyBars } from './LatencyBars.tsx'
import card from './sideCard.module.css'

const QUESTION_SHORT_LENGTH = 60
const RERANK_SCORE_DIGITS = 2
const DISTANCE_DIGITS = 3
const CITED_LEGEND = '✓ atıf yapılan'
const ANSWER_CHUNK_LEGEND = 'sarı: cevabın geçtiği parça'
const BELOW_THRESHOLD_NOTE = 'Bu turda modele hiçbir parça gitmedi: en iyi parçanın rerank puanı eşiğin altında kaldı.'

type ContextChunksCardProps = {
  question: string
  sources: SourcesEvent
  done: DoneEvent
  onChunkOpen: (citation: ChunkCitation) => void
}

export function ContextChunksCard({ question, sources, done, onChunkOpen }: ContextChunksCardProps) {
  const titleId = useId()
  const isBelowThreshold = done.answerSource === 'BELOW_RERANK_THRESHOLD'
  return (
    <section className={card.card} aria-labelledby={titleId}>
      <h2 id={titleId} className={card.title}>
        Son cevapta modele giden parçalar
      </h2>
      <div className={card.sub}>{subtitleText(question, sources)}</div>
      {isBelowThreshold ? (
        <div className={card.sub}>{BELOW_THRESHOLD_NOTE}</div>
      ) : (
        <ContextChunkList sources={sources} onChunkOpen={onChunkOpen} />
      )}
      <LatencyBars done={done} />
    </section>
  )
}

type ContextChunkListProps = {
  sources: SourcesEvent
  onChunkOpen: (citation: ChunkCitation) => void
}

function ContextChunkList({ sources, onChunkOpen }: ContextChunkListProps) {
  return (
    <>
      <div className={styles.list}>
        {sources.contextChunks.map((chunk) => (
          <button
            key={chunk.n}
            type="button"
            className={chunk.containsAnswer ? `${styles.chunk} ${styles.gold}` : styles.chunk}
            onClick={() => onChunkOpen({ chunkId: chunk.chunkId, n: chunk.n })}
          >
            <span className={styles.number}>
              {`[${chunk.n}]`}
              {sources.citations.includes(chunk.n) ? <span className={styles.check}> ✓</span> : null}
            </span>
            <span className={styles.text}>{`s. ${chunk.pageNumber} · ${chunk.heading} · #${chunk.chunkNo}`}</span>
            <small className={styles.scores}>{scoreText(chunk, sources)}</small>
          </button>
        ))}
      </div>
      <div className={card.sub}>{legendText(sources)}</div>
    </>
  )
}

function legendText(sources: SourcesEvent): string {
  return sources.contextChunks.some((chunk) => chunk.containsAnswer)
    ? `${CITED_LEGEND} · ${ANSWER_CHUNK_LEGEND}`
    : CITED_LEGEND
}

function subtitleText(question: string, sources: SourcesEvent): string {
  const subtitle = `"${shortenText(question, QUESTION_SHORT_LENGTH)}" · ${strategyLabel(sources)}`
  const rewriteLabel = queryRewriteLabel(sources)
  return rewriteLabel === null ? subtitle : `${subtitle} · ${rewriteLabel}`
}

function scoreText(chunk: ContextChunk, sources: SourcesEvent): string {
  const distance = `d ${formatDecimal(chunk.distance, DISTANCE_DIGITS)}`
  if (sources.retrievalStrategy === 'RERANK' && chunk.rerankScore !== null) {
    return `rerank ${formatDecimal(chunk.rerankScore, RERANK_SCORE_DIGITS)} · ${distance}`
  }
  return distance
}
