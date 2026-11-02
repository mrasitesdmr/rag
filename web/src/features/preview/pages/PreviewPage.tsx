import { useMemo, useState } from 'react'
import { ApiError, errorCodeOf } from '../../../shared/api/ApiError.ts'
import { ChunkDialog } from '../../../shared/chunk/ChunkDialog.tsx'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import { PageHeader } from '../../../shared/components/PageHeader.tsx'
import hidden from '../../../shared/components/visuallyHidden.module.css'
import { useLiveConfigQuery } from '../../../shared/liveConfig/liveConfigQueries.ts'
import { useSuggestionsQuery } from '../api/previewQueries.ts'
import { ContextChunksCard } from '../components/ContextChunksCard.tsx'
import { HowItWorksCard } from '../components/HowItWorksCard.tsx'
import { LiveConfigStrip } from '../components/LiveConfigStrip.tsx'
import { MessageComposer } from '../components/MessageComposer.tsx'
import { MessageList } from '../components/MessageList.tsx'
import { PhoneFrame } from '../components/PhoneFrame.tsx'
import { QuestionListDialog } from '../components/QuestionListDialog.tsx'
import { SuggestionChips } from '../components/SuggestionChips.tsx'
import { useAnswerStream } from '../hooks/useAnswerStream.ts'
import { useHandoffRequests } from '../hooks/useHandoffRequests.ts'
import { useTurnFeedback } from '../hooks/useTurnFeedback.ts'
import { createSuggestionMatcher } from '../model/suggestionMatcher.ts'
import { autocompleteItems, followUpsFor, listQuestionTexts, suggestionChips } from '../model/suggestions.ts'
import type { PreviewTurn, SuggestionItem } from '../model/types.ts'
import styles from './PreviewPage.module.css'

const UNEXPECTED_ERROR = 'UNEXPECTED_ERROR'

export function PreviewPage() {
  const answerStream = useAnswerStream()
  const feedback = useTurnFeedback()
  const handoff = useHandoffRequests()
  const liveConfig = useLiveConfigQuery()
  const [openChunkId, setOpenChunkId] = useState<number | null>(null)
  const suggestionsQuery = useSuggestionsQuery()
  const suggestions = suggestionsQuery.data
  const [isQuestionListOpen, setIsQuestionListOpen] = useState(false)
  const matcher = useMemo(
    () => createSuggestionMatcher(suggestions === undefined ? [] : listQuestionTexts(suggestions)),
    [suggestions],
  )
  const turns = answerStream.turns
  const hasTurns = turns.length > 0
  const lastTurn = turns.at(-1)

  function autocompleteItemsFor(input: string): SuggestionItem[] {
    return suggestions === undefined ? [] : autocompleteItems(suggestions, turns, input, matcher)
  }

  function handleSuggestionSelect(item: SuggestionItem) {
    if (item.kind === 'followUp') {
      answerStream.sendQuestion(item.text)
    } else {
      answerStream.askInNewConversation(item.text)
    }
  }

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
                feedback={feedback}
                handoff={handoff}
                onCitationOpen={setOpenChunkId}
              />
              <SuggestionChips
                items={suggestions === undefined ? [] : suggestionChips(suggestions, turns)}
                hasTurns={hasTurns}
                isEmpty={suggestions !== undefined && suggestions.questionCount === 0}
                isDisabled={answerStream.isStreaming}
                onSelect={handleSuggestionSelect}
              />
              <MessageComposer
                isStreaming={answerStream.isStreaming}
                hasTurns={hasTurns}
                autocompleteItemsFor={autocompleteItemsFor}
                onSend={answerStream.sendQuestion}
                onSuggestionSelect={handleSuggestionSelect}
                onQuestionListOpen={() => setIsQuestionListOpen(true)}
              />
            </PhoneFrame>
          </div>
          <div className={styles.side}>
            {liveConfig.data !== undefined ? <LiveConfigStrip config={liveConfig.data} /> : null}
            {liveConfig.data === undefined && liveConfig.isError ? (
              <ErrorAlert errorCode={liveConfig.error instanceof ApiError ? liveConfig.error.errorCode : UNEXPECTED_ERROR} />
            ) : null}
            {lastTurn !== undefined &&
            lastTurn.phase === 'done' &&
            lastTurn.sources !== null &&
            lastTurn.sources.generationMode === 'RAG' &&
            lastTurn.done !== null ? (
              <ContextChunksCard
                question={lastTurn.question}
                sources={lastTurn.sources}
                done={lastTurn.done}
                onChunkOpen={setOpenChunkId}
              />
            ) : (
              <HowItWorksCard config={liveConfig.data} />
            )}
          </div>
        </div>
      </div>
      <div className={hidden.visuallyHidden} aria-live="polite">
        {announcementFor(lastTurn)}
      </div>
      {isQuestionListOpen ? (
        <QuestionListDialog
          suggestions={suggestions}
          errorCode={suggestions === undefined && suggestionsQuery.isError ? errorCodeOf(suggestionsQuery.error) : null}
          followUps={suggestions === undefined ? [] : followUpsFor(suggestions, turns)}
          isStreaming={answerStream.isStreaming}
          onSelect={handleSuggestionSelect}
          onClose={() => setIsQuestionListOpen(false)}
        />
      ) : null}
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
