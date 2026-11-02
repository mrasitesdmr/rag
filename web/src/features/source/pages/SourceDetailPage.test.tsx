import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import {
  chunkDetail,
  chunkSummary,
  failedSource,
  GUIDE_FILE,
  GUIDE_NAME,
  job,
  pageOf,
  source,
  workspace,
} from '../../../test/fixtures.ts'
import { countOf, recordRequests, serveDetail, serveJobSequence, sleep } from '../../../test/handlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'

function metaValue(label: string): HTMLElement {
  const value = screen.getByText(label, { selector: 'dt' }).nextElementSibling
  if (!(value instanceof HTMLElement)) {
    throw new Error(`no value for ${label}`)
  }
  return value
}

function tableWithHeader(header: string): HTMLElement {
  const table = screen.getByText(header, { selector: 'th' }).closest('table')
  if (table === null) {
    throw new Error(`no table for ${header}`)
  }
  return table
}

function bodyRows(table: HTMLElement): HTMLElement[] {
  return within(table).getAllByRole('row').slice(1)
}

function cellTexts(row: HTMLElement): (string | null)[] {
  return within(row).getAllByRole('cell').map((cell) => cell.textContent)
}

describe('SourceDetailPage', () => {
  it('givenReadySource_whenDetailLoads_thenHeaderAndPdfLink', async () => {
    const log = recordRequests()
    serveDetail()

    renderAppAt('/kaynaklar/10001')

    expect(await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })).toBeInTheDocument()
    expect(
      screen.getByText(`${GUIDE_FILE} · 412.873 bayt · sürüm 2026.10 · son senkron 29.10.2026 16:15`),
    ).toBeInTheDocument()
    const crumbs = screen.getByText('›').parentElement as HTMLElement
    expect(within(crumbs).getByRole('link', { name: 'Bilgi kaynakları' })).toHaveAttribute('href', '/kaynaklar')
    expect(within(crumbs).getByText(GUIDE_NAME)).toBeInTheDocument()
    const pdfLink = screen.getByRole('link', { name: "PDF'i aç" })
    expect(pdfLink).toHaveAttribute('href', '/api/workspaces/42/sources/10001/file')
    expect(pdfLink).toHaveAttribute('target', '_blank')
    expect(pdfLink).toHaveAttribute('rel', 'noopener noreferrer')
    expect(countOf(log, '/api/workspaces/42/sources/10001/file')).toBe(0)
    expect(document.title).toBe(`${GUIDE_NAME} · Raşit Bankası Dijital Asistan Yönetimi`)
  })

  it('givenSourceNeverSynced_whenDetailLoads_thenSyncedTextIsDash', async () => {
    serveDetail({ source: failedSource() })

    renderAppAt('/kaynaklar/10002')

    expect(await screen.findByText('atlaskap.pdf · 1.000 bayt · sürüm 1.0 · son senkron –')).toBeInTheDocument()
  })

  it('givenDetailRoute_whenRendered_thenSourcesNavItemIsCurrent', async () => {
    serveDetail()

    renderAppAt('/kaynaklar/10001')

    await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })
    const menu = screen.getByRole('navigation', { name: 'Menü' })
    expect(within(menu).getByRole('link', { name: 'Bilgi kaynakları' })).toHaveAttribute('aria-current', 'page')
  })

  it('givenReadySource_whenDetailLoads_thenMetaBoxesFromBackend', async () => {
    serveDetail()

    renderAppAt('/kaynaklar/10001')

    await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })
    await waitFor(() => expect(metaValue('Embedding')).toHaveTextContent('bge-m3 · 1024'))
    expect(metaValue('Sayfa / bölüm')).toHaveTextContent('70 / 11')
    expect(metaValue('Parça')).toHaveTextContent('154 · ort. 285 token')
    expect(metaValue('Parça boyutu')).toHaveTextContent('1.600-2.000 karakter')
    expect(metaValue('Örtüşme')).toHaveTextContent('200 karakter')
    expect(metaValue('Index')).toHaveTextContent('HNSW · cosine')
  })

  it('givenDifferentWorkspaceSettings_whenDetailLoads_thenMetaBoxesFollowBackendNotConstants', async () => {
    server.use(
      http.get('/api/workspaces/42', () =>
        HttpResponse.json(
          workspace({
            targetChunkChars: 1000,
            maxChunkChars: 1400,
            overlapChars: 100,
            embeddingModel: 'nomic-embed',
            embeddingDimensions: 768,
            vectorIndexType: 'IVFFLAT',
            distanceMetric: 'EUCLIDEAN',
          }),
        ),
      ),
    )
    serveDetail({ source: source({ pageCount: 12, chapterCount: 3, chunkCount: 1500, averageTokenCount: 301 }) })

    renderAppAt('/kaynaklar/10001')

    await waitFor(() => expect(metaValue('Embedding')).toHaveTextContent('nomic-embed · 768'))
    expect(metaValue('Sayfa / bölüm')).toHaveTextContent('12 / 3')
    expect(metaValue('Parça')).toHaveTextContent('1.500 · ort. 301 token')
    expect(metaValue('Parça boyutu')).toHaveTextContent('1.000-1.400 karakter')
    expect(metaValue('Örtüşme')).toHaveTextContent('100 karakter')
    expect(metaValue('Index')).toHaveTextContent('IVFFLAT · euclidean')
  })

  it('givenSourceWithoutVersion_whenDetailLoads_thenFirstTwoBoxesDashAndChunkTableEmpty', async () => {
    serveDetail({ source: failedSource(), totalChunks: 0 })

    renderAppAt('/kaynaklar/10002')

    await screen.findByRole('heading', { level: 1, name: 'Atlaskap El Kitabı' })
    expect(metaValue('Sayfa / bölüm')).toHaveTextContent('–')
    expect(metaValue('Parça')).toHaveTextContent('–')
    expect(await screen.findByText('Eşleşen parça yok.')).toBeInTheDocument()
    expect(await screen.findByText('0 / 0')).toBeInTheDocument()
  })

  it('givenFailedAndDoneJobs_whenDetailLoads_thenNewestFirstWithDuration', async () => {
    const done = job({
      jobId: 2,
      stage: 'DONE',
      totalChunks: 154,
      startedAt: '2026-10-29T13:15:35.000Z',
      finishedAt: '2026-10-29T13:15:43.440Z',
    })
    const failed = job({
      jobId: 1,
      stage: 'FAILED',
      failedStage: 'EMBEDDING',
      errorCode: 'EMBEDDING_UNAVAILABLE',
      totalChunks: null,
      startedAt: null,
      finishedAt: null,
    })
    serveDetail({ source: source({ latestJob: done }), jobs: [done, failed] })

    renderAppAt('/kaynaklar/10001')

    await screen.findByRole('heading', { level: 2, name: 'İçe alma işleri' })
    await waitFor(() => expect(bodyRows(tableWithHeader('İş'))).toHaveLength(2))
    const [first, second] = bodyRows(tableWithHeader('İş'))
    expect(screen.getAllByRole('columnheader').slice(0, 6).map((header) => header.textContent)).toEqual([
      'İş',
      'Dosya',
      'Sürüm',
      'Parça',
      'Süre',
      'Durum',
    ])
    expect(cellTexts(first as HTMLElement)).toEqual(['#2', GUIDE_FILE, '2026.10', '154', '8,44 sn', 'DONE'])
    expect(cellTexts(second as HTMLElement)).toEqual(['#1', GUIDE_FILE, '2026.10', '–', '–', 'FAILED'])
  })

  it('givenThirteenPages_whenNextClicked_thenRequestsPage1Size12', async () => {
    const log = recordRequests()
    serveDetail()
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')

    await screen.findByText('1 / 13')

    const chunksTable = tableWithHeader('Sayfa · başlık')
    expect(bodyRows(chunksTable)).toHaveLength(12)
    expect(screen.getByText('154 / 154')).toBeInTheDocument()
    const pager = screen.getByRole('navigation', { name: 'Parça sayfaları' })
    expect(within(pager).getByRole('button', { name: 'Önceki' })).toBeDisabled()
    const firstRequest = log.urls.find((url) => url.pathname.endsWith('/chunks'))
    expect(firstRequest?.search).toBe('?page=0&size=12')

    await actor.click(within(pager).getByRole('button', { name: 'Sonraki' }))

    expect(await screen.findByText('2 / 13')).toBeInTheDocument()
    expect(log.urls.map((url) => url.search)).toContain('?page=1&size=12')
    await waitFor(() => expect(cellTexts(bodyRows(tableWithHeader('Sayfa · başlık'))[0] as HTMLElement)[0]).toBe('12'))
    expect(within(pager).getByRole('button', { name: 'Önceki' })).toBeEnabled()
  })

  it('givenLastPage_whenReached_thenNextDisabledAndPartialPageShown', async () => {
    serveDetail()
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    await screen.findByText('1 / 13')
    const pager = screen.getByRole('navigation', { name: 'Parça sayfaları' })

    for (let index = 0; index < 12; index += 1) {
      await actor.click(within(pager).getByRole('button', { name: 'Sonraki' }))
      await screen.findByText(`${index + 2} / 13`)
    }

    expect(within(pager).getByRole('button', { name: 'Sonraki' })).toBeDisabled()
    await waitFor(() => expect(bodyRows(tableWithHeader('Sayfa · başlık'))).toHaveLength(10))
  })

  it('givenChunkRows_whenRendered_thenFirstLineAndHeadingAreShortenedLikeLab', async () => {
    const longHeading = 'A'.repeat(60)
    const content = `satır bir\n\n  satır   iki ${'x'.repeat(100)}`
    serveDetail()
    server.use(
      http.get('/api/workspaces/42/sources/10001/chunks', () =>
        HttpResponse.json(
          pageOf([chunkSummary(4, { heading: longHeading, content, pageNumber: 19, tokenCount: 297 })], {
            size: 12,
            totalElements: 1,
          }),
        ),
      ),
    )

    renderAppAt('/kaynaklar/10001')

    await screen.findByText('s. 19')
    const row = bodyRows(tableWithHeader('Sayfa · başlık'))[0] as HTMLElement
    expect(cellTexts(row)).toEqual([
      '4',
      `s. 19${'A'.repeat(41)}…`,
      `satır bir satır iki ${'x'.repeat(67)}…`,
      '297',
    ])
  })

  it('givenFilterTyped_whenDebounced_thenRequestsTrimmedQueryFromPage0WithOneRequest', async () => {
    const log = recordRequests()
    serveDetail()
    server.use(
      http.get('/api/workspaces/42/sources/10001/chunks', ({ request }) => {
        const params = new URL(request.url).searchParams
        const page = Number(params.get('page'))
        if (params.get('query') === 'asgari ödeme') {
          return HttpResponse.json(
            pageOf([chunkSummary(1), chunkSummary(2), chunkSummary(3)], { page, size: 12, totalElements: 3 }),
          )
        }
        return HttpResponse.json(
          pageOf(Array.from({ length: 12 }, (_, index) => chunkSummary(page * 12 + index)), {
            page,
            size: 12,
            totalElements: 154,
          }),
        )
      }),
    )
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    await screen.findByText('1 / 13')
    await actor.click(screen.getByRole('button', { name: 'Sonraki' }))
    await screen.findByText('2 / 13')

    await actor.type(screen.getByRole('searchbox', { name: 'Parçalarda ara' }), '  asgari ödeme ')

    expect(await screen.findByText('3 / 154')).toBeInTheDocument()
    const withQuery = log.urls.filter((url) => url.searchParams.has('query'))
    expect(withQuery.map((url) => url.search)).toEqual(['?query=asgari+%C3%B6deme&page=0&size=12'])
    expect(screen.getByText('1 / 1')).toBeInTheDocument()
  })

  it('givenFilterCleared_whenDebounced_thenQueryParamIsNotSentAgain', async () => {
    const log = recordRequests()
    serveDetail()
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    await screen.findByText('1 / 13')
    const box = screen.getByRole('searchbox', { name: 'Parçalarda ara' })
    await actor.type(box, 'abc')
    await waitFor(() => expect(log.urls.some((url) => url.searchParams.get('query') === 'abc')).toBe(true))
    const before = log.urls.length

    await actor.clear(box)

    await waitFor(() => expect(log.urls.length).toBeGreaterThan(before))
    expect(log.urls.at(-1)?.searchParams.has('query')).toBe(false)
  })

  it('givenNoMatch_whenFiltered_thenShowsEmptyRowAndOnePage', async () => {
    serveDetail()
    server.use(
      http.get('/api/workspaces/42/sources/10001/chunks', ({ request }) => {
        const query = new URL(request.url).searchParams.get('query')
        return HttpResponse.json(
          query === null
            ? pageOf([chunkSummary(0)], { size: 12, totalElements: 154 })
            : pageOf([], { size: 12, totalElements: 0 }),
        )
      }),
    )
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    await screen.findByText('154 / 154')

    await actor.type(screen.getByRole('searchbox', { name: 'Parçalarda ara' }), 'olmayan ifade')

    expect(await screen.findByText('Eşleşen parça yok.')).toBeInTheDocument()
    expect(screen.getByText('0 / 154')).toBeInTheDocument()
    const pager = screen.getByRole('navigation', { name: 'Parça sayfaları' })
    expect(within(pager).getByText('1 / 1')).toBeInTheDocument()
    expect(within(pager).getByRole('button', { name: 'Önceki' })).toBeDisabled()
    expect(within(pager).getByRole('button', { name: 'Sonraki' })).toBeDisabled()
  })

  it('givenFilterBox_whenRendered_thenLabPlaceholderAndMaxLength200', async () => {
    serveDetail()

    renderAppAt('/kaynaklar/10001')

    const box = await screen.findByRole('searchbox', { name: 'Parçalarda ara' })
    expect(box).toHaveAttribute('placeholder', 'Parçalarda ara (başlık ya da metin)')
    expect(box).toHaveAttribute('maxLength', '200')
  })

  it('givenChunkRow_whenClickedAndEscPressed_thenDialogShowsChunkAndFocusReturns', async () => {
    serveDetail()
    server.use(http.get('/api/workspaces/42/chunks/101', () => HttpResponse.json(chunkDetail({ chunkId: 101 }))))
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    const rowButton = await screen.findByRole('button', { name: /^s\. 4Başlık 1$/ })

    await actor.click(rowButton)

    const dialog = await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })
    expect(dialog).toHaveTextContent('chunk_id 101')
    await actor.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(rowButton).toHaveFocus()
  })

  it('givenChunkRow_whenEmptyCellClicked_thenDialogOpensAndFocusReturnsToRowButton', async () => {
    serveDetail()
    server.use(http.get('/api/workspaces/42/chunks/101', () => HttpResponse.json(chunkDetail({ chunkId: 101 }))))
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    const rowButton = await screen.findByRole('button', { name: /^s\. 4Başlık 1$/ })

    await actor.click(within(rowButton.closest('tr') as HTMLElement).getByText('201'))

    await screen.findByRole('dialog')
    await actor.click(screen.getAllByRole('button', { name: 'Kapat' })[1] as HTMLElement)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(rowButton).toHaveFocus()
  })

  it('givenHtmlInChunkHeadingAndText_whenRendered_thenShownAsTextWithoutElements', async () => {
    const html = '<img src=x onerror=alert(1)>'
    serveDetail()
    server.use(
      http.get('/api/workspaces/42/sources/10001/chunks', () =>
        HttpResponse.json(pageOf([chunkSummary(1, { heading: html, content: html, chunkId: 77 })], { size: 12 })),
      ),
      http.get('/api/workspaces/42/chunks/77', () =>
        HttpResponse.json(chunkDetail({ chunkId: 77, heading: html, content: html })),
      ),
    )
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')

    await actor.click(await screen.findByRole('button', { name: `s. 4${html}` }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('region', { name: 'Parça metni' })).toHaveTextContent(html)
    expect(document.querySelector('img')).toBeNull()
  })

  it('givenUnknownSource_whenDetailLoads_thenNotFoundMessage', async () => {
    server.use(http.get('/api/workspaces/42/sources/99999', () => problem(404, 'SOURCE_NOT_FOUND')))

    renderAppAt('/kaynaklar/99999')

    expect(await screen.findByRole('heading', { level: 1, name: 'Kaynak bulunamadı.' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Bilgi kaynaklarına dön' })).toHaveAttribute('href', '/kaynaklar')
    expect(screen.queryByRole('link', { name: "PDF'i aç" })).not.toBeInTheDocument()
    expect(document.title).toBe('Kaynak bulunamadı · Raşit Bankası Dijital Asistan Yönetimi')
  })

  it.each(['abc', '12abc', '-1', '1.5', '１２'])(
    'givenNonNumericId_whenDetailLoads_thenNotFoundWithoutRequest %s',
    async (id) => {
      const log = recordRequests()

      renderAppAt(`/kaynaklar/${id}`)

      expect(await screen.findByRole('heading', { level: 1, name: 'Kaynak bulunamadı.' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Bilgi kaynaklarına dön' })).toBeInTheDocument()
      await sleep(100)
      expect(log.urls.filter((url) => url.pathname.startsWith('/api/workspaces'))).toHaveLength(0)
    },
  )

  it('givenNotFoundLinkClicked_whenFollowed_thenBackOnSourcesList', async () => {
    server.use(http.get('/api/workspaces/42/sources/99999', () => problem(404, 'SOURCE_NOT_FOUND')))
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/99999')

    await actor.click(await screen.findByRole('link', { name: 'Bilgi kaynaklarına dön' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeInTheDocument()
  })

  it('givenServerError_whenDetailLoads_thenFallbackAlert', async () => {
    server.use(http.get('/api/workspaces/42/sources/10001', () => problem(500, 'INTERNAL_ERROR')))

    renderAppAt('/kaynaklar/10001')

    expect(await screen.findByRole('alert', undefined, { timeout: 4000 })).toHaveTextContent(
      'Beklenmeyen bir hata oluştu. Tekrar dene.',
    )
  })

  it('givenJobsEndpointFails_whenDetailLoads_thenOnlyJobsSectionShowsError', async () => {
    serveDetail()
    server.use(http.get('/api/workspaces/42/sources/10001/ingestion-jobs', () => problem(404, 'SOURCE_NOT_FOUND')))

    renderAppAt('/kaynaklar/10001')

    expect(await screen.findByRole('alert')).toHaveTextContent('Kaynak bulunamadı.')
    expect(screen.getByRole('heading', { level: 1, name: GUIDE_NAME })).toBeInTheDocument()
    expect(await screen.findByText('1 / 13')).toBeInTheDocument()
  })

  it('givenRunningLatestJob_whenJobDone_thenDetailJobsAndChunksRefetched', async () => {
    const log = recordRequests()
    const running = source({
      pageCount: null,
      chapterCount: null,
      chunkCount: null,
      averageTokenCount: null,
      syncedAt: null,
      latestJob: job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null }),
    })
    const done = source({ latestJob: job({ jobId: 7 }) })
    let sourceCall = 0
    serveDetail({ source: running, jobs: [running.latestJob], totalChunks: 0 })
    server.use(
      http.get('/api/workspaces/42/sources/10001', () => {
        sourceCall += 1
        return HttpResponse.json(sourceCall === 1 ? running : done)
      }),
    )
    serveJobSequence(7, job({ jobId: 7, stage: 'EMBEDDING', finishedAt: null }), job({ jobId: 7 }))
    renderAppAt('/kaynaklar/10001')
    await screen.findByRole('heading', { level: 1, name: GUIDE_NAME })
    expect(metaValue('Sayfa / bölüm')).toHaveTextContent('–')

    await waitFor(() => expect(metaValue('Sayfa / bölüm')).toHaveTextContent('70 / 11'), { timeout: 8000 })

    await sleep(1500)
    expect(countOf(log, '/api/workspaces/42/sources/10001')).toBe(2)
    expect(countOf(log, '/api/workspaces/42/sources/10001/ingestion-jobs')).toBe(2)
    expect(countOf(log, '/api/workspaces/42/sources/10001/chunks')).toBe(2)
    expect(countOf(log, '/api/workspaces/42/ingestion-jobs/7')).toBe(2)
  }, 20000)

  it('givenDetailPage_whenRendered_thenHasNoAxeViolations', async () => {
    serveDetail()
    const { container } = renderAppAt('/kaynaklar/10001')
    await screen.findByText('1 / 13')
    await waitFor(() => expect(metaValue('Embedding')).toHaveTextContent('bge-m3 · 1024'))

    expect(await axe(container)).toHaveNoViolations()
  })

  it('givenChunkDialogOpen_whenRendered_thenHasNoAxeViolations', async () => {
    serveDetail()
    server.use(http.get('/api/workspaces/42/chunks/101', () => HttpResponse.json(chunkDetail({ chunkId: 101 }))))
    const actor = userEvent.setup()
    renderAppAt('/kaynaklar/10001')
    await actor.click(await screen.findByRole('button', { name: /^s\. 4Başlık 1$/ }))
    await screen.findByRole('dialog', { name: '2.16 Kayıp ya da çalıntı bildirimi' })

    // The lab's dialog title is an h3 under the page h1 by plan; heading-order is reported separately in the test report.
    expect(await axe(document.body, { rules: { 'heading-order': { enabled: false } } })).toHaveNoViolations()
  })
})
