import { useId, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { canSendMessage, MESSAGE_MAX_LENGTH } from '../model/messageRules.ts'
import { autocompleteOptionId } from '../model/suggestions.ts'
import { normalizeSuggestionText } from '../model/suggestionText.ts'
import type { SuggestionItem } from '../model/types.ts'
import { AutocompleteList } from './AutocompleteList.tsx'
import styles from './MessageComposer.module.css'

type MessageComposerProps = {
  isStreaming: boolean
  hasTurns: boolean
  autocompleteItemsFor: (input: string) => SuggestionItem[]
  onSend: (text: string) => void
  onSuggestionSelect: (item: SuggestionItem) => void
  onQuestionListOpen: () => void
}

export function MessageComposer({
  isStreaming,
  hasTurns,
  autocompleteItemsFor,
  onSend,
  onSuggestionSelect,
  onQuestionListOpen,
}: MessageComposerProps) {
  const listId = useId()
  const [value, setValue] = useState('')
  const [isListOpen, setIsListOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState<number | null>(null)
  const items = isListOpen ? autocompleteItemsFor(value) : []
  const isListVisible = items.length > 0
  const canSend = !isStreaming && canSendMessage(value)

  function closeList() {
    setIsListOpen(false)
    setActiveIndex(null)
  }

  function reset() {
    setValue('')
    closeList()
  }

  function selectSuggestion(item: SuggestionItem) {
    onSuggestionSelect(item)
    reset()
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const activeItem = isListVisible && activeIndex !== null ? items.at(activeIndex) : undefined
    if (activeItem !== undefined) {
      selectSuggestion(activeItem)
      return
    }
    if (!canSend) {
      return
    }
    onSend(value)
    reset()
  }

  function handleChange(nextValue: string) {
    setValue(nextValue)
    setIsListOpen(normalizeSuggestionText(nextValue) !== '')
    setActiveIndex(null)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (!isListVisible) {
      return
    }
    const count = items.length
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActiveIndex((index) => (index === null ? 0 : (index + 1) % count))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActiveIndex((index) => (index === null ? 0 : (index - 1 + count) % count))
    } else if (event.key === 'Escape') {
      event.preventDefault()
      closeList()
    }
  }

  return (
    <form className={styles.composer} autoComplete="off" noValidate onSubmit={handleSubmit}>
      {isListVisible ? (
        <AutocompleteList
          id={listId}
          items={items}
          activeIndex={activeIndex}
          hasTurns={hasTurns}
          onSelect={selectSuggestion}
        />
      ) : null}
      <button
        type="button"
        className={`${styles.iconButton} ${styles.light}`}
        aria-label="Soru listesi"
        title="Kayıtlı sorular"
        onClick={onQuestionListOpen}
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.2"
          aria-hidden="true"
          focusable="false"
        >
          <path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" />
        </svg>
      </button>
      <input
        className={styles.input}
        name="text"
        role="combobox"
        aria-label="Soru"
        aria-autocomplete="list"
        aria-expanded={isListVisible}
        aria-controls={isListVisible ? listId : undefined}
        aria-activedescendant={
          isListVisible && activeIndex !== null ? autocompleteOptionId(listId, activeIndex) : undefined
        }
        placeholder="Sorunu yaz…"
        maxLength={MESSAGE_MAX_LENGTH}
        value={value}
        disabled={isStreaming}
        onChange={(event) => handleChange(event.target.value)}
        onKeyDown={handleKeyDown}
        onBlur={closeList}
      />
      <button type="submit" className={styles.iconButton} aria-label="Gönder" disabled={!canSend}>
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2.4"
          aria-hidden="true"
          focusable="false"
        >
          <path d="M5 12h14M13 6l6 6-6 6" />
        </svg>
      </button>
    </form>
  )
}
