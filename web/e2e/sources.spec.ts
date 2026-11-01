import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import path from 'node:path'

const GUIDE_PATH = path.resolve(import.meta.dirname, '../../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf')
const NO_BOOKMARKS_PATH = path.resolve(import.meta.dirname, 'fixtures/yer-imi-yok.pdf')
const UPLOAD_NAME = `E2E Rehber ${Date.now()}`
const NAME_TAKEN_TEXT = 'Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz.'

async function openSources(page: Page) {
  await page.goto('/kaynaklar')
  await expect(page.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeVisible()
}

async function upload(page: Page, filePath: string, name: string, version: string) {
  await page.getByRole('button', { name: 'Belge ekle' }).first().click()
  const dialog = page.getByRole('dialog', { name: 'Belge ekle' })
  await dialog.getByLabel('PDF dosyası').setInputFiles(filePath)
  await dialog.getByLabel('Kaynak adı').fill(name)
  await dialog.getByLabel('Sürüm etiketi').fill(version)
  await dialog.getByRole('button', { name: 'Yükle ve işle' }).click()
  return dialog
}

test('givenEmptyKnowledgeBase_whenSourcesOpened_thenShowsEmptyState', async ({ page }) => {
  await openSources(page)
  const emptyState = page.getByRole('heading', { level: 2, name: 'Henüz belge yok' })
  test.skip(!(await emptyState.isVisible({ timeout: 3000 }).catch(() => false)), 'knowledge base is not empty')

  await expect(emptyState).toBeVisible()
  await expect(page.getByRole('button', { name: 'Belge ekle' })).toHaveCount(2)
})

test.describe.serial('upload flow', () => {
  test('givenGuidePdf_whenUploadedViaDialog_thenReadyAndDetailFilterPagingAndChunkDialogWork', async ({ page }) => {
    test.setTimeout(240_000)
    await openSources(page)

    await upload(page, GUIDE_PATH, UPLOAD_NAME, '2026.10')

    await expect(page.getByRole('dialog')).toHaveCount(0)
    const row = page.getByRole('row', { name: new RegExp(UPLOAD_NAME) })
    await expect(row).toBeVisible()
    await expect(row.getByText('Hazır')).toBeVisible({ timeout: 120_000 })
    await expect(row).toContainText('PDF · 70 sayfa')
    await expect(row).toContainText('2026.10')

    await row.getByRole('link', { name: UPLOAD_NAME }).click()
    await expect(page.getByRole('heading', { level: 1, name: UPLOAD_NAME })).toBeVisible()
    const jobsTable = page.getByRole('table').first()
    await expect(jobsTable.getByRole('row').nth(1)).toContainText('DONE')
    await expect(page.getByText('154 / 154')).toBeVisible()

    await page.getByRole('button', { name: 'Sonraki' }).click()
    await expect(page.getByText('2 / 13')).toBeVisible()
    const chunkRows = page.getByRole('table').nth(1).getByRole('row')
    await expect(chunkRows.nth(1).getByRole('cell').first()).toHaveText('12')

    await page.getByRole('searchbox', { name: 'Parçalarda ara' }).fill('asgari ödeme')
    const filtered = page.getByRole('table').nth(1).locator('tbody tr')
    await expect(filtered.first()).toBeVisible()
    await expect(page.getByText(/^\d+ \/ 154$/).filter({ hasNotText: '154 / 154' })).toBeVisible()

    const firstButton = filtered.first().getByRole('button')
    await firstButton.click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    const heading = (await dialog.getByRole('heading', { level: 3 }).innerText()).toLocaleLowerCase('tr')
    const body = (await dialog.getByRole('region', { name: 'Parça metni' }).innerText()).toLocaleLowerCase('tr')
    expect(`${heading} ${body}`.replace(/\s+/g, ' ')).toContain('asgari ödeme')
    await expect(dialog.getByText(/^embedding \[/)).toContainText('değer daha')
    await page.keyboard.press('Escape')
    await expect(dialog).toHaveCount(0)
    await expect(firstButton).toBeFocused()
  })

  test('givenExistingName_whenUploadedAgain_thenNameTakenMessage', async ({ page }) => {
    await openSources(page)

    const dialog = await upload(page, GUIDE_PATH, UPLOAD_NAME, '2026.11')

    await expect(dialog.getByText(NAME_TAKEN_TEXT)).toBeVisible()
    await expect(dialog).toBeVisible()
  })

  test('givenReadySource_whenPdfOpened_thenNewTabReceivesPdf', async ({ page, context }) => {
    await openSources(page)
    await page.getByRole('row', { name: new RegExp(UPLOAD_NAME) }).getByRole('link', { name: UPLOAD_NAME }).click()
    const link = page.getByRole('link', { name: "PDF'i aç" })
    const href = await link.getAttribute('href')

    const [popup] = await Promise.all([context.waitForEvent('page'), link.click()])
    const response = await page.request.get(href ?? '')

    expect(popup).toBeTruthy()
    await expect(link).toHaveAttribute('target', '_blank')
    expect(response.status()).toBe(200)
    expect(response.headers()['content-type']).toContain('application/pdf')
    expect(response.headers()['x-content-type-options']).toContain('nosniff')
  })
})

test('givenPdfWithoutBookmarks_whenUploaded_thenFixMessageAndNoNewRow', async ({ page }) => {
  await openSources(page)
  await expect(page.getByRole('table').or(page.getByRole('heading', { level: 2, name: 'Henüz belge yok' }))).toBeVisible()
  const before = await page.getByRole('row').count()

  const dialog = await upload(page, NO_BOOKMARKS_PATH, `Yer imsiz ${Date.now()}`, '1')

  await expect(dialog.getByText(/PDF'te yer imi \(başlık\) yok/)).toBeVisible()
  await dialog.getByRole('button', { name: 'Vazgeç' }).click()
  await expect(page.getByRole('row')).toHaveCount(before)
})
