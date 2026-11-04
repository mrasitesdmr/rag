import { chromium, expect, request as playwrightRequest, test } from '@playwright/test'
import type { Browser, Page } from '@playwright/test'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:3000'
const GUIDE_PATH = path.resolve(import.meta.dirname, '../../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf')
const GOLDEN_PATH = path.resolve(import.meta.dirname, '../../docs/rehber/golden/rehber-golden-v1.json')
const LAB_HTML_URL = `file://${path.resolve(import.meta.dirname, '../../kaynaklar/rag.html')}`
const NO_BOOKMARKS_PATH = path.resolve(import.meta.dirname, 'fixtures/yer-imi-yok.pdf')

const LOST_CARD_QUESTION = 'Kartımı kaybettim, ne yapmalıyım?'
const FOLLOW_UP_QUESTION = 'Yenisi kaç günde gelir?'
const OFF_TOPIC_QUESTION = 'Bitcoin alıp satabilir miyim?'
const NOT_FOUND_SENTENCE = 'Bu konuda rehberimizde bilgi bulamadım.'

const GUIDE_NAME = 'Raşit Bankası Ürün ve Hizmet Rehberi'
const NO_BOOKMARKS_MESSAGE_START = "PDF'te yer imi (başlık) yok."

const ANSWER_TIMEOUT_MS = 120_000
const INGESTION_TIMEOUT_MS = 240_000
const EVAL_TIMEOUT_MS = 15 * 60_000

type SourcePage = { content: unknown[] }

test.skip(process.env.E2E_ACCEPTANCE !== '1', 'acceptance run needs E2E_ACCEPTANCE=1 and an empty knowledge base')

test.describe.configure({ mode: 'serial' })
test.setTimeout(20 * 60_000)

let browser: Browser
let page: Page
let bookmarkedPdfPath: string

test.beforeAll(async () => {
  const api = await playwrightRequest.newContext({ baseURL: BASE_URL })
  try {
    const response = await api.get('/api/workspaces/42/sources?page=0&size=20')
    const sources = (await response.json()) as SourcePage
    if (sources.content.length > 0) {
      throw new Error('acceptance needs an empty knowledge base')
    }
  } finally {
    await api.dispose()
  }

  browser = await chromium.launch()
  page = await browser.newPage({ baseURL: BASE_URL })

  const labBrowser = await chromium.launch()
  try {
    const labPage = await labBrowser.newPage()
    await labPage.goto(LAB_HTML_URL)
    bookmarkedPdfPath = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'rag-acceptance-')), 'rag-anlatim.pdf')
    await labPage.pdf({ path: bookmarkedPdfPath, outline: true, tagged: true })
  } finally {
    await labBrowser.close()
  }
})

test.afterAll(async () => {
  await page?.close()
  await browser?.close()
})

async function openUploadDialog(filePath: string, name: string, version: string) {
  await page.getByRole('button', { name: 'Belge ekle' }).first().click()
  const dialog = page.getByRole('dialog', { name: 'Belge ekle' })
  await dialog.getByLabel('PDF dosyası').setInputFiles(filePath)
  await dialog.getByLabel('Kaynak adı').fill(name)
  await dialog.getByLabel('Sürüm etiketi').fill(version)
  await dialog.getByRole('button', { name: 'Yükle ve işle' }).click()
  return dialog
}

async function openNewPreview() {
  await page.goto('/onizleme')
  await expect(page.getByRole('heading', { level: 1, name: 'Asistan önizleme' })).toBeVisible()
}

async function sendQuestion(text: string) {
  await page.getByRole('combobox', { name: 'Soru' }).fill(text)
  await page.getByRole('button', { name: 'Gönder' }).click()
}

async function waitUntilAnswerDone() {
  await expect(page.getByRole('combobox', { name: 'Soru' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  await expect(page.getByRole('heading', { name: 'Son cevapta modele giden parçalar' })).toBeVisible({
    timeout: ANSWER_TIMEOUT_MS,
  })
}

test('givenEmptySystem_whenScreensOpened_thenEmptyStatesShown', async () => {
  await page.goto('/kaynaklar')
  await expect(page.getByRole('heading', { level: 2, name: 'Henüz belge yok' })).toBeVisible()

  await page.goto('/konusmalar')
  await expect(page.getByRole('heading', { level: 2, name: 'Henüz konuşma yok' })).toBeVisible()

  await page.goto('/degerlendirme')
  await expect(page.getByText('Golden set yüklenmemiş.', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: /^Ölçümü çalıştır/ })).toBeDisabled()
})

test('givenGuidePdf_whenUploadedViaDialogAndPageReloaded_thenProgressContinuesAndSourceReady', async () => {
  await page.goto('/kaynaklar')
  await expect(page.getByRole('heading', { level: 2, name: 'Henüz belge yok' })).toBeVisible()

  await openUploadDialog(GUIDE_PATH, GUIDE_NAME, '2026.10')
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await page.reload()

  const row = page.getByRole('row', { name: new RegExp(GUIDE_NAME) })
  await expect(row).toBeVisible()
  await expect(
    row.getByText(/^(QUEUED|PARSING|CHUNKING|EMBEDDING|WRITING)$/),
    'ingestion finished before the reload; the progress-after-reload check needs a running job',
  ).toBeVisible()
  await expect(row.getByText('Hazır')).toBeVisible({ timeout: INGESTION_TIMEOUT_MS })
  await expect(row.getByRole('cell').nth(3)).toHaveText('154')
})

test('givenBookmarkedPdf_whenUploadedViaDialog_thenSourceReady', async () => {
  await page.goto('/kaynaklar')
  await expect(page.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeVisible()

  await openUploadDialog(bookmarkedPdfPath, 'RAG anlatımı', '1')

  await expect(page.getByRole('dialog')).toHaveCount(0)
  const row = page.getByRole('row', { name: /RAG anlatımı/ })
  await expect(row.getByText('Hazır')).toBeVisible({ timeout: INGESTION_TIMEOUT_MS })
  const chunkCount = Number(await row.getByRole('cell').nth(3).innerText())
  expect(chunkCount).toBeGreaterThan(0)
  await expect(page.getByRole('row')).toHaveCount(3)
})

test('givenPdfWithoutBookmarks_whenUploaded_thenRejectedAndNoNewRow', async () => {
  await page.goto('/kaynaklar')
  await expect(page.getByRole('row')).toHaveCount(3)

  const dialog = await openUploadDialog(NO_BOOKMARKS_PATH, 'Yer imsiz deneme', '1')

  await expect(dialog.getByText(NO_BOOKMARKS_MESSAGE_START)).toBeVisible()
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: 'Vazgeç' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.getByRole('row')).toHaveCount(3)
})

test('givenGuideReady_whenQuestionAsked_thenAnswerStreamsAndCitationOpensChunkDialog', async () => {
  await openNewPreview()
  const messages = page.getByRole('region', { name: 'Mesajlar' })
  const input = page.getByRole('combobox', { name: 'Soru' })

  await sendQuestion(LOST_CARD_QUESTION)
  await expect(input).toBeDisabled()
  const baselineLength = (await messages.innerText()).length
  await expect
    .poll(async () => (await input.isDisabled()) && (await messages.innerText()).length > baselineLength + 10, {
      timeout: ANSWER_TIMEOUT_MS,
      intervals: [100],
      message: 'answer text must appear while the question box is still disabled (streaming)',
    })
    .toBe(true)
  await waitUntilAnswerDone()

  const citation = messages.getByRole('button', { name: /· s\. \d+$/ }).first()
  await expect(citation).toBeVisible()
  await citation.click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('heading', { level: 3 })).toHaveText(/^\[\d\] s\. \d+ · .+/)
  await expect(dialog.getByRole('region', { name: 'Parça metni' })).not.toBeEmpty()
  await expect(dialog.getByText(/^chunk_no/)).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
})

test('givenOffTopicQuestion_whenAsked_thenNotFoundSentenceAndHandoffQueued', async () => {
  await openNewPreview()

  await sendQuestion(OFF_TOPIC_QUESTION)
  await waitUntilAnswerDone()

  const messages = page.getByRole('region', { name: 'Mesajlar' })
  await expect(messages.getByText(NOT_FOUND_SENTENCE, { exact: true })).toBeVisible()
  await expect(messages.getByRole('button', { name: /· s\. \d+$/ })).toHaveCount(0)
  await expect(messages.getByRole('button', { name: /^Kaynak \d+$/ })).toHaveCount(0)
  await page.getByRole('button', { name: 'Temsilciye bağlan' }).click()
  await expect(page.getByText('Temsilci kuyruğundasın')).toBeVisible()
})

test('givenGreeting_whenSent_thenFixedReplyWithoutCitation', async () => {
  await openNewPreview()

  await sendQuestion('Selam')
  await waitUntilAnswerDone()

  const messages = page.getByRole('region', { name: 'Mesajlar' })
  await expect(messages.getByText(/dijital asistanıyım/)).toBeVisible()
  await expect(messages.getByRole('button', { name: /^Kaynak \d+$/ })).toHaveCount(0)
  await expect(messages.getByRole('button', { name: /· s\. \d+$/ })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Temsilciye bağlan' })).toHaveCount(0)
})

test('givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsRewrittenSearchQuery', async () => {
  await openNewPreview()
  await sendQuestion(LOST_CARD_QUESTION)
  await waitUntilAnswerDone()

  await sendQuestion(FOLLOW_UP_QUESTION)

  await expect(page.getByRole('combobox', { name: 'Soru' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  const card = page.getByRole('region', { name: 'Son cevapta modele giden parçalar' })
  const searchLine = card.getByText(/· arama: "/)
  await expect(searchLine).toBeVisible({ timeout: ANSWER_TIMEOUT_MS })
  const searchText = await searchLine.innerText()
  const rewritten = /arama: "(.*)"/.exec(searchText)?.[1] ?? ''
  expect(rewritten.length).toBeGreaterThan(0)
  expect(rewritten).not.toBe(FOLLOW_UP_QUESTION)
  await expect(page.getByText('yeniden yazma', { exact: true })).toBeVisible()
})

test('givenAnsweredTurn_whenYesClicked_thenFeedbackSavedAndPressed', async () => {
  const group = page.getByRole('group', { name: 'Geri bildirim' }).last()
  await expect(group.getByText('Faydalı mı?')).toBeVisible()

  await group.getByRole('button', { name: 'Evet' }).click()

  await expect(group.getByRole('button', { name: 'Evet' })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.getByText('Geri bildirim kaydedildi', { exact: true })).toBeVisible()
})

test('givenThreeConversations_whenListAndDetailOpened_thenTurnsStatusChunksAndHandoffShown', async () => {
  await page.goto('/konusmalar')
  await expect(page.getByRole('heading', { level: 1, name: 'Konuşmalar' })).toBeVisible()

  const rows = page.locator('tbody tr')
  await expect(rows).toHaveCount(3)
  await expect(rows.first()).toContainText(LOST_CARD_QUESTION)
  await expect(rows.first().getByRole('cell').nth(2)).toHaveText('2')
  await expect(page.getByRole('link', { name: /^Konuşmalar\s*3$/ })).toBeVisible()

  await rows.filter({ hasText: OFF_TOPIC_QUESTION }).getByRole('link', { name: /^cnv_\d+$/ }).click()
  await expect(page.getByRole('heading', { level: 1, name: /^cnv_\d+$/ })).toBeVisible()
  await expect(page.getByText('BİLMİYOR', { exact: true })).toBeVisible()
  await expect(page.getByText(/^Temsilciye aktarıldı · /)).toBeVisible()

  await page.goto('/konusmalar')
  await rows
    .filter({ hasText: LOST_CARD_QUESTION })
    .filter({ has: page.getByRole('cell', { name: '1', exact: true }) })
    .getByRole('link', { name: /^cnv_\d+$/ })
    .click()
  await expect(page.getByText(`1. ${LOST_CARD_QUESTION}`)).toBeVisible()
  const chunkTable = page.getByRole('table').filter({ has: page.getByRole('columnheader', { name: 'Vector #' }) })
  await expect(chunkTable.locator('tbody tr')).toHaveCount(5)
})

test('givenGuideReady_whenChunksFilteredAndPaged_thenMatchingRowsAndNextPage', async () => {
  await page.goto('/kaynaklar')
  await page.getByRole('row', { name: new RegExp(GUIDE_NAME) }).getByRole('link', { name: GUIDE_NAME }).click()
  await expect(page.getByRole('heading', { level: 1, name: GUIDE_NAME })).toBeVisible()
  const chunkRows = page.getByRole('table').nth(1).locator('tbody tr')

  const filter = page.getByRole('searchbox', { name: 'Parçalarda ara' })
  await filter.fill('asgari ödeme')
  await expect(page.getByText(/^\d+ \/ 154$/).filter({ hasNotText: '154 / 154' })).toBeVisible()
  const filteredCount = await chunkRows.count()
  expect(filteredCount).toBeGreaterThan(0)
  for (let index = 0; index < filteredCount; index += 1) {
    const rowText = (await chunkRows.nth(index).innerText()).replace(/\s+/g, ' ').toLocaleLowerCase('tr')
    expect(rowText.includes('asgari ödeme') || (await openedChunkContains(index, 'asgari ödeme'))).toBe(true)
  }

  await filter.fill('')
  await expect(page.getByText('154 / 154')).toBeVisible()
  const firstNumberOnPageOne = await chunkRows.first().getByRole('cell').first().innerText()
  await page.getByRole('button', { name: 'Sonraki' }).click()
  await expect(page.getByText(/^2 \/ \d+$/)).toBeVisible()
  await expect(chunkRows.first().getByRole('cell').first()).not.toHaveText(firstNumberOnPageOne)
})

async function openedChunkContains(rowIndex: number, needle: string): Promise<boolean> {
  const row = page.getByRole('table').nth(1).locator('tbody tr').nth(rowIndex)
  await row.getByRole('button').click()
  const dialog = page.getByRole('dialog')
  const heading = await dialog.getByRole('heading', { level: 3 }).innerText()
  const body = await dialog.getByRole('region', { name: 'Parça metni' }).innerText()
  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
  return `${heading} ${body}`.replace(/\s+/g, ' ').toLocaleLowerCase('tr').includes(needle)
}

test('givenGoldenSetLoaded_whenEvalRunStarted_thenProgressThenKpisAndRunRow', async () => {
  const api = await playwrightRequest.newContext({ baseURL: BASE_URL })
  try {
    const response = await api.put('/api/assistant/golden-set', {
      data: JSON.parse(fs.readFileSync(GOLDEN_PATH, 'utf8')) as unknown,
    })
    expect(response.status()).toBe(200)
  } finally {
    await api.dispose()
  }

  await page.goto('/degerlendirme')
  const runButton = page.getByRole('button', { name: /^Ölçümü çalıştır · (rerank 20→5|top-5)$/ })
  await expect(runButton).toBeEnabled()
  await runButton.click()

  await expect(page.getByRole('button', { name: /^Ölçülüyor · \d+\/100$/ })).toBeVisible()
  await expect(page.getByText(/^0,\d\d$/).first()).toBeVisible({ timeout: EVAL_TIMEOUT_MS })
  await expect(page.getByText(/^\d+\/100 soru/)).toBeVisible()
  const runsTable = page.getByRole('table').first()
  await expect(runsTable.getByRole('cell', { name: /^ev_\d+$/ })).toHaveCount(1)
})
