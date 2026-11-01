---
name: e2e-testing
description: Playwright E2E test pattern'leri, Page Object Model, configuration, CI/CD entegrasyonu, artifact yönetimi ve flaky test stratejileri. Playwright testleri yazarken, page object'leri yapılandırırken, asistan sohbetini (akan cevap, alıntılar, geri bildirim, temsilciye aktarma), dosya yükleme ve içe alma ilerlemesini (sayfa yenilense de devam etmesi dahil) ya da değerlendirme çalıştırmasını uçtan uca test ederken veya CI'da flaky E2E koşularını düzeltirken kullan.
license: MIT
metadata:
  origin: ECC
  source: https://github.com/affaan-m/ECC/tree/main/skills/e2e-testing
---

# E2E Testing Pattern'leri

Stabil, hızlı ve bakımı kolay E2E test suite'leri kurmak için kapsamlı Playwright pattern'leri.

> **Ürün hedefi:** Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder. Sistem konsolu olmadığı için akışın adımları (yeniden yazılan sorgu, modele giden parçalar, strateji) ekranda değil tur kaydında (Konuşmalar ekranı) doğrulanır.

> **Proje kuralı:** Test adları `given<Precondition>_when<Action>_then<ExpectedResult>` (İngilizce, Playwright'ta `test(...)`), gövde `// Arrange` / `// Act` / `// Assert`, locator'lar Türkçe UI metniyle `getByRole` > `getByLabel` > `getByText` > `getByTestId` sırasıyla, sabit bekleme (`waitForTimeout`) ve `networkidle` yok, type-only import'lar `import type`, göreli import'lar uzantılı (`frontend-conventions` bölüm 2, 6, 11).

## Test dosyası organizasyonu

```
web/
├── e2e/
│   ├── preview.spec.ts          Önizleme: soru, akan cevap, alıntıdan parçaya gitme, geri bildirim, temsilciye aktarma
│   ├── conversations.spec.ts    Konuşmalar listesi ve konuşma detayı (modele giden parçalar)
│   ├── sources.spec.ts          Bilgi kaynakları, yükleme, ilerleme, geçersiz dosya
│   ├── evaluation.spec.ts       Değerlendirme çalıştırması ve sonuçları
│   ├── pages/
│   │   └── PreviewPage.ts       Page object'ler (birden fazla spec kullanınca açılır)
│   └── fixtures/                (gerekirse) örnek dosyalar
└── playwright.config.ts
```

Dosya adları lab'ın yan menüsünü izleyen hedeftir; kesin liste ilgili task'ın planındadır. Aşağıdaki yükleme/ilerleme örneklerinin çalışan karşılığı referans uygulamadadır (`../embedding-pgvector/frontend/e2e`, okunur kalıp).

## Cevap akışı (SSE) ve gerçek dil modeli

Uçtan uca test gerçek dil modeliyle koşar; aynı soru her koşuda harfi harfine aynı cevabı vermeyebilir (temperature 0 olsa bile model ya da sürüm değişebilir).

- Cevabın tam metnine assertion yapılmaz. Bunun yerine akışın bittiği (gönderme butonu yeniden etkin, "yazıyor" göstergesi gitti), en az bir alıntının görünmesi, alıntıya tıklayınca beklenen parçanın (başlık/sayfa) açılması ve sistem talimatındaki sabit cümleler ("Bu konuda rehberimizde bilgi bulamadım.") doğrulanır.
- Bekleme sabit süreyle değil, akışın bittiğini gösteren UI durumuyla yapılır; lokal model yavaş olabileceği için bu beklemenin timeout'u büyüktür ve sabit olarak (`ANSWER_TIMEOUT_MS`) tanımlanır.
- Akışın kendisini doğrulamak için `page.waitForResponse` ile `text/event-stream` cevabının `200` döndüğü kontrol edilebilir; olay içerikleri component testlerinde MSW ile doğrulanır.
- Model kapalıyken davranış (hata mesajı) ayrı bir koşuda, backend'in model adresi ulaşılamayan bir porta yönlendirilerek test edilir (`deployment/CLAUDE.md`); Ollama'yı kapatmak gerekmez.

```typescript
test('givenGuideIngested_whenAskGoldCardAnnualFee_thenAnswerFinishesWithCitationToCardFeesSection', async ({ page }) => {
  // Arrange
  await page.goto('/onizleme')

  // Act
  await page.getByLabel('Soru').fill('Gold kartın yıllık aidatı ne kadar?')
  await page.getByRole('button', { name: 'Gönder' }).click()

  // Assert
  await expect(page.getByRole('button', { name: 'Gönder' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
  const firstCitation = page.getByRole('button', { name: /^Kaynak 1/ })
  await expect(firstCitation).toBeVisible()
  await firstCitation.click()
  await expect(page.getByRole('dialog')).toContainText('Kart aidatları')
})
```

Örnekteki etiket ve metinler kalıbı gösterir; gerçek metinler `handoff/02-frontend-implementation.md` "Ekran metinleri"ndedir.

## Page Object Model (POM)

```typescript
// e2e/pages/PreviewPage.ts
import type { Locator, Page } from '@playwright/test'

export class PreviewPage {
  readonly page: Page
  readonly questionInput: Locator
  readonly sendButton: Locator

  constructor(page: Page) {
    this.page = page
    this.questionInput = page.getByLabel('Soru')
    this.sendButton = page.getByRole('button', { name: 'Gönder' })
  }

  async goto() {
    await this.page.goto('/onizleme')
  }

  async ask(question: string) {
    await this.questionInput.fill(question)
    await this.sendButton.click()
  }

  citation(number: number): Locator {
    return this.page.getByRole('button', { name: `Kaynak ${number}` }).last()
  }
}
```

- Locator'lar web-first'tür: her eylemde yeniden çözülür ve actionability (görünür, stabil, etkin) için otomatik bekler; POM'da ekstra `waitFor`/`waitForLoadState` gerekmez.
- `goto` sonrasında `waitForLoadState('networkidle')` kullanılmaz: Playwright dokümantasyonu bunu önermez ve iş durumu polling'i (ya da ileride açık STOMP bağlantısı) olan sayfada ağ hiç boşalmaz. Beklenen elemanı `expect(...)` ile bekle.

## Test yapısı

```typescript
// e2e/preview.spec.ts
import { expect, test } from '@playwright/test'
import { PreviewPage } from './pages/PreviewPage.ts'

const GOLD_FEE_QUESTION = 'Gold kartın yıllık aidatı ne kadar?'

test.describe('Preview', () => {
  test('givenGuideIngested_whenQuestionAnswered_thenCitationOpensChunk', async ({ page }) => {
    // Arrange
    const previewPage = new PreviewPage(page)
    await previewPage.goto()

    // Act
    await previewPage.ask(GOLD_FEE_QUESTION)
    await expect(previewPage.sendButton).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })
    await previewPage.citation(1).click()

    // Assert
    // Cevabın tam metnine bakılmaz: akış bitti, en az bir [n] alıntısı var ve alıntı parçayı açıyor
    await expect(page.getByRole('dialog')).toContainText('Raşit Bankası Ürün ve Hizmet Rehberi')
  })
})
```

## İçe alma ilerlemesi: yenileme ve ikinci sekme

İlerleme polling ile izlenir (`frontend-conventions` bölüm 8). Süren işin bilgisi backend'den (`latestJob`) geldiği için sayfa yenilenince ya da adres ikinci bir sekmede açılınca ilerleme kaldığı yerden görünmelidir; E2E bunu doğrular. Aynı context'teki sayfalar `localStorage`'ı (görünüm tercihleri, bölüm 9) ve cookie'leri paylaşır; tercihlerin sekmeler arasında karışmaması gereken senaryolarda `browser.newContext()` ile ayrı context aç.

```typescript
// e2e/sources.spec.ts
test('givenUploadDialog_whenUploadDocumentAndReload_thenProgressContinuesAndBecomesReady', async ({ page }) => {
  // Arrange
  const sourceName = `E2E rehber ${Date.now()}`  // kaynak adı tekildir; her koşuda yeni ad
  await page.goto('/kaynaklar')
  await page.getByRole('button', { name: 'Belge ekle' }).click()
  const dialog = page.getByRole('dialog', { name: 'Belge ekle' })
  await dialog.getByLabel('Dosya').setInputFiles(GUIDE_PDF)
  await dialog.getByLabel('Kaynak adı').fill(sourceName)
  await dialog.getByLabel('Sürüm etiketi').fill('2026.10')
  const uploadResponse = page.waitForResponse(
    (response) => response.url().endsWith('/api/workspaces/42/sources') && response.status() === 202,
  )

  // Act
  await dialog.getByRole('button', { name: 'Yükle ve işle' }).click()
  await uploadResponse

  // Assert
  const row = page.getByRole('row', { name: new RegExp(sourceName) })
  await expect(row.getByRole('progressbar')).toBeVisible()
  await page.reload()
  await expect(row.getByRole('progressbar')).toBeVisible()
  // Sabit bekleme yok; gerçek Ollama ile iş uzun sürebileceği için timeout büyük
  await expect(row.getByText('Hazır')).toBeVisible({ timeout: INGESTION_TIMEOUT_MS })
})
```

- Gerçek Ollama ile iş çoğu zaman 1-2 saniyede biter; "yenilemeden sonra ilerleme görünüyor" adımı bu yüzden işin süresine bağlıdır ve kırılgan olabilir. Kalıcı çözüm yavaşlatılmış bir embedding ortamıdır; o kurulana kadar bu risk raporda yazılır.
- Testler gerçek veriyi değiştirir (yeni belge). Kaynak adı tekil olduğu için her koşu benzersiz ad kullanır; veriyi kalıcı değiştiren testler sona konur; temiz başlangıç gerekiyorsa `docker compose ... down -v` + `up` (`deployment/CLAUDE.md`).

## Playwright configuration

Hedef config (`web/playwright.config.ts`):

```typescript
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
```

> **Proje kuralı:** Sadece Chromium projesi tanımlanır. `webServer` yoktur: testler dışarıdan açılmış sisteme karşı koşar; varsayılan Docker Compose'daki site (`http://localhost:3000`, `deployment/CLAUDE.md`), dev server için `E2E_BASE_URL=http://localhost:5173 npm run test:e2e`. Değişkenin adı her yerde `E2E_BASE_URL`'dir; `BASE_URL` kullanılmaz. Testler aynı veritabanını değiştirdiği için `workers: 1` ve `retries: 0`'dır; kırılgan bir test retry ile gizlenmez.

Uçtan uca testler gerçek backend, PostgreSQL ve lokal modelleri (dil modeli, embedding, reranker) gerektirir; Ollama Mac'te `ollama serve` ile açık olmalıdır (`ollama.md`). Modeller yavaş olduğu için `timeout` cevap akışı testlerinde artırılır.

## Flaky test pattern'leri

### Karantina

```typescript
test('givenIngestionRunning_whenUploadOpened_thenUploadButtonIsDisabled', async ({ page }) => {
  test.fixme(true, 'Flaky - Issue #123')
  // test kodu...
})

test('givenSlowChatModel_whenQuestionSent_thenTypingIndicatorShownUntilDone', async ({ page }) => {
  test.skip(!!process.env.CI, 'Flaky in CI - Issue #123')
  // test kodu...
})
```

### Flaky'liği tespit etme

```bash
npx playwright test e2e/preview.spec.ts --repeat-each=10
npx playwright test e2e/preview.spec.ts --retries=3
```

### Yaygın sebepler ve çözümler

**Eski `page.click(selector)` API'si:**
```typescript
// Kötü: discouraged selector tabanlı API, CSS/test-id'ye bağlı
await page.click('[data-testid="send-button"]')

// İyi: role tabanlı, strict ve her eylemde yeniden çözülen locator
await page.getByRole('button', { name: 'Gönder' }).click()
```

**Network zamanlaması:**
```typescript
// Kötü: keyfi timeout
await page.waitForTimeout(5000)

// İyi: kullanıcının gördüğü sonucu bekle
await expect(page.getByRole('button', { name: 'Gönder' })).toBeEnabled({ timeout: ANSWER_TIMEOUT_MS })

// Gerekirse belirli response'u bekle (eylemden ÖNCE başlat ki kaçmasın)
const answerResponse = page.waitForResponse(
  (response) =>
    response.url().includes('/api/assistant/conversations/') &&
    response.url().endsWith('/messages') &&
    (response.headers()['content-type'] ?? '').startsWith('text/event-stream'),
)
await page.getByRole('button', { name: 'Gönder' }).click()
await answerResponse
```

**Animasyon zamanlaması:**
```typescript
// Locator.click() elemanın görünür, stabil (animasyonu bitmiş) ve etkin olmasını zaten bekler;
// ekstra waitFor veya networkidle gerekmez
await page.getByRole('button', { name: 'Belge ekle' }).click()
```

## Artifact yönetimi

### Screenshot'lar

```typescript
await page.screenshot({ path: 'artifacts/preview-page.png' })
await page.screenshot({ path: 'artifacts/full-page.png', fullPage: true })
await page.getByRole('dialog').screenshot({ path: 'artifacts/cited-chunk.png' })
```

### Trace'ler

Çoğu durumda config'deki `trace: 'on-first-retry'` yeterlidir. Elle trace gerekiyorsa Playwright Tracing API'si context üzerinden kullanılır:

```typescript
await context.tracing.start({ screenshots: true, snapshots: true })
// ... test eylemleri ...
await context.tracing.stop({ path: 'artifacts/trace.zip' })
```

Trace'i `npx playwright show-trace artifacts/trace.zip` ile aç. `browser.startTracing()` farklı bir şeydir: Chromium'a özel, DevTools performans paneli için düşük seviye bir trace üretir; Playwright trace viewer ile açılmaz.

### Video

```typescript
// playwright.config.ts içinde
use: {
  video: 'retain-on-failure',
}
// Videolar outputDir (ör. artifacts/) altına yazılır
```

## CI/CD entegrasyonu

```yaml
# .github/workflows/e2e.yml
name: E2E Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-node@v7
        with:
          node-version: 24
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npx playwright test
        env:
          E2E_BASE_URL: ${{ vars.STAGING_URL }}
      - uses: actions/upload-artifact@v7
        if: always()
        with:
          name: playwright-report
          path: web/playwright-report/
          retention-days: 30
```

`E2E_BASE_URL` sadece testler zaten çalışan bir sisteme karşı koşulacaksa verilir: bu projede devops'un `docker compose` ile açık bıraktığı lokal sistem (http://localhost:3000). Deploy edilmiş bir ortam henüz yoktur. Docker ve ortamlar `deployment/` ile devops agent'ının sorumluluğundadır.

## Test raporu şablonu

```markdown
# E2E Test Raporu

**Tarih:** YYYY-MM-DD HH:MM
**Süre:** Xm Ys
**Durum:** PASSING / FAILING

## Özet
- Toplam: X | Geçen: Y (Z%) | Kalan: A | Flaky: B | Atlanan: C

## Başarısız testler

### test-adı
**Dosya:** `e2e/preview.spec.ts:45`
**Hata:** Expected element to be visible
**Screenshot:** artifacts/failed.png
**Önerilen çözüm:** [açıklama]

## Artifact'ler
- HTML Report: playwright-report/index.html
- Screenshot'lar: artifacts/*.png
- Videolar: artifacts/**/*.webm
- Trace'ler: artifacts/**/*.zip
```

## Kritik akış testi

Geri alınamayan ya da gerçek kaynak harcayan akışlarda (belge yükleyip bilgi bankasını kalıcı değiştirme, gerçek modellerle uzun değerlendirme çalıştırması vb.) testi production'a karşı koşturma, onay adımını doğrula ve sonucu backend response'u üzerinden bekle. Locator ve AAA kuralları burada da geçerlidir:

```typescript
test('givenUploadDialog_whenUserConfirmsUpload_thenIngestionStarts', async ({ page }) => {
  test.skip(!!process.env.E2E_BASE_URL, 'Veriyi kalıcı değiştirir; sadece lokal ortamda koşar')

  // Arrange
  await page.goto('/kaynaklar')
  await page.getByRole('button', { name: 'Belge ekle' }).click()
  const dialog = page.getByRole('dialog', { name: 'Belge ekle' })
  await dialog.getByLabel('Dosya').setInputFiles(GUIDE_PDF)
  await dialog.getByLabel('Kaynak adı').fill(`E2E rehber ${Date.now()}`)
  await dialog.getByLabel('Sürüm etiketi').fill('2026.10')

  // Act
  const uploadResponse = page.waitForResponse(
    (response) => response.url().endsWith('/api/workspaces/42/sources') && response.request().method() === 'POST',
  )
  await dialog.getByRole('button', { name: 'Yükle ve işle' }).click()
  await uploadResponse

  // Assert
  await expect(page.getByRole('progressbar')).toBeVisible()
})
```

(Orijinal skill'deki Wallet/Web3 ve ödeme örnekleri bu projeyle ilgisiz olduğu için çıkarıldı.)

## Git'e girmeyen çıktılar

`playwright-report/`, `artifacts/` (config'teki `outputDir`), `playwright-results.xml` ve `playwright-results.json` üretilen dosyalardır; `web/.gitignore`'a eklenir, commit'lenmez.

## İlgili skill'ler

- `react-testing` - component seviyesinde testler ve RTL/Playwright karar sınırı
- `webapp-testing` - çalışan uygulamayı Python Playwright script'iyle hızlıca keşfetme ve doğrulama
