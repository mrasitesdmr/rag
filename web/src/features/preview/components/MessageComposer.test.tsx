import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { describe, expect, it, vi } from 'vitest'
import type { SuggestionItem } from '../model/types.ts'
import { MessageComposer } from './MessageComposer.tsx'

const ASGARI: SuggestionItem = { text: 'Asgari ödeme tutarı nedir?', kind: 'question' }
const THREE_ITEMS: SuggestionItem[] = [
  { text: 'Birinci öneri', kind: 'question' },
  { text: 'İkinci öneri', kind: 'question' },
  { text: 'Üçüncü öneri', kind: 'question' },
]

function itemsFor(input: string): SuggestionItem[] {
  if (input.includes('asgari')) {
    return [ASGARI]
  }
  if (input.includes('öneri')) {
    return THREE_ITEMS
  }
  return []
}

type Overrides = {
  isStreaming?: boolean
  hasTurns?: boolean
  onSend?: (text: string) => void
  onSuggestionSelect?: (item: SuggestionItem) => void
  onQuestionListOpen?: () => void
}

function renderComposer(overrides: Overrides = {}) {
  return render(
    <MessageComposer
      isStreaming={overrides.isStreaming ?? false}
      hasTurns={overrides.hasTurns ?? false}
      autocompleteItemsFor={itemsFor}
      onSend={overrides.onSend ?? (() => undefined)}
      onSuggestionSelect={overrides.onSuggestionSelect ?? (() => undefined)}
      onQuestionListOpen={overrides.onQuestionListOpen ?? (() => undefined)}
    />,
  )
}

const input = () => screen.getByRole('combobox', { name: 'Soru' })

describe('MessageComposer', () => {
  it('givenWhitespaceOnly_whenTyped_thenSendDisabled', async () => {
    const onSend = vi.fn()
    renderComposer({ onSend })

    await userEvent.type(input(), '   ')
    await userEvent.keyboard('{Enter}')

    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
    expect(onSend).not.toHaveBeenCalled()
  })

  it('givenPasted1005Chars_whenPasted_thenInputKeeps1000', async () => {
    renderComposer()

    await userEvent.click(input())
    await userEvent.paste('a'.repeat(1005))

    expect(input()).toHaveValue('a'.repeat(1000))
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeEnabled()
  })

  it('givenTypedQuestion_whenEnterPressed_thenSendsAndClearsInput', async () => {
    const onSend = vi.fn()
    renderComposer({ onSend })

    await userEvent.type(input(), 'Kartımı kaybettim{Enter}')

    expect(onSend).toHaveBeenCalledExactlyOnceWith('Kartımı kaybettim')
    expect(input()).toHaveValue('')
  })

  it('givenStreaming_whenRendered_thenInputAndSendDisabledEvenWithText', () => {
    renderComposer({ isStreaming: true })

    expect(input()).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
  })

  it('givenStreaming_whenQuestionListButtonClicked_thenOpenCallbackStillCalled', async () => {
    const onQuestionListOpen = vi.fn()
    renderComposer({ isStreaming: true, onQuestionListOpen })

    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))

    expect(onQuestionListOpen).toHaveBeenCalledOnce()
  })

  it('givenTypedAsgari_whenArrowDownAndEnter_thenSelectedSuggestionSent', async () => {
    const onSuggestionSelect = vi.fn()
    const onSend = vi.fn()
    renderComposer({ onSuggestionSelect, onSend })

    await userEvent.type(input(), 'asgari')

    expect(screen.getByRole('listbox')).toBeInTheDocument()
    expect(input()).toHaveAttribute('aria-expanded', 'true')
    expect(input()).not.toHaveAttribute('aria-activedescendant')
    await userEvent.keyboard('{ArrowDown}')
    const option = screen.getByRole('option', { name: ASGARI.text })
    expect(option).toHaveAttribute('aria-selected', 'true')
    expect(input()).toHaveAttribute('aria-activedescendant', option.id)
    await userEvent.keyboard('{Enter}')

    expect(onSuggestionSelect).toHaveBeenCalledExactlyOnceWith(ASGARI)
    expect(onSend).not.toHaveBeenCalled()
    expect(input()).toHaveValue('')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('givenTypedAsgariWithoutArrow_whenEnter_thenTypedTextSentNotSuggestion', async () => {
    const onSuggestionSelect = vi.fn()
    const onSend = vi.fn()
    renderComposer({ onSuggestionSelect, onSend })

    await userEvent.type(input(), 'asgari{Enter}')

    expect(onSend).toHaveBeenCalledExactlyOnceWith('asgari')
    expect(onSuggestionSelect).not.toHaveBeenCalled()
  })

  it('givenOpenList_whenEscape_thenListClosedAndValueKept', async () => {
    renderComposer()

    await userEvent.type(input(), 'asgari')
    await userEvent.keyboard('{Escape}')

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(input()).toHaveValue('asgari')
    expect(input()).toHaveAttribute('aria-expanded', 'false')
  })

  it('givenUnlistedText_whenEnter_thenTypedTextSent', async () => {
    const onSend = vi.fn()
    renderComposer({ onSend })

    await userEvent.type(input(), 'Döviz kurları ne?')

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    expect(input()).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByText(/listeden seç/i)).not.toBeInTheDocument()
    await userEvent.keyboard('{Enter}')
    expect(onSend).toHaveBeenCalledExactlyOnceWith('Döviz kurları ne?')
  })

  it('givenThreeItems_whenArrowUpFromStart_thenWrapsToLast', async () => {
    renderComposer()
    await userEvent.type(input(), 'öneri')
    const options = screen.getAllByRole('option')

    await userEvent.keyboard('{ArrowUp}')
    expect(options[0]).toHaveAttribute('aria-selected', 'true')
    await userEvent.keyboard('{ArrowUp}')
    expect(options[2]).toHaveAttribute('aria-selected', 'true')
    expect(options[0]).toHaveAttribute('aria-selected', 'false')
    await userEvent.keyboard('{ArrowDown}')
    expect(options[0]).toHaveAttribute('aria-selected', 'true')
  })

  it('givenLastItemActive_whenArrowDown_thenWrapsToFirst', async () => {
    renderComposer()
    await userEvent.type(input(), 'öneri')

    await userEvent.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{ArrowDown}')

    expect(screen.getAllByRole('option')[0]).toHaveAttribute('aria-selected', 'true')
  })

  it('givenActiveOption_whenTypingMore_thenActiveSelectionReset', async () => {
    renderComposer()
    await userEvent.type(input(), 'öneri')
    await userEvent.keyboard('{ArrowDown}')

    await userEvent.type(input(), ' x')

    expect(input()).not.toHaveAttribute('aria-activedescendant')
  })

  it('givenOptionClicked_whenMouseSelect_thenSuggestionSelectedWithoutLosingItToBlur', async () => {
    const onSuggestionSelect = vi.fn()
    renderComposer({ onSuggestionSelect })
    await userEvent.type(input(), 'asgari')

    await userEvent.click(screen.getByRole('option', { name: ASGARI.text }))

    expect(onSuggestionSelect).toHaveBeenCalledExactlyOnceWith(ASGARI)
    expect(input()).toHaveValue('')
  })

  it('givenOpenList_whenInputBlurs_thenListClosed', async () => {
    renderComposer()
    await userEvent.type(input(), 'asgari')

    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
  })

  it('givenOpenAutocomplete_whenRendered_thenNoAxeViolations', async () => {
    const { container } = renderComposer({ hasTurns: true })
    await userEvent.type(input(), 'öneri')
    await userEvent.keyboard('{ArrowDown}')

    const results = await axe(container)

    expect(results.violations).toEqual([])
  })
})
