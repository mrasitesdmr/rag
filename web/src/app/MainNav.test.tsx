import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http } from 'msw'
import { describe, expect, it } from 'vitest'
import { conversationSummary } from '../test/conversationFixtures.ts'
import { serveConversations } from '../test/conversationHandlers.ts'
import { renderAppAt } from '../test/renderWithProviders.tsx'
import { problem, server } from '../test/server.ts'
import { done, lostCardAnswer } from '../test/previewFixtures.ts'
import { servePreview } from '../test/previewHandlers.ts'

function conversationsLink() {
  return within(screen.getByRole('navigation', { name: 'Menü' })).getByRole('link', { name: /^Konuşmalar/ })
}

describe('MainNav conversation count', () => {
  it('givenThreeConversations_whenNavRendered_thenCountShown', async () => {
    serveConversations([1, 2, 3].map((id) => conversationSummary({ conversationId: id })))

    renderAppAt('/onizleme')

    expect(await within(conversationsLink()).findByText('3')).toBeInTheDocument()
    expect(conversationsLink()).toHaveAccessibleName(/^Konuşmalar\s*3$/)
    expect(within(within(screen.getByRole('navigation', { name: 'Menü' })).getByRole('link', { name: 'Önizleme' })).queryByText('3')).not.toBeInTheDocument()
  })

  it('givenNoConversations_whenNavRendered_thenCountZeroShown', async () => {
    renderAppAt('/onizleme')

    expect(await within(conversationsLink()).findByText('0')).toBeInTheDocument()
  })

  it('givenCountRequestFails_whenNavRendered_thenNoCountBadge', async () => {
    server.use(http.get('/api/assistant/conversations', () => problem(500, 'INTERNAL_ERROR')))

    renderAppAt('/onizleme')

    await screen.findByRole('navigation', { name: 'Menü' })
    await new Promise((resolve) => setTimeout(resolve, 150))
    expect(conversationsLink()).toHaveAccessibleName('Konuşmalar')
  })

  it('givenConversationDetailOpen_whenNavRendered_thenConversationsItemStaysSelected', async () => {
    renderAppAt('/konusmalar/12')

    await screen.findByRole('heading', { level: 1, name: 'cnv_12' })
    expect(conversationsLink()).toHaveAttribute('aria-current', 'page')
  })

  it('givenAnswerDone_whenStreamEnds_thenConversationCountRefetched', async () => {
    // Arrange
    const user = userEvent.setup()
    let total = 0
    server.use(
      http.get('/api/assistant/conversations', ({ request }) => {
        const size = Number(new URL(request.url).searchParams.get('size') ?? 20)
        return Response.json({ content: [], page: 0, size, totalElements: total, totalPages: 0 })
      }),
    )
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    expect(await within(conversationsLink()).findByText('0')).toBeInTheDocument()
    total = 1

    // Act
    await user.type(screen.getByRole('combobox', { name: 'Soru' }), 'Kartımı kaybettim, ne yapmalıyım?')
    await user.click(screen.getByRole('button', { name: 'Gönder' }))

    // Assert
    expect(await within(conversationsLink()).findByText('1')).toBeInTheDocument()
    expect(done).toBeDefined()
  })
})
