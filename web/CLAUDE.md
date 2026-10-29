<!--
AMAÇ: Claude (ve ekip üyeleri) web/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
İçerik: ürün hedefi, harita, komutlar, dosya sahipliği, tuzaklar, iş → skill yönlendirmesi.
Kuralların ayrıntısı frontend-conventions skill'inde. Orada yazanı burada tekrar etme.
Durum: iskelet ve kabuk TASK-002'de Vite react-ts şablonuyla kuruldu (üst bar, menü, dört ekranın başlığı, "Sayfa bulunamadı"). Ekran içerikleri sonraki task'larda gelir.
-->

# web (React yönetim paneli)

## Ürün hedefi
Geliştirilecek ürün `kaynaklar/rag-lab.html`'in **sol yarısının aynısıdır**: Raşit Bankası · Dijital Asistan Yönetimi paneli; yan menüde Önizleme, Konuşmalar, Bilgi kaynakları ve Değerlendirme. "Aynısı" şu demektir: ekranlar, yan menü, bileşen düzeni, Türkçe metinler, renkler ve etkileşimler lab'daki gibidir. Sağ yarı (sistem konsolu: mimari haritası, adım adım akış, Network / Veritabanı / Sıralama / Prompt / Loglar / Kod sekmeleri ve üstündeki düğmeler) ekrana gelmez. O akışı (sorguyu yeniden yazma, retrieval, rerank, prompt kurma, cevap üretme, alıntı) backend gerçekten yürütür ve her adımın sonucunu tura kaydeder.

Lab'da üretim modu (Sadece LLM / RAG), retrieval (top-5 / rerank 20→5), takip sorusunu yeniden yazma ve reranker durumu sağ yarıdaki düğmelerle değişir. Sağ yarı gelmediği için bu ayarlar ekrandan değiştirilmez; `assistant-api` yapılandırmasıdır (`assistant.*`, ortam değişkeniyle). Başlıktaki "Canlı yapılandırma" rozeti lab'daki gibi kalır; Değerlendirme'deki ölçüm düğmesi etkin stratejiyi lab'daki gibi gösterir ("Ölçümü çalıştır · rerank 20→5"), değeri backend verir.

## Teknoloji
- Node 26 (`engines: >=26 <27`), npm 11, React 19.3, TypeScript 6.0 (`~6.0.3`; güncel Vite şablonu TS 7 getiriyor, proje kuralı gereği 6'da tutuldu), Vite 8.3, oxlint 1.90. Kesin sürümler `package.json` / `package-lock.json`'dadır.
- Kurulu runtime paketleri: `react`, `react-dom`, `react-router` (7.x, declarative mode: `BrowserRouter` + `Routes`), `@fontsource/source-sans-3` (400-800; CDN yok). Kurulu test paketleri: `vitest` (jsdom), `@vitest/coverage-v8`, Testing Library (`react`, `dom`, `user-event`, `jest-dom`), `vitest-axe`, `@playwright/test` (sadece chromium).
- Hedef runtime paketleri (henüz yok): `@tanstack/react-query`, `react-error-boundary` (ilk sunucu çağrısını yapan task), `@fontsource/jetbrains-mono` (ilk monospace metni gösteren task). Hedef test paketi: `msw` (ilk sunucu çağrısını yapan task).
- Cevap akışı (SSE) native `fetch` + `ReadableStream` ile okunur; ek paket gerekmez (`frontend-conventions` bölüm 1, 8; `sse-streaming`).
- Referans uygulama: `../embedding-pgvector/frontend`: Bilgi kaynakları, yükleme ve içe alma ekranlarının çalışan örneği; okunur kalıp referansı, değiştirilmez.
- **Henüz olmayanlar:** `@tanstack/react-query`, `react-error-boundary`, `msw`, `@fontsource/jetbrains-mono`, `AppProviders.tsx`, `shared/api/**` (sunucu çağrısı yok). `@stomp/stompjs`, Stryker (mutation testi), form kütüphanesi, global store (Zustand), markdown render kütüphanesi de yok. Skill örnekleri kullansa bile bunları varmış gibi kod yazma; eklemek plan kararıdır.
- Vite dev server `/api/assistant`'ı `http://localhost:8090`'a (assistant-api), `/api/workspaces`'ı `http://localhost:8080`'e (retrieval-api) proxy'ler (`vite.config.ts`); SSE için proxy tamponlamamalıdır (`vite-patterns`, `sse-streaming`). Docker'da aynı yönlendirmeyi nginx yapar (`deployment/CLAUDE.md`).
- `kaynaklar/rag-lab.html`'in sol yarısı ürünün kendisidir (Ürün hedefi); `kaynaklar/rag.html` anlatımdır. Değiştirilmez.

## Klasör yapısı
Feature-first. Ayrıntılı ağaç `frontend-conventions` bölüm 4'te.
```
web/
├── e2e/                     Playwright testleri
├── public/                  favicon
└── src/
    ├── main.tsx             font import'ları + StrictMode + BrowserRouter + App (AppProviders ilk provider gerektiren task'ta)
    ├── index.css            design token'ları (--color-*, --space-*, --radius-*, --text-*), reset
    ├── app/                 App (route'lar), AdminLayout, AppHeader, MainNav, NotFoundPage
    ├── features/            preview, conversation, source, evaluation (lab'ın yan menüsü)
    ├── shared/              components/ (PageHeader); hedef: api/ (httpClient, eventStream, ApiError, errorMessages, pagination), format/, toast/
    └── test/                Vitest setup, MSW server ve handler'lar, SSE yardımcıları (tester)
```
- Feature adları servis paketleriyle aynı: `conversation`, `evaluation` → assistant-api; `source` → retrieval-api. `preview` sadece web'dedir: Önizleme ekranı assistant-api'nin `conversation` API'sini kullanır.
- Bir feature başka bir feature'ı import etmez; ortak kod `shared/`'a gider.
- Adresler (lab): `/onizleme`, `/konusmalar`, `/kaynaklar`, `/degerlendirme` (TASK-002); `/` → `/onizleme`, bilinmeyen adres → "Sayfa bulunamadı". Hedef: `/konusmalar/:conversationId` (TASK-014), `/kaynaklar/:sourceId` (TASK-007); o task'ta ilgili `NavLink`'in `end` prop'u kaldırılır ki detayda menü öğesi seçili kalsın. Adreste çalışma alanı yoktur (M6). Bilgi kaynakları çağrıları `/api/workspaces/42/...`.
- Test dosyası, test ettiği dosyanın yanında durur (`X.test.tsx`).

## Komutlar
Hepsi `web/` içinde çalıştırılır.
- Bağımlılıkları kur: `npm ci` (`package-lock.json`'a göre; lock dosyasını değiştirmez)
- Dev server: `npm run dev` → http://localhost:5173 (terminali bloklar; arka planda başlat, iş bitince durdur). Sunucu çağrısı yapan ekranlar için assistant-api (8090) ve retrieval-api (8080) açık olmalı (kabuk tek başına açılır).
- Build + tip kontrolü: `npm run build`
- Lint: `npm run lint` (uyarı bırakılmaz)
- Testler: `npm test` (`vitest run`); izleme modu `npm run test:watch`. Setup dosyası `src/test/setup.ts`'tir (`vite.config.ts` → `test.setupFiles`); test dosyası yoksa vitest "No test files found" ile 1 döner.
- Tek test: `npx vitest run <dosya> -t '<ad>'`
- Coverage: `npm run test:coverage` (v8; satır eşiği %80 `vite.config.ts`'te; `src/test/**`, `src/main.tsx` ve test dosyaları hariç)
- Uçtan uca (`playwright.config.ts`: `testDir: ./e2e`, sadece chromium): önce bir kez `npx playwright install chromium`, sonra sistem açıkken `npm run test:e2e` (Docker'daki site için varsayılan `http://localhost:3000`, dev server için `E2E_BASE_URL=http://localhost:5173 npm run test:e2e`).
- Handoff'a `STATUS: DONE` yazmadan önce `build`, `lint` ve testler hatasız geçmeli.

## Dosya sahipliği
| Yol | Sahibi |
|-----|--------|
| `src/**` (testler hariç), `index.html`, `public/` | frontend-coder |
| `package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig*.json`, `.oxlintrc.json`, `.gitignore`, test aracı config'leri | frontend-coder |
| `*.test.ts(x)`, `src/test/**`, `e2e/**` | tester |
| `Dockerfile`, `.dockerignore`, `nginx.conf` | devops |

- Paket ekleme ve kaldırma sadece frontend-coder'ın işidir. Tester'a test paketi gerekiyorsa frontend-coder'a mesaj atar.
- Tester testi geçirmek için uygulama kodunu değiştirmez; bulduğu hatayı raporlar.

## Kurallar
- Bağlayıcı kurallar `frontend-conventions` skill'indedir: dil, yorum yasağı, isimlendirme, API katmanı, cevap akışı, `errorCode` → Türkçe mesaj, stil, testler. web/ altında kod ya da test yazmadan önce bu skill'i yükle. Başka bir skill'in örneği bununla çelişirse `frontend-conventions` geçerlidir.
- API type'ları ve çağrıları `docs/api/openapi.yaml` ile (iki servis, path önekleri: `/api/assistant/**` → assistant-api, `/api/workspaces/**` → retrieval-api), SSE olay type'ları `docs/api/sse.md` ile birebir aynıdır. Sözleşmede olmayan endpoint, alan ya da olay kullanılmaz.
- Kod biçimini düzeltecek bir formatter (Prettier) yok; biçime elle uyulur: 2 boşluk, tek tırnak, noktalı virgül yok, çok satırda sondaki virgül.

## Tuzaklar
Bunların çoğu Vite react-ts şablonunun `tsconfig` ayarlarından gelir; uyulmazsa build ya da lint kırılır.
- `erasableSyntaxOnly` açık: `enum` ve constructor parameter property yazılamaz. Yerine string literal union kullanılır.
- `verbatimModuleSyntax` açık: sadece tip olan import'lar `import type { ... }` ile yazılır.
- Import'lar dosya uzantısıyla yazılır: `./MessageComposer.tsx`.
- Lint aracı oxlint'tir. ESLint ya da Prettier config'i eklenmez.
- Sayfa başlığı React 19'un `<title>` öğesiyle component içinde verilir; çocuğu tek bir string olmalıdır.
- React 19: `forwardRef` yerine `ref` prop'u, basit formlarda `useActionState`. StrictMode'da effect'ler iki kez çalışır; subscription'lar ve akışlar buna dayanıklı olmalıdır (akış effect'ten değil handler'dan başlatılır).
- `EventSource` POST yapamaz ve başlık gönderemez; cevap akışı `fetch` ile okunur. Akış sırasında gelen bir olay iki `ReadableStream` parçasına bölünebilir; ayrıştırıcı satır tamponu tutar.
- jsdom `showModal`/`close` desteklemez; native `<dialog>` kullanılırsa testte `src/test/setup.ts`'te taklit gerekir.
- `VITE_` ile başlayan env değişkenleri tarayıcıya gömülür, yani herkese açıktır. Bu değişkenlere gizli bilgi konmaz (`frontend-security`).

## Hangi iş → hangi skill
| İş | Skill |
|----|-------|
| Her kod ve test | `frontend-conventions` (zorunlu) |
| Component, hook, state'in yeri | `react-patterns`, `react-composition-patterns` |
| Server state, cache, akan cevap, canlı veri | `react-state-management`, `sse-streaming` |
| Değerlendirme ekranı | `rag-evaluation` |
| Lab'dan ekran ve token aktarma, lab'da karşılığı olmayan durum | `frontend-design`, `web-design-guidelines` |
| Form, modal, klavye, ekran okuyucu, akış duyuruları | `frontend-a11y` |
| Vite config, proxy, env | `vite-patterns` |
| Kullanıcı girdisi, model çıktısını gösterme, token, yeni paket | `frontend-security` |
| Performans (uzun konuşma, hızlı gelen token'lar) | `frontend-performance`, `react-best-practices` |
| Component ve hook testi | `react-testing`, `writing-good-tests` |
| Uçtan uca test | `e2e-testing`; çalışan uygulamayı tarayıcıda denemek için `webapp-testing` |
| Review | `code-review-and-quality` |
| Hata ayıklama | `systematic-debugging` |
