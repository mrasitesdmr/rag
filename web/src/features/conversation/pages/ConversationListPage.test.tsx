import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axe } from 'vitest-axe'
import { conversationSummary } from '../../../test/conversationFixtures.ts'
import { serveConversationSequence, serveConversations } from '../../../test/conversationHandlers.ts'
import { countOf, recordRequests } from '../../../test/handlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'

const LONG_QUESTION = `${'Kartımı kaybettim '.repeat(4)}ne yapmalıyım`

function rowOf(label: string) {
  return screen.getByRole('link', { name: label }).closest('tr') as HTMLElement
}

describe('ConversationListPage', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('givenConversations_whenRendered_thenRowShowsLabCells', async () => {
    // Arrange
    vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-02T12:00:00Z') })
    serveConversations([conversationSummary({ firstQuestion: LONG_QUESTION })])

    // Act
    renderAppAt('/konusmalar')

    // Assert
    const row = within(await screen.findByRole('link', { name: 'cnv_12' }).then((link) => link.closest('tr') as HTMLElement))
    expect(LONG_QUESTION.length).toBeGreaterThan(60)
    expect(row.getByText(`${LONG_QUESTION.slice(0, 59)}…`)).toBeInTheDocument()
    expect(row.getByText('2')).toBeInTheDocument()
    expect(row.getByText('DAYANAKLI')).toBeInTheDocument()
    expect(row.getByText('Önizleme')).toBeInTheDocument()
    expect(row.getByText('13:31')).toBeInTheDocument()
    expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual([
      'Konuşma',
      'İlk soru',
      'Tur',
      'Durum',
      'Kanal',
      'Saat',
    ])
  })

  it('givenConversationFromAnotherDay_whenRendered_thenDateAndTimeShown', async () => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-05T12:00:00Z') })
    serveConversations([conversationSummary()])

    renderAppAt('/konusmalar')

    expect(await within(await screen.findByRole('link', { name: 'cnv_12' }).then((l) => l.closest('tr') as HTMLElement)).findByText('02.11.2026 13:31')).toBeInTheDocument()
  })

  it('givenShortQuestion_whenRendered_thenShownWithoutEllipsis', async () => {
    serveConversations([conversationSummary({ firstQuestion: 'Kısa soru' })])

    renderAppAt('/konusmalar')

    expect(await screen.findByText('Kısa soru')).toBeInTheDocument()
  })

  it('givenNoConversations_whenRendered_thenEmptyStateWithPreviewLink', async () => {
    renderAppAt('/konusmalar')

    expect(await screen.findByRole('heading', { name: 'Henüz konuşma yok' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: "Önizleme'ye git" })).toHaveAttribute('href', '/onizleme')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it.each(['click', 'enter'])('givenRow_whenOpened_thenNavigatesToDetail %s', async (how) => {
    const user = userEvent.setup()
    serveConversations([conversationSummary()])
    renderAppAt('/konusmalar')
    const link = await screen.findByRole('link', { name: 'cnv_12' })

    if (how === 'click') {
      await user.click(within(rowOf('cnv_12')).getByText('Önizleme'))
    } else {
      link.focus()
      await user.keyboard('{Enter}')
    }

    expect(await screen.findByRole('heading', { level: 1, name: 'cnv_12' })).toBeInTheDocument()
  })

  it('givenHandedOffConversation_whenRendered_thenTemsilcideChipOnlyOnThatRow', async () => {
    serveConversations([
      conversationSummary({ conversationId: 13, handedOff: true }),
      conversationSummary({ conversationId: 12 }),
    ])

    renderAppAt('/konusmalar')

    await screen.findByRole('link', { name: 'cnv_13' })
    expect(within(rowOf('cnv_13')).getByText('Temsilcide')).toBeInTheDocument()
    expect(within(rowOf('cnv_12')).queryByText('Temsilcide')).not.toBeInTheDocument()
  })

  it.each([
    ['IN_PROGRESS', 'işleniyor'],
    ['INCOMPLETE', 'TAMAMLANMADI'],
    ['UNGROUNDED', 'DAYANAKSIZ'],
    ['DONT_KNOW', 'BİLMİYOR'],
    ['WRONG', 'HATALI'],
    ['GROUNDED', 'DAYANAKLI'],
    ['SMALL_TALK', 'SOHBET'],
  ] as const)('givenLastTurnStatus_whenRendered_thenChipShowsLabel %s', async (status, label) => {
    serveConversations([conversationSummary({ lastTurnStatus: status })])

    renderAppAt('/konusmalar')

    await screen.findByRole('link', { name: 'cnv_12' })
    expect(within(rowOf('cnv_12')).getByText(label)).toBeInTheDocument()
  })

  it('givenInProgressConversation_whenPolled_thenChipUpdatesAndPollingStops', async () => {
    // Arrange
    const log = recordRequests()
    serveConversationSequence(
      [conversationSummary({ lastTurnStatus: 'IN_PROGRESS' })],
      [conversationSummary({ lastTurnStatus: 'GROUNDED' })],
    )

    // Act
    renderAppAt('/konusmalar')

    // Assert
    expect(await screen.findByText('işleniyor')).toBeInTheDocument()
    expect(await screen.findByText('DAYANAKLI', {}, { timeout: 4000 })).toBeInTheDocument()
    const listRequests = () =>
      log.urls.filter((url) => url.pathname === '/api/assistant/conversations' && url.searchParams.get('size') === '20')
    expect(listRequests()).toHaveLength(2)
    await new Promise((resolve) => setTimeout(resolve, 2400))
    expect(listRequests()).toHaveLength(2)
    expect(countOf(log, '/api/assistant/conversations')).toBeGreaterThanOrEqual(3)
  }, 12000)

  it('givenMoreThanOnePage_whenNextClicked_thenSecondPageRequested', async () => {
    const user = userEvent.setup()
    serveConversations(
      Array.from({ length: 21 }, (_, index) => conversationSummary({ conversationId: 100 - index })),
    )
    renderAppAt('/konusmalar')
    await screen.findByRole('link', { name: 'cnv_100' })

    await user.click(within(screen.getByRole('navigation', { name: 'Konuşma sayfaları' })).getByRole('button', { name: 'Sonraki' }))

    expect(await screen.findByRole('link', { name: 'cnv_80' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'cnv_100' })).not.toBeInTheDocument()
  })

  it('givenServerError_whenRendered_thenErrorAlert', async () => {
    server.use(http.get('/api/assistant/conversations', () => problem(500, 'INTERNAL_ERROR')))

    renderAppAt('/konusmalar')

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu. Tekrar dene.')
  })

  it('givenListAndEmptyState_whenRendered_thenNoAxeViolations', async () => {
    serveConversations([conversationSummary({ handedOff: true })])
    const { container } = renderAppAt('/konusmalar')
    await screen.findByRole('link', { name: 'cnv_12' })

    const results = await axe(container)

    expect(results.violations).toEqual([])
    await waitFor(() => expect(screen.getByRole('table')).toBeInTheDocument())
  })
})
