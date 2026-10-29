import { expect, test } from '@playwright/test'

const PROBLEM_JSON = /application\/problem\+json/

test('givenRunningSystem_whenRootOpened_thenShowsPreviewScreen', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/onizleme$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Asistan önizleme' })).toBeVisible()
})

test('givenSourcesUrl_whenOpenedDirectlyAndReloaded_thenStaysOnSourcesScreen', async ({ page }) => {
  const response = await page.goto('/kaynaklar')
  expect(response?.status()).toBe(200)
  await expect(page.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeVisible()

  const reloaded = await page.reload()

  expect(reloaded?.status()).toBe(200)
  await expect(page).toHaveURL(/\/kaynaklar$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Bilgi kaynakları' })).toBeVisible()
})

test('givenUnknownAssistantPath_whenRequestedThroughWeb_thenReturns404NotFoundProblem', async ({ request }) => {
  const response = await request.get('/api/assistant/yok')

  expect(response.status()).toBe(404)
  expect(response.headers()['content-type']).toMatch(PROBLEM_JSON)
  expect((await response.json()).errorCode).toBe('NOT_FOUND')
})

test('givenUnknownWorkspacesPath_whenRequestedThroughWeb_thenReturns404NotFoundProblem', async ({ request }) => {
  const response = await request.get('/api/workspaces/42/yok')

  expect(response.status()).toBe(404)
  expect(response.headers()['content-type']).toMatch(PROBLEM_JSON)
  const body = await response.json()
  expect(body.errorCode).toBe('NOT_FOUND')
  expect(body.instance).toBe('/api/workspaces/42/yok')
})

test('givenApiPathOutsideBothPrefixes_whenRequested_thenReturns404NotFoundProblemNotHtml', async ({ request }) => {
  const response = await request.get('/api/yok')

  expect(response.status()).toBe(404)
  expect(response.headers()['content-type']).toMatch(PROBLEM_JSON)
  expect(await response.text()).not.toContain('<html')
  expect((await response.json()).errorCode).toBe('NOT_FOUND')
})

test('givenNarrowViewport_whenShellRendered_thenBadgeHiddenAndNoHorizontalPageOverflow', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 740 })
  await page.goto('/onizleme')

  await expect(page.getByText('Canlı yapılandırma')).toBeHidden()
  const scrollWidth = await page.evaluate(() => document.documentElement.scrollWidth)
  expect(scrollWidth).toBeLessThanOrEqual(360)
  const menu = page.getByRole('navigation', { name: 'Menü' })
  for (const name of ['Önizleme', 'Konuşmalar', 'Bilgi kaynakları', 'Değerlendirme']) {
    await expect(menu.getByRole('link', { name })).toHaveCount(1)
  }
})
