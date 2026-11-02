import { expect, request as playwrightRequest, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const GUIDE_PATH = path.resolve(import.meta.dirname, '../../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf')
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:3000'
const NOT_FOUND_SENTENCE = 'Bu konuda rehberimizde bilgi bulamadım.'
const ANSWER_TIMEOUT_MS = 120_000

type SourceListItem = { latestJob: { stage: string } | null }
type JobResponse = { stage: string; statusUrl: string }

test.beforeAll(async () => {
  test.setTimeout(300_000)
  const api = await playwrightRequest.newContext({ baseURL: BASE_URL })
  try {
    const list = await api.get('/api/workspaces/42/sources?page=0&size=20')
    const sources = ((await list.json()) as { content: SourceListItem[] }).content
    if (sources.some((source) => source.latestJob?.stage === 'DONE')) {
      return
    }
    const created = await api.post('/api/workspaces/42/sources', {
      multipart: {
        file: { name: path.basename(GUIDE_PATH), mimeType: 'application/pdf', buffer: fs.readFileSync(GUIDE_PATH) },
        name: 'Raşit Bankası Ürün ve Hizmet Rehberi',
        version: '2026.10',
      },
    })
    expect(created.status()).toBe(202)
    const job = (await created.json()) as JobResponse
    await expect
      .poll(async () => ((await (await api.get(job.statusUrl)).json()) as JobResponse).stage, {
        timeout: 240_000,
        intervals: [2_000],
      })
      .toBe('DONE')
  } finally {
    await api.dispose()
  }
})

async function askQuestion(page: Page, text: string) {
  await page.goto('/onizleme')
  await expect(page.getByRole('heading', { level: 1, name: 'Asistan önizleme' })).toBeVisible()
  await page.getByRole('textbox', { name: 'Soru' }).fill(text)
  await page.getByRole('button', { name: 'Gönder' }).click()
}

async function waitUntilAnswerDone(page: Page) {
  await expect(page.getByRole('textbox', { name: 'Soru' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  await expect(page.getByRole('heading', { name: 'Son cevapta modele giden parçalar' })).toBeVisible({
    timeout: ANSWER_TIMEOUT_MS,
  })
}

test('givenGuideIngested_whenLostCardQuestionAsked_thenStreamedAnswerWithCitationOpensChunk', async ({ page }) => {
  test.setTimeout(180_000)

  await askQuestion(page, 'Kartımı kaybettim, ne yapmalıyım?')
  await waitUntilAnswerDone(page)

  const messages = page.getByRole('region', { name: 'Mesajlar' })
  await expect(messages.getByRole('button', { name: /^Kaynak \d$/ }).first()).toBeVisible()
  await expect(messages.getByRole('button', { name: /· s\. \d+$/ }).first()).toBeVisible()
  const card = page.getByRole('region', { name: 'Son cevapta modele giden parçalar' })
  await expect(card.getByRole('button', { name: /^\[\d\]/ })).toHaveCount(5)
  await expect(card.getByText('✓ atıf yapılan')).toBeVisible()

  await messages.getByRole('button', { name: /^Kaynak \d$/ }).first().click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toHaveCount(0)
})

test('givenGuideIngested_whenOffTopicQuestionAsked_thenNotFoundSentenceAndNoSourceButtons', async ({ page }) => {
  test.setTimeout(180_000)

  await askQuestion(page, 'Bugün hava nasıl olacak?')
  await waitUntilAnswerDone(page)

  const messages = page.getByRole('region', { name: 'Mesajlar' })
  await expect(messages.getByText(NOT_FOUND_SENTENCE)).toBeVisible()
  await expect(messages.getByRole('button', { name: /· s\. \d+$/ })).toHaveCount(0)
})

test('givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsSearchQueryAndRewriteRow', async ({ page }) => {
  test.setTimeout(300_000)

  await askQuestion(page, 'Kartımı kaybettim, ne yapmalıyım?')
  await waitUntilAnswerDone(page)
  await page.getByRole('textbox', { name: 'Soru' }).fill('Yenisi kaç günde gelir?')
  await page.getByRole('button', { name: 'Gönder' }).click()

  await expect(page.getByRole('textbox', { name: 'Soru' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  const card = page.getByRole('region', { name: 'Son cevapta modele giden parçalar' })
  await expect(card.getByText(/· arama: "/)).toBeVisible({ timeout: ANSWER_TIMEOUT_MS })
  await expect(page.getByText('yeniden yazma', { exact: true })).toBeVisible()
})

test('givenRunningSystem_whenPreviewOpens_thenStripMatchesLiveConfig', async ({ page, request }) => {
  const response = await request.get(`${BASE_URL}/api/assistant/live-config`)
  expect(response.status()).toBe(200)
  const config = (await response.json()) as { chatModel: string; generationMode: string }

  await page.goto('/onizleme')

  const strip = page.getByRole('group', { name: 'Canlı yapılandırma' })
  await expect(strip).toBeVisible()
  await expect(strip).toContainText(`Model ${config.chatModel}`)
  await expect(strip).toContainText('Bilgi kaynakları')
  if (config.generationMode === 'LLM_ONLY') {
    await expect(strip).toContainText('Retrieval kapalı (Sadece LLM)')
  }
})
