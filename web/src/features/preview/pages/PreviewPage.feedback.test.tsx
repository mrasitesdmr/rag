import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { deferred } from '../../../test/handlers.ts'
import { handoffResponse, serveFeedback } from '../../../test/feedbackHandlers.ts'
import { done, lostCardAnswer, sources, streamError, token } from '../../../test/previewFixtures.ts'
import { servePreview } from '../../../test/previewHandlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem } from '../../../test/server.ts'
import { controllableSseStream, sseResponse } from '../../../test/sse.ts'
import { HttpResponse } from 'msw'

const QUESTION = 'Kartımı kaybettim, ne yapmalıyım?'
const NOT_FOUND_TEXT = 'Bu konuda rehberimizde bilgi bulamadım.'

async function ask(text: string) {
  await userEvent.type(screen.getByRole('combobox', { name: 'Soru' }), text)
  await userEvent.click(screen.getByRole('button', { name: 'Gönder' }))
}

function notFoundAnswer(turnId = 12) {
  return [token(NOT_FOUND_TEXT), sources({ contextChunks: [], citations: [] }), done({ notFoundAnswer: true, turnId })]
}

function serveNotFoundTurns(firstTurnId = 12) {
  let next = firstTurnId
  return servePreview(() => {
    const turnId = next
    next += 1
    return sseResponse(notFoundAnswer(turnId))
  })
}

async function renderDoneTurn() {
  servePreview(lostCardAnswer())
  const view = renderAppAt('/onizleme')
  await ask(QUESTION)
  await screen.findByRole('group', { name: 'Geri bildirim' })
  return view
}

const yes = () => screen.getByRole('button', { name: 'Evet' })
const no = () => screen.getByRole('button', { name: 'Hayır' })

describe('PreviewPage feedback and handoff', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('givenDoneTurn_whenRendered_thenFeedbackRowWithAnswerTime', async () => {
    vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-02T10:31:05Z') })
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const group = await screen.findByRole('group', { name: 'Geri bildirim' })
    expect(within(group).getByText('Faydalı mı?')).toBeInTheDocument()
    expect(within(group).getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'false')
    expect(within(group).getByRole('button', { name: 'Hayır' })).toHaveAttribute('aria-pressed', 'false')
    expect(within(group).getByText('13:31')).toBeInTheDocument()
  })

  it('givenDoneTurn_whenYesClicked_thenRequestSentAndYesPressed', async () => {
    const log = serveFeedback()
    await renderDoneTurn()

    await userEvent.click(yes())

    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))
    expect(log.feedback).toEqual([{ turnId: 12, body: { helpful: true } }])
    expect(no()).toHaveAttribute('aria-pressed', 'false')
    expect((await screen.findByText('Geri bildirim kaydedildi')).closest('[aria-live="polite"]')).not.toBeNull()
  })

  it('givenYesSelected_whenNoClicked_thenNoPressedAndReviewToast', async () => {
    const log = serveFeedback()
    await renderDoneTurn()
    await userEvent.click(yes())
    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))

    await userEvent.click(no())

    await waitFor(() => expect(no()).toHaveAttribute('aria-pressed', 'true'))
    expect(yes()).toHaveAttribute('aria-pressed', 'false')
    expect(log.feedback.map((entry) => entry.body)).toEqual([{ helpful: true }, { helpful: false }])
    expect(
      (await screen.findByText('Geri bildirim kaydedildi; bu tur incelemeye düştü')).closest('[aria-live="polite"]'),
    ).not.toBeNull()
  })

  it('givenYesSelected_whenYesClickedAgain_thenNoRequest', async () => {
    const log = serveFeedback()
    await renderDoneTurn()
    await userEvent.click(yes())
    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))

    await userEvent.click(yes())

    expect(log.feedback).toHaveLength(1)
    expect(yes()).toHaveAttribute('aria-pressed', 'true')
  })

  it.each([
    ['TURN_NOT_COMPLETED', 409, 'Tamamlanmamış bir cevaba geri bildirim verilemez.'],
    ['TURN_NOT_FOUND', 404, 'Bu cevap bulunamadı. Sayfayı yenile.'],
  ])('givenFeedbackFails_when%sOnYesClick_thenAlertAndSelectionUnchanged', async (code, status, text) => {
    serveFeedback({ feedbackReply: () => problem(status, code) })
    await renderDoneTurn()

    await userEvent.click(yes())

    expect(await screen.findByRole('alert')).toHaveTextContent(text)
    expect(yes()).toHaveAttribute('aria-pressed', 'false')
    expect(yes()).toBeEnabled()
  })

  it('givenNetworkError_whenYesClicked_thenAlertAndSelectionUnchanged', async () => {
    serveFeedback({ feedbackReply: () => HttpResponse.error() })
    await renderDoneTurn()

    await userEvent.click(yes())

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor. Bağlantını kontrol et.')
    expect(yes()).toHaveAttribute('aria-pressed', 'false')
  })

  it('givenFailedFeedback_whenRetriedAndSucceeds_thenAlertGoneAndYesPressed', async () => {
    let calls = 0
    serveFeedback({
      feedbackReply: () => {
        calls += 1
        return calls === 1 ? problem(500, 'INTERNAL_ERROR') : new HttpResponse(null, { status: 204 })
      },
    })
    await renderDoneTurn()
    await userEvent.click(yes())
    await screen.findByRole('alert')

    await userEvent.click(yes())

    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('givenStreamingTurn_whenRendered_thenNoFeedbackOrHandoffRow', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(QUESTION)
    stream.push(token(NOT_FOUND_TEXT))

    await screen.findByText(/Bu konuda rehberimizde/)
    expect(screen.queryByText('Faydalı mı?')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Temsilciye bağlan' })).not.toBeInTheDocument()
    stream.push(sources({ contextChunks: [], citations: [] }), done({ notFoundAnswer: true }))
    stream.close()
    expect(await screen.findByText('Faydalı mı?')).toBeInTheDocument()
  })

  it('givenFailedTurn_whenRendered_thenNoFeedbackOrHandoffRow', async () => {
    servePreview([token('Kart'), streamError('CHAT_MODEL_UNAVAILABLE')])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    await screen.findByRole('alert')
    expect(screen.queryByText('Faydalı mı?')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Temsilciye bağlan' })).not.toBeInTheDocument()
  })

  it('givenAnsweredTurn_whenRendered_thenNoHandoffButton', async () => {
    await renderDoneTurn()

    expect(screen.queryByRole('button', { name: 'Temsilciye bağlan' })).not.toBeInTheDocument()
    expect(screen.queryByText('Temsilci kuyruğundasın')).not.toBeInTheDocument()
  })

  it('givenNotFoundTurn_whenRendered_thenHandoffButtonAndFeedbackRow', async () => {
    servePreview(notFoundAnswer())
    renderAppAt('/onizleme')

    await ask('Bitcoin alıp satabilir miyim?')

    expect(await screen.findByRole('button', { name: 'Temsilciye bağlan' })).toBeEnabled()
    expect(screen.getByRole('group', { name: 'Geri bildirim' })).toBeInTheDocument()
  })

  it('givenNotFoundTurn_whenHandoffClicked_thenQueuedText', async () => {
    const log = serveFeedback()
    servePreview(notFoundAnswer())
    renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')

    await userEvent.click(await screen.findByRole('button', { name: 'Temsilciye bağlan' }))

    expect(await screen.findByText('Temsilci kuyruğundasın')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Temsilciye bağlan' })).not.toBeInTheDocument()
    expect(log.handoff).toEqual([{ conversationId: 1, body: { reason: 'NO_ANSWER', turnId: 12 } }])
  })

  it('givenHandedOffConversation_whenSecondNotFoundTurnAndNewConversation_thenQueuedOnlyInThatConversation', async () => {
    const log = serveFeedback({ handoffReply: (request) => handoffResponse({ conversationId: Number(new URL(request.url).pathname.split('/')[4]) }) })
    serveNotFoundTurns()
    renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')
    await userEvent.click(await screen.findByRole('button', { name: 'Temsilciye bağlan' }))
    await screen.findByText('Temsilci kuyruğundasın')

    await ask('Altın alabilir miyim?')

    await waitFor(() => expect(screen.getAllByText('Temsilci kuyruğundasın')).toHaveLength(2))
    expect(screen.queryByRole('button', { name: 'Temsilciye bağlan' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Yeni konuşma' }))
    await ask('Döviz alabilir miyim?')

    expect(await screen.findByRole('button', { name: 'Temsilciye bağlan' })).toBeEnabled()
    expect(screen.getAllByText('Temsilci kuyruğundasın')).toHaveLength(2)
    await userEvent.click(screen.getByRole('button', { name: 'Temsilciye bağlan' }))
    await waitFor(() => expect(screen.getAllByText('Temsilci kuyruğundasın')).toHaveLength(3))
    expect(log.handoff.map((entry) => entry.conversationId)).toEqual([1, 2])
    expect(log.handoff.map((entry) => entry.body)).toEqual([
      { reason: 'NO_ANSWER', turnId: 12 },
      { reason: 'NO_ANSWER', turnId: 14 },
    ])
  })

  it('givenHandoffNotAllowed_whenHandoffClicked_thenAlertAndButtonStays', async () => {
    serveFeedback({ handoffReply: () => problem(409, 'HANDOFF_NOT_ALLOWED') })
    servePreview(notFoundAnswer())
    renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')

    await userEvent.click(await screen.findByRole('button', { name: 'Temsilciye bağlan' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Temsilciye sadece asistanın cevap bulamadığı sorularda bağlanabilirsin.',
    )
    expect(screen.getByRole('button', { name: 'Temsilciye bağlan' })).toBeEnabled()
    expect(screen.queryByText('Temsilci kuyruğundasın')).not.toBeInTheDocument()
  })

  it('givenPendingHandoff_whenClickedTwice_thenOneRequest', async () => {
    const gate = deferred()
    const log = serveFeedback({
      handoffReply: async () => {
        await gate.promise
        return handoffResponse()
      },
    })
    servePreview(notFoundAnswer())
    renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')
    const button = await screen.findByRole('button', { name: 'Temsilciye bağlan' })

    await userEvent.click(button)
    await userEvent.click(button)

    await waitFor(() => expect(button).toBeDisabled())
    expect(log.handoff).toHaveLength(1)
    gate.resolve()
    expect(await screen.findByText('Temsilci kuyruğundasın')).toBeInTheDocument()
    expect(log.handoff).toHaveLength(1)
  })

  it('givenPendingFeedback_whenOtherButtonClicked_thenOneRequestAndBothDisabled', async () => {
    const gate = deferred()
    const log = serveFeedback({
      feedbackReply: async () => {
        await gate.promise
        return new HttpResponse(null, { status: 204 })
      },
    })
    await renderDoneTurn()

    await userEvent.click(yes())
    await waitFor(() => expect(no()).toBeDisabled())
    await userEvent.click(no())

    expect(yes()).toBeDisabled()
    expect(log.feedback).toHaveLength(1)
    gate.resolve()
    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))
    expect(no()).toBeEnabled()
  })

  it('givenFeedbackAndHandoffRows_whenRendered_thenNoAxeViolations', async () => {
    servePreview(notFoundAnswer())
    const { container } = renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')
    await screen.findByRole('button', { name: 'Temsilciye bağlan' })

    expect(await axe(container)).toHaveNoViolations()
  })

  it('givenHandedOffConversation_whenQuestionSent_thenAnswerStreams', async () => {
    serveFeedback()
    let call = 0
    const log = servePreview(() => {
      call += 1
      return sseResponse(call === 1 ? notFoundAnswer() : lostCardAnswer())
    })
    renderAppAt('/onizleme')
    await ask('Bitcoin alıp satabilir miyim?')
    await userEvent.click(await screen.findByRole('button', { name: 'Temsilciye bağlan' }))
    await screen.findByText('Temsilci kuyruğundasın')

    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled()
    await ask(QUESTION)

    expect(await screen.findByRole('button', { name: 'Kaynak 1' })).toBeInTheDocument()
    expect(log.messages.map((message) => message.conversationId)).toEqual([1, 1])
    expect(log.conversationBodies).toHaveLength(1)
  })

  it('givenTwoDoneTurns_whenFeedbackOnFirst_thenSecondUnaffected', async () => {
    serveFeedback()
    serveNotFoundTurns()
    renderAppAt('/onizleme')
    await ask('Birinci soru')
    await screen.findByRole('group', { name: 'Geri bildirim' })
    await ask('İkinci soru')
    await waitFor(() => expect(screen.getAllByRole('group', { name: 'Geri bildirim' })).toHaveLength(2))
    const [first, second] = screen.getAllByRole('group', { name: 'Geri bildirim' })

    await userEvent.click(within(first).getByRole('button', { name: 'Evet' }))

    await waitFor(() => expect(within(first).getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'true'))
    expect(within(second).getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'false')
    expect(within(second).getByRole('button', { name: 'Hayır' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('givenFeedbackBeforeNewConversation_whenNewConversationStarted_thenPreviousSelectionKept', async () => {
    const log = serveFeedback()
    serveNotFoundTurns()
    renderAppAt('/onizleme')
    await ask('Birinci soru')
    await screen.findByRole('group', { name: 'Geri bildirim' })
    await userEvent.click(yes())
    await waitFor(() => expect(yes()).toHaveAttribute('aria-pressed', 'true'))

    await userEvent.click(screen.getByRole('button', { name: 'Yeni konuşma' }))
    await ask('İkinci soru')

    await waitFor(() => expect(screen.getAllByRole('group', { name: 'Geri bildirim' })).toHaveLength(2))
    const [first, second] = screen.getAllByRole('group', { name: 'Geri bildirim' })
    expect(within(first).getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(second).getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'false')
    await userEvent.click(within(second).getByRole('button', { name: 'Hayır' }))
    await waitFor(() => expect(log.feedback.map((entry) => entry.turnId)).toEqual([12, 13]))
  })
})
