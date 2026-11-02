import { screen, waitForElementToBeRemoved, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { axe } from 'vitest-axe'
import {
  conversationDetail,
  followUpTurn,
  handoff,
  promptChunks,
  turn,
  turnChunk,
} from '../../../test/conversationFixtures.ts'
import { serveDetailSequence } from '../../../test/conversationHandlers.ts'
import { chunkDetail } from '../../../test/fixtures.ts'
import { recordRequests } from '../../../test/handlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'

const FIRST = '1. Kartımı kaybettim, ne yapmalıyım?'
const SECOND = '2. Yenisi kaç günde gelir?'

function cardOf(question: string) {
  return screen.getByText(question).closest('article') as HTMLElement
}

async function renderDetail(detail = conversationDetail()) {
  serveDetailSequence(12, detail)
  const view = renderAppAt('/konusmalar/12')
  await screen.findByRole('heading', { level: 1, name: 'cnv_12' })
  return view
}

describe('ConversationDetailPage', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('givenConversation_whenRendered_thenBreadcrumbTitleAndSubtitle', async () => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-02T12:00:00Z') })

    await renderDetail()

    expect(within(screen.getByRole('main')).getByRole('link', { name: 'Konuşmalar' })).toHaveAttribute(
      'href',
      '/konusmalar',
    )
    expect(within(screen.getByRole('main')).getByText('cnv_12', { selector: 'b' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('cnv_12')
    expect(screen.getByText('Önizleme · 13:31 · 2 tur')).toBeInTheDocument()
  })

  it('givenFollowUpTurn_whenRendered_thenSearchQueryLineOnlyWhenDifferent', async () => {
    await renderDetail()

    expect(cardOf(SECOND)).toHaveTextContent('Arama sorgusu: Kaybolan kartın yenisi kaç günde gelir?')
    expect(cardOf(FIRST)).not.toHaveTextContent('Arama sorgusu')
  })

  it('givenRerankTurn_whenRendered_thenChunkTableColumnsAndValues', async () => {
    await renderDetail()

    const card = within(cardOf(SECOND))
    expect(card.getAllByRole('columnheader').map((header) => header.textContent)).toEqual([
      'Prompt',
      'Parça',
      'Vector #',
      'Mesafe',
      'Rerank',
      'Atıf',
    ])
    const rows = card.getAllByRole('row').slice(1)
    expect(rows).toHaveLength(5)
    expect(within(rows[0]).getAllByRole('cell').map((cell) => cell.textContent)).toEqual([
      '[1]',
      's. 19 · #31',
      '7',
      '0,3124',
      '5,360',
      '✓',
    ])
    expect(within(rows[1]).getAllByRole('cell').map((cell) => cell.textContent)).toEqual([
      '[2]',
      's. 20 · #32',
      '1',
      '0,3500',
      '1,200',
      '',
    ])
  })

  it('givenTopKTurn_whenRendered_thenNoRerankColumn', async () => {
    const topK = turn({
      retrievalStrategy: 'TOP_K',
      candidates: 5,
      contextChunks: promptChunks({ rerankScore: null }),
    })

    await renderDetail(conversationDetail({ turns: [topK] }))

    expect(screen.queryByRole('columnheader', { name: 'Rerank' })).not.toBeInTheDocument()
    expect(screen.getAllByRole('columnheader')).toHaveLength(5)
    expect(screen.getByText('RAG · top-5')).toBeInTheDocument()
  })

  it('givenRerankFallbackTurn_whenRendered_thenRerankerDownLabelAndNoRerankColumn', async () => {
    const fallback = turn({
      retrievalStrategy: 'RERANK_FALLBACK',
      contextChunks: [turnChunk(1, { rerankScore: null })],
    })

    await renderDetail(conversationDetail({ turns: [fallback] }))

    expect(screen.getByText('RAG · reranker DOWN')).toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Rerank' })).not.toBeInTheDocument()
  })

  it('givenLlmOnlyTurn_whenRendered_thenWarningInsteadOfTable', async () => {
    const llmOnly = turn({
      generationMode: 'LLM_ONLY',
      retrievalStrategy: 'NONE',
      candidates: null,
      topK: null,
      searchQuery: null,
      cited: [],
      contextChunks: [],
      status: 'UNGROUNDED',
      helpful: null,
      answer: 'Genel bilgi [1].',
    })

    await renderDetail(conversationDetail({ turns: [llmOnly] }))

    expect(screen.getByText('Bu turda modele hiçbir parça gitmedi (Sadece LLM).')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getByText('Sadece LLM')).toBeInTheDocument()
    expect(screen.getByText('DAYANAKSIZ')).toBeInTheDocument()
    expect(screen.queryByText(/rehber/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kaynak 1' })).not.toBeInTheDocument()
  })

  it('givenAnsweredTurn_whenRendered_thenNumbersLine', async () => {
    await renderDetail(conversationDetail({ turns: [turn()] }))

    const numbers = screen.getByText('üretim', { exact: false }).parentElement as HTMLElement
    expect(numbers).toHaveTextContent('DAYANAKLI')
    expect(numbers).toHaveTextContent('üretim RAG · rerank 20→5')
    expect(numbers).toHaveTextContent('rehber 2026.10')
    expect(numbers).toHaveTextContent('prompt 1.234 token')
    expect(numbers).toHaveTextContent('cevap 87 token')
    expect(numbers).toHaveTextContent('süre 3,42 sn')
    expect(numbers).toHaveTextContent('geri bildirim Hayır')
    expect(numbers).not.toHaveTextContent('sebep')
  })

  it('givenHelpfulTrueAndNull_whenRendered_thenFeedbackEvetOrHidden', async () => {
    await renderDetail(conversationDetail({ turns: [turn({ helpful: true }), followUpTurn({ helpful: null })] }))

    expect(cardOf(FIRST)).toHaveTextContent('geri bildirim Evet')
    expect(cardOf(SECOND)).not.toHaveTextContent('geri bildirim')
  })

  it.each([
    ['IN_PROGRESS', 'işleniyor'],
    ['INCOMPLETE', 'TAMAMLANMADI'],
    ['UNGROUNDED', 'DAYANAKSIZ'],
    ['DONT_KNOW', 'BİLMİYOR'],
    ['WRONG', 'HATALI'],
    ['GROUNDED', 'DAYANAKLI'],
  ] as const)('givenTurnStatus_whenRendered_thenChipShowsLabel %s', async (status, label) => {
    await renderDetail(conversationDetail({ turns: [turn({ status })] }))

    expect(within(cardOf(FIRST)).getByText(label)).toBeInTheDocument()
  })

  it('givenFailedTurn_whenRendered_thenIncompleteChipReasonAndPartialAnswer', async () => {
    const failed = turn({
      status: 'INCOMPLETE',
      errorCode: 'CHAT_MODEL_UNAVAILABLE',
      answer: 'Kartınızı hemen',
      promptTokens: null,
      completionTokens: null,
      helpful: null,
      cited: [],
    })

    await renderDetail(conversationDetail({ turns: [failed] }))

    expect(screen.getByText('TAMAMLANMADI')).toBeInTheDocument()
    expect(screen.getByText('dil modeline ulaşılamadı')).toBeInTheDocument()
    expect(screen.getByText('Kartınızı hemen')).toBeInTheDocument()
    expect(cardOf(FIRST)).not.toHaveTextContent('prompt')
    expect(cardOf(FIRST)).not.toHaveTextContent('cevap 0')
  })

  it('givenRetrievalFailedTurn_whenRendered_thenNoAnswerAndNoChunksTexts', async () => {
    const failed = turn({
      status: 'INCOMPLETE',
      errorCode: 'RETRIEVAL_UNAVAILABLE',
      answer: null,
      contextChunks: [],
      promptTokens: null,
      completionTokens: null,
      cited: [],
      helpful: null,
    })

    await renderDetail(conversationDetail({ turns: [failed] }))

    expect(screen.getByText('Cevap üretilmedi.')).toBeInTheDocument()
    expect(screen.getByText('Bu turda modele hiçbir parça gitmedi.')).toBeInTheDocument()
    expect(screen.getByText('bilgi bankasına ulaşılamadı')).toBeInTheDocument()
    expect(cardOf(FIRST)).not.toHaveTextContent('rehber')
  })

  it('givenInProgressTurn_whenRendered_thenProcessingCardAndCountIncludesIt', async () => {
    await renderDetail(
      conversationDetail({
        inProgressTurn: { turnNo: 3, question: 'Peki ek kart?', startedAt: '2026-11-02T10:40:00Z' },
      }),
    )

    const card = within(cardOf('3. Peki ek kart?'))
    expect(card.getByText('işleniyor')).toBeInTheDocument()
    expect(card.getByText('…')).toBeInTheDocument()
    expect(screen.getByText(/· 3 tur$/)).toBeInTheDocument()
  })

  it('givenInProgressDetail_whenPolled_thenTurnAppearsAndPollingStops', async () => {
    const log = recordRequests()
    serveDetailSequence(
      12,
      conversationDetail({
        turns: [turn()],
        inProgressTurn: { turnNo: 2, question: 'Peki ek kart?', startedAt: '2026-11-02T10:40:00Z' },
      }),
      conversationDetail({ turns: [turn(), followUpTurn({ question: 'Peki ek kart?', searchQuery: 'Peki ek kart?' })] }),
    )

    renderAppAt('/konusmalar/12')

    expect(await screen.findByText('işleniyor')).toBeInTheDocument()
    await waitForElementToBeRemoved(() => screen.queryByText('işleniyor'), { timeout: 4000 })
    expect(screen.getByText('2. Peki ek kart?')).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 2400))
    expect(log.urls.filter((url) => url.pathname === '/api/assistant/conversations/12')).toHaveLength(2)
  }, 12000)

  it('givenUnknownConversation_whenRendered_thenNotFoundPage', async () => {
    server.use(http.get('/api/assistant/conversations/99', () => problem(404, 'CONVERSATION_NOT_FOUND')))

    renderAppAt('/konusmalar/99')

    expect(await screen.findByRole('heading', { level: 1, name: 'Konuşma bulunamadı.' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: "Konuşmalar'a dön" })).toHaveAttribute('href', '/konusmalar')
  })

  it.each(['abc', '12abc', '-1', '1.5'])('givenNonNumericId_whenRendered_thenNotFoundWithoutRequest %s', async (id) => {
    const log = recordRequests()

    renderAppAt(`/konusmalar/${id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Konuşma bulunamadı.' })).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 100))
    expect(log.urls.filter((url) => url.pathname.startsWith('/api/assistant/conversations/'))).toHaveLength(0)
  })

  it('givenServerError_whenRendered_thenErrorAlert', async () => {
    server.use(http.get('/api/assistant/conversations/12', () => problem(500, 'INTERNAL_ERROR')))

    renderAppAt('/konusmalar/12')

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu. Tekrar dene.')
  })

  it('givenConversationWithoutTurns_whenRendered_thenNoTurnsText', async () => {
    await renderDetail(conversationDetail({ turns: [] }))

    expect(screen.getByText('Bu konuşmada henüz tur yok.')).toBeInTheDocument()
    expect(screen.getByText(/· 0 tur$/)).toBeInTheDocument()
  })

  it('givenHandedOffConversation_whenRendered_thenHandoffLine', async () => {
    await renderDetail(conversationDetail({ handoff: handoff() }))

    expect(screen.getByText('Temsilciye aktarıldı · bireysel kuyruğu · 13:31 · sebep: cevap yok')).toBeInTheDocument()
  })

  it('givenNotHandedOff_whenRendered_thenNoHandoffLine', async () => {
    await renderDetail()

    expect(screen.queryByText(/Temsilciye aktarıldı/)).not.toBeInTheDocument()
  })

  it.each(['row', 'citation'])('givenChunkRowOrCitation_whenClicked_thenChunkDialogOpens %s', async (how) => {
    const user = userEvent.setup()
    const log = recordRequests()
    server.use(http.get('/api/workspaces/42/chunks/151', () => HttpResponse.json(chunkDetail())))
    await renderDetail(conversationDetail({ turns: [turn()] }))

    if (how === 'row') {
      await user.click(screen.getByText('[1]'))
    } else {
      await user.click(screen.getByRole('button', { name: 'Kaynak 1' }))
    }

    const dialog = await screen.findByRole('dialog')
    expect(await within(dialog).findByText('2.16 Kayıp ya da çalıntı bildirimi')).toBeInTheDocument()
    expect(log.urls.some((url) => url.pathname === '/api/workspaces/42/chunks/151')).toBe(true)
  })

  it('givenCitationOutsideContext_whenRendered_thenNotAButton', async () => {
    await renderDetail(conversationDetail({ turns: [turn({ answer: 'Kapat [9].' })] }))

    expect(screen.queryByRole('button', { name: 'Kaynak 9' })).not.toBeInTheDocument()
  })

  it('givenDeletedChunk_whenOpened_thenNotFoundMessage', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/workspaces/42/chunks/151', () => problem(404, 'CHUNK_NOT_FOUND')))
    await renderDetail(conversationDetail({ turns: [turn()] }))

    await user.click(screen.getByText('[1]'))

    expect(await within(await screen.findByRole('dialog')).findByText('Parça bulunamadı.')).toBeInTheDocument()
    expect(screen.getByText('s. 19 · #31')).toBeInTheDocument()
  })

  it('givenDetailWithAllCards_whenRendered_thenNoAxeViolations', async () => {
    const { container } = await renderDetail(
      conversationDetail({
        handoff: handoff(),
        inProgressTurn: { turnNo: 3, question: 'Peki ek kart?', startedAt: '2026-11-02T10:40:00Z' },
      }),
    )

    const results = await axe(container)

    expect(results.violations).toEqual([])
  })
})
