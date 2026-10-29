TASK: TASK-002
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `vite-patterns`, `frontend-design`, `frontend-a11y` (planın "Yükleyeceği skill'ler" listesi).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| F-1 iskelet (K1, AC2) | `package.json`, `package-lock.json`, `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`, `index.html`, `.gitignore`, `.oxlintrc.json` | Vite `react-ts` şablonundan (`npm create vite@latest`, scratchpad'de kurulup kopyalandı). `engines.node` `>=26 <27`. Script'ler planla aynı (`dev`, `build`, `lint`, `test`, `test:watch`, `test:coverage`, `test:e2e`; şablondan `preview` kaldı). `tsconfig.node.json` `include`'una `playwright.config.ts` eklendi. `.oxlintrc.json` referanstaki kalıp (`jsx-a11y` dahil). `.gitignore`: `node_modules`, `dist`, `coverage`, `test-results`, `playwright-report`, `blob-report`, `*.local` |
| F-1 `vite.config.ts` (K7) | `vite.config.ts` | `vitest/config`'ten `defineConfig`. Proxy `/api/assistant` → `http://localhost:8090`, `/api/workspaces` → `http://localhost:8080`. `test.environment: 'jsdom'`, `setupFiles: ['./src/test/setup.ts']` (tester yazar), `exclude: node_modules/**, dist/**, e2e/**`, coverage v8 `src/**/*.{ts,tsx}`, hariç `src/test/**`, `src/main.tsx`, `**/*.test.*`, `thresholds.lines: 80` |
| F-1 `playwright.config.ts` | `playwright.config.ts` | `testDir: './e2e'`, `baseURL: E2E_BASE_URL ?? http://localhost:3000`, sadece chromium, `workers: 1`, `trace: retain-on-failure` |
| F-1 silinecekler / Kararlar #8 | `public/favicon.svg` | Şablonun `App.css`, `src/assets/*`, `public/icons.svg`'si web'e hiç kopyalanmadı. Şablonun `favicon.svg`'si yerine lab satır 366'daki logo işareti |
| F-2 route'lar (K8, G3) | `src/app/App.tsx`, `src/main.tsx` | `App` sadece `<Routes>`; layout route `AdminLayout`, index → `<Navigate to="/onizleme" replace />`, dört ekran, `*` → `NotFoundPage`. `main.tsx`: Source Sans 3 400/500/600/700/800 CSS'leri, `index.css`, `<StrictMode><BrowserRouter><App/></BrowserRouter></StrictMode>` (Kararlar #11) |
| F-3 token'lar (K9) | `src/index.css` | Renkler lab `--s-*` değerleri birebir (`--color-*`), kabuktaki ham hex'ler token oldu (`--color-nav-ink`, `--color-nav-icon`, `--color-badge-ink`, `--color-badge-line`, `--color-focus`). Fontlar `--font-site`, `--font-mono`. Ölçüler `--text-*`, `--space-*`, `--size-*`, `--radius-badge`, `--line-height-*`, `--letter-spacing-tight`. Global: box-sizing, body 14 px, `button/input/a:focus-visible` outline, `prefers-reduced-motion` (lab satır 351). Component CSS'lerinde ham hex/px yok |
| F-4 `AdminLayout` (Kararlar #2) | `src/app/AdminLayout.tsx` + `.module.css` | `100dvh` grid, `auto auto minmax(0,1fr)`; `<main>` `overflow:auto`, padding 16/18/28, gap 12 (lab `.page`) |
| F-4 `AppHeader` (Kararlar #4, #5) | `src/app/AppHeader.tsx` + `.module.css` | `<header>`, logo `<Link to="/onizleme" aria-label="Önizleme">` + SVG (`aria-hidden`, renkler token'dan `fill="var(--color-…)"`) + "Raşit Bankası" / "Dijital Asistan Yönetimi". Rozet "Canlı yapılandırma" (`title` lab'daki gibi), `@media (max-width: 640px)` altında gizli. Kullanıcı alanı yok |
| F-4 `MainNav` (Kararlar #3, K10) | `src/app/MainNav.tsx` + `.module.css` | `<nav aria-label="Menü">`, yatay, `overflow-x:auto`. Dosya içi `NAV_ITEMS` (`path`, `label`, `icon`), ikonlar lab satır 374-377. Her öğe `NavLink` (seçili olan `aria-current="page"`, sınıf `isActive`: bordo alt çizgi, `--color-brand-dark` metin, `--color-brand` ikon). Sayaç yok. Bkz. "Plandan sapmalar": `end` prop'u |
| F-4 `PageHeader` (Kararlar #6, #12) | `src/shared/components/PageHeader.tsx` + `.module.css` | Props `{ section, title, description }`. Breadcrumb `<div>`: "Dijital Asistan", "›", `<b>{section}</b>`; `<h1>`, `<p>`; React 19 `<title>{`${section} · Raşit Bankası Dijital Asistan Yönetimi`}</title>` |
| F-4/F-5 sayfalar (S2, Kart soru 1 = A) | `src/features/preview/pages/PreviewPage.tsx`, `src/features/conversation/pages/ConversationListPage.tsx`, `src/features/source/pages/SourceListPage.tsx`, `src/features/evaluation/pages/EvaluationPage.tsx`, `src/app/NotFoundPage.tsx` | Her biri sadece `PageHeader`. Metinler F-5 tablosundan; Bilgi kaynakları ve Değerlendirme açıklamaları önerilen (A) metinle (model adı ve soru sayısı yok) |
| Plan "Çalışacak roller": `web/CLAUDE.md` "Hedef" satırları | `web/CLAUDE.md` | Durum yorumu, Teknoloji (kurulu sürümler, henüz olmayanlar), Klasör yapısı, adresler, Komutlar güncellendi |

## Değişen dosyalar
Yeni (hepsi `web/` altında):
- `package.json`, `package-lock.json`, `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`, `vite.config.ts`, `playwright.config.ts`, `.oxlintrc.json`, `.gitignore`, `index.html`, `public/favicon.svg`
- `src/main.tsx`, `src/index.css`
- `src/app/App.tsx`, `src/app/AdminLayout.tsx`, `src/app/AdminLayout.module.css`, `src/app/AppHeader.tsx`, `src/app/AppHeader.module.css`, `src/app/MainNav.tsx`, `src/app/MainNav.module.css`, `src/app/NotFoundPage.tsx`
- `src/shared/components/PageHeader.tsx`, `src/shared/components/PageHeader.module.css`
- `src/features/preview/pages/PreviewPage.tsx`, `src/features/conversation/pages/ConversationListPage.tsx`, `src/features/source/pages/SourceListPage.tsx`, `src/features/evaluation/pages/EvaluationPage.tsx`

Değişen: `web/CLAUDE.md`. Silinen: yok.

Eklenen paketler (kurulu sürüm, `package.json` aralığı):
- Runtime: `react` 19.3.1 (^19.3.1), `react-dom` 19.3.1 (^19.3.1), `react-router` 7.18.5 (^7.18.5), `@fontsource/source-sans-3` 5.3.1 (^5.3.1)
- Dev: `typescript` 6.0.3 (~6.0.3), `vite` 8.3.5, `@vitejs/plugin-react` 6.1.2, `@types/react` 19.3.0, `@types/react-dom` 19.3.0, `@types/node` 24.19.2, `oxlint` 1.90.0, `vitest` 5.0.5, `jsdom` 30.1.3, `@vitest/coverage-v8` 5.0.5, `@testing-library/react` 16.3.3, `@testing-library/dom` 10.4.2, `@testing-library/user-event` 14.6.8, `@testing-library/jest-dom` 7.0.1, `vitest-axe` 0.1.0, `@playwright/test` 1.64.1
- `npm install` sonucu: 0 vulnerabilities.

## Ekran metinleri
- Üst bar: logo linki erişilebilir adı "Önizleme" (`href="/onizleme"`), metin "Raşit Bankası", "Dijital Asistan Yönetimi"; rozet "Canlı yapılandırma" (`title`: "Önizleme, canlı asistanla aynı yapılandırmayı kullanır").
- Menü: `<nav aria-label="Menü">`; linkler "Önizleme" (`/onizleme`), "Konuşmalar" (`/konusmalar`), "Bilgi kaynakları" (`/kaynaklar`), "Değerlendirme" (`/degerlendirme`). Seçili link `aria-current="page"`. İkonlar `aria-hidden`, link adı sadece metin.
- Breadcrumb: "Dijital Asistan" › `<b>section</b>`.

| Adres | section (breadcrumb) | h1 | açıklama | `document.title` |
|---|---|---|---|---|
| `/onizleme` (ve `/`) | Önizleme | Asistan önizleme | Raşit Mobil'deki asistanı müşterinin gördüğü hâliyle dene; önizleme konuşmaları da Konuşmalar'a düşer. | Önizleme · Raşit Bankası Dijital Asistan Yönetimi |
| `/konusmalar` | Konuşmalar | Konuşmalar | Her tur, modele giden parçalarla birlikte saklanır. Yanlış bir cevapta önce doğru parçanın prompt'a girip girmediğine bakılır. | Konuşmalar · Raşit Bankası Dijital Asistan Yönetimi |
| `/kaynaklar` | Bilgi kaynakları | Bilgi kaynakları | Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça embedding modeliyle embed edilip pgvector'e yazılır. | Bilgi kaynakları · Raşit Bankası Dijital Asistan Yönetimi |
| `/degerlendirme` | Değerlendirme | Değerlendirme | Rehberde cevabı olan sorular; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar. | Değerlendirme · Raşit Bankası Dijital Asistan Yönetimi |
| başka her adres (`/yok`, `/konusmalar/12`) | Sayfa bulunamadı | Sayfa bulunamadı | Adresi kontrol et ya da menüden bir ekran seç. | Sayfa bulunamadı · Raşit Bankası Dijital Asistan Yönetimi |

- `index.html` varsayılan başlığı: "Raşit Bankası · Dijital Asistan Yönetimi".
- Hata metni / `errorMessages.ts` yok (plan: sunucu çağrısı yok).

## Plandan sapmalar
1. **`NavLink`'lere `end` prop'u eklendi.** React Router'ın `NavLink`'i alt yolları da aktif sayar; `end` olmadan `/konusmalar/12`'de "Konuşmalar" `aria-current="page"` alıyordu, bu G3/T-F6'yı ("hiçbir menü öğesi seçili değil") çiğner. `end` ile plan davranışı sağlanıyor. Bu bir sapma değil, planın uygulanış ayrıntısı; ama TASK-007 / TASK-014'te detay ekranları gelince lab'daki gibi detayda da menü öğesinin seçili kalması için `end` kaldırılmalı (`web/CLAUDE.md`'ye not düşüldü). Kimseyle konuşulmadı (plan davranışını değiştirmiyor).
2. **TypeScript 6'da tutuldu.** Güncel `create-vite` react-ts şablonu `typescript ~7.0.2` getiriyor; plan "`typescript` (~6)" ve `web/CLAUDE.md` "TypeScript 6" dediği için `~6.0.3` kuruldu. Plana uygun; sadece bilgi için.
3. **Rozet `@media` ile gizleniyor** (lab'da `@container`). Plan F-4 `@media (max-width: 640px)` diyor; lab container'ı tarayıcı çerçevesi içindeydi, üründe viewport aynı genişlik. Plana uygun.
4. **Logo SVG renkleri `fill="var(--color-brand)"` vb.** Lab'daki ham hex yerine token (çizim aynı). Plana uygun ("ham hex kalmaz").
5. **Şablondan `preview` script'i kaldı** (plan listesinde yok, şablonun parçası; build'i yerelde denemek için). Ek dependency değil.

Sözleşmede eksik ya da hata görülmedi (bu task'ta web sunucu çağrısı yapmıyor).

## Doğrulama
- `cd web && npm run build` → geçti (exit 0; `tsc -b` + `vite build`, 39 modül, `dist/` sonra silindi).
- `cd web && npm run lint` → geçti (exit 0, 0 uyarı 0 hata; `npx oxlint --deny-warnings` de exit 0). oxlint temiz çıktıda bir şey yazmıyor; hatalı bir dosyayla uyarı ürettiği ayrıca doğrulandı.
- `cd web && npm test` → çalıştırıldı: "No test files found, exiting with code 1". Test dosyası henüz yok (tester yazacak); bu yüzden "geçti" sayılmaz.
- Elle tarayıcı kontrolü (dev server `npx vite --port 5179` + Playwright chromium script'i, scratchpad'de; kapatıldı):
  - `/` → URL `/onizleme`, title "Önizleme · Raşit Bankası Dijital Asistan Yönetimi".
  - Menüden "Bilgi kaynakları" → URL `/kaynaklar`, title "Bilgi kaynakları · …", `aria-current` sadece "Bilgi kaynakları"nda.
  - `/konusmalar/12` → h1 "Sayfa bulunamadı", `aria-current` sayısı 0.
  - 360×740 `/degerlendirme` → rozet görünmüyor, `scrollWidth` 360.
  - 1480×900 ekran görüntüsü lab'ın `.sbody` görüntüsüyle yan yana: üst bar (52 px), menü, seçili öğe çizgisi, breadcrumb (y≈117) ve h1 (y≈149) konumları, renkler ve font aynı. Farklar beklenenler: kullanıcı alanı, sayaç, ekran içerikleri.

## Tester için notlar
- **MSW handler'ı gereken endpoint yok**; kabuk hiç istek atmıyor. `msw` bu task'ta kurulmadı (Kararlar #10).
- `vite.config.ts` → `test.setupFiles: ['./src/test/setup.ts']`; dosya tester'ın. `@testing-library/jest-dom` ve `vitest-axe` matcher'ları orada bağlanmalı. `globals` açık değil (`describe/it/expect` import edilir).
- CSS Modules testte varsayılan (scoped) davranışta; sınıf adına göre sorgu yerine rol/metin kullanın. Rozetin dar ekranda gizlenmesi CSS media query'si olduğu için jsdom'da test edilemez; E-6'da (Playwright) doğrulanır.
- `App` router içermez: `render(<MemoryRouter initialEntries={['/kaynaklar']}><App /></MemoryRouter>)`.
- Sorgular: `getByRole('navigation', { name: 'Menü' })`, içinde `getByRole('link', { name: 'Bilgi kaynakları' })`; logo `within(getByRole('banner')).getByRole('link', { name: 'Önizleme' })` → `href="/onizleme"`. Menüde de "Önizleme" adında bir link olduğu için logo sorgusu `banner` içinde yapılmalı.
- Breadcrumb ayrı `<span>`/`<b>` öğelerinde: "Dijital Asistan", "›", section metni. Section ile h1 aynı metinse (`Konuşmalar`, `Bilgi kaynakları`, `Değerlendirme`, `Sayfa bulunamadı`) `getByText` birden fazla eşleşir; h1 için `getByRole('heading', { level: 1 })` kullanın. "Konuşmalar" metni ayrıca menü linkinde de var.
- Menü tıklaması (T-F3) sonrası React Router geçişi transition içinde render ediyor; `findByRole('heading', { name: 'Bilgi kaynakları' })` ile bekleyin. Playwright'ta da tıklamadan hemen sonra `aria-current` okumak eski değeri verebiliyor; `expect(...).toHaveAttribute` gibi bekleyen assertion kullanın.
- `document.title`: React 19 `<title>`'ı `<head>`'e taşır; jsdom'da `waitFor(() => expect(document.title).toBe(...))` gerekebilir.
- Sabit veri yok (T-F5): "Konuşmalar" linkinin erişilebilir adı tam "Konuşmalar"; header'da kullanıcı adı/baş harf öğesi yok.
- E2E (E-1…E-6) için `playwright.config.ts` hazır; `npx playwright install chromium` bu makinede çalıştırıldı.

## Düzeltme
Yok.

STATUS: DONE
