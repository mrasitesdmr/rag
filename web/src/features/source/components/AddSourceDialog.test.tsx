import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { axe } from 'vitest-axe'
import { describe, expect, it } from 'vitest'
import { failedSource, GUIDE_NAME, job, source } from '../../../test/fixtures.ts'
import { deferred, serveSourceList, serveSourceListSequence } from '../../../test/handlers.ts'
import { renderAppAt } from '../../../test/renderWithProviders.tsx'
import { problem, server } from '../../../test/server.ts'

const MIB = 1024 * 1024
const user = () => userEvent.setup({ applyAccept: false })

function pdfFile(name = 'atlaskap.pdf', size?: number): File {
  const file = new File(['%PDF-bytes'], name, { type: 'application/pdf' })
  if (size !== undefined) {
    Object.defineProperty(file, 'size', { value: size })
  }
  return file
}

async function openDialog(): Promise<ReturnType<typeof userEvent.setup>> {
  serveSourceList([source()])
  const actor = user()
  renderAppAt('/kaynaklar')
  await actor.click(await screen.findByRole('button', { name: 'Belge ekle' }))
  await screen.findByRole('dialog', { name: 'Belge ekle' })
  return actor
}

async function fillAndSubmit(
  actor: ReturnType<typeof userEvent.setup>,
  values: { file?: File | null; name?: string; version?: string },
) {
  if (values.file) {
    await actor.upload(screen.getByLabelText('PDF dosyası'), values.file)
  }
  if (values.name !== undefined) {
    await actor.type(screen.getByLabelText('Kaynak adı'), values.name)
  }
  if (values.version !== undefined) {
    await actor.type(screen.getByLabelText('Sürüm etiketi'), values.version)
  }
  await actor.click(screen.getByRole('button', { name: 'Yükle ve işle' }))
}

function trackPosts() {
  const posts: Request[] = []
  server.events.on('request:start', ({ request }) => {
    if (request.method === 'POST') {
      posts.push(request)
    }
  })
  return posts
}

describe('AddSourceDialog', () => {
  it('givenListPage_whenAddClickedAndEscPressed_thenFocusesFileButtonThenReturnsFocus', async () => {
    const actor = await openDialog()

    expect(screen.getByRole('button', { name: 'Dosya seç' })).toHaveFocus()
    await actor.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Belge ekle' })).toHaveFocus()
  })

  it.each([
    ['Vazgeç button', async (actor: ReturnType<typeof userEvent.setup>) => actor.click(screen.getByRole('button', { name: 'Vazgeç' }))],
    ['× button', async (actor: ReturnType<typeof userEvent.setup>) => actor.click(screen.getByRole('button', { name: 'Kapat' }))],
  ])('givenOpenDialog_whenClosedWith_thenDialogGoneAndFocusReturns %s', async (_label, close) => {
    const actor = await openDialog()

    await close(actor)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Belge ekle' })).toHaveFocus()
  })

  it('givenOpenDialog_whenRendered_thenShowsLabTexts', async () => {
    await openDialog()
    const dialog = screen.getByRole('dialog', { name: 'Belge ekle' })

    expect(
      within(dialog).getByText(
        "Asistanın cevap verirken arayacağı PDF'i seç. Belgenin yer imleri (başlıkları) olmalı; parçalar bu başlıklarla adlandırılır. En fazla 20 MB.",
      ),
    ).toBeInTheDocument()
    expect(within(dialog).getByText('Dosya seçilmedi')).toBeInTheDocument()
    expect(within(dialog).getByText('PDF · en fazla 20 MB')).toBeInTheDocument()
    expect(within(dialog).getByText('Boş bırakırsan dosya adı kullanılır.')).toBeInTheDocument()
    expect(within(dialog).getByLabelText('Sürüm etiketi')).toHaveAttribute('placeholder', 'ör. 2026.10')
  })

  it('givenSelectedFile_whenChosen_thenShowsNameAndSizeInBytes', async () => {
    const actor = await openDialog()

    await actor.upload(screen.getByLabelText('PDF dosyası'), pdfFile('rehber.pdf', 1120236))

    expect(screen.getByText('rehber.pdf')).toBeInTheDocument()
    expect(screen.getByText('1.120.236 bayt')).toBeInTheDocument()
  })

  it('givenNoFileAndBlankVersion_whenSubmit_thenFieldMessagesAndNoRequest', async () => {
    const posts = trackPosts()
    const actor = await openDialog()

    await actor.click(screen.getByRole('button', { name: 'Yükle ve işle' }))

    expect(screen.getByText('Bir PDF dosyası seç.')).toBeInTheDocument()
    const version = screen.getByLabelText('Sürüm etiketi')
    expect(version).toHaveAttribute('aria-invalid', 'true')
    expect(version).toHaveAccessibleDescription('Sürüm etiketi zorunlu; en fazla 40 karakter.')
    expect(screen.getByLabelText('PDF dosyası')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('button', { name: 'Dosya seç' })).toHaveFocus()
    expect(posts).toHaveLength(0)
  })

  it('givenFixedField_whenTyping_thenItsErrorDisappears', async () => {
    const actor = await openDialog()
    await actor.click(screen.getByRole('button', { name: 'Yükle ve işle' }))

    await actor.type(screen.getByLabelText('Sürüm etiketi'), '1')

    expect(screen.queryByText('Sürüm etiketi zorunlu; en fazla 40 karakter.')).not.toBeInTheDocument()
    expect(screen.getByText('Bir PDF dosyası seç.')).toBeInTheDocument()
  })

  it.each([
    ['one byte over 20 MiB', pdfFile('buyuk.pdf', 20 * MIB + 1), "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç."],
    ['txt file', pdfFile('notlar.txt'), 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    ['empty file', pdfFile('bos.pdf', 0), 'Seçtiğin dosya boş. Başka bir PDF seç.'],
  ])('givenRejectedFile_whenSubmit_thenMessageAndNoRequest %s', async (_label, file, message) => {
    const posts = trackPosts()
    const actor = await openDialog()

    await fillAndSubmit(actor, { file, version: '1' })

    expect(screen.getByText(message)).toBeInTheDocument()
    expect(posts).toHaveLength(0)
  })

  it('givenFileOfExactly20MiB_whenSubmit_thenRequestIsSent', async () => {
    const posts = trackPosts()
    server.use(http.post('/api/workspaces/42/sources', () => problem(422, 'PDF_UNREADABLE')))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile('sinir.pdf', 20 * MIB), version: '1' })

    await waitFor(() => expect(posts).toHaveLength(1))
  })

  it('givenServerRejectsNoBookmarks_whenSubmit_thenFixInstructionUnderFileAndValuesKept', async () => {
    server.use(http.post('/api/workspaces/42/sources', () => problem(422, 'PDF_NO_BOOKMARKS')))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile('yer-imi-yok.pdf'), name: 'Yer imsiz', version: '3' })

    const fixText =
      "PDF'te yer imi (başlık) yok. Word'de başlıkları \"Başlık 1\", \"Başlık 2\" stilleriyle yaz; \"Farklı kaydet → PDF → Seçenekler\"de \"Şunu kullanarak yer işaretleri oluştur: Başlıklar\"ı işaretleyip yeniden kaydet."
    expect(await screen.findByText(fixText)).toBeInTheDocument()
    expect(screen.getByLabelText('PDF dosyası')).toHaveAccessibleDescription(expect.stringContaining(fixText))
    expect(screen.getByRole('dialog', { name: 'Belge ekle' })).toBeInTheDocument()
    expect(screen.getByText('yer-imi-yok.pdf')).toBeInTheDocument()
    expect(screen.getByLabelText('Kaynak adı')).toHaveValue('Yer imsiz')
    expect(screen.getByLabelText('Sürüm etiketi')).toHaveValue('3')
    expect(screen.getByRole('button', { name: 'Yükle ve işle' })).toBeEnabled()
  })

  it.each([
    ['PDF_NO_TEXT', 'PDF\'te okunabilir metin yok; taranmış bir görüntü olabilir.'],
    ['PDF_UNREADABLE', 'PDF açılamadı; dosya bozuk ya da şifreli olabilir.'],
    ['UNSUPPORTED_FILE_TYPE', 'Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil.'],
    ['FILE_TOO_LARGE', "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç."],
  ])('givenServerFileCode_whenSubmit_thenMessageUnderFile %s', async (code, startsWith) => {
    server.use(http.post('/api/workspaces/42/sources', () => problem(code === 'FILE_TOO_LARGE' ? 413 : 422, code)))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), version: '1' })

    const message = await screen.findByText(new RegExp(`^${startsWith.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`))
    expect(screen.getByLabelText('PDF dosyası')).toHaveAccessibleDescription(
      expect.stringContaining(message.textContent ?? ''),
    )
  })

  it('givenServerRejectsNameTaken_whenSubmit_thenMessageUnderName', async () => {
    server.use(http.post('/api/workspaces/42/sources', () => problem(409, 'SOURCE_NAME_TAKEN')))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), name: GUIDE_NAME, version: '1' })

    const message = 'Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz.'
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByLabelText('Kaynak adı')).toHaveAccessibleDescription(expect.stringContaining(message))
    expect(screen.getByLabelText('Kaynak adı')).toHaveAttribute('aria-invalid', 'true')
  })

  it.each([
    ['version', 'Sürüm etiketi zorunlu; en fazla 40 karakter.', 'Sürüm etiketi'],
    ['name', 'Kaynak adı 2-80 karakter olmalı.', 'Kaynak adı'],
  ])('givenServerViolationOn%s_whenSubmit_thenRuleMessageNotBackendMessage', async (field, ruleMessage, label) => {
    server.use(
      http.post('/api/workspaces/42/sources', () =>
        problem(400, 'VALIDATION_FAILED', [{ field, message: 'backend english text' }]),
      ),
    )
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), version: '1' })

    expect(await screen.findByText(ruleMessage)).toBeInTheDocument()
    expect(screen.getByLabelText(label)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.queryByText('backend english text')).not.toBeInTheDocument()
  })

  it.each([
    ['EMBEDDING_MODEL_MISMATCH', 409, 'Sistemin embedding modeli çalışma alanınınkiyle aynı değil; belge yüklenemiyor. Sistem ayarını kontrol et.'],
    ['INTERNAL_ERROR', 500, 'Beklenmeyen bir hata oluştu. Tekrar dene.'],
  ])('givenNonFieldError_whenSubmit_thenAlertAtTopOfDialog %s', async (code, status, message) => {
    server.use(http.post('/api/workspaces/42/sources', () => problem(status, code)))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), version: '1' })

    const dialog = screen.getByRole('dialog', { name: 'Belge ekle' })
    expect(await within(dialog).findByRole('alert')).toHaveTextContent(message)
  })

  it('givenNetworkError_whenSubmit_thenNetworkMessageInDialogAndDialogStaysOpen', async () => {
    server.use(http.post('/api/workspaces/42/sources', () => HttpResponse.error()))
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), version: '1' })

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor. Bağlantını kontrol et.')
    expect(screen.getByRole('dialog', { name: 'Belge ekle' })).toBeInTheDocument()
  })

  it('givenValidUpload_whenSubmit_thenDialogClosesAndNewRowQueued', async () => {
    let form: FormData | null = null
    const queued = job({ jobId: 9, sourceId: 10003, stage: 'QUEUED', fileName: 'atlaskap.pdf', version: '1.0', startedAt: null, finishedAt: null })
    const added = source({
      sourceId: 10003,
      name: 'Atlaskap El Kitabı',
      fileName: 'atlaskap.pdf',
      version: '1.0',
      pageCount: null,
      chapterCount: null,
      chunkCount: null,
      averageTokenCount: null,
      syncedAt: null,
      latestJob: queued,
    })
    serveSourceListSequence([source()], [source(), added])
    server.use(
      http.post('/api/workspaces/42/sources', async ({ request }) => {
        form = await request.formData()
        return HttpResponse.json(queued, { status: 202 })
      }),
      http.get('/api/workspaces/42/ingestion-jobs/9', () => HttpResponse.json(queued)),
    )
    const actor = user()
    renderAppAt('/kaynaklar')
    await actor.click(await screen.findByRole('button', { name: 'Belge ekle' }))

    await fillAndSubmit(actor, { file: pdfFile('atlaskap.pdf'), name: '  Atlaskap El Kitabı ', version: ' 1.0 ' })

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    const row = await screen.findByRole('row', { name: /Atlaskap El Kitabı/ })
    expect(within(row).getByText('QUEUED')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Belge ekle' })).toHaveFocus()
    const sent = form as FormData | null
    expect(sent?.get('name')).toBe('Atlaskap El Kitabı')
    expect(sent?.get('version')).toBe('1.0')
    const sentFile = sent?.get('file')
    expect(sentFile).toBeInstanceOf(File)
    expect((sentFile as File).name).toBe('atlaskap.pdf')
    expect(await (sentFile as File).text()).toBe('%PDF-bytes')
  })

  it('givenBlankName_whenSubmit_thenNamePartIsNotSent', async () => {
    let form: FormData | null = null
    const queued = job({ jobId: 9, sourceId: 10003, stage: 'QUEUED' })
    server.use(
      http.post('/api/workspaces/42/sources', async ({ request }) => {
        form = await request.formData()
        return HttpResponse.json(queued, { status: 202 })
      }),
      http.get('/api/workspaces/42/ingestion-jobs/9', () => HttpResponse.json(queued)),
    )
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), name: '   ', version: '2' })

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    const sent = form as FormData | null
    expect(sent?.has('name')).toBe(false)
    expect(sent?.get('version')).toBe('2')
  })

  it('givenPendingUpload_whenEscPressed_thenDialogStaysAndButtonsDisabled', async () => {
    const gate = deferred()
    const queued = job({ jobId: 9, sourceId: 10003, stage: 'QUEUED' })
    server.use(
      http.post('/api/workspaces/42/sources', async () => {
        await gate.promise
        return HttpResponse.json(queued, { status: 202 })
      }),
      http.get('/api/workspaces/42/ingestion-jobs/9', () => HttpResponse.json(queued)),
    )
    const actor = await openDialog()

    await fillAndSubmit(actor, { file: pdfFile(), version: '1' })
    const submit = await screen.findByRole('button', { name: 'Yükleniyor…' })
    await actor.keyboard('{Escape}')
    await actor.click(screen.getByRole('button', { name: 'Kapat' }))

    expect(submit).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Vazgeç' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Kapat' })).toBeDisabled()
    expect(screen.getByRole('dialog', { name: 'Belge ekle' })).toBeInTheDocument()
    gate.resolve()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('givenEmptyKnowledgeBase_whenAddedFromEmptyState_thenDialogOpensAndEscClosesIt', async () => {
    const actor = user()
    renderAppAt('/kaynaklar')
    await screen.findByText('Henüz belge yok')
    const emptyStateButton = screen.getAllByRole('button', { name: 'Belge ekle' })[1] as HTMLElement

    await actor.click(emptyStateButton)

    expect(await screen.findByRole('dialog', { name: 'Belge ekle' })).toBeInTheDocument()
    await actor.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('givenOpenDialogWithError_whenRendered_thenHasNoAxeViolations', async () => {
    server.use(http.post('/api/workspaces/42/sources', () => problem(409, 'SOURCE_NAME_TAKEN')))
    const actor = await openDialog()
    await fillAndSubmit(actor, { file: pdfFile(), name: 'Aynı', version: '1' })
    await screen.findByText(/Bu adla bir kaynak zaten var/)

    // The lab's dialog title is an h3 under the page h1 by plan; heading-order is reported separately in the test report.
    expect(await axe(document.body, { rules: { 'heading-order': { enabled: false } } })).toHaveNoViolations()
  })

  it('givenFailedSourceInList_whenDialogOpened_thenRetryButtonStillWorksAfterClose', async () => {
    serveSourceList([failedSource()])
    const actor = user()
    renderAppAt('/kaynaklar')
    await actor.click(await screen.findByRole('button', { name: 'Belge ekle' }))
    await actor.keyboard('{Escape}')

    expect(screen.getByRole('button', { name: 'Yeniden dene' })).toBeEnabled()
  })
})
