import { Dialog } from '../../../shared/components/Dialog.tsx'
import { ErrorAlert } from '../../../shared/components/ErrorAlert.tsx'
import type { SuggestionItem, SuggestionSetResponse } from '../model/types.ts'
import styles from './QuestionListDialog.module.css'

type QuestionListDialogProps = {
  suggestions: SuggestionSetResponse | undefined
  errorCode: string | null
  followUps: string[]
  isStreaming: boolean
  onSelect: (item: SuggestionItem) => void
  onClose: () => void
}

export function QuestionListDialog({
  suggestions,
  errorCode,
  followUps,
  isStreaming,
  onSelect,
  onClose,
}: QuestionListDialogProps) {
  const title =
    suggestions === undefined
      ? 'Kayıtlı sorular'
      : `Kayıtlı sorular · ${suggestions.questionCount} soru, ${suggestions.followUpCount} takip`

  function handleSelect(item: SuggestionItem) {
    onSelect(item)
    onClose()
  }

  return (
    <Dialog title={title} onClose={onClose}>
      <p className={styles.note}>
        Ürün ekibinin hazırladığı öneri sorular; sorunu her zaman kendin de yazabilirsin. Takip soruları ilk sorudan
        sonra "devam" önerisi olarak görünür.
      </p>
      {errorCode === null ? null : <ErrorAlert errorCode={errorCode} />}
      {suggestions !== undefined && suggestions.questionCount === 0 ? (
        <p className={styles.note}>
          Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.
        </p>
      ) : null}
      {suggestions !== undefined && (followUps.length > 0 || suggestions.categories.length > 0) ? (
        <div className={styles.list}>
          {followUps.length > 0 ? (
            <>
              <h4 className={styles.heading}>Bu konuşmada devam</h4>
              {followUps.map((text) => (
                <QuestionButton
                  key={`followUp-${text}`}
                  item={{ text, kind: 'followUp' }}
                  isDisabled={isStreaming}
                  onSelect={handleSelect}
                />
              ))}
            </>
          ) : null}
          {suggestions.categories.map((category) => (
            <div key={category.name} className={styles.category}>
              <h4 className={styles.heading}>{category.name}</h4>
              {category.questions.map((question) => (
                <QuestionButton
                  key={question.text}
                  item={{ text: question.text, kind: 'question' }}
                  isDisabled={isStreaming}
                  onSelect={handleSelect}
                />
              ))}
            </div>
          ))}
        </div>
      ) : null}
    </Dialog>
  )
}

type QuestionButtonProps = {
  item: SuggestionItem
  isDisabled: boolean
  onSelect: (item: SuggestionItem) => void
}

function QuestionButton({ item, isDisabled, onSelect }: QuestionButtonProps) {
  return (
    <button type="button" className={styles.question} disabled={isDisabled} onClick={() => onSelect(item)}>
      {item.text}
    </button>
  )
}
