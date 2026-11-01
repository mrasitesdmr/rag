import { useState } from 'react'
import { ChunkDialog } from '../../../shared/chunk/ChunkDialog.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import hidden from '../../../shared/components/visuallyHidden.module.css'
import { ContextChunksCard } from '../components/ContextChunksCard.tsx'
import { HowItWorksCard } from '../components/HowItWorksCard.tsx'
import { MessageComposer } from '../components/MessageComposer.tsx'
import { MessageList } from '../components/MessageList.tsx'
import { PhoneFrame } from '../components/PhoneFrame.tsx'
import { useAnswerStream } from '../hooks/useAnswerStream.ts'
import type { PreviewTurn } from '../model/types.ts'
import styles from './PreviewPage.module.css'

export function PreviewPage() {
  const answerStream = useAnswerStream()
  const [openChunkId, setOpenChunkId] = useState<number | null>(null)
  const lastTurn = answerStream.turns.at(-1)

  return (
    <>
      <PageHeader
        section="Önizleme"
        title="Asistan önizleme"
        description="Raşit Mobil'deki asistanı müşterinin gördüğü hâliyle dene; önizleme konuşmaları da Konuşmalar'a düşer."
      />
      <div className={styles.container}>
        <div className={styles.preview}>
          <div className={styles.phoneSlot}>
            <PhoneFrame isStreaming={answerStream.isStreaming} onNewConversation={answerStream.startNewConversation}>
              <MessageList
                previousTurns={answerStream.previousTurns}
                turns={answerStream.turns}
                conversationId={answerStream.conversationId}
                onCitationOpen={setOpenChunkId}
              />
              <MessageComposer isStreaming={answerStream.isStreaming} onSend={answerStream.sendQuestion} />
            </PhoneFrame>
          </div>
          <div className={styles.side}>
            {lastTurn !== undefined && lastTurn.phase === 'done' && lastTurn.sources !== null && lastTurn.done !== null ? (
              <ContextChunksCard
                question={lastTurn.question}
                sources={lastTurn.sources}
                done={lastTurn.done}
                onChunkOpen={setOpenChunkId}
              />
            ) : (
              <HowItWorksCard />
            )}
          </div>
        </div>
      </div>
      <div className={hidden.visuallyHidden} aria-live="polite">
        {announcementFor(lastTurn)}
      </div>
      {openChunkId === null ? null : <ChunkDialog chunkId={openChunkId} onClose={() => setOpenChunkId(null)} />}
    </>
  )
}

function announcementFor(turn: PreviewTurn | undefined): string {
  if (turn === undefined) {
    return ''
  }
  switch (turn.phase) {
    case 'waiting':
    case 'streaming':
      return 'Asistan yazıyor…'
    case 'done':
      return 'Cevap tamamlandı.'
    case 'failed':
      return ''
  }
}
