import { Fragment } from 'react'
import { Link } from 'react-router'
import { messageFor } from '../../../shared/api/errorMessages.ts'
import { formatAnswer } from '../model/answerFormat.ts'
import type { AnswerInline } from '../model/answerFormat.ts'
import { citedChunks, contextChunkOf, sourceButtonLabel, sourceButtonTitle } from '../model/citations.ts'
import { MESSAGE_RULE_MESSAGE } from '../model/messageRules.ts'
import type { PreviewTurn, SourcesEvent } from '../model/types.ts'
import styles from './AnswerMessage.module.css'
import message from './message.module.css'

const VALIDATION_FAILED = 'VALIDATION_FAILED'

type AnswerMessageProps = { turn: PreviewTurn; onCitationOpen: (chunkId: number) => void }

export function AnswerMessage({ turn, onCitationOpen }: AnswerMessageProps) {
  const isFailed = turn.phase === 'failed'
  const doneSources = turn.phase === 'done' ? turn.sources : null
  const className = `${message.message} ${message.assistant}${isFailed ? ` ${message.bad}` : ''}`

  if (turn.phase === 'waiting' && turn.answer === '') {
    return (
      <div className={className}>
        <div className={message.bubble}>
          <span className={styles.typing} aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
        </div>
      </div>
    )
  }

  const blocks = formatAnswer(turn.answer, doneSources === null ? null : doneSources.contextChunks.length)
  const sourceChunks = doneSources !== null && turn.done?.notFoundAnswer === false ? citedChunks(doneSources) : []

  function openCitation(n: number) {
    const chunk = doneSources === null ? undefined : contextChunkOf(doneSources, n)
    if (chunk !== undefined) {
      onCitationOpen(chunk.chunkId)
    }
  }

  return (
    <div className={className}>
      <div className={message.bubble}>
        {blocks.map((block, blockIndex) =>
          block.kind === 'paragraph' ? (
            <p key={blockIndex}>
              <Inlines inlines={block.inlines} onCitation={openCitation} />
            </p>
          ) : (
            <ul key={blockIndex}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>
                  <Inlines inlines={item} onCitation={openCitation} />
                </li>
              ))}
            </ul>
          ),
        )}
        {isFailed && turn.errorCode !== null ? (
          <p role="alert" className={styles.error}>
            {turn.errorCode === VALIDATION_FAILED ? MESSAGE_RULE_MESSAGE : messageFor(turn.errorCode)}
          </p>
        ) : null}
      </div>
      {sourceChunks.length > 0 ? (
        <div className={styles.sources}>
          {sourceChunks.map((chunk) => (
            <button
              key={chunk.chunkId}
              type="button"
              className={styles.sourceButton}
              title={sourceButtonTitle(chunk)}
              onClick={() => onCitationOpen(chunk.chunkId)}
            >
              {sourceButtonLabel(chunk)}
            </button>
          ))}
        </div>
      ) : null}
      {doneSources !== null && isEmptyKnowledgeBase(doneSources) ? (
        <div className={styles.note}>
          <span>Bilgi bankasında henüz belge yok.</span>
          <Link to="/kaynaklar" className={styles.noteLink}>
            Bilgi kaynakları'ndan belge ekle.
          </Link>
        </div>
      ) : null}
    </div>
  )
}

function isEmptyKnowledgeBase(sources: SourcesEvent): boolean {
  return sources.generationMode === 'RAG' && sources.contextChunks.length === 0
}

function Inlines({ inlines, onCitation }: { inlines: AnswerInline[]; onCitation: (n: number) => void }) {
  return inlines.map((inline, index) => {
    switch (inline.kind) {
      case 'text':
        return <Fragment key={index}>{inline.text}</Fragment>
      case 'bold':
        return (
          <strong key={index}>
            <Inlines inlines={inline.children} onCitation={onCitation} />
          </strong>
        )
      case 'citation':
        return (
          <button
            key={index}
            type="button"
            className={styles.cite}
            aria-label={`Kaynak ${inline.n}`}
            onClick={() => onCitation(inline.n)}
          >
            {inline.n}
          </button>
        )
    }
  })
}
