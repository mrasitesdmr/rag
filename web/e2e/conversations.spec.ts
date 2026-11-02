import { expect, request as playwrightRequest, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'

const GUIDE_PATH = path.resolve(import.meta.dirname, '../../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf')
const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:3000'
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
  await page.getByRole('combobox', { name: 'Soru' }).fill(text)
  await page.getByRole('button', { name: 'Gönder' }).click()
  await expect(page.getByRole('combobox', { name: 'Soru' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  await expect(page.getByRole('heading', { name: 'Son cevapta modele giden parçalar' })).toBeVisible({
    timeout: ANSWER_TIMEOUT_MS,
  })
}

test('givenAnsweredQuestion_whenConversationsOpened_thenTopRowAndDetailTable', async ({ page }) => {
  test.setTimeout(180_000)
  const question = 'Kartımı kaybettim, ne yapmalıyım?'
  await askQuestion(page, question)

  await page.getByRole('link', { name: /^Konuşmalar/ }).click()

  await expect(page.getByRole('heading', { level: 1, name: 'Konuşmalar' })).toBeVisible()
  const topRow = page.getByRole('row').nth(1)
  await expect(topRow).toContainText(question)
  await expect(topRow).toContainText('Önizleme')
  await expect(topRow.getByRole('cell').nth(2)).toHaveText('1')
  await expect(page.getByRole('link', { name: /^Konuşmalar\s*\d+$/ })).toBeVisible()

  await topRow.getByRole('link', { name: /^cnv_\d+$/ }).click()

  await expect(page.getByRole('heading', { level: 1, name: /^cnv_\d+$/ })).toBeVisible()
  await expect(page.getByText(`1. ${question}`)).toBeVisible()
  await expect(page.getByRole('columnheader', { name: 'Vector #' })).toBeVisible()
  expect(await page.getByRole('row').count()).toBeGreaterThanOrEqual(2)
  await expect(page.getByText(/^(DAYANAKLI|DAYANAKSIZ|BİLMİYOR|HATALI|TAMAMLANMADI)$/)).toBeVisible()
})

test('givenUnknownConversation_whenOpened_thenNotFoundMessage', async ({ page }) => {
  await page.goto('/konusmalar/999999999')

  await expect(page.getByRole('heading', { level: 1, name: 'Konuşma bulunamadı.' })).toBeVisible()
  await expect(page.getByRole('link', { name: "Konuşmalar'a dön" })).toBeVisible()
})
