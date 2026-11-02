TASK: TASK-016
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `rag-evaluation`, `react-state-management`, `frontend-a11y`, `frontend-design` (lab token'ları: `.kpis`/`.kpi`, `.ctxl button.gold`, `.sect`, `.st`, `.callout`).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Değerlendirme type'ları (sözleşmeyle birebir) | `features/evaluation/model/types.ts` | `GoldenSetSummaryResponse`, `EVAL_RUN_STATUSES`, `EVAL_RUN_ERROR_CODES`, `EVAL_RETRIEVAL_STRATEGIES`, `EvalRunResponse`, `EvalQuestionResultResponse`, `EvalRunDetailResponse`, `EvalOverviewResponse` |
| Kurallar / sabitler | `model/evaluationRules.ts` | `EVAL_POLL_INTERVAL_MS = 1000`, `RECALL_PASS_THRESHOLD = 0.95` (ham değerle), `EVAL_RUNS_PAGE_SIZE = 20`, `KPI_FRACTION_DIGITS = 2`, `RUN_TABLE_FRACTION_DIGITS = 3` |
| Etiket fonksiyonları | `model/evaluationLabels.ts` | `evalRunLabel`, `strategyText`, `retrievalText`, `vectorRankText`, `promptPositionText`, `runButtonText`, `recallSubtitle`, `retrievalSubtitle`, `runFailureText` |
| Lab metinleri | `model/evaluationTexts.ts` | Bütün ekran metinleri tek yerde (aşağıda "Ekran metinleri") |
| API | `api/evaluationApi.ts` | `getEvalOverview` (GET `/api/assistant/evals/overview`), `startEvalRun` (POST `/api/assistant/evals`, gövdesiz), `listEvalRuns({ page, size })` (GET `/api/assistant/evals?page=&size=`) |
| Sorgular, 5a / 5c | `api/evaluationQueries.ts` | `evaluationKeys = { all, overview, runsAll, runs(params) }`. `useEvalOverviewQuery`: `runningRun` doluyken 1 sn yoklar, 4xx'te durur; bir önceki cache'te `runningRun` dolu, yenisinde `null` ise `runs` invalidate. `useEvalRunsQuery(params, isRunning)`: aynı aralık, `keepPreviousData`. `useStartEvalRunMutation`: 202 gövdesi `setQueryData(overview)` ile `runningRun`'a yazılır, ilk yoklama beklenmez; `runs` invalidate; 409 `EVAL_RUN_IN_PROGRESS`'te overview invalidate |
| 3, 7a: kutular | `components/EvalKpis.tsx` + `.module.css` | Lab `.kpis`: recall@5 (2 hane; ≥ 0,95 yeşil `good`, değilse kırmızı `bad`), MRR (2 hane), Kaçan (> 0 kırmızı, 0 yeşil), Retrieval (16 px, `strategyText` + `retrievalSubtitle`). 620 px altı 2 sütun (`@container`) |
| 7e: Çalıştırmalar | `components/EvalRunTable.tsx` | Sütunlar Çalıştırma, Retrieval, Rehber, recall@5, MRR, Saat; 3 hane; `RUNNING` → `ölçülüyor…` / `–`, `FAILED` → `başarısız` / `–`; Saat `formatListTime`. Altında `Pagination` (`aria-label="Çalıştırma sayfaları"`, sadece `totalPages > 1`) |
| 7b, 7c: Kaçanlar tablosu | `components/MissedQuestionTable.tsx` (+ `.module.css`) | Sütunlar Soru, Vector #, Prompt'taki yeri; `20+` (`candidates` + "+"), `StatusBadge ok` "N. sıra" / `bad` "yok"; boşsa tek satır metin. Satır `title="Önizlemede sor"`, tıklanınca `navigate('/onizleme', { state: { question } })`; soru metni satır içindeki düğmededir (klavye yolu, TurnChunkTable kalıbı) |
| 4a, 4b, 1a, 5b, 7d, sayfa | `pages/EvaluationPage.tsx` + `.module.css` | Düğme `PageHeader actions`'ta; disabled: live-config yok, `LLM_ONLY`, overview yok, `goldenSet === null`, `runningRun !== null`, mutation sürüyor. Sıra: Sadece LLM uyarısı → (live-config hatası) → set yok bilgisi → başlatma hatası (`ErrorAlert`) → son biten `FAILED` ise not → kutular ya da "Henüz ölçüm yok." → Çalıştırmalar (liste boş değilse ya da `lastDoneRun` varsa) → Kaçanlar (`lastDoneRun` varsa). Başlık açıklaması set varken `questionCount` ile |
| Durum duyurusu | `hooks/useEvalRunAnnouncement.ts` | `aria-live="polite"` görünmez bölge: `runningRun` kimliği değişince "Ölçüm başladı." / "Ölçüm tamamlandı." / "Ölçüm tamamlanamadı." (render sırasında state ayarı; effect yok) |
| Hata metinleri | `shared/api/errorMessages.ts` | `GOLDEN_SET_NOT_LOADED`, `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE`, `EVAL_RUN_IN_PROGRESS`, `EVAL_NOT_AVAILABLE_IN_LLM_ONLY` (planla birebir) |
| 8: Önizleme sarı | `features/preview/model/types.ts`, `model/answerStreamEvents.ts`, `components/ContextChunksCard.tsx` + `.module.css`, `index.css` | `ContextChunk.containsAnswer: boolean`; doğrulayıcı boolean değilse `sources` olayını geçersiz sayar. `containsAnswer` parçaya `gold` sınıfı (kenar `--color-gold-line` #e8cf91, zemin `--color-gold-bg` #fffaf0; lab'daki gibi hover'da da altın kalır). Alt satır: "✓ atıf yapılan", en az bir parça işaretliyse " · sarı: cevabın geçtiği parça" |
| 7c, K9: Değerlendirme → Önizleme | `features/preview/model/types.ts` (`PreviewNavigationState`), `model/previewNavigation.ts` (`previewNavigationStateOf(unknown)`), `pages/PreviewPage.tsx` | `useLocation().state` `{ question: string }` ise effect `setTimeout(…, 0)` kurar, temizlikte `clearTimeout`. Zamanlayıcı `useEffectEvent` ile `askInNewConversation(question)` çağırır, ardından `navigate(pathname, { replace: true, state: null })`. Effect bağımlılıkları `[question, location.key]` |
| 8: Konuşmalar | `features/conversation/model/types.ts`, `model/turnStatus.ts`, `model/conversationTexts.ts`, `components/TurnChunkTable.tsx` | `TURN_STATUSES`'a `MISSED`, `GENERATION_ERROR` (sözleşme sırası); etiketler `KAÇIRDI` / `ÜRETİM HATASI`, ton `bad` (lab `cls:'bad'`). `TurnChunkResponse.containsAnswer`; Parça hücresinde düğmeden sonra `<StatusBadge tone="run">cevap burada</StatusBadge>` |
| Ortak bileşen | `shared/components/Callout.tsx` | `hasIcon?: boolean` (varsayılan `true`). "Henüz ölçüm yok." lab'da ikonsuz `.callout` olduğu için `hasIcon={false}` |
| Token'lar | `index.css` | `--color-gold-line`, `--color-gold-bg`, `--text-kpi-value` 20px, `--text-kpi-strategy` 16px, `--radius-kpi` 10px |
| Doküman | `web/CLAUDE.md` | Durum yorumu (TASK-016), klasör ağacına `evaluation/` ve `previewNavigation`, Callout `hasIcon` |

## Değişen dosyalar
Yeni:
- `web/src/features/evaluation/api/evaluationApi.ts`, `api/evaluationQueries.ts`
- `web/src/features/evaluation/components/EvalKpis.tsx`, `EvalKpis.module.css`, `EvalRunTable.tsx`, `MissedQuestionTable.tsx`, `MissedQuestionTable.module.css`
- `web/src/features/evaluation/hooks/useEvalRunAnnouncement.ts`
- `web/src/features/evaluation/model/types.ts`, `evaluationRules.ts`, `evaluationLabels.ts`, `evaluationTexts.ts`
- `web/src/features/evaluation/pages/EvaluationPage.module.css`
- `web/src/features/preview/model/previewNavigation.ts`

Değişen:
- `web/src/features/evaluation/pages/EvaluationPage.tsx` (baştan yazıldı)
- `web/src/features/preview/model/types.ts`, `model/answerStreamEvents.ts`, `components/ContextChunksCard.tsx`, `components/ContextChunksCard.module.css`, `pages/PreviewPage.tsx`
- `web/src/features/conversation/model/types.ts`, `model/turnStatus.ts`, `model/conversationTexts.ts`, `components/TurnChunkTable.tsx`
- `web/src/shared/api/errorMessages.ts`, `web/src/shared/components/Callout.tsx`, `web/src/index.css`
- `web/CLAUDE.md`

Silinen: yok. Eklenen paket: yok.

## Ekran metinleri
**Değerlendirme (`/degerlendirme`)**
- Başlık: "Değerlendirme"; `<title>`: "Değerlendirme · Raşit Bankası Dijital Asistan Yönetimi".
- Açıklama, set varken: "Rehberde cevabı olan {questionCount} soru; her birinde cevabın geçtiği parça işaretli. Ölçüm retrieval'ı çalıştırır ve doğru parçanın modele giden 5 parçaya girip girmediğine bakar." Set yokken: "Rehberde cevabı olan sorular; her birinde …" (aynı devam).
- Düğme: "Ölçümü çalıştır · rerank 20→5" / "Ölçümü çalıştır · top-5" (live-config'ten; yüklenmeden "Ölçümü çalıştır"). Ölçüm sürerken "Ölçülüyor · {processedCount}/{measuredCount}" (ör. "Ölçülüyor · 37/100"), disabled.
- Uyarı (Sadece LLM): "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al."
- Set yok: "Golden set yüklenmemiş."
- Ölçüm yok: "Henüz ölçüm yok. Mevcut retrieval ayarıyla çalıştır; sonra ayarı değiştirip tekrar çalıştırarak iki satırı karşılaştır."
- Başarısız son ölçüm: "Son ölçüm tamamlanamadı: bilgi bankasına ulaşılamadı." (RETRIEVAL_UNAVAILABLE) / "… servis ölçüm sırasında yeniden başladı." (EVAL_RUN_INTERRUPTED) / "… beklenmeyen bir hata oluştu." (INTERNAL_ERROR).
- Kutular: "recall@5" + "{hit}/{measured} soru" (+ " · {unmatched} soru ölçülemedi"); "MRR" + "doğru parçanın sırası"; "Kaçan" + "doğru parça prompt'ta yok"; "Retrieval" + "rehber {sourceVersions}" (+ " · reranker DOWN: {n} soru").
- Bölüm başlıkları (h2): "Çalıştırmalar"; "Kaçanlar ve sırası değişenler" + yanında "son çalıştırma".
- Çalıştırmalar sütunları: Çalıştırma, Retrieval, Rehber, recall@5, MRR, Saat. Hücreler: "ev_12", "rerank 20→5 · reranker DOWN (2)", "0,947", "ölçülüyor…", "başarısız", "–".
- Kaçanlar sütunları: Soru, Vector #, Prompt'taki yeri. Hücreler: "20+", "2. sıra", "yok". Boş: "Bütün sorularda doğru parça vector sırasıyla aynı yerde." Satır `title`: "Önizlemede sor"; soru metni bir `button`'un adıdır.
- Sayfalama: `nav` `aria-label="Çalıştırma sayfaları"`, "Önceki" / "Sonraki".
- Canlı bölge: "Ölçüm başladı." / "Ölçüm tamamlandı." / "Ölçüm tamamlanamadı."
- Yükleniyor: "Yükleniyor…".
- `role="alert"` hata metinleri (`messageFor`): `GOLDEN_SET_NOT_LOADED` "Golden set yüklenmemiş. Önce golden set'i yükle."; `GOLDEN_SET_NOT_IN_KNOWLEDGE_BASE` "Golden set'in belgesi bilgi bankasında yok. Önce belgeyi yükle."; `EVAL_RUN_IN_PROGRESS` "Bir ölçüm zaten sürüyor. Bitmesini bekle."; `EVAL_NOT_AVAILABLE_IN_LLM_ONLY` "Sadece LLM modunda arama yok; ölçmek için üretimi RAG'e al."; `RETRIEVAL_UNAVAILABLE` "Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene."

**Önizleme:** parça kartı alt satırı "✓ atıf yapılan" ya da "✓ atıf yapılan · sarı: cevabın geçtiği parça".
**Konuşmalar:** durum çipleri "KAÇIRDI", "ÜRETİM HATASI"; parça tablosunda "cevap burada".

## Plandan sapmalar
- Plandan sapma yok. İki küçük ekleme, planla çelişmiyor:
  - `evaluationKeys`'e `runsAll` önek anahtarı eklendi. Böylece `runs` bütün sayfalarıyla tek çağrıda invalidate ediliyor.
  - `Callout`'a `hasIcon` prop'u eklendi. Sebebi: lab'daki "Henüz ölçüm yok." kutusu ikonsuz.
- Live-config okunamazsa düğme kapalı kalır ve `ErrorAlert` görünür. Bu, Önizleme'deki davranışın aynısı.

## Doğrulama
- `cd web && npm run lint` → **geçti** (oxlint, 0 uyarı / 0 hata).
- `cd web && npm run build` → **kaldı. Hatalar sadece tester dosyalarında**, uygulama kodunda tip hatası yok:
  - `src/test/conversationFixtures.ts(29,3)`: `TurnChunkResponse.containsAnswer` eksik.
  - `src/test/previewFixtures.ts(6,3)`: `ContextChunk.containsAnswer` eksik.
  - İki alan da sözleşmede zorunlu. Fixture'lar bunu henüz içermiyor; `tsconfig.app.json` `src/test`'i de kapsadığı için build kırılıyor.
- Uygulama kodu tek başına (test dosyaları hariç, geçici tsconfig ile `tsc --noEmit`) → **geçti**, çıkış 0. `npx vite build` → **geçti**.
- `cd web && npm test` → 587 testten **30'u kaldı**, 557'si geçti. Kalanların hepsi Önizleme'de: `answerStreamEvents.test.ts`, `PreviewPage.test.tsx`, `PreviewPage.suggestions.test.tsx`, `PreviewPage.feedback.test.tsx`.
  - Sebep: `previewFixtures.contextChunk()`'ta `containsAnswer` yok. Plan gereği doğrulayıcı bu durumda `sources` olayını geçersiz sayıyor (W14 ve sözleşme).
- Teyit: web, scratchpad'e kopyalandı ve kopyada sadece iki fixture'a `containsAnswer: false` eklendi. Gerçek test dosyalarına dokunulmadı. Kopyada:
  - `tsc -b` → geçti.
  - `vitest run` → **35 dosya, 587/587 geçti**.
- Gerçek sistemde tarayıcıyla bakılmadı; backend paralel yazılıyor. Lab'la görsel karşılaştırma qa'da (K5).

## Tester için notlar
- **İlk iş fixture:** `src/test/previewFixtures.ts` `contextChunk()` ve `src/test/conversationFixtures.ts` `turnChunk()` varsayılanlarına `containsAnswer: false` eklenmeli. Bu eklenince build ve mevcut 587 test geçiyor (scratchpad'de denendi).
- **MSW handler'ı gereken uçlar:** `GET /api/assistant/evals/overview`, `GET /api/assistant/evals?page=&size=` (sayfa boyutu 20), `POST /api/assistant/evals` (202 + `EvalRunResponse`), `GET /api/assistant/live-config`.
  - Değerlendirme sayfası bu uçlara her açılışta gider.
  - W8'de Önizleme'ye geçince `POST /api/assistant/conversations` ve `POST …/messages` (SSE) de gerekir; öneriler için `GET /api/assistant/suggestions` var.
- **Yoklama (W9):**
  - Overview, `runningRun` doluyken her 1000 ms'de yeniden çekilir, `runningRun: null` gelince durur.
  - Başlatma sonrası ilk "Ölçülüyor · 0/3" metni POST'un 202 gövdesinden gelir; overview'ın yeniden çekilmesi beklenmez.
  - `runningRun` doludan `null`'a dönünce `GET /evals` bir kez daha çekilir.
  - Çalıştırmalar listesi de ölçüm sürerken 1 sn'de bir çekilir.
  - Gerçek zamanlayıcıyla 1 sn bekleme gerekir; `findBy*` varsayılan 1000 ms zaman aşımı sınırda kalabilir, `timeout` artırılabilir.
- **Renk sınıfları (W5):** CSS Module sınıf adları `good` (yeşil) ve `bad` (kırmızı); kutudaki değer `<b>` öğesidir. Önizleme'de işaretli parça düğmesinin sınıfı `gold`. Vitest'in varsayılan "stable" CSS Modules adlandırması özgün adı içerir (ör. `_bad_xxxx`).
- **W8 (StrictMode):** satıra ya da satırdaki soru düğmesine tıklamak `/onizleme`'ye `state: { question }` ile geçer. PreviewPage soruyu bir `setTimeout(0)` sonrasında bir kez sorar ve state'i `replace` ile siler. Test router'ı `MemoryRouter` + `Routes` içinde iki sayfayla kurulmalı. Ayrıca `initialEntries`'e `{ pathname: '/onizleme', state: { question } }` verilerek Önizleme tek başına da test edilebilir.
- **W11:** başlatma hatası `role="alert"` içinde `messageFor(errorCode)` metniyle görünür ve bir sonraki tıklamaya kadar kalır. `EVAL_RUN_IN_PROGRESS`'te overview de yeniden çekilir.
- **W12:** başarısızlık notu, `lastFinishedRun.status === 'FAILED'` iken görünür (`lastDoneRun` ayrıca doluysa kutular da görünür).
- **Düğme disabled durumları:** live-config yüklenmemiş ya da hatalı, `LLM_ONLY`, overview yüklenmemiş, `goldenSet: null`, `runningRun` dolu, POST sürüyor.
- **Görünürlük kuralları:**
  - Çalıştırmalar bölümü `lastDoneRun` yokken de `totalElements > 0` ise görünür.
  - Kaçanlar bölümü sadece `lastDoneRun` varken görünür.
  - "Henüz ölçüm yok." `lastDoneRun === null` iken görünür, süren ölçüm olsa bile.
- **Saat sütunu:** `formatListTime(startedAt, now)`; `now` sayfa açılışında bir kez alınır.

## Düzeltme
Yok.

STATUS: DONE
