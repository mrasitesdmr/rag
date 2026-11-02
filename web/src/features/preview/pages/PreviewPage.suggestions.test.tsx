import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { done, lostCardAnswer, sources, token } from '../../../test/previewFixtures.ts'
import { servePreview } from '../../../test/previewHandlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'
import { controllableSseStream } from '../../../test/sse.ts'
import {
  emptySuggestionSet,
  FOLLOW_UP_FREEZE,
  FOLLOW_UP_NEW_CARD,
  FORGOT_PASSWORD,
  LOST_CARD,
  MINIMUM_PAYMENT,
  serveSuggestions,
  suggestionSet,
} from '../../../test/suggestionFixtures.ts'

const chips = () => screen.getByRole('group', { name: 'Soru önerileri' })
const chip = (name: string) => within(chips()).getByRole('button', { name })
const input = () => screen.getByRole('combobox', { name: 'Soru' })

async function waitForChips() {
  await within(chips()).findByRole('button', { name: LOST_CARD })
}

async function askLostCardViaChip() {
  await waitForChips()
  await userEvent.click(chip(LOST_CARD))
  await within(chips()).findByRole('button', { name: FOLLOW_UP_NEW_CARD })
}

describe('PreviewPage suggestions', () => {
  it('givenEmptyConversation_whenStarterChipClicked_thenQuestionAsked', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await waitForChips()
    expect(within(chips()).getAllByRole('button').map((button) => button.textContent)).toEqual([LOST_CARD, MINIMUM_PAYMENT])
    expect(chip(MINIMUM_PAYMENT)).not.toHaveAttribute('title')
    await userEvent.click(chip(LOST_CARD))

    await screen.findByRole('button', { name: 'Kaynak 1' })
    expect(log.conversationBodies).toHaveLength(1)
    expect(log.messages).toHaveLength(1)
    expect(log.messages[0]?.body).toEqual({ text: LOST_CARD })
  })

  it('givenAnsweredListQuestion_whenFollowUpChipClicked_thenSameConversation', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await askLostCardViaChip()

    const followUp = chip(FOLLOW_UP_NEW_CARD)
    expect(followUp).toHaveAttribute('title', 'Bu konuşmada devam')
    expect(within(chips()).getAllByRole('button')[0]).toBe(followUp)
    await userEvent.click(followUp)

    await waitFor(() => expect(log.messages).toHaveLength(2))
    expect(log.messages[1]?.body).toEqual({ text: FOLLOW_UP_NEW_CARD })
    expect(log.messages[1]?.conversationId).toBe(log.messages[0]?.conversationId)
    expect(log.conversationBodies).toHaveLength(1)
  })

  it('givenConversationWithTurn_whenQuestionChipClicked_thenNewConversation', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await askLostCardViaChip()

    const other = chip(MINIMUM_PAYMENT)
    expect(other).toHaveAttribute('title', 'Yeni konuşmada sorulur')
    await userEvent.click(other)

    expect(await screen.findByText('Yeni konuşma · 2')).toBeInTheDocument()
    expect(log.conversationBodies).toHaveLength(2)
    expect(log.messages[1]?.conversationId).toBe(2)
    expect(log.messages[1]?.body).toEqual({ text: MINIMUM_PAYMENT })
    expect(screen.getByText(LOST_CARD, { selector: 'div,p,span' })).toBeInTheDocument()
  })

  it('givenStreamingAnswer_whenRendered_thenChipsDisabled', async () => {
    serveSuggestions(suggestionSet())
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')
    await waitForChips()

    await userEvent.click(chip(LOST_CARD))
    stream.push(token('Kartını '))
    await screen.findByText('Kartını')

    for (const button of within(chips()).getAllByRole('button')) {
      expect(button).toBeDisabled()
    }
    stream.push(token('kapat [1].'), sources(), done())
    stream.close()
    await waitFor(() => expect(chip(FOLLOW_UP_NEW_CARD)).toBeEnabled())
    expect(chip(MINIMUM_PAYMENT)).toBeEnabled()
  })

  it('givenConversationWithTurn_whenTyping_thenFollowUpAndNewConversationGroups', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await askLostCardViaChip()

    await userEvent.type(input(), 'kart')

    const followUpGroup = screen.getByRole('group', { name: 'Bu konuşmada devam' })
    const newGroup = screen.getByRole('group', { name: 'Yeni konuşmada' })
    expect(within(followUpGroup).getAllByRole('option').map((o) => o.textContent)).toEqual([FOLLOW_UP_FREEZE])
    expect(within(newGroup).getAllByRole('option').map((o) => o.textContent)).toContain(FORGOT_PASSWORD)
    await userEvent.click(within(newGroup).getByRole('option', { name: FORGOT_PASSWORD }))

    expect(await screen.findByText('Yeni konuşma · 2')).toBeInTheDocument()
    expect(log.conversationBodies).toHaveLength(2)
    expect(log.messages[1]?.body).toEqual({ text: FORGOT_PASSWORD })
  })

  it('givenEmptyConversation_whenTypingAsgariArrowDownEnter_thenSuggestionAskedInOneConversation', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await waitForChips()

    await userEvent.type(input(), 'asgari')
    expect(screen.queryByRole('group', { name: 'Yeni konuşmada' })).not.toBeInTheDocument()
    await userEvent.keyboard('{ArrowDown}{Enter}')

    await waitFor(() => expect(log.messages).toHaveLength(1))
    expect(log.messages[0]?.body).toEqual({ text: MINIMUM_PAYMENT })
    expect(log.conversationBodies).toHaveLength(1)
  })

  it('givenUnlistedQuestion_whenEnter_thenAskedAsTyped', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await waitForChips()

    await userEvent.type(input(), 'Döviz kurları ne?{Enter}')

    await waitFor(() => expect(log.messages).toHaveLength(1))
    expect(log.messages[0]?.body).toEqual({ text: 'Döviz kurları ne?' })
  })

  it('givenSuggestions_whenQuestionListOpenedAndQuestionClicked_thenAskedAndClosed', async () => {
    serveSuggestions(suggestionSet())
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await askLostCardViaChip()
    const opener = screen.getByRole('button', { name: 'Soru listesi' })

    await userEvent.click(opener)

    const dialog = await screen.findByRole('dialog', { name: 'Kayıtlı sorular · 4 soru, 2 takip' })
    expect(within(dialog).getAllByRole('heading', { level: 4 }).map((h) => h.textContent)).toEqual([
      'Bu konuşmada devam',
      'Kartlar',
      'Krediler',
    ])
    expect(within(dialog).getByRole('button', { name: FOLLOW_UP_FREEZE })).toBeEnabled()
    await userEvent.click(within(dialog).getByRole('button', { name: FORGOT_PASSWORD }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await waitFor(() => expect(log.messages).toHaveLength(2))
    expect(log.messages[1]?.body).toEqual({ text: FORGOT_PASSWORD })
    expect(log.conversationBodies).toHaveLength(2)
    expect(opener).toHaveFocus()
  })

  it('givenEmptyConversation_whenQuestionListOpened_thenNoFollowUpHeadingAndEscapeClosesWithFocusBack', async () => {
    serveSuggestions(suggestionSet())
    renderAppAt('/onizleme')
    await waitForChips()
    const opener = screen.getByRole('button', { name: 'Soru listesi' })

    await userEvent.click(opener)

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).queryByRole('heading', { name: 'Bu konuşmada devam' })).not.toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Kapat' })).toHaveFocus()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(opener).toHaveFocus()
  })

  it('givenStreamingAnswer_whenQuestionListOpened_thenQuestionsDisabledButListOpens', async () => {
    serveSuggestions(suggestionSet())
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')
    await waitForChips()
    await userEvent.click(chip(LOST_CARD))
    stream.push(token('Kartını '))
    await screen.findByText('Kartını')

    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('button', { name: MINIMUM_PAYMENT })).toBeDisabled()
    stream.close()
  })

  it('givenEmptySuggestionSet_whenPageOpens_thenEmptyStates', async () => {
    serveSuggestions(emptySuggestionSet())
    renderAppAt('/onizleme')

    expect(await within(chips()).findByText('Henüz soru önerisi yok.')).toBeInTheDocument()
    await userEvent.type(input(), 'kart')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))
    const dialog = await screen.findByRole('dialog', { name: 'Kayıtlı sorular · 0 soru, 0 takip' })
    expect(
      within(dialog).getByText('Henüz kayıtlı soru yok. Öneri listesi yüklenince sorular burada kategorilere göre görünür.'),
    ).toBeInTheDocument()
  })

  it('givenSuggestionsFail_whenPageOpens_thenNoChipsDialogAlertAndChatWorks', async () => {
    server.use(http.get('/api/assistant/suggestions', () => problem(500, 'INTERNAL_ERROR')))
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await waitFor(() => expect(within(chips()).queryAllByRole('button')).toHaveLength(0))
    await userEvent.type(input(), 'kart')
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument()
    await userEvent.clear(input())
    expect(within(chips()).queryByText('Henüz soru önerisi yok.')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))
    const dialog = await screen.findByRole('dialog', { name: 'Kayıtlı sorular' })
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu. Tekrar dene.')
    await userEvent.keyboard('{Escape}')
    await userEvent.type(input(), 'Kartımı kaybettim{Enter}')

    expect(await screen.findByRole('button', { name: 'Kaynak 1' })).toBeInTheDocument()
    expect(log.messages[0]?.body).toEqual({ text: 'Kartımı kaybettim' })
  })

  it('givenChips_whenRendered_thenNoAxeViolations', async () => {
    serveSuggestions(suggestionSet())
    const { container } = renderAppAt('/onizleme')
    await waitForChips()

    const results = await axe(container)

    expect(results.violations).toEqual([])
  })

  it('givenQuestionListDialog_whenRendered_thenNoAxeViolations', async () => {
    serveSuggestions(suggestionSet())
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await askLostCardViaChip()
    await userEvent.click(screen.getByRole('button', { name: 'Soru listesi' }))
    const dialog = await screen.findByRole('dialog')

    const results = await axe(dialog)

    expect(results.violations).toEqual([])
  })
})
