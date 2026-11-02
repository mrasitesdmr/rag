import { Callout } from '../../../shared/components/Callout.tsx'
import { StatusBadge } from '../../../shared/components/StatusBadge.tsx'
import { formatDuration } from '../../../shared/format/formatDuration.ts'
import { formatInteger } from '../../../shared/format/formatNumber.ts'
import { AnswerText } from '../../../shared/turn/AnswerText.tsx'
import { LLM_ONLY_NO_CHUNKS, NO_ANSWER, NO_CHUNKS } from '../model/conversationTexts.ts'
import {
  failureReasonLabel,
  generationLabel,
  guideVersionLabel,
  hasDistinctSearchQuery,
} from '../model/turnLabels.ts'
import { TURN_STATUS_LABELS, turnStatusTone } from '../model/turnStatus.ts'
import type { TurnResponse } from '../model/types.ts'
import styles from './turnCard.module.css'
import { TurnChunkTable } from './TurnChunkTable.tsx'

type TurnCardProps = { turn: TurnResponse; onChunkOpen: (chunkId: number) => void }

export function TurnCard({ turn, onChunkOpen }: TurnCardProps) {
  const isLlmOnly = turn.generationMode === 'LLM_ONLY'
  const guideVersion = guideVersionLabel(turn)

  function openCitation(n: number) {
    const chunk = turn.contextChunks.find((contextChunk) => contextChunk.n === n)
    if (chunk !== undefined) {
      onChunkOpen(chunk.chunkId)
    }
  }

  return (
    <article className={styles.turnCard}>
      <div className={styles.question}>{`${turn.turnNo}. ${turn.question}`}</div>
      {hasDistinctSearchQuery(turn) ? (
        <div className={styles.searchQuery}>
          Arama sorgusu: <code>{turn.searchQuery}</code>
        </div>
      ) : null}
      <div className={styles.answer}>
        {turn.answer === null ? (
          <p>{NO_ANSWER}</p>
        ) : (
          <AnswerText
            text={turn.answer}
            contextSize={isLlmOnly ? null : turn.contextChunks.length}
            onCitation={openCitation}
          />
        )}
      </div>
      <div className={styles.numbers}>
        <span>
          <StatusBadge tone={turnStatusTone(turn.status)}>{TURN_STATUS_LABELS[turn.status]}</StatusBadge>
        </span>
        <span>
          üretim <b>{generationLabel(turn)}</b>
        </span>
        {guideVersion === null ? null : (
          <span>
            rehber <b>{guideVersion}</b>
          </span>
        )}
        {turn.promptTokens === null ? null : (
          <span>
            prompt <b>{formatInteger(turn.promptTokens)}</b> token
          </span>
        )}
        {turn.completionTokens === null ? null : (
          <span>
            cevap <b>{formatInteger(turn.completionTokens)}</b> token
          </span>
        )}
        <span>
          süre <b>{formatDuration(turn.latencyMs)}</b>
        </span>
        {turn.helpful === null ? null : (
          <span>
            geri bildirim <b>{turn.helpful ? 'Evet' : 'Hayır'}</b>
          </span>
        )}
        {turn.status === 'INCOMPLETE' && turn.errorCode !== null ? (
          <span>
            sebep <b>{failureReasonLabel(turn.errorCode)}</b>
          </span>
        ) : null}
      </div>
      {isLlmOnly ? (
        <Callout tone="warning">{LLM_ONLY_NO_CHUNKS}</Callout>
      ) : turn.contextChunks.length === 0 ? (
        <Callout tone="warning">{NO_CHUNKS}</Callout>
      ) : (
        <TurnChunkTable
          chunks={turn.contextChunks}
          cited={turn.cited}
          hasRerank={turn.retrievalStrategy === 'RERANK'}
          onChunkOpen={onChunkOpen}
        />
      )}
    </article>
  )
}
