import { useId } from 'react'
import { autocompleteOptionId } from '../model/suggestions.ts'
import type { SuggestionItem } from '../model/types.ts'
import styles from './AutocompleteList.module.css'

type AutocompleteListProps = {
  id: string
  items: SuggestionItem[]
  activeIndex: number | null
  hasTurns: boolean
  onSelect: (item: SuggestionItem) => void
}

type IndexedItem = { item: SuggestionItem; index: number }

export function AutocompleteList({ id, items, activeIndex, hasTurns, onSelect }: AutocompleteListProps) {
  const headingId = useId()
  const indexed = items.map((item, index) => ({ item, index }))
  const followUps = indexed.filter((entry) => entry.item.kind === 'followUp')
  const questions = indexed.filter((entry) => entry.item.kind === 'question')

  function renderOption({ item, index }: IndexedItem) {
    const isActive = index === activeIndex
    return (
      // Focus stays on the combobox input (APG pattern); arrow keys and Enter are handled there.
      // oxlint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
      <div
        key={`${item.kind}-${item.text}`}
        id={autocompleteOptionId(id, index)}
        role="option"
        aria-selected={isActive}
        className={isActive ? `${styles.option} ${styles.selected}` : styles.option}
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => onSelect(item)}
      >
        {item.text}
      </div>
    )
  }

  return (
    <div id={id} role="listbox" aria-label="Soru önerileri" className={styles.list}>
      {followUps.length > 0 ? (
        <div role="group" aria-labelledby={`${headingId}-follow-up`}>
          <div role="presentation" id={`${headingId}-follow-up`} className={styles.heading}>
            Bu konuşmada devam
          </div>
          {followUps.map(renderOption)}
        </div>
      ) : null}
      {questions.length > 0 && hasTurns ? (
        <div role="group" aria-labelledby={`${headingId}-new-conversation`}>
          <div role="presentation" id={`${headingId}-new-conversation`} className={styles.heading}>
            Yeni konuşmada
          </div>
          {questions.map(renderOption)}
        </div>
      ) : (
        questions.map(renderOption)
      )}
    </div>
  )
}
