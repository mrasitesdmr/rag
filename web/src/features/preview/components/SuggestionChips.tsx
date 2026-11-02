import type { SuggestionItem } from '../model/types.ts'
import styles from './SuggestionChips.module.css'

type SuggestionChipsProps = {
  items: SuggestionItem[]
  hasTurns: boolean
  isEmpty: boolean
  isDisabled: boolean
  onSelect: (item: SuggestionItem) => void
}

export function SuggestionChips({ items, hasTurns, isEmpty, isDisabled, onSelect }: SuggestionChipsProps) {
  return (
    <div className={styles.chips} role="group" aria-label="Soru önerileri">
      {isEmpty ? (
        <p className={styles.empty}>Henüz soru önerisi yok.</p>
      ) : (
        items.map((item) => (
          <button
            key={`${item.kind}-${item.text}`}
            type="button"
            className={item.kind === 'followUp' ? `${styles.chip} ${styles.followUp}` : styles.chip}
            title={titleFor(item, hasTurns)}
            disabled={isDisabled}
            onClick={() => onSelect(item)}
          >
            {item.text}
          </button>
        ))
      )}
    </div>
  )
}

function titleFor(item: SuggestionItem, hasTurns: boolean): string | undefined {
  if (item.kind === 'followUp') {
    return 'Bu konuşmada devam'
  }
  return hasTurns ? 'Yeni konuşmada sorulur' : undefined
}
