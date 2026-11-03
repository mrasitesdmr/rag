import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { errorCodeOf, isApiErrorWithCode } from '../../../shared/api/ApiError.ts'
import { ChunkDialog } from '../../../shared/chunk/ChunkDialog.tsx'
import type { ChunkCitation } from '../../../shared/chunk/types.ts'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import { conversationLabel } from '../../../shared/conversation/conversationLabel.ts'
import { formatListTime } from '../../../shared/format/formatDateTime.ts'
import { useConversationDetailQuery } from '../api/conversationQueries.ts'
import { HandoffLine } from '../components/HandoffLine.tsx'
import { InProgressTurnCard } from '../components/InProgressTurnCard.tsx'
import { TurnCard } from '../components/TurnCard.tsx'
import { BACK_TO_LIST, NO_TURNS, NOT_FOUND_TITLE } from '../model/conversationTexts.ts'
import { channelLabel } from '../model/turnLabels.ts'
import type { ConversationDetailResponse } from '../model/types.ts'
import styles from './ConversationPages.module.css'

const CONVERSATION_ID_PATTERN = /^\d+$/
const CONVERSATION_NOT_FOUND = 'CONVERSATION_NOT_FOUND'

export function ConversationDetailPage() {
  const { conversationId } = useParams()
  if (conversationId === undefined || !CONVERSATION_ID_PATTERN.test(conversationId)) {
    return <ConversationNotFound />
  }
  const id = Number(conversationId)
  return <ConversationDetail key={id} conversationId={id} />
}

function ConversationNotFound() {
  return (
    <div className={styles.notFound}>
      <title>Konuşma bulunamadı · Raşit Bankası Dijital Asistan Yönetimi</title>
      <h1>{NOT_FOUND_TITLE}</h1>
      <Link to="/konusmalar">{BACK_TO_LIST}</Link>
    </div>
  )
}

function ConversationDetail({ conversationId }: { conversationId: number }) {
  const detailQuery = useConversationDetailQuery(conversationId)
  const detail = detailQuery.data

  if (detail === undefined) {
    if (isApiErrorWithCode(detailQuery.error, CONVERSATION_NOT_FOUND)) {
      return <ConversationNotFound />
    }
    if (detailQuery.isError) {
      return <ErrorAlert errorCode={errorCodeOf(detailQuery.error)} />
    }
    return <p className={styles.status}>Yükleniyor…</p>
  }
  return <ConversationDetailContent detail={detail} />
}

function ConversationDetailContent({ detail }: { detail: ConversationDetailResponse }) {
  const [openCitation, setOpenCitation] = useState<ChunkCitation | null>(null)
  const [now] = useState(() => new Date())
  const label = conversationLabel(detail.conversationId)
  const turnCount = detail.turns.length + (detail.inProgressTurn === null ? 0 : 1)

  return (
    <>
      <PageHeader
        section={label}
        title={label}
        parent={{ label: 'Konuşmalar', to: '/konusmalar' }}
        description={`${channelLabel(detail.channel)} · ${formatListTime(detail.createdAt, now)} · ${turnCount} tur`}
      />
      {detail.handoff === null ? null : <HandoffLine handoff={detail.handoff} />}
      {turnCount === 0 ? <p className={styles.status}>{NO_TURNS}</p> : null}
      {detail.turns.map((turn) => (
        <TurnCard key={turn.turnId} turn={turn} onChunkOpen={setOpenCitation} />
      ))}
      {detail.inProgressTurn === null ? null : <InProgressTurnCard turn={detail.inProgressTurn} />}
      {openCitation === null ? null : (
        <ChunkDialog
          chunkId={openCitation.chunkId}
          citationNumber={openCitation.n}
          onClose={() => setOpenCitation(null)}
        />
      )}
    </>
  )
}
