import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { failedSource, GUIDE_FILE, GUIDE_NAME, job, pageOf, source } from '../../../test/fixtures.ts'
import {
  countOf,
  deferred,
  recordRequests,
  serveDetail,
  serveJobSequence,
  serveSourceList,
  serveSourceListSequence,
  sleep,
} from '../../../test/handlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'

describe('SourceListPage', () => {
  it('givenSlowServer_whenPageLoads_thenShowsLoadingText', () => {
    renderAppAt('/kaynaklar')

    expect(screen.getByText('Yükleniyor…')).toBeInTheDocument()
  })

  it('givenNoSources_whenPageLoads_thenShowsEmptyStateAndWorkspaceCallout', async () => {
    serveSourceList([])

    renderAppAt('/kaynaklar')

    expect(await screen.findByRole('heading', { level: 2, name: 'Henüz belge yok' })).toBeInTheDocument()
    expect(
      screen.getByText('Asistan, cevap verirken buraya eklediğin belgelerde arar. İlk belgeni ekleyerek başla.'),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Belge ekle' })).toHaveLength(2)
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    const callout = await screen.findByText(/Modeli değiştirmek bütün parçaların yeniden embed edilmesi demek/)
    expect(callout).toHaveTextContent(
      'Çalışma alanı Raşit Bankası · Dijital Asistan (ws 42) · embedding modeli bge-m3@1024. Modeli değiştirmek bütün parçaların yeniden embed edilmesi demek; retrieval-api farklı modelle yüklemeyi 409 ile reddeder.',
    )
  })

  it('givenWorkspaceFromBackend_whenPageLoads_thenCalloutAndDescriptionUseBackendValues', async () => {
    server.use(
      http.get('/api/workspaces/42', () =>
        HttpResponse.json({
          workspaceId: 42,
          name: 'Başka Çalışma Alanı',
          embeddingModel: 'nomic-embed',
          embeddingDimensions: 768,
          targetChunkChars: 1600,
          maxChunkChars: 2000,
          overlapChars: 200,
          vectorIndexType: 'HNSW',
          distanceMetric: 'COSINE',
        }),
      ),
    )

    renderAppAt('/kaynaklar')

    const callout = await screen.findByText(/Modeli değiştirmek/)
    expect(callout).toHaveTextContent('Çalışma alanı Başka Çalışma Alanı (ws 42) · embedding modeli nomic-embed@768.')
    expect(screen.getByText(/her parça nomic-embed ile embed edilip pgvector'e yazılır\./)).toBeInTheDocument()
  })

  it('givenReadySource_whenPageLoads_thenRowShowsLabColumns', async () => {
    serveSourceList([source()])

    renderAppAt('/kaynaklar')

    const row = await screen.findByRole('row', { name: new RegExp(GUIDE_NAME) })
    expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual([
      'Kaynak',
      'Tür',
      'Sürüm',
      'Parça',
      'Durum',
      'Son senkron',
    ])
    const cells = within(row).getAllByRole('cell')
    expect(cells.map((cell) => cell.textContent)).toEqual([
      `${GUIDE_NAME}${GUIDE_FILE}`,
      'PDF · 70 sayfa',
      '2026.10',
      '154',
      'Hazır',
      '29.10.2026 16:15',
    ])
    expect(within(row).getByRole('link', { name: GUIDE_NAME })).toHaveAttribute('href', '/kaynaklar/10001')
  })

  it('givenSourceNeverIngested_whenPageLoads_thenPagesCountAndSyncShowDashes', async () => {
    serveSourceList([failedSource()])

    renderAppAt('/kaynaklar')

    const cells = within(await screen.findByRole('row', { name: /Atlaskap El Kitabı/ })).getAllByRole('cell')
    expect(cells[1]).toHaveTextContent('PDF')
    expect(cells[1]).not.toHaveTextContent('sayfa')
    expect(cells[3]).toHaveTextContent('–')
    expect(cells[5]).toHaveTextContent('–')
  })

  it('givenHtmlInSourceName_whenRendered_thenShownAsTextWithoutElements', async () => {
    const html = '<img src=x onerror=alert(1)>'
    serveSourceList([source({ name: html })])

    const { container } = renderAppAt('/kaynaklar')

    expect(await screen.findByText(html)).toBeInTheDocument()
    expect(container.querySelector('img')).toBeNull()
  })

  it('givenEmbeddingJob_whenJobAdvancesToDone_thenRowShowsHazirAndPollingStops', async () => {
    const running = source({
      chunkCount: null,
      pageCount: null,
      syncedAt: null,
      latestJob: job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null }),
    })
    const log = recordRequests()
    serveSourceListSequence([running], [source({ latestJob: job({ jobId: 7 }) })])
    serveJobSequence(
      7,
      job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null }),
      job({ jobId: 7, stage: 'WRITING', finishedAt: null }),
      job({ jobId: 7, stage: 'DONE' }),
    )

    renderAppAt('/kaynaklar')

    expect(await screen.findByText('EMBEDDING')).toBeInTheDocument()
    expect(await screen.findByText('Hazır', undefined, { timeout: 8000 })).toBeInTheDocument()
    await sleep(1600)
    expect(countOf(log, '/api/workspaces/42/ingestion-jobs/7')).toBe(3)
  }, 20000)

  it('givenRunningJob_whenNavigatedAway_thenNoMoreJobRequests', async () => {
    const log = recordRequests()
    serveSourceList([source({ latestJob: job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null }) })])
    server.use(
      http.get('/api/workspaces/42/ingestion-jobs/7', () =>
        HttpResponse.json(job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null })),
      ),
    )
    renderAppAt('/kaynaklar')
    await waitFor(() => expect(countOf(log, '/api/workspaces/42/ingestion-jobs/7')).toBeGreaterThanOrEqual(1))

    await userEvent.click(within(screen.getByRole('navigation', { name: 'Menü' })).getByRole('link', { name: 'Önizleme' }))
    await sleep(100)
    const countAfterLeaving = countOf(log, '/api/workspaces/42/ingestion-jobs/7')
    await sleep(2300)

    expect(countOf(log, '/api/workspaces/42/ingestion-jobs/7')).toBe(countAfterLeaving)
  }, 15000)

  it('givenFailedSource_whenRetryClicked_thenRowShowsQueued', async () => {
    const failed = failedSource()
    const queued = job({ jobId: 6, sourceId: 10002, stage: 'QUEUED', fileName: 'atlaskap.pdf', version: '1.0', finishedAt: null, startedAt: null })
    let retryBody = 'not-read'
    let retryCalls = 0
    serveSourceListSequence([failed], [{ ...failed, latestJob: queued }])
    server.use(
      http.post('/api/workspaces/42/sources/10002/ingestion-jobs', async ({ request }) => {
        retryCalls += 1
        retryBody = await request.text()
        return HttpResponse.json(queued, { status: 202 })
      }),
      http.get('/api/workspaces/42/ingestion-jobs/6', () => HttpResponse.json(queued)),
    )
    renderAppAt('/kaynaklar')
    const row = await screen.findByRole('row', { name: /Atlaskap El Kitabı/ })
    expect(within(row).getByText('Başarısız')).toBeInTheDocument()
    expect(
      within(row).getByText('Embedding modeline ulaşılamadı. Model sunucusu açılınca yeniden dene.'),
    ).toBeInTheDocument()

    await userEvent.click(within(row).getByRole('button', { name: 'Yeniden dene' }))

    expect(await within(screen.getByRole('row', { name: /Atlaskap El Kitabı/ })).findByText('QUEUED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Yeniden dene' })).not.toBeInTheDocument()
    expect(retryCalls).toBe(1)
    expect(retryBody).toBe('')
  })

  it('givenRetryConflict_whenRetryClicked_thenShowsInProgressMessageAndRefetchesList', async () => {
    const log = recordRequests()
    serveSourceList([failedSource()])
    server.use(
      http.post('/api/workspaces/42/sources/10002/ingestion-jobs', () => problem(409, 'INGESTION_IN_PROGRESS')),
    )
    renderAppAt('/kaynaklar')
    await userEvent.click(await screen.findByRole('button', { name: 'Yeniden dene' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Bu belgenin işi zaten sürüyor.')
    await waitFor(() => expect(countOf(log, '/api/workspaces/42/sources')).toBeGreaterThanOrEqual(2))
  })

  it('givenRetryInFlight_whenClickedAgain_thenButtonDisabledAndSingleRequest', async () => {
    const gate = deferred()
    let retryCalls = 0
    const queued = job({ jobId: 6, sourceId: 10002, stage: 'QUEUED' })
    serveSourceListSequence([failedSource()], [{ ...failedSource(), latestJob: queued }])
    server.use(
      http.post('/api/workspaces/42/sources/10002/ingestion-jobs', async () => {
        retryCalls += 1
        await gate.promise
        return HttpResponse.json(queued, { status: 202 })
      }),
      http.get('/api/workspaces/42/ingestion-jobs/6', () =>
        HttpResponse.json(job({ jobId: 6, sourceId: 10002, stage: 'QUEUED' })),
      ),
    )
    renderAppAt('/kaynaklar')
    const button = await screen.findByRole('button', { name: 'Yeniden dene' })

    await userEvent.click(button)
    await waitFor(() => expect(button).toBeDisabled())
    await userEvent.click(button)
    gate.resolve()

    await waitFor(() => expect(screen.queryByRole('button', { name: 'Yeniden dene' })).not.toBeInTheDocument())
    expect(retryCalls).toBe(1)
  })

  it('givenRetryButtonInRow_whenClicked_thenDoesNotNavigateToDetail', async () => {
    serveSourceList([failedSource()])
    server.use(
      http.post('/api/workspaces/42/sources/10002/ingestion-jobs', () => problem(409, 'INGESTION_IN_PROGRESS')),
    )
    renderAppAt('/kaynaklar')

    await userEvent.click(await screen.findByRole('button', { name: 'Yeniden dene' }))

    await screen.findByRole('alert')
    expect(screen.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeInTheDocument()
  })

  it('givenSourceRow_whenNameLinkClicked_thenNavigatesToDetail', async () => {
    serveSourceList([source()])
    serveDetail()
    renderAppAt('/kaynaklar')

    await userEvent.click(await screen.findByRole('link', { name: GUIDE_NAME }))

    expect(await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: "PDF'i aç" })).toHaveAttribute('href', '/api/workspaces/42/sources/10001/file')
  })

  it('givenSourceRow_whenEmptyCellClicked_thenNavigatesToDetail', async () => {
    serveSourceList([source()])
    serveDetail()
    renderAppAt('/kaynaklar')
    const row = await screen.findByRole('row', { name: new RegExp(GUIDE_NAME) })

    await userEvent.click(within(row).getByText('PDF · 70 sayfa'))

    expect(await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })).toBeInTheDocument()
  })

  it('givenTwoPagesOfSources_whenNextClicked_thenRequestsPage1Size20', async () => {
    const log = recordRequests()
    server.use(
      http.get('/api/workspaces/42/sources', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json(
          pageOf([source({ sourceId: 10001 + page, name: `Belge ${page + 1}` })], { page, size: 20, totalElements: 21 }),
        )
      }),
    )
    renderAppAt('/kaynaklar')
    expect(await screen.findByText('Belge 1')).toBeInTheDocument()
    const pager = screen.getByRole('navigation', { name: 'Kaynak sayfaları' })
    expect(within(pager).getByText('1 / 2')).toBeInTheDocument()

    await userEvent.click(within(pager).getByRole('button', { name: 'Sonraki' }))

    expect(await screen.findByText('Belge 2')).toBeInTheDocument()
    const sourceRequests = log.urls.filter((url) => url.pathname === '/api/workspaces/42/sources')
    expect(sourceRequests.map((url) => url.search)).toContain('?page=1&size=20')
  })

  it('givenSinglePageOfSources_whenRendered_thenNoPager', async () => {
    serveSourceList([source()])

    renderAppAt('/kaynaklar')

    await screen.findByRole('row', { name: new RegExp(GUIDE_NAME) })
    expect(screen.queryByRole('navigation', { name: 'Kaynak sayfaları' })).not.toBeInTheDocument()
  })

  it('givenNetworkError_whenListLoads_thenShowsNetworkMessage', async () => {
    server.use(http.get('/api/workspaces/42/sources', () => HttpResponse.error()))

    renderAppAt('/kaynaklar')

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor. Bağlantını kontrol et.')
  })

  it('givenServerError_whenListLoads_thenShowsFallbackMessage', async () => {
    server.use(http.get('/api/workspaces/42/sources', () => problem(500, 'INTERNAL_ERROR')))

    renderAppAt('/kaynaklar')

    expect(await screen.findByRole('alert', undefined, { timeout: 4000 })).toHaveTextContent(
      'Beklenmeyen bir hata oluştu. Tekrar dene.',
    )
  })

  it('givenListPage_whenRendered_thenHasNoAxeViolations', async () => {
    serveSourceList([source(), failedSource()])
    const { container } = renderAppAt('/kaynaklar')
    await screen.findByRole('row', { name: new RegExp(GUIDE_NAME) })
    await screen.findByText(/Modeli değiştirmek/)

    expect(await axe(container)).toHaveNoViolations()
  })

  it('givenEmptyListPage_whenRendered_thenHasNoAxeViolations', async () => {
    const { container } = renderAppAt('/kaynaklar')
    await screen.findByText('Henüz belge yok')

    expect(await axe(container)).toHaveNoViolations()
  })
})
