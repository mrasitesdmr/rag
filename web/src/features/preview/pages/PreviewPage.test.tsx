import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { GUIDE_NAME, liveConfig } from '../../../test/fixtures.ts'
import { serveLiveConfig } from '../../../test/liveConfigHandlers.ts'
import { deferred } from '../../../test/handlers.ts'
import {
  contextChunk,
  done,
  llmOnlyAnswer,
  lostCardAnswer,
  sources,
  streamError,
  token,
} from '../../../test/previewFixtures.ts'
import { serveChunkDetails, servePreview } from '../../../test/previewHandlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'
import { controllableSseStream, sseResponse } from '../../../test/sse.ts'
import { WELCOME_MESSAGE } from '../components/MessageList.tsx'

const QUESTION = 'Kartımı kaybettim, ne yapmalıyım?'
const SOURCE_BUTTON = 'Raşit Bankası Ürün ve H… · s. 19'

async function ask(text: string) {
  await userEvent.type(screen.getByRole('combobox', { name: 'Soru' }), text)
  await userEvent.click(screen.getByRole('button', { name: 'Gönder' }))
}

function liveRegion(container: HTMLElement): HTMLElement {
  const region = container.querySelector<HTMLElement>('[aria-live="polite"]')
  if (region === null) {
    throw new Error('live region missing')
  }
  return region
}

describe('PreviewPage', () => {
  it('givenFreshPage_whenRendered_thenWelcomeMessageAndHowItWorksCardAndEnabledInput', () => {
    renderAppAt('/onizleme')

    expect(screen.getByText(WELCOME_MESSAGE)).toBeInTheDocument()
    expect(screen.getByText('Dijital Asistan', { selector: 'b' })).toBeInTheDocument()
    expect(screen.getByText('Raşit Mobil · 7/24')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeEnabled()
    expect(screen.queryByRole('button', { name: 'Kayıtlı sorular' })).not.toBeInTheDocument()
  })

  it('givenStreamedTokens_whenQuestionSent_thenAnswerGrowsBeforeDone', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(QUESTION)
    stream.push(token('Kartını '))

    expect(await screen.findByText('Kartını')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
    stream.push(token('hemen kapat [1].'), sources(), done())
    stream.close()

    expect(await screen.findByRole('button', { name: 'Kaynak 1' })).toBeInTheDocument()
    expect(screen.getByText(/Kartını\s+hemen kapat/)).toBeInTheDocument()
    await userEvent.type(screen.getByRole('combobox', { name: 'Soru' }), 'Başka soru')
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeEnabled()
  })

  it('givenStreamInProgress_whenRendered_thenInputSendAndNewConversationDisabled', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(QUESTION)
    stream.push(token('Kartını '))

    await screen.findByText('Kartını')
    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Gönder' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeDisabled()
    stream.push(sources(), done())
    stream.close()
    await waitFor(() => expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled())
    expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeEnabled()
  })

  it('givenWaitingForFirstToken_whenQuestionSent_thenQuestionShownWithTrimmedTextAndInputCleared', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(`  ${QUESTION}  `)

    expect(await screen.findByText(QUESTION)).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Soru' })).toHaveValue('')
    stream.close()
  })

  it('givenTwoQuestions_whenSent_thenOneConversationCreatedAndBothMessagesUseIt', async () => {
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(`  ${QUESTION}  `)
    await screen.findByRole('button', { name: 'Kaynak 1' })
    await ask('Limitimi nasıl artırırım?')
    await waitFor(() => expect(log.messages).toHaveLength(2))

    expect(log.conversationBodies).toEqual([{ channel: 'PREVIEW' }])
    expect(log.messages.map((message) => message.conversationId)).toEqual([1, 1])
    expect(log.messages[0]?.body).toEqual({ text: QUESTION })
    expect(log.messages[1]?.body).toEqual({ text: 'Limitimi nasıl artırırım?' })
    expect(log.messages[0]?.accept).toBe('text/event-stream, application/problem+json')
  })

  it('givenAnsweredTurn_whenNewConversationThenQuestion_thenNewConversationAndSeparator', async () => {
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    await ask(QUESTION)
    await screen.findByRole('button', { name: 'Kaynak 1' })

    await userEvent.click(screen.getByRole('button', { name: 'Yeni konuşma' }))
    await ask('Limitimi nasıl artırırım?')

    expect(await screen.findByText('Yeni konuşma · 2')).toBeInTheDocument()
    expect(log.conversationBodies).toHaveLength(2)
    expect(log.messages.map((message) => message.conversationId)).toEqual([1, 2])
    const messages = screen.getByRole('region', { name: 'Mesajlar' })
    const text = messages.textContent ?? ''
    expect(text.indexOf(QUESTION)).toBeLessThan(text.indexOf('Yeni konuşma · 2'))
    expect(text.indexOf('Yeni konuşma · 2')).toBeLessThan(text.indexOf('Limitimi nasıl artırırım?'))
  })

  it('givenNewConversationBeforeAnyQuestion_whenClicked_thenNoSeparatorAndNoRequest', async () => {
    const log = servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await userEvent.click(screen.getByRole('button', { name: 'Yeni konuşma' }))

    expect(screen.queryByText(/^Yeni konuşma ·?/, { selector: 'div' })).not.toBeInTheDocument()
    expect(log.conversationBodies).toEqual([])
  })

  it('givenDoneAnswer_whenCitationClicked_thenChunkDialogOpensAndFocusReturns', async () => {
    servePreview(lostCardAnswer())
    const requested = serveChunkDetails()
    renderAppAt('/onizleme')
    await ask(QUESTION)
    const citation = await screen.findByRole('button', { name: 'Kaynak 1' })

    await userEvent.click(citation)

    const dialog = await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })
    expect(requested).toEqual([151])
    await userEvent.keyboard('{Escape}')
    await waitFor(() => expect(dialog).not.toBeInTheDocument())
    expect(citation).toHaveFocus()
  })

  it('givenOutOfRangeNumberInAnswer_whenDone_thenNoCitationButtonForIt', async () => {
    servePreview([token('Bak [7] ve [1].'), sources(), done()])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('button', { name: 'Kaynak 1' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kaynak 7' })).not.toBeInTheDocument()
    expect(screen.getByText(/Bak \[7\] ve/)).toBeInTheDocument()
  })

  it('givenCitationNumbersWhileStreaming_whenNotDoneYet_thenShownAsPlainText', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(QUESTION)
    stream.push(token('Kapat [1].'))

    expect(await screen.findByText('Kapat [1].')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kaynak 1' })).not.toBeInTheDocument()
    stream.push(sources(), done())
    stream.close()
    expect(await screen.findByRole('button', { name: 'Kaynak 1' })).toBeInTheDocument()
  })

  it('givenCitedAnswer_whenDone_thenSourceButtonsShown', async () => {
    servePreview([token('a [1] b [3]'), sources({ citations: [3, 1] }), done()])
    const requested = serveChunkDetails()
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const first = await screen.findByRole('button', { name: 'Raşit Bankası Ürün ve H… · s. 21' })
    const second = screen.getByRole('button', { name: SOURCE_BUTTON })
    expect(first).toHaveAttribute('title', `${GUIDE_NAME} · sayfa 21`)
    expect(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    await userEvent.click(first)
    await screen.findByRole('dialog')
    expect(requested).toEqual([153])
  })

  it('givenNotFoundAnswer_whenDone_thenNoSourceButtons', async () => {
    servePreview([
      token('Bu konuda rehberimizde bilgi bulamadım. [1]'),
      sources({ citations: [1] }),
      done({ notFoundAnswer: true }),
    ])
    renderAppAt('/onizleme')

    await ask('Bitcoin alıp satabilir miyim?')

    expect(await screen.findByText(/Bu konuda rehberimizde bilgi bulamadım\./)).toBeInTheDocument()
    await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })
    expect(screen.queryByRole('button', { name: /· s\. \d+/ })).not.toBeInTheDocument()
  })

  it('givenEmptyContext_whenDone_thenShowsAddDocumentHint', async () => {
    servePreview([
      token('Bu konuda rehberimizde bilgi bulamadım.'),
      sources({ contextChunks: [], citations: [] }),
      done({
        notFoundAnswer: true,
        promptTokens: null,
        completionTokens: null,
        rerankLatencyMs: null,
        generationLatencyMs: null,
        timeToFirstTokenMs: null,
      }),
    ])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText('Bilgi bankasında henüz belge yok.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: "Bilgi kaynakları'ndan belge ekle." })).toHaveAttribute('href', '/kaynaklar')
  })

  it('givenAnswerWithContext_whenDone_thenNoAddDocumentHint', async () => {
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    await screen.findByRole('button', { name: 'Kaynak 1' })
    expect(screen.queryByText('Bilgi bankasında henüz belge yok.')).not.toBeInTheDocument()
  })

  it('givenRetrievalUnavailable_whenQuestionSent_thenAlertAndInputEnabled', async () => {
    servePreview(() => problem(503, 'RETRIEVAL_UNAVAILABLE'))
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.',
    )
    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeEnabled()
  })

  it('givenErrorEventAfterToken_whenStreaming_thenKeepsPartialAnswerAndShowsModelMessage', async () => {
    servePreview([token('Kartını '), streamError('CHAT_MODEL_UNAVAILABLE')])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.',
    )
    expect(screen.getByText(/Kartını/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /· s\. \d+/ })).not.toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled()
  })

  it('givenStreamEndsWithoutDone_whenStreaming_thenInterruptedMessage', async () => {
    servePreview([token('Kartını ')])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent('Cevap yarıda kesildi. Soruyu yeniden gönder.')
    expect(screen.getByRole('combobox', { name: 'Soru' })).toBeEnabled()
  })

  it('givenNetworkDown_whenQuestionSent_thenNetworkMessage', async () => {
    servePreview(() => HttpResponse.error())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor. Bağlantını kontrol et.')
  })

  it('givenServerRejectsValidation_whenQuestionSent_thenRuleMessage', async () => {
    servePreview(() => problem(400, 'VALIDATION_FAILED', [{ field: 'text', message: 'size must be at most 1000' }]))
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent('Soru 1-1000 karakter olmalı.')
  })

  it('givenTurnInProgress409_whenQuestionSent_thenInProgressMessage', async () => {
    servePreview(() => problem(409, 'TURN_IN_PROGRESS'))
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Bu konuşmada bir cevap hâlâ yazılıyor. Bitince yeniden sor.',
    )
  })

  it('givenInternalErrorEvent_whenStreaming_thenFallbackMessage', async () => {
    servePreview([streamError('INTERNAL_ERROR')])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu. Tekrar dene.')
  })

  it('givenStreamInProgress_whenPageUnmounted_thenRequestAborted', async () => {
    const stream = controllableSseStream()
    const started = deferred()
    const aborted = deferred<boolean>()
    servePreview((request) => {
      request.signal.addEventListener('abort', () => aborted.resolve(true))
      started.resolve()
      return stream.response()
    })
    const { unmount } = renderAppAt('/onizleme')
    await ask(QUESTION)
    await started.promise
    stream.push(token('Kartını '))
    await screen.findByText('Kartını')

    unmount()

    await expect(Promise.race([aborted.promise, new Promise((resolve) => setTimeout(() => resolve(false), 2000))])).resolves.toBe(true)
  })

  it('givenConversationNotFound_whenQuestionSent_thenMessageAndNextQuestionOpensNewConversation', async () => {
    let call = 0
    const log = servePreview(() => {
      call += 1
      return call === 1 ? problem(404, 'CONVERSATION_NOT_FOUND') : sseResponse(lostCardAnswer())
    }, [1, 2])
    renderAppAt('/onizleme')

    await ask(QUESTION)
    expect(await screen.findByRole('alert')).toHaveTextContent('Konuşma bulunamadı. Yeni konuşma başlat.')
    await ask(QUESTION)

    await screen.findByRole('button', { name: 'Kaynak 1' })
    expect(log.conversationBodies).toHaveLength(2)
    expect(log.messages.map((message) => message.conversationId)).toEqual([1, 2])
  })

  it('givenNoAnswerYet_whenRendered_thenHowItWorksCard', async () => {
    renderAppAt('/onizleme')

    expect(screen.getByRole('heading', { name: 'Asistan nasıl cevaplıyor?' })).toBeInTheDocument()
    expect(
      await screen.findByText(
        'Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Son cevapta modele giden parçalar' })).not.toBeInTheDocument()
  })

  it('givenLiveConfig_whenPageOpens_thenStripShowsConfiguredValues', async () => {
    renderAppAt('/onizleme')

    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(Array.from(strip.querySelectorAll('span')).map((span) => span.textContent)).toEqual([
      'Model qwen3.5:9b',
      'Embedding bge-m3',
      'Retrieval rerank 20→5',
      'Soru yeniden yazma açık',
      `Bilgi kaynakları ${GUIDE_NAME} · 2026.10`,
    ])
  })

  it('givenOtherConfiguredModelAndNumbers_whenPageOpens_thenStripAndCardFollowConfig', async () => {
    serveLiveConfig([liveConfig({ chatModel: 'llama3:8b', candidates: 30, topK: 3 })])
    renderAppAt('/onizleme')

    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(strip).toHaveTextContent('Model llama3:8b')
    expect(strip).toHaveTextContent('Retrieval rerank 30→3')
    expect(
      screen.getByText(/30 aday reranker ile puanlanıp en iyi 3 parça llama3:8b modeline gider\./),
    ).toBeInTheDocument()
  })

  it('givenLlmOnlyConfig_whenPageOpens_thenRetrievalOffDangerAndLlmHowItWorks', async () => {
    serveLiveConfig([liveConfig({ generationMode: 'LLM_ONLY', rerankerStatus: 'NOT_USED' })])
    renderAppAt('/onizleme')

    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(within(strip).getByText('kapalı (Sadece LLM)').closest('span')).toHaveAttribute('data-tone', 'danger')
    expect(
      screen.getByText(
        'Soru önce rehberde aranır (bge-m3 + pgvector), bu modda arama kapalı; soru doğrudan modele gider.',
      ),
    ).toBeInTheDocument()
  })

  it('givenEmptyKnowledgeBase_whenPageOpens_thenWarningNoDocumentsInStrip', async () => {
    serveLiveConfig([liveConfig({ knowledgeBase: { status: 'EMPTY', searchableSourceCount: 0, sources: [] } })])
    renderAppAt('/onizleme')

    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(within(strip).getByText('belge yok').closest('span')).toHaveAttribute('data-tone', 'warning')
  })

  it('givenRetrievalApiDown_whenPageOpens_thenStripShowsDashAndUnreachableWarning', async () => {
    serveLiveConfig([
      liveConfig({
        embeddingModel: null,
        embeddingDimensions: null,
        knowledgeBase: { status: 'UNAVAILABLE', searchableSourceCount: null, sources: [] },
      }),
    ])
    renderAppAt('/onizleme')

    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(strip).toHaveTextContent('Embedding —')
    expect(within(strip).getByText('ulaşılamıyor').closest('span')).toHaveAttribute('data-tone', 'warning')
    expect(strip).toHaveTextContent('Model qwen3.5:9b')
  })

  it('givenLlmOnlyAnswerWithCitationText_whenDone_thenPlainTextAndHowItWorksStays', async () => {
    serveLiveConfig([liveConfig({ generationMode: 'LLM_ONLY', rerankerStatus: 'NOT_USED' })])
    servePreview(llmOnlyAnswer())
    renderAppAt('/onizleme')
    await screen.findByRole('group', { name: 'Canlı yapılandırma' })

    await ask(QUESTION)

    expect(await screen.findByText('Kartını kapat [1].')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeEnabled())
    expect(screen.queryByRole('button', { name: /Kaynak 1/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Rehber · s\./ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /· s\. / })).not.toBeInTheDocument()
    expect(screen.queryByText('Bilgi bankasında henüz belge yok.')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Asistan nasıl cevaplıyor?' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Son cevapta modele giden parçalar' })).not.toBeInTheDocument()
  })

  it('givenLlmOnlyAnswerWithEmptyKnowledgeBase_whenDone_thenNoAddDocumentHint', async () => {
    serveLiveConfig([
      liveConfig({
        generationMode: 'LLM_ONLY',
        rerankerStatus: 'NOT_USED',
        knowledgeBase: { status: 'EMPTY', searchableSourceCount: 0, sources: [] },
      }),
    ])
    servePreview(llmOnlyAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText('Kartını kapat [1].')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: 'Yeni konuşma' })).toBeEnabled())
    expect(screen.queryByText(/henüz belge yok/)).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Belge ekle/ })).not.toBeInTheDocument()
  })

  it('givenAnswerDone_whenDone_thenLiveConfigRefetchedAndRerankerDownShown', async () => {
    const log = serveLiveConfig([liveConfig(), liveConfig({ rerankerStatus: 'DOWN' })])
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')
    const strip = await screen.findByRole('group', { name: 'Canlı yapılandırma' })
    expect(within(strip).getByText('rerank 20→5')).toBeInTheDocument()
    expect(log.requests).toBe(1)

    await ask(QUESTION)

    await waitFor(() => expect(log.requests).toBe(2))
    expect(await screen.findByText('reranker DOWN', { selector: 'b' })).toBeInTheDocument()
  })

  it('givenStreamErrorEvent_whenStreamFinishes_thenLiveConfigRefetched', async () => {
    const log = serveLiveConfig([liveConfig(), liveConfig({ rerankerStatus: 'DOWN' })])
    servePreview([token('Kart'), streamError('CHAT_MODEL_UNAVAILABLE')])
    renderAppAt('/onizleme')
    await screen.findByRole('group', { name: 'Canlı yapılandırma' })

    await ask(QUESTION)

    await waitFor(() => expect(log.requests).toBe(2))
  })

  it('givenRetrievalUnavailableBeforeStream_whenQuestionSent_thenLiveConfigRefetched', async () => {
    const log = serveLiveConfig([
      liveConfig(),
      liveConfig({ knowledgeBase: { status: 'UNAVAILABLE', searchableSourceCount: null, sources: [] } }),
    ])
    servePreview(() => problem(503, 'RETRIEVAL_UNAVAILABLE'))
    renderAppAt('/onizleme')
    await screen.findByRole('group', { name: 'Canlı yapılandırma' })

    await ask(QUESTION)

    await waitFor(() => expect(log.requests).toBe(2))
    expect(await screen.findByText('ulaşılamıyor', { selector: 'b' })).toBeInTheDocument()
  })

  it('givenLiveConfigFails_whenPageOpens_thenAlertAndChatWorks', async () => {
    serveLiveConfig([problem(500, 'INTERNAL_ERROR')])
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    expect(await screen.findByRole('alert')).toHaveTextContent('Beklenmeyen bir hata oluştu. Tekrar dene.')
    expect(screen.queryByRole('group', { name: 'Canlı yapılandırma' })).not.toBeInTheDocument()
    expect(
      screen.getByText('Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Soru önce rehberde aranır/)).not.toBeInTheDocument()

    await ask(QUESTION)

    expect(await screen.findByText('hemen kapat', { exact: false })).toBeInTheDocument()
  })

  it('givenAnswerStillStreaming_whenRendered_thenStillHowItWorksCard', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    renderAppAt('/onizleme')

    await ask(QUESTION)
    stream.push(token('Kartını '), sources())

    await screen.findByText('Kartını')
    expect(screen.getByRole('heading', { name: 'Asistan nasıl cevaplıyor?' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Son cevapta modele giden parçalar' })).not.toBeInTheDocument()
    stream.push(done())
    stream.close()
    expect(await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })).toBeInTheDocument()
  })

  it('givenRerankDone_whenRendered_thenContextCardWithScoresAndChecks', async () => {
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const card = (await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })).closest('section')
    if (card === null) {
      throw new Error('card missing')
    }
    expect(screen.queryByRole('heading', { name: 'Asistan nasıl cevaplıyor?' })).not.toBeInTheDocument()
    expect(within(card).getByText(`"${QUESTION}" · rerank 20→5`)).toBeInTheDocument()
    const rows = within(card).getAllByRole('button')
    expect(rows).toHaveLength(5)
    expect(rows[0]).toHaveTextContent('[1] ✓')
    expect(rows[0]).toHaveTextContent('s. 19 · 2.16 Kayıp ya da çalıntı bildirimi · #31')
    expect(rows[0]).toHaveTextContent('rerank 5,36 · d 0,312')
    expect(rows[1]).not.toHaveTextContent('✓')
    expect(rows[1]).toHaveTextContent('rerank 2,10 · d 0,341')
    expect(rows[4]).toHaveTextContent('rerank -11,04 · d 0,370')
    expect(within(card).getByText('✓ atıf yapılan')).toBeInTheDocument()
    expect(card).not.toHaveTextContent(/sarı|cevabın geçtiği/i)
  })

  it('givenTopKDone_whenRendered_thenTop5AndDistanceOnly', async () => {
    servePreview([
      token('Kartını kapat [1].'),
      sources({
        retrievalStrategy: 'TOP_K',
        contextChunks: [1, 2, 3, 4, 5].map((n) => contextChunk(n, { rerankScore: null })),
      }),
      done({ rerankLatencyMs: null }),
    ])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText(`"${QUESTION}" · top-5`)).toBeInTheDocument()
    const row = screen.getByRole('button', { name: /^\[1\]/ })
    expect(row).toHaveTextContent('d 0,312')
    expect(row).not.toHaveTextContent('rerank')
  })

  it('givenFallbackDone_whenRendered_thenRerankerDownLabel', async () => {
    servePreview([
      token('Kartını kapat [1].'),
      sources({
        retrievalStrategy: 'RERANK_FALLBACK',
        contextChunks: [1, 2, 3, 4, 5].map((n) => contextChunk(n, { rerankScore: null })),
      }),
      done({ rerankLatencyMs: 5003 }),
    ])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText(`"${QUESTION}" · reranker DOWN · vector sırası`)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^\[2\]/ })).not.toHaveTextContent('rerank')
    expect(screen.getByText('5,00 sn')).toBeInTheDocument()
  })

  it('givenLongQuestion_whenContextCardShown_thenQuestionShortenedToSixtyCharacters', async () => {
    const longQuestion = `${'Çok uzun bir soru '.repeat(5)}sonu`
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(longQuestion)

    const expected = `"${longQuestion.slice(0, 59)}…" · rerank 20→5`
    expect(await screen.findByText(expected)).toBeInTheDocument()
  })

  it('givenRewrittenSecondTurn_whenDone_thenSubtitleShowsSearchQuery', async () => {
    servePreview([
      token('Teslim süresi 3 iş günü [1].'),
      sources({ searchQuery: 'Kaybolan kartın yenisi kaç günde gelir?', queryRewriteStatus: 'REWRITTEN' }),
      done({ queryRewriteLatencyMs: 812 }),
    ])
    renderAppAt('/onizleme')

    await ask('Yenisi kaç günde gelir?')

    expect(
      await screen.findByText(
        '"Yenisi kaç günde gelir?" · rerank 20→5 · arama: "Kaybolan kartın yenisi kaç günde gelir?"',
      ),
    ).toBeInTheDocument()
  })

  it('givenRewriteFailed_whenDone_thenSubtitleShowsFailureLabelWithoutSearchQuery', async () => {
    servePreview([
      token('Cevap [1].'),
      sources({ searchQuery: 'Yenisi kaç günde gelir?', queryRewriteStatus: 'FAILED' }),
      done({ queryRewriteLatencyMs: 40 }),
    ])
    renderAppAt('/onizleme')

    await ask('Yenisi kaç günde gelir?')

    expect(
      await screen.findByText('"Yenisi kaç günde gelir?" · rerank 20→5 · yeniden yazma başarısız'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/arama:/)).not.toBeInTheDocument()
  })

  it('givenNoHistoryTurn_whenDone_thenSubtitleHasNoSearchOrFailureLabel', async () => {
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText(`"${QUESTION}" · rerank 20→5`)).toBeInTheDocument()
  })

  it('givenRewrittenDone_whenRendered_thenRewriteLatencyRowBeforeRetrievalRow', async () => {
    servePreview([
      token('Cevap [1].'),
      sources({ searchQuery: 'Kaybolan kartın yenisi kaç günde gelir?', queryRewriteStatus: 'REWRITTEN' }),
      done({ queryRewriteLatencyMs: 812 }),
    ])
    renderAppAt('/onizleme')

    await ask('Yenisi kaç günde gelir?')

    const heading = await screen.findByRole('heading', { level: 3, name: /^Süre/ })
    const rows = heading.nextElementSibling
    expect(rows).toHaveTextContent('yeniden yazma')
    expect(rows).toHaveTextContent('812 ms')
    expect(rows?.textContent?.indexOf('yeniden yazma')).toBeLessThan(
      rows?.textContent?.indexOf('retrieval-api araması') ?? -1,
    )
  })

  it('givenRerankDone_whenRendered_thenLatencyRowsAndHostLabel', async () => {
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const heading = await screen.findByRole('heading', { level: 3, name: /^Süre/ })
    expect(heading).toHaveTextContent('Süre · 4,73 sn · Apple M5 · 16 GB')
    const rows = heading.nextElementSibling
    expect(rows).toHaveTextContent('retrieval-api araması')
    expect(rows).toHaveTextContent('84 ms')
    expect(rows).toHaveTextContent('rerank')
    expect(rows).toHaveTextContent('712 ms')
    expect(rows).toHaveTextContent('qwen3.5 cevabı')
    expect(rows).toHaveTextContent('3,92 sn')
  })

  it('givenTopKDoneWithoutHostLabel_whenRendered_thenNoRerankRowNoHost', async () => {
    servePreview([
      token('Kartını kapat [1].'),
      sources({ retrievalStrategy: 'TOP_K' }),
      done({ rerankLatencyMs: null, hostLabel: null }),
    ])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const heading = await screen.findByRole('heading', { level: 3, name: /^Süre/ })
    expect(heading).toHaveTextContent(/^Süre · 4,73 sn$/)
    expect(heading.nextElementSibling).not.toHaveTextContent('rerank')
    expect(screen.queryByText(/Apple/)).not.toBeInTheDocument()
  })

  it('givenContextRow_whenClicked_thenChunkDialogOpens', async () => {
    servePreview(lostCardAnswer())
    const requested = serveChunkDetails()
    renderAppAt('/onizleme')
    await ask(QUESTION)
    const rows = await screen.findAllByRole('button', { name: /^\[\d\]/ })

    await userEvent.click(rows[2] as HTMLElement)

    await screen.findByRole('dialog', { name: 'Parça 153 başlığı' })
    expect(requested).toEqual([153])
  })

  it('givenSecondQuestionStreaming_whenRendered_thenSideCardShowsHowItWorksAgain', async () => {
    const stream = controllableSseStream()
    let call = 0
    servePreview(() => {
      call += 1
      return call === 1 ? sseResponse(lostCardAnswer()) : stream.response()
    })
    renderAppAt('/onizleme')
    await ask(QUESTION)
    await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })

    await ask('Limitimi nasıl artırırım?')

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Asistan nasıl cevaplıyor?' })).toBeInTheDocument())
    stream.close()
  })

  it('givenAnsweredPreview_whenRendered_thenHasNoAxeViolations', async () => {
    servePreview(lostCardAnswer())
    const { container } = renderAppAt('/onizleme')
    await ask(QUESTION)
    await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })

    expect(await axe(container)).toHaveNoViolations()
  })

  it('givenHtmlInAnswerToken_whenRendered_thenShownAsText', async () => {
    servePreview([token('<img src=x onerror=alert(1)>'), sources(), done()])
    const { container } = renderAppAt('/onizleme')

    await ask(QUESTION)

    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeInTheDocument()
    expect(container.querySelector('img')).toBeNull()
  })

  it('givenDoneEvent_whenStreamFinishes_thenLiveRegionAnnouncesCompletion', async () => {
    const stream = controllableSseStream()
    servePreview(() => stream.response())
    const { container } = renderAppAt('/onizleme')

    await ask(QUESTION)
    await waitFor(() => expect(liveRegion(container)).toHaveTextContent('Asistan yazıyor…'))
    await act(async () => {
      stream.push(token('Kartını '), sources(), done())
      stream.close()
    })

    await waitFor(() => expect(liveRegion(container)).toHaveTextContent('Cevap tamamlandı.'))
  })

  it('givenErrorEvent_whenStreamFinishes_thenLiveRegionHasNoErrorTextAndAlertShowsItOnce', async () => {
    servePreview([token('Kartını '), streamError('CHAT_MODEL_UNAVAILABLE')])
    const { container } = renderAppAt('/onizleme')

    await ask(QUESTION)

    await screen.findByRole('alert')
    const message = 'Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.'
    expect(liveRegion(container)).not.toHaveTextContent(message)
    expect(screen.getAllByText(message)).toHaveLength(1)
  })

  it('givenPreviewRoute_whenLeftAndReturned_thenFreshConversation', async () => {
    const log = servePreview(lostCardAnswer())
    server.use(http.get('/api/workspaces/42/sources', () => HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })))
    renderAppAt('/onizleme')
    await ask(QUESTION)
    await screen.findByRole('button', { name: 'Kaynak 1' })

    await userEvent.click(within(screen.getByRole('navigation', { name: 'Menü' })).getByRole('link', { name: 'Bilgi kaynakları' }))
    await screen.findByRole('heading', { level: 1, name: 'Bilgi kaynakları' })
    await userEvent.click(within(screen.getByRole('navigation', { name: 'Menü' })).getByRole('link', { name: 'Önizleme' }))

    expect(await screen.findByText(WELCOME_MESSAGE)).toBeInTheDocument()
    expect(screen.queryByText(QUESTION)).not.toBeInTheDocument()
    expect(log.conversationBodies).toHaveLength(1)
  })
  it('givenAnswerChunkInSources_whenAnswerDone_thenCardMarksChunkAndShowsLegend', async () => {
    const chunks = [1, 2, 3, 4, 5].map((n) => contextChunk(n, { containsAnswer: n === 2 }))
    servePreview([token('Kartını hemen kapat [1].'), sources({ contextChunks: chunks }), done()])
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const card = (await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })).closest('section') as HTMLElement
    const buttons = within(card).getAllByRole('button').filter((button) => /^\[\d\]/.test(button.textContent ?? ''))
    expect(buttons).toHaveLength(5)
    expect(buttons.map((button) => /gold/.test(button.className))).toEqual([false, true, false, false, false])
    expect(within(card).getByText('✓ atıf yapılan · sarı: cevabın geçtiği parça')).toBeInTheDocument()
  })

  it('givenNoAnswerChunkInSources_whenAnswerDone_thenLegendHasNoYellowNote', async () => {
    servePreview(lostCardAnswer())
    renderAppAt('/onizleme')

    await ask(QUESTION)

    const card = (await screen.findByRole('heading', { name: 'Son cevapta modele giden parçalar' })).closest('section') as HTMLElement
    expect(within(card).getByText('✓ atıf yapılan')).toBeInTheDocument()
    expect(card).not.toHaveTextContent('sarı')
  })
})
