TASK: TASK-007
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `react-state-management`, `frontend-a11y`, `frontend-security`, `frontend-design` (lab'ın `--s-*` değerlerinden token aktarma; boş durum gibi lab'da olmayan görünümler).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Yeni paketler (kart sorusu 1, onaylı A) | `package.json`, `package-lock.json` | Runtime: `@tanstack/react-query` 5.105.0, `react-error-boundary` 6.1.6, `@fontsource/jetbrains-mono` 5.3.0 (önceki oturumdan kalanlar plana karşı doğrulandı; üçü de planda). Test: `msw` 3.0.3 eklendi. Planda olmayan paket yok. |
| Kabuk | `main.tsx`, `app/AppProviders.tsx`, `app/queryClient.ts`, `app/AdminLayout.tsx` (+css), `app/App.tsx`, `app/MainNav.tsx` | JetBrains Mono 400; `BrowserRouter` içinde `AppProviders`; QueryClient: 4xx `ApiError`'da yeniden deneme yok, diğerlerinde 1 kez, mutation yeniden denenmez (Kararlar #16). `<Outlet>` `ErrorBoundary` ile sarılı, fallback `role="alert"` `FALLBACK_PAGE_MESSAGE`, adres değişince sıfırlanır. Route `kaynaklar/:sourceId`. Menüde sadece `/kaynaklar`'da `end` yok. |
| Token'lar | `index.css` | Lab `.tbl`, `.st`, `.btn`, `.callout`, `.meta`, `.fbox`, `.pager`, `.dlg`, `.file`, `.fld`, `.cfg`, `.ctext`, `.vecs` değerleri token oldu; component CSS'inde ham hex yok. |
| shared/api | `ApiError.ts` (+`isClientError`, `isApiErrorWithCode`, `errorCodeOf`), `httpClient.ts` (`request`, `withQuery`, `toApiError`), `errorMessages.ts`, `pagination.ts`, `workspace.ts` | `FormData`'da `Content-Type` yazılmaz. Gövdesiz/`errorCode`'suz hata → `UNEXPECTED_ERROR`, fetch fırlatırsa `NETWORK_ERROR`. |
| shared/format | `formatNumber.ts`, `formatDateTime.ts`, `formatDuration.ts`, `formatText.ts` | `Europe/Istanbul`, `dd.MM.yyyy HH:mm`; süre lab `msTxt`; `shortenText` lab `short`. |
| shared/components | `Dialog`, `Pagination`, `StatusBadge`, `ErrorAlert`, `Callout`, `PageHeader` (parent/actions/ReactNode açıklama), `button.module.css`, `table.module.css`, `visuallyHidden.module.css` | Dialog: portal, `role="dialog"` + `aria-modal` + `aria-labelledby` (h3), Esc, Tab tuzağı, dış tık, × "Kapat"; açılınca `initialFocusRef` yoksa ×; kapanınca açılıştaki odaklı öğeye döner (DOM'daysa); `canClose=false` iken Esc/×/dış tık çalışmaz. |
| K8, Kararlar #9 | `shared/chunk/types.ts`, `chunkApi.ts`, `chunkQueries.ts`, `ChunkDialog.tsx` (+css) | Sadece `chunkId` prop'u; başlık = `heading`; şerit chunk_id/chunk_no/token/model/kaynak; metin `role="region"` `aria-label="Parça metni"` `tabIndex=0`, `pre-wrap`; embedding `toFixed(5)` + "… N değer daha"; "Kapat". |
| model | `features/source/model/types.ts`, `ingestionRules.ts`, `sourceUploadRules.ts`, `chunkRules.ts` | Alan adları sözleşmeyle birebir. `sourceUploadRules.ts`'te ayrıca `normalizeUploadInput` ve `toUploadErrors(error)` (Kararlar #14 eşlemesi: dosya kodları → dosya alanı, `SOURCE_NAME_TAKEN` → ad, `violations[].field` → alanın kural mesajı, diğerleri → pencere üstü). |
| api | `features/source/api/sourceApi.ts`, `sourceQueries.ts` | Plandaki fonksiyon ve key'ler. Başarılı yükleme/yeniden denemede dönen iş `sourceKeys.job`'a ve listedeki/detaydaki `latestJob`'a yazılır (satır hemen QUEUED), sonra kaynak görünümleri invalidate edilir. Yeniden dene hatasında da liste yenilenir (G4). |
| K9 | `hooks/useIngestionJobsPolling.ts` | Aktif her iş için `useQueries`, `initialData` = gelen iş, 1 sn, iş bitince ya da 4xx'te durur; iş aktiften `DONE`/`FAILED`'a geçince kaynak görünümleri invalidate (iş sorgularının kendisi hariç: `invalidateSourceViews`). Sayfa kapanınca durur. |
| Filtre | `hooks/useDebouncedValue.ts`, `SourceDetailPage.tsx` | 300 ms; filtre değişince sayfa 0 (sayfa, ait olduğu sorguyla birlikte tutulur; ekstra istek yok). |
| K1, K2a, K4, K3 | `pages/SourceListPage.tsx`, `components/SourceTable.tsx`, `SourceEmptyState.tsx`, `WorkspaceCallout.tsx`, `PdfIcon.tsx` | Satıra tık detaya gider (link ve düğme tıkları hariç). Sayfa sayısı > 1 ise "Kaynak sayfaları" pager'ı. |
| K2 | `components/AddSourceDialog.tsx` (+css) | `onSubmit` + mutation (Kararlar #13-#15). İlk hata alanına odak gider. |
| K5-K8 | `pages/SourceDetailPage.tsx`, `components/SourceMetaGrid.tsx`, `IngestionJobTable.tsx`, `ChunkTable.tsx`, `pages/SourcePages.module.css` | İş tablosu satırları polling verisiyle güncellenir. Parça satırına tıklanınca önce satırın düğmesine odak verilir, pencere kapanınca odak oraya döner. |
| K10, K11 | `errorMessages.ts`, tüm bileşenler | Metinler kartla birebir; `dangerouslySetInnerHTML`/`innerHTML` yok (grep temiz). PDF `<a target="_blank" rel="noopener noreferrer">`. |
| `web/CLAUDE.md` | `web/CLAUDE.md` | Durum, Teknoloji (kurulu paketler), Henüz olmayanlar, klasör haritası, adresler, Komutlar notu, Tuzaklar (Dialog portal, testte QueryClientProvider). |

## Değişen dosyalar
- Yeni: `src/app/AppProviders.tsx`, `src/app/queryClient.ts`; `src/shared/api/{ApiError,httpClient,errorMessages,pagination,workspace}.ts`; `src/shared/format/{formatNumber,formatDateTime,formatDuration,formatText}.ts`; `src/shared/components/{Dialog,Pagination,StatusBadge,ErrorAlert,Callout}.tsx` + `.module.css`'leri, `button.module.css`, `table.module.css`, `visuallyHidden.module.css`; `src/shared/chunk/{types,chunkApi,chunkQueries}.ts`, `ChunkDialog.tsx`, `ChunkDialog.module.css`; `src/features/source/model/{types,ingestionRules,sourceUploadRules,chunkRules}.ts`; `src/features/source/api/{sourceApi,sourceQueries}.ts`; `src/features/source/hooks/{useIngestionJobsPolling,useDebouncedValue}.ts`; `src/features/source/components/{SourceTable,SourceEmptyState,WorkspaceCallout,PdfIcon,AddSourceDialog,SourceMetaGrid,IngestionJobTable,ChunkTable}.tsx` + css'ler; `src/features/source/pages/SourceDetailPage.tsx`, `SourcePages.module.css`.
- Değişen: `src/main.tsx`, `src/index.css`, `src/app/{App,AdminLayout,MainNav}.tsx`, `src/app/AdminLayout.module.css`, `src/shared/components/PageHeader.tsx` + css, `src/features/source/pages/SourceListPage.tsx`, `package.json`, `package-lock.json`, `CLAUDE.md`.
- Silinen: yok.
- Paketler: `@tanstack/react-query` ^5.105.0, `react-error-boundary` ^6.1.6, `@fontsource/jetbrains-mono` ^5.3.0 (dependencies); `msw` ^3.0.3 (devDependencies).

## Ekran metinleri
**Liste (`/kaynaklar`)**
- Breadcrumb "Dijital Asistan › Bilgi kaynakları", h1 "Bilgi kaynakları", açıklama "Asistanın cevap verirken aradığı belgeler. Belge retrieval-api'ye yüklenir; sayfalara ayrılır, parçalanır, her parça bge-m3 ile embed edilip pgvector'e yazılır." (çalışma alanı gelmeden: "… her parça embedding modeliyle embed edilip …").
- Düğme "Belge ekle" (başlıkta; boş durumda ikincisi). Yüklenirken "Yükleniyor…".
- Tablo başlıkları: Kaynak, Tür, Sürüm, Parça, Durum, Son senkron. Hücreler: ad (link, `/kaynaklar/{id}`) + dosya adı; "PDF · 70 sayfa" (sayfa yoksa "PDF"); sürüm; parça ya da "–"; son senkron "29.10.2026 16:15" ya da "–".
- Durum: süren iş → aşama adı (QUEUED, PARSING, …) sarı; "Hazır" yeşil; "Başarısız" kırmızı + sebep (`messageFor(job.errorCode)`) + düğme "Yeniden dene" (istek sürerken pasif).
- Pager `nav aria-label="Kaynak sayfaları"` (sadece > 1 sayfa).
- Boş durum: h2 "Henüz belge yok", "Asistan, cevap verirken buraya eklediğin belgelerde arar. İlk belgeni ekleyerek başla.", "Belge ekle".
- Bilgi kutusu: "Çalışma alanı **Raşit Bankası · Dijital Asistan** (ws 42) · embedding modeli **bge-m3@1024**. Modeli değiştirmek bütün parçaların yeniden embed edilmesi demek; retrieval-api farklı modelle yüklemeyi 409 ile reddeder."

**Belge ekle penceresi** (`role="dialog"`, başlık h3 "Belge ekle")
- Açıklama: "Asistanın cevap verirken arayacağı PDF'i seç. Belgenin yer imleri (başlıkları) olmalı; parçalar bu başlıklarla adlandırılır. En fazla 20 MB."
- Dosya kutusu: "Dosya seçilmedi" / "PDF · en fazla 20 MB"; seçilince dosya adı + "1.120.236 bayt". Düğme "Dosya seç" (açılınca odak burada). Gizli dosya girişi `aria-label="PDF dosyası"`.
- "Kaynak adı" (label) + ipucu "Boş bırakırsan dosya adı kullanılır."; "Sürüm etiketi" (label, placeholder "ör. 2026.10").
- Düğmeler: × (`aria-label="Kapat"`), "Vazgeç", "Yükle ve işle" (gönderilirken "Yükleniyor…"; o sırada tüm düğmeler ve alanlar pasif, Esc/dış tık kapatmaz).
- Alan hataları alanın altında (`aria-describedby`; ad/sürüm input'unda ve dosya girişinde `aria-invalid`): "Bir PDF dosyası seç." · "Seçtiğin dosya boş. Başka bir PDF seç." · "Dosya 20 MB'tan büyük. Daha küçük bir PDF seç." · "Sadece PDF yükleyebilirsin. Seçtiğin dosya PDF değil." · "Dosya adı en fazla 255 karakter olabilir." · "Seçtiğin dosya boş ya da adı 255 karakterden uzun. Başka bir PDF seç." (sunucu `file` ihlali) · PDF_UNREADABLE / PDF_NO_TEXT / PDF_NO_BOOKMARKS kart metinleri (dosya alanı) · "Bu adla bir kaynak zaten var. Kaynak adı alanına başka bir ad yaz." · "Kaynak adı 2-80 karakter olmalı." · "Sürüm etiketi zorunlu; en fazla 40 karakter."
- Alana ait olmayan hata pencerenin üstünde `role="alert"` (ör. EMBEDDING_MODEL_MISMATCH, NETWORK_ERROR).

**Detay (`/kaynaklar/:sourceId`)**
- Breadcrumb link "Bilgi kaynakları" (`/kaynaklar`) › **ad**; h1 ad; açıklama "{dosya} · 412.873 bayt · sürüm 2026.10 · son senkron 29.10.2026 16:15" (senkron yoksa "–").
- Link-düğme "PDF'i aç" → `/api/workspaces/42/sources/{id}/file`, `target="_blank"`, `rel="noopener noreferrer"`.
- Meta kutuları (`dl`/`dt`/`dd`): "Sayfa / bölüm" "70 / 11"; "Parça" "154 · ort. 285 token"; "Parça boyutu" "1.600-2.000 karakter"; "Örtüşme" "200 karakter"; "Embedding" "bge-m3 · 1024"; "Index" "HNSW · cosine". Güncel sürüm yoksa ilk ikisi "–".
- h2 "İçe alma işleri"; tablo İş ("#2"), Dosya, Sürüm, Parça ("–"), Süre ("8,44 sn" / "–"), Durum (aşama adı, DONE yeşil, FAILED kırmızı, diğerleri sarı).
- h2 "Parçalar" + sağda "{totalElements} / {chunkCount ?? 0}"; filtre `aria-label="Parçalarda ara"`, placeholder "Parçalarda ara (başlık ya da metin)", `maxLength=200`, `type="search"`.
- Parça tablosu: #, Sayfa · başlık (düğme: "s. 19" + 42 karakterlik başlık), İlk satır (88 karakter), Token; boş → "Eşleşen parça yok.". Pager `nav aria-label="Parça sayfaları"`: "1 / 13", "Önceki", "Sonraki".
- Bulunamadı (404 `SOURCE_NOT_FOUND` ya da sayı olmayan id): h1 "Kaynak bulunamadı." + link "Bilgi kaynaklarına dön". Sayfa başlığı "Kaynak bulunamadı · Raşit Bankası Dijital Asistan Yönetimi".
- Diğer hatalar `role="alert"` (`ErrorAlert`), yüklenirken "Yükleniyor…".

**Parça penceresi**: başlık h3 = `heading` (yüklenirken "Parça"); şerit "chunk_id 151", "chunk_no 31", "token 297", "model bge-m3@1024", "kaynak Raşit Bankası Ürün ve Hizmet Rehberi · 2026.10"; "embedding [0.01086, -0.00193, …, … 1016 değer daha]"; düğmeler × ("Kapat") ve "Kapat".

**Genel**: sayfa çökerse `role="alert"` "Beklenmeyen bir hata oluştu. Sayfayı yenile."; bilinmeyen kod "Beklenmeyen bir hata oluştu. Tekrar dene."

## Plandan sapmalar
Davranışı ya da sözleşmeyi değiştiren sapma yok; aşağıdakiler planın boş bıraktığı ayrıntılarda yaptığım seçimler (kimseyle konuşulmadı, hepsi küçük):
1. Liste açıklamasında çalışma alanı gelmeden önceki metin "embedding modeli ile" yerine TASK-002'deki "embedding modeliyle". Türkçesi daha doğru ve mevcut testteki metinle aynı.
2. Parça penceresinin başlığı veri gelene kadar "Parça". Plan bunu belirtmiyordu (Dialog `title` zorunlu).
3. Belge ekle penceresinde gönderim sürerken düğmelerin yanında "Dosya seç" ve alanlar da pasif. Böylece istek giderken seçim değişmez.
4. `sourceUploadRules.ts`'e `normalizeUploadInput` ve `toUploadErrors` eklendi. Kararlar #14 eşlemesi saf fonksiyonda dursun ve tek başına test edilebilsin diye.
5. `invalidateSourceViews`: plan "bitince `sourceKeys.all` invalidate" diyor. Ben iş sorgularını (`sourceKeys.job`) bunun dışında tuttum, yoksa biten iş bir kez daha istenirdi (F3'teki "iş ucuna tam 3 istek" beklentisiyle çelişirdi).
6. Ad ve sürüm alanlarında `maxLength` yok. Sınır `validateSourceUpload` ile kontrol ediliyor, "… en fazla 40 karakter" mesajı da görünebiliyor. Filtre kutusunda plandaki gibi `maxLength=200` var.

## Doğrulama
- `cd web && npm run build`: geçti (tsc + vite build, hata yok).
- `cd web && npm run lint`: geçti, 0 uyarı / 0 hata. Uyarı veren üç yer satır içi `oxlint-disable` ile bastırıldı, her birinde tek satırlık gerekçe var: Dialog arka plan tıkı (klavye karşılığı Esc), satıra tık (klavye yolu link/düğme), parça düğmesinin etiketi çocuklarındaki ifadelerden geliyor.
- `cd web && npm test`: **11 geçti, 3 kaldı (14)**. Kalan üçü `App.test.tsx`'te `/kaynaklar`'ı render eden testler (`…LabBreadcrumbTitleAndDescription /kaynaklar`, `…SourcesMenuItemClicked…`, `…SetsDocumentTitle`). Sebep: tester'ın `src/test/renderWithProviders.tsx`'i (`renderAppAt`) henüz `QueryClientProvider` sarmıyor; sayfa artık `useQuery` kullandığı için hata veriyor. Plana göre beklenen durum ("Frontend: `renderWithProviders` `QueryClient` (retry kapalı) + `MemoryRouter` ile"). Düzeltmesi tester'ın dosyasında; uygulama kodunda hata yok.
- `grep -rn "dangerouslySetInnerHTML\|innerHTML\|eval(\|console.log" web/src`: boş.
- Elle tarayıcı denemesi (yerel retrieval-api 8080 + `npm run dev` 5173 + Playwright betiği, scratchpad'de): liste gerçek veriyle (2 kaynak, "Hazır", bilgi kutusu backend'den); "Belge ekle" → odak "Dosya seç"; boş gönderimde alan hataları; mevcut adla yükleme → 409, ad alanının altında kart metni; Esc → odak "Belge ekle"; detay başlığı, meta kutuları ("70 / 11", "154 · ort. 283 token", "1.600-2.000 karakter", "HNSW · cosine"), iş tablosu "#1 · 154 · 9,01 sn · DONE"; "Sonraki" → ilk satır `12` (#12); "asgari ödeme" → "8 / 154"; parça penceresi açıldı, Esc ile kapandı, odak satır düğmesine döndü; 360 px'te tablolar yatay kayıyor. Konsolda sadece beklenen 409 kaydı vardı. Yerel veritabanına yeni kaynak yazmamak için başarılı yükleme ve polling'i elle denemedim; bunları F3/F13/F24 ve E1 doğrulayacak. Not: `retrieval-api`'nin eski testleri yeni `SourceResponse` kurucusuyla derlenmiyor (`SourceControllerTest:178`), servisi `-Dmaven.test.skip=true` ile açtım; bu tester'ın işi.

## Tester için notlar
- **Provider:** `renderWithProviders`/`renderAppAt` `QueryClientProvider` sarmalı; `createQueryClient()` (`app/queryClient.ts`) kullanılabilir ama testte `retry: false` ile yeni bir `QueryClient` daha uygun. Mevcut 3 `App.test.tsx` testi bu düzeltmeyle geçmeli. `/kaynaklar` artık `GET /api/workspaces/42` ve `GET /api/workspaces/42/sources?page=0&size=20` ister, bu yüzden kabuk testlerinde de handler gerekir. Açıklama metni çalışma alanı gelince "… her parça bge-m3 ile embed edilip …" olur.
- **MSW handler'ları (hepsi retrieval-api, sözleşmeyle aynı):** `GET /api/workspaces/42`; `GET /api/workspaces/42/sources` (`page`, `size`); `POST /api/workspaces/42/sources` (multipart: `file`, ad doluysa `name`, `version`; 202 `IngestionJobResponse`); `GET /api/workspaces/42/sources/:sourceId`; `GET /api/workspaces/42/sources/:sourceId/ingestion-jobs?page=0&size=20`; `POST /api/workspaces/42/sources/:sourceId/ingestion-jobs` (gövde yok); `GET /api/workspaces/42/ingestion-jobs/:jobId`; `GET /api/workspaces/42/sources/:sourceId/chunks?[query=]&page&size=12` (boş filtrede `query` gönderilmez); `GET /api/workspaces/42/chunks/:chunkId`. PDF ucu sadece `href`, istek atılmaz.
- **Polling (F3/F24/F28):** liste ya da detaydaki `latestJob` aktifse `GET .../ingestion-jobs/{jobId}` çağrılır. `initialData` stale sayıldığı için ilk istek hemen, sonra 1000 ms aralıkla gider; iş `DONE`/`FAILED` olunca durur. Geçişte liste, detay, işler ve parça sorguları yeniden istenir. İş ucunun kendisi bir daha istenmez. Sabit bekleme yerine `findBy*` / `waitFor` (timeout > 1 sn) ya da fake timer kullanılabilir.
- **Yeniden dene (F4/F5):** başarıda satır yanıt beklemeden QUEUED olur (dönen iş cache'e yazılır), sonra liste yeniden istenir. Hatada (`409 INGESTION_IN_PROGRESS`) tablonun üstünde `role="alert"` çıkar ve liste yeniden istenir.
- **Dosya yükleme (F8-F14):** gizli giriş `getByLabelText('PDF dosyası')`. Giriş `accept="application/pdf,.pdf"` taşıyor; user-event v14'te `applyAccept` varsayılan olarak açık, bu yüzden `notlar.txt` testi için `userEvent.setup({ applyAccept: false })` gerekir. "Dosya seç" düğmesi gizli girişin `click()`'ini çağırır.
- **Dialog:** `document.body`'ye portal edilir. `screen` ile sorgula; axe testinde `container` yerine `document.body` ya da `baseElement` ver. Parça penceresinde iki "Kapat" düğmesi var (× `aria-label="Kapat"` ve metin "Kapat"), lab'daki gibi; `getAllByRole` ya da `within(dialog)` kullanılabilir.
- **Filtre (F20):** 300 ms debounce; filtre değişince istek `page=0` ile gider, araya ek istek girmez. Filtre ve sayfa adreste tutulmaz.
- **Bulunamadı (F23):** `/kaynaklar/abc`'de hiç istek atılmaz (kaynak, iş, parça ve çalışma alanı dahil).
- **Odak (F21):** satırın boş yerine tıklanınca da önce satırdaki düğmeye odak verilir; pencere kapanınca odak o düğmeye döner.
- **Bilinen kenar durum:** pencere boş durumdaki "Belge ekle"den açılırsa, yükleme bitip liste dolunca boş durum kaybolur ve odak `body`'ye düşer. Başlıktaki "Belge ekle"den açıldığında odak doğru döner (F7/F13 bunu kullanmalı).

STATUS: DONE
