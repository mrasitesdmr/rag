import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { describe, expect, it } from 'vitest'
import { axe } from 'vitest-axe'
import { App } from '../../../app/App.tsx'
import {
  doneRunDetail,
  evalRun,
  failedRun,
  goldenSet,
  missedQuestion,
  overview,
  runningRun,
  serveEvaluation,
} from '../../../test/evaluationFixtures.ts'
import { liveConfig } from '../../../test/fixtures.ts'
import { sleep } from '../../../test/handlers.ts'
import { serveLiveConfig } from '../../../test/liveConfigHandlers.ts'
import { lostCardAnswer } from '../../../test/previewFixtures.ts'
import { servePreview } from '../../../test/previewHandlers.ts'
import { renderAppAt, renderWithProviders } from '../../../test/renderWithProviders.tsx'
import { problem } from '../../../test/server.ts'

const RUN_BUTTON = /^Ölçümü çalıştır|^Ölçülüyor/

async function openPage() {
  const view = renderAppAt('/degerlendirme')
  await screen.findByRole('button', { name: RUN_BUTTON })
  return view
}

async function openWithOverview(...overviews: Parameters<typeof serveEvaluation>[0]) {
  const log = serveEvaluation(overviews)
  await openPage()
  await screen.findByRole('heading', { name: 'Değerlendirme', level: 1 })
  return log
}

describe('EvaluationPage', () => {
  it('givenNoGoldenSet_whenPageOpens_thenRunButtonDisabledAndNoticeShown', async () => {
    serveEvaluation([overview({ goldenSet: null })])
    await openPage()

    expect(await screen.findByText('Golden set yüklenmemiş.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: RUN_BUTTON })).toBeDisabled()
    expect(screen.getByText(/^Rehberde cevabı olan sorular; her birinde/)).toBeInTheDocument()
  })

  it('givenGoldenSet_whenPageOpens_thenDescriptionShowsQuestionCountAndButtonEnabled', async () => {
    serveEvaluation([overview({ goldenSet: goldenSet({ questionCount: 100 }) })])
    await openPage()

    expect(await screen.findByText(/^Rehberde cevabı olan 100 soru; her birinde/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: RUN_BUTTON })).toBeEnabled()
  })

  it('givenLlmOnlyMode_whenPageOpens_thenRunButtonDisabledAndWarningShown', async () => {
    serveEvaluation([overview()])
    serveLiveConfig([liveConfig({ generationMode: 'LLM_ONLY' })])
    await openPage()

    expect(await screen.findByText("Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al.")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: RUN_BUTTON })).toBeDisabled()
  })

  it.each([
    ['RERANK', 'Ölçümü çalıştır · rerank 20→5'],
    ['TOP_K', 'Ölçümü çalıştır · top-5'],
  ] as const)('givenStrategy%s_whenPageOpens_thenButtonShowsStrategy', async (strategy, expected) => {
    serveEvaluation([overview()])
    serveLiveConfig([liveConfig({ retrievalStrategy: strategy })])
    await openPage()

    expect(await screen.findByRole('button', { name: expected })).toBeEnabled()
  })

  it('givenNoDoneRun_whenPageOpens_thenShowsNoMeasurementText', async () => {
    await openWithOverview(overview())

    expect(
      screen.getByText(
        'Henüz ölçüm yok. Mevcut retrieval ayarıyla çalıştır; sonra ayarı değiştirip tekrar çalıştırarak iki satırı karşılaştır.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText('doğru parçanın sırası')).not.toBeInTheDocument()
  })

  it.each([
    [0.947, '0,95', true],
    [0.95, '0,95', false],
    [0.9, '0,90', true],
  ])('givenRecall%s_whenPageOpens_thenShowsRoundedValueWithRedFlag%s', async (recall, shown, isRed) => {
    const run = doneRunDetail({ recallAtK: recall, mrr: 0.871 })
    await openWithOverview(overview({ lastDoneRun: run, lastFinishedRun: run }))

    const value = await screen.findByText(shown, { selector: 'b' })
    expect(/bad/.test(value.className)).toBe(isRed)
    expect(/good/.test(value.className)).toBe(!isRed)
    expect(screen.getByText('0,87', { selector: 'b' })).toBeInTheDocument()
  })

  it('givenUnmatchedAndFallback_whenPageOpens_thenSubtitlesShowCounts', async () => {
    const run = doneRunDetail({
      measuredCount: 99,
      hitCount: 94,
      missedCount: 5,
      unmatchedCount: 1,
      recallAtK: 94 / 99,
      rerankFallbackCount: 12,
    })
    await openWithOverview(overview({ lastDoneRun: run, lastFinishedRun: run }))

    expect(await screen.findByText('94/99 soru · 1 soru ölçülemedi')).toBeInTheDocument()
    expect(screen.getByText('5', { selector: 'b' })).toBeInTheDocument()
    expect(screen.getByText('rehber 2026.10 · reranker DOWN: 12 soru')).toBeInTheDocument()
    expect(screen.getByText('rerank 20→5', { selector: 'b' })).toBeInTheDocument()
  })

  it('givenAllMissedZero_whenPageOpens_thenMissedKpiGreen', async () => {
    const run = doneRunDetail({ hitCount: 100, missedCount: 0, recallAtK: 1, mrr: 1 })
    await openWithOverview(overview({ lastDoneRun: run, lastFinishedRun: run }))

    const missed = await screen.findByText('0', { selector: 'b' })
    expect(missed.className).toMatch(/good/)
  })

  it('givenMissedAndMovedRows_whenPageOpens_thenShowsTwentyPlusAndPositions', async () => {
    const run = doneRunDetail({
      missedAndMovedQuestions: [
        missedQuestion(5, { text: 'Kaçan soru?', vectorRank: null, promptPosition: null }),
        missedQuestion(9, { text: 'Kayan soru?', vectorRank: 7, promptPosition: 2 }),
      ],
    })
    await openWithOverview(overview({ lastDoneRun: run, lastFinishedRun: run }))

    const rows = within((await screen.findAllByRole('table'))[1]).getAllByRole('row')
    expect(within(rows[1]).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['Kaçan soru?', '20+', 'yok'])
    expect(within(rows[2]).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['Kayan soru?', '7', '2. sıra'])
    expect(screen.getByRole('heading', { name: 'Kaçanlar ve sırası değişenler' })).toBeInTheDocument()
  })

  it('givenNoMissedRows_whenPageOpens_thenShowsAllSamePlaceText', async () => {
    const run = doneRunDetail({ missedAndMovedQuestions: [] })
    await openWithOverview(overview({ lastDoneRun: run, lastFinishedRun: run }))

    expect(await screen.findByText('Bütün sorularda doğru parça vector sırasıyla aynı yerde.')).toBeInTheDocument()
  })

  it('givenMissedRow_whenClicked_thenPreviewAsksQuestionOnceInNewConversation', async () => {
    const run = doneRunDetail({
      missedAndMovedQuestions: [missedQuestion(5, { text: 'Kaçan soru?', vectorRank: null, promptPosition: null })],
    })
    serveEvaluation([overview({ lastDoneRun: run, lastFinishedRun: run })])
    const log = servePreview(lostCardAnswer())
    renderWithProviders(
      <StrictMode>
        <App />
      </StrictMode>,
      '/degerlendirme',
    )

    await userEvent.click(await screen.findByRole('button', { name: 'Kaçan soru?' }))

    await screen.findByRole('combobox', { name: 'Soru' })
    await waitFor(() => expect(log.messages).toHaveLength(1))
    await sleep(100)
    expect(log.conversationBodies).toHaveLength(1)
    expect(log.messages).toHaveLength(1)
    expect(log.messages[0].body).toEqual({ text: 'Kaçan soru?' })
  })

  it('givenStartedRun_whenPolling_thenShowsProgressAndStopsWhenDone', async () => {
    const done = doneRunDetail()
    const log = serveEvaluation([
      overview(),
      overview({ runningRun: runningRun({ measuredCount: 3, processedCount: 2 }) }),
      overview({ lastDoneRun: done, lastFinishedRun: done }),
    ])
    await openPage()
    await screen.findByText(/^Henüz ölçüm yok/)

    await userEvent.click(screen.getByRole('button', { name: RUN_BUTTON }))

    expect(await screen.findByRole('button', { name: 'Ölçülüyor · 0/3' })).toBeDisabled()
    expect(await screen.findByRole('button', { name: 'Ölçülüyor · 2/3' }, { timeout: 3000 })).toBeDisabled()
    expect(await screen.findByText('doğru parçanın sırası', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^Ölçümü çalıştır/ })).toBeEnabled()
    const overviewCalls = log.overview
    await sleep(1400)
    expect(log.overview).toBe(overviewCalls)
    expect(log.starts).toBe(1)
  }, 15000)

  it('givenRunningRunInOverview_whenPageOpens_thenButtonShowsProgressAndIsDisabled', async () => {
    serveEvaluation([overview({ runningRun: runningRun({ processedCount: 37, measuredCount: 100 }) })])
    await openPage()

    expect(await screen.findByRole('button', { name: 'Ölçülüyor · 37/100' })).toBeDisabled()
  })

  it.each([
    [409, 'GOLDEN_SET_NOT_LOADED', "Golden set yüklenmemiş. Önce golden set'i yükle."],
    [409, 'GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE', "Golden set'in belgesi bilgi bankasında yok. Önce belgeyi yükle."],
    [409, 'EVAL_RUN_IN_PROGRESS', 'Bir ölçüm zaten sürüyor. Bitmesini bekle.'],
    [409, 'EVAL_NOT_AVAILABLE_IN_LLM_ONLY', "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al."],
    [503, 'RETRIEVAL_UNAVAILABLE', 'Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.'],
  ])('givenStartError%s%s_whenRunClicked_thenShowsMessage', async (status, errorCode, message) => {
    const log = serveEvaluation([overview()], [], { start: problem(status, errorCode) })
    await openPage()
    await screen.findByText(/^Henüz ölçüm yok/)

    await userEvent.click(screen.getByRole('button', { name: RUN_BUTTON }))

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    if (errorCode === 'EVAL_RUN_IN_PROGRESS') {
      await waitFor(() => expect(log.overview).toBe(2))
    }
  })

  it('givenLastFinishedRunInterrupted_whenPageOpens_thenShowsFailureNote', async () => {
    await openWithOverview(overview({ lastFinishedRun: failedRun({ errorCode: 'EVAL_RUN_INTERRUPTED' }) }))

    expect(
      await screen.findByText('Son ölçüm tamamlanamadı: servis ölçüm sırasında yeniden başladı.'),
    ).toBeInTheDocument()
  })

  it.each([
    ['RETRIEVAL_UNAVAILABLE', 'Son ölçüm tamamlanamadı: bilgi bankasına ulaşılamadı.'],
    ['INTERNAL_ERROR', 'Son ölçüm tamamlanamadı: beklenmeyen bir hata oluştu.'],
  ] as const)('givenLastFinishedRunFailedWith%s_whenPageOpens_thenShowsReason', async (errorCode, note) => {
    await openWithOverview(overview({ lastFinishedRun: failedRun({ errorCode }) }))

    expect(await screen.findByText(note)).toBeInTheDocument()
  })

  it('givenTwentyOneRuns_whenPageOpens_thenFirstPageAndRowTexts', async () => {
    const runs = Array.from({ length: 21 }, (_, index) => evalRun({ evalRunId: 21 - index }))
    runs[0] = runningRun({ evalRunId: 21 })
    runs[1] = failedRun({ evalRunId: 20 })
    runs[9] = evalRun({ evalRunId: 12, recallAtK: 0.947, mrr: 0.8123, rerankFallbackCount: 2 })
    const done = doneRunDetail()
    const log = serveEvaluation([overview({ lastDoneRun: done, lastFinishedRun: done })], runs)
    renderAppAt('/degerlendirme')
    await screen.findByText('ev_21')

    const table = screen.getAllByRole('table')[0]
    const rows = within(table).getAllByRole('row')
    expect(rows).toHaveLength(21)
    const cells = (row: HTMLElement) => within(row).getAllByRole('cell').map((cell) => cell.textContent)
    expect(cells(rows[1]).slice(0, 5)).toEqual(['ev_21', 'rerank 20→5', '2026.10', 'ölçülüyor…', '–'])
    expect(cells(rows[2]).slice(0, 5)).toEqual(['ev_20', 'rerank 20→5', '2026.10', 'başarısız', '–'])
    expect(cells(rows[10]).slice(0, 5)).toEqual(['ev_12', 'rerank 20→5 · reranker DOWN (2)', '2026.10', '0,947', '0,812'])
    expect(log.runs[0].searchParams.get('size')).toBe('20')

    await userEvent.click(screen.getByRole('button', { name: 'Sonraki' }))

    expect(await screen.findByText('ev_1')).toBeInTheDocument()
    expect(within(screen.getAllByRole('table')[0]).getAllByRole('row')).toHaveLength(2)
    expect(log.runs.at(-1)?.searchParams.get('page')).toBe('1')
  })

  it('givenLoadedEvaluationPage_whenAxeRuns_thenNoViolations', async () => {
    const run = doneRunDetail({ missedAndMovedQuestions: [missedQuestion(5, { vectorRank: null, promptPosition: null })] })
    serveEvaluation([overview({ lastDoneRun: run, lastFinishedRun: run })], [evalRun()])
    const { container } = renderAppAt('/degerlendirme')
    await screen.findByText('ev_7')

    expect(await axe(container)).toHaveNoViolations()
  })
})
