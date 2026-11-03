TASK: TASK-018
# Frontend uygulama raporu

## Kullanılan skill'ler
- `frontend-conventions`, `react-patterns` (zorunlu)
- `writing-good-tests` (planın "Ek skill"i; Stryker ayarı ve hayatta kalan mutant yorumu için)

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| devDependencies: `@stryker-mutator/core`, `@stryker-mutator/vitest-runner` (aynı ana sürüm, en güncel) | `web/package.json`, `web/package-lock.json` | İkisi de `10.1.1` (`npm view` ile en güncel). Vitest 5 desteği doğrulandı: vitest-runner'ın peer'i `vitest: >=2.0.0`; projede `vitest 5.0.5`. Kurulum ve koşu sorunsuz, planner'a gitmeye gerek kalmadı |
| `web/stryker.config.mjs` (K9) | `web/stryker.config.mjs` | `testRunner: 'vitest'`, `plugins: ['@stryker-mutator/vitest-runner']`, `vitest: { configFile: 'vite.config.ts' }`, `coverageAnalysis: 'perTest'`, `mutate`: `src/**/*.{ts,tsx}`, hariç `src/**/*.test.{ts,tsx}`, `src/test/**`, `src/main.tsx`, `src/**/*.d.ts`; `reporters: ['html', 'clear-text', 'progress']`, `htmlReporter.fileName: 'reports/mutation/index.html'`, `thresholds: { high: 90, low: 80, break: 80 }`. Default export (config dosyası istisnası). Yorum yok |
| Script | `web/package.json` | `"test:mutation": "stryker run"` |
| `.gitignore` | `web/.gitignore` | `reports/`, `.stryker-tmp/` eklendi |
| `web/CLAUDE.md` "Teknoloji" | `web/CLAUDE.md` | "Henüz olmayanlar"dan Stryker çıkarıldı; kurulu test paketlerine `@stryker-mutator/core` + `@stryker-mutator/vitest-runner` 10.1 (`stryker.config.mjs`, eşik %80) eklendi |
| `web/CLAUDE.md` "Komutlar" | `web/CLAUDE.md` | Yeni satır: `npm run test:mutation` → kapsam, rapor `reports/mutation/index.html`, eşik %80 (`thresholds.break`), skor altındaysa komut 1 döner, `.stryker-tmp/` |
| Doğrulama: build, lint, test, Stryker bir kez | — | Aşağıda "Doğrulama" |

Uygulama kodu (`src/**`) değişmedi; test yazılmadı.

## Değişen dosyalar
- Yeni: `web/stryker.config.mjs`
- Değişen: `web/package.json` (2 devDependency + `test:mutation` script'i), `web/package-lock.json`, `web/.gitignore`, `web/CLAUDE.md`
- Silinen: yok
- Eklenen paketler: `@stryker-mutator/core@^10.1.1`, `@stryker-mutator/vitest-runner@^10.1.1` (devDependencies). Lock dosyasında 220 → 421 girdi; önceden kilitli paketlerin hiçbirinin sürümü değişmedi (eski ve yeni lock karşılaştırıldı).

## Ekran metinleri
Yok (UI değişmedi).

## Plandan sapmalar
Yok.

## Doğrulama
| Komut (`web/` içinde) | Sonuç |
|---|---|
| `npm view @stryker-mutator/vitest-runner peerDependencies` | `{ vitest: '>=2.0.0', '@stryker-mutator/core': '10.1.1' }` → Vitest 5.0.5 destekleniyor |
| `npm install --save-dev @stryker-mutator/core@10.1.1 @stryker-mutator/vitest-runner@10.1.1` | Kuruldu |
| `npm run build` | Geçti (exit 0) |
| `npm run lint` | Geçti (oxlint, uyarı yok, exit 0) |
| `npm test` | Geçti: 36 dosya, 620/620 test |
| `npm run test:mutation` (bir kez) | **Exit 1**: final mutation score **%79,36**, break eşiği 80'in altında. Süre 53 dk 37 sn (9 test runner süreci). Rapor `web/reports/mutation/index.html` |
| `npm audit --omit=dev` | 0 bulgu |
| `npm audit` (tam) | 5 moderate, hepsi Stryker'ın dev ağacında (aşağıda) |

### Stryker sonucu
- Mutasyona uğrayan dosya: 116 (473 dosyadan), mutant: 3397
- Killed 2630, Timeout 50, Survived 610, NoCoverage 87, RuntimeError 20 (skora girmez)
- Skor: (2630 + 50) / (2630 + 50 + 610 + 87) = 2680 / 3377 = **%79,36**; kapsanan kodda %81,46
- RuntimeError'ların 18'i `src/shared/format/formatDateTime.ts`, 1'i `formatNumber.ts`, 1'i `AddSourceDialog.tsx`: modül düzeyindeki `Intl.DateTimeFormat`/`NumberFormat` seçeneklerinin mutasyonu (ör. boş `timeZone`) modül yüklenirken `RangeError` atıyor; Stryker bunları skor dışı sayar.

En düşük skorlu dosyalar (skor; killed/timeout/survived/no coverage):
| Dosya | Skor | K | T | S | NC |
|---|---|---|---|---|---|
| `src/app/AppProviders.tsx` | %0 | 0 | 0 | 0 | 1 |
| `src/features/preview/hooks/useClock.ts` | %0 | 0 | 0 | 6 | 0 |
| `src/features/source/components/PdfIcon.tsx` | %0 | 0 | 0 | 1 | 0 |
| `src/shared/components/Callout.tsx` | %7,69 | 1 | 0 | 12 | 0 |
| `src/features/preview/components/MessageList.tsx` | %12,99 | 10 | 0 | 25 | 42 |
| `src/features/evaluation/hooks/useEvalRunAnnouncement.ts` | %19,44 | 7 | 0 | 29 | 0 |
| `src/features/preview/components/LatencyBars.tsx` | %39,29 | 11 | 0 | 16 | 1 |
| `src/features/source/components/IngestionJobTable.tsx` | %43,33 | 13 | 0 | 17 | 0 |
| `src/features/source/hooks/useIngestionJobsPolling.ts` | %47,06 | 15 | 1 | 18 | 0 |
| `src/features/preview/model/suggestionMatcher.ts` | %50,00 | 37 | 0 | 36 | 1 |
| `src/shared/toast/ToastProvider.tsx` | %57,69 | 15 | 0 | 11 | 0 |
| `src/shared/chunk/chunkQueries.ts` | %58,33 | 7 | 0 | 3 | 2 |
| `src/features/evaluation/api/evaluationQueries.ts` | %60,78 | 30 | 1 | 18 | 2 |
| `src/features/evaluation/model/evaluationTexts.ts` | %64,52 | 20 | 0 | 11 | 0 |
| `src/features/source/components/AddSourceDialog.tsx` | %65,74 | 71 | 0 | 35 | 2 |

En çok tespit edilmeyen mutant (survived + no coverage): `MessageList.tsx` 67, `preview/model/answerStreamEvents.ts` 45, `AddSourceDialog.tsx` 37, `suggestionMatcher.ts` 37, `preview/hooks/useAnswerStream.ts` 36, `useEvalRunAnnouncement.ts` 29, `shared/api/eventStream.ts` 27, `shared/components/Dialog.tsx` 26, `AnswerMessage.tsx` 24, `EvaluationPage.tsx` 23, `PreviewPage.tsx` 22, `evaluationQueries.ts` 20.

%80'e 21 mutant eksik (eşik için 2702 tespit gerekir). Plana göre test ekleyerek eşiği geçirmek tester'ın işidir.

### Ölü kod / eşdeğer mutant gözlemleri
Kesin ölü kod görmedim. Örneklediğim dosyalarda:
- `preview/model/answerStreamEvents.ts` satır 51, 55, 67, 71, 75: `typeof value === 'number' && Number.isInteger(value)` / `Number.isFinite(value)` ve `typeof value === 'string' && LIST.includes(value)` kalıplarında `typeof` kontrolü davranış olarak gereksiz (sağ taraf zaten tip dışı değerde `false` döner). Bu satırlardaki `typeof` mutantları **eşdeğer mutant**; testle öldürülemez. Kodu sadeleştirmek (typeof kısmını kaldırmak) bu mutantları yok eder; plan uygulama kodu değişikliği öngörmediği için dokunmadım. Gerekirse lead/planner kararıyla frontend-coder'a döner.
- `MessageList.tsx` satır 94-114 (kaydırma hesabı: `offsetTop`, `scrollHeight`, `clientHeight`): jsdom yerleşim hesaplamadığı için bu değerler 0; 42 NoCoverage ve kaydırma mutantlarının çoğu jsdom sınırından. Ölü kod değil; Playwright tarafında doğrulanabilir ya da tester'ın kararı.
- `useClock.ts`, `Callout.tsx`, `PdfIcon.tsx`, `AppProviders.tsx`: kullanılan kod, test eksik (ölü değil).

### `npm audit` (K8)
- `npm audit --omit=dev`: 0 bulgu → K8'e göre `NEEDS_FIX` yok.
- Tam `npm audit`: 5 moderate, hepsi yeni eklenen dev aracında:
  - `minimatch <11.0.1` (`@stryker-mutator/core` üzerinden) → düzeltme yok; `@stryker-mutator/core` ve `vitest-runner` bu yüzden "moderate" listeleniyor.
  - `qs 6.15.1` (`@stryker-mutator/core` → `typed-rest-client 2.3.1`) → `npm audit fix` ile düzeltmesi var.
  - K8: bulgu sadece dev aracında, high/critical değil → raporda gerekçeyle kalır. `npm audit fix` çalıştırmadım (plan istemiyor; dev ağacı dışında lock'u da değiştirebilir).

## Tester için notlar
- Komut: `cd web && npm run test:mutation`. Bu makinede tam koşu ~54 dk sürdü (3397 mutant, 9 süreç). Hızlı deneme için tek dosya: `npx stryker run --mutate src/features/preview/hooks/useClock.ts` (ya da birkaç dosya virgülle).
- Rapor: `web/reports/mutation/index.html` (koşudan kalan rapor diskte; `.gitignore`'da).
- Komut, skor 80'in altındayken exit 1 döner (`thresholds.break: 80`); C9'un "komut 0 döner" şartı eşik geçilince sağlanır.
- `vitest.configFile: 'vite.config.ts'` olduğu için `src/test/setup.ts` ve MSW kurulumu Stryker'da da aynen çalışır.
- Eşdeğer mutantlar (yukarıdaki `answerStreamEvents.ts` `typeof` kontrolleri) testle öldürülemez; bunlar için test yazmaya çalışma.
- RuntimeError (20) skora girmez; `formatDateTime.ts`'teki modül düzeyi `Intl` seçeneklerinden geliyor.

## Düzeltme
—

## Düzeltme turu 1
Kaynak: `handoff/05-qa-report.md` "Düzeltme" (qa, 3 madde: lab'dan bilinçsiz fark). Lab'daki biçimler `kaynaklar/rag-lab.html`'den doğrulandı: parça başlığı `h = "s. {pg} · {title}"` (`pageLabel`), pencere `chunkModal(id, v, n)` → `${n ? `[${n}] ` : ''}${h}`, ayraç `Yeni konuşma · ${conv.id}` (`cnv_…`).

| Madde | Yapılan değişiklik | Dosya(lar) |
|---|---|---|
| 1. Parça penceresi başlığı | `ChunkDialog`'a isteğe bağlı `citationNumber` prop'u eklendi. Başlık `s. {pageNumber} · {heading}`; `citationNumber` varsa `[n] s. {pageNumber} · {heading}`. Yüklenirken başlık yine "Parça". Açan taraflar artık `chunkId` yerine `ChunkCitation = { chunkId, n }` (yeni tip, `shared/chunk/types.ts`) iletiyor: Önizleme'de cevaptaki `[n]` alıntısı, cevap altındaki kaynak düğmesi, "Son cevapta modele giden parçalar" kartı; Konuşmalar'da parça tablosu satırı ve cevaptaki `[n]` alıntısı. Bilgi kaynakları değişmedi (`[n]`'siz) | `shared/chunk/ChunkDialog.tsx`, `shared/chunk/types.ts`, `features/preview/components/AnswerMessage.tsx`, `features/preview/components/ContextChunksCard.tsx`, `features/preview/components/MessageList.tsx`, `features/preview/pages/PreviewPage.tsx`, `features/conversation/components/TurnCard.tsx`, `features/conversation/components/TurnChunkTable.tsx`, `features/conversation/pages/ConversationDetailPage.tsx` |
| 2. Konuşmalar parça hücresi | Hücre metni `s. {pageNumber} · {heading} · #{chunkNo}` (lab `.tbl .tx`; uzun başlık satır içinde kayar). Başlık artık görünür olduğu için düğmedeki `title={heading}` kaldırıldı | `features/conversation/components/TurnChunkTable.tsx` |
| 3. Önizleme ayracı | `Yeni konuşma · cnv_{id}`. `conversationLabel` (`cnv_<id>`) `features/conversation/model/turnLabels.ts`'ten `shared/conversation/conversationLabel.ts`'e taşındı (feature'lar birbirini import etmez); Konuşmalar listesi ve detayı da oradan alıyor. Kimlik henüz yokken metin yine "Yeni konuşma" | yeni `shared/conversation/conversationLabel.ts`; `features/preview/components/MessageList.tsx`, `features/conversation/model/turnLabels.ts`, `features/conversation/components/ConversationTable.tsx`, `features/conversation/pages/ConversationDetailPage.tsx` |
| `web/CLAUDE.md` | Klasör haritası: `shared/conversation/conversationLabel`, `ChunkDialog` başlık biçimi, `turnLabels`'tan `cnv_<id>` çıkarıldı | `web/CLAUDE.md` |

### qa tarifinden fark (lab'a uyum)
qa, Konuşmalar'dan açılan pencerenin başlığını `[n]`'siz tarif etmişti. Lab'da Konuşmalar parça satırları `data-cite="{turn}:{k+1}"` taşıyor, bu da `openCite` → `chunkModal(id, ver, n)` çağırıyor; yani başlık `[n] s. …`. Pencereyi `[n]`'siz açan tek yer `data-chunk` (Bilgi kaynakları). Lab'a uygun olanı uyguladım; qa'ya ve lead'e mesajla bildirdim. qa lab'ı kontrol edip onayladı: Konuşmalar'da da `[n]` doğru, geri alınmayacak.

### Yeni ekran metinleri (tester için)
- Parça penceresi başlığı (dialog adı):
  - Önizleme ve Konuşmalar'dan: `[{n}] s. {pageNumber} · {heading}` (ör. `[1] s. 11 · 2.16 Kayıp ya da çalıntı bildirimi`)
  - Bilgi kaynakları'ndan ya da `citationNumber` olmadan: `s. {pageNumber} · {heading}`
  - Yüklenirken: `Parça`
- Konuşmalar parça hücresi: `s. {pageNumber} · {heading} · #{chunkNo}` (ör. `s. 19 · {heading} · #31`)
- Önizleme ayracı: `Yeni konuşma · cnv_{conversationId}` (ör. `Yeni konuşma · cnv_2`)

### Doğrulama (düzeltme turu 1)
| Komut (`web/` içinde) | Sonuç |
|---|---|
| `npm run build` | Geçti |
| `npm run lint` | Geçti (uyarı yok) |
| `npx vitest run` | 46 dosya, 728 testten **711 geçti, 17 kaldı**. Kalan 17 testin hepsi eski metni arıyor; başka bir kırılma yok (aşağıdaki liste). Testleri tester günceller (qa'nın zinciri: frontend-coder → tester) |
| Stryker | Koşmadım. Düzeltme turunda tester ve qa koşuyor (C9) |

Eski metin yüzünden kalan testler:
- `shared/chunk/ChunkDialog.test.tsx` (6): dialog adı `heading` ("2.16 Kayıp ya da çalıntı bildirimi", "Başka parça") → `s. {pageNumber} · {heading}` (prop'suz render)
- `features/source/pages/SourceDetailPage.test.tsx` (2): dialog adı `heading` → `s. {pageNumber} · {heading}`
- `features/preview/pages/PreviewPage.test.tsx` (3): dialog adı (ör. "Parça 153 başlığı") → `[{n}] s. {pageNumber} · {heading}`; ayraç `Yeni konuşma · 2` → `Yeni konuşma · cnv_2`
- `features/preview/pages/PreviewPage.suggestions.test.tsx` (2): ayraç `Yeni konuşma · 2` → `Yeni konuşma · cnv_2`
- `features/conversation/pages/ConversationDetailPage.test.tsx` (4): hücre `s. 19 · #31` → `s. 19 · {heading} · #31`; dialog adı → `[{n}] s. {pageNumber} · {heading}`

E2E: `acceptance.spec.ts` A5 ("başlıkta `s. `") artık karşılanıyor. A10'daki `openedChunkContains`, h3 metnini gövdeyle birleştirdiği için etkilenmiyor. Diğer spec'ler pencereyi adıyla aramıyor; kırılma beklemiyorum, ama koşmadım.

STATUS: DONE
