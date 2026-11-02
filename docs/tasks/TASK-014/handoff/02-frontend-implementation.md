TASK: TASK-014
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `frontend-design` (lab `.tbl`, `.st`, `.turn`, `.callout.w`, `.side .cnt` aktarımı, boş durum), `frontend-a11y` (tıklanabilir satırlar, tablo başlıkları), `react-state-management` (polling, invalidation), `frontend-security` (cevap düz metin, `dangerouslySetInnerHTML` yok).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Karar 20, F21: `answerFormat` taşıması | `shared/turn/answerFormat.ts` (`git mv`, içerik aynı) | Eski yer: `features/preview/model/answerFormat.ts` |
| Karar 20: `AnswerText` | `shared/turn/AnswerText.tsx`, `AnswerText.module.css` | Props `{ text, contextSize, onCitation }`; paragraf, liste, kalın, `aria-label="Kaynak {n}"` alıntı düğmesi. `.cite` stili `AnswerMessage.module.css`'ten buraya taşındı |
| `AnswerMessage` `AnswerText`'i kullanır | `features/preview/components/AnswerMessage.tsx`, `.module.css` | Yerel `Inlines` silindi; görünüm ve davranış aynı |
| Karar 11: `conversationKeys` | `shared/conversation/conversationKeys.ts` | `all`, `list(params)`, `count`, `detail(id)` |
| Karar 15: `formatListTime` | `shared/format/formatDateTime.ts` | Europe/Istanbul'da aynı günse `HH:mm`, değilse `dd.MM.yyyy HH:mm` |
| Plan: Önizleme akışı sorguları yeniler | `features/preview/hooks/useAnswerStream.ts` | `refreshLiveConfig` → `refreshAfterTurn`: `liveConfigKeys.all` ile birlikte `conversationKeys.all` invalidate edilir. Bu `done` ve `error` olayında, akış öncesi `ApiError`'da da olur |
| Tipler (sözleşme ile birebir) | `features/conversation/model/types.ts` | `TURN_STATUSES`, `RetrievalStrategy`, `TurnErrorCode`, `HandoffReason`, `ConversationSummaryResponse`, `ConversationDetailResponse`, `InProgressTurnResponse`, `TurnResponse`, `TurnChunkResponse`, `HandoffResponse` (preview'dan import edilmez) |
| Kurallar | `model/conversationRules.ts` | `CONVERSATION_PAGE_SIZE 20`, `CONVERSATION_POLL_INTERVAL_MS 2000`, `FIRST_QUESTION_MAX_LENGTH 60`, `DISTANCE_FRACTION_DIGITS 4`, `RERANK_FRACTION_DIGITS 3` |
| K5: durum etiketleri | `model/turnStatus.ts` | `TURN_STATUS_LABELS`, `turnStatusTone` (plandaki ton eşlemesi) |
| K3d, Karar 14, 16, 17 | `model/turnLabels.ts` | `conversationLabel`, `channelLabel`, `generationLabel`, `guideVersionLabel`, `failureReasonLabel`, `hasDistinctSearchQuery` |
| Ekran metinleri | `model/conversationTexts.ts` | Plandaki sabitler birebir |
| API | `api/conversationApi.ts` | `listConversations(params)`, `getConversationDetail(id)` |
| Karar 12, 13 (K2c, K4, G5) | `api/conversationQueries.ts` | `useConversationsQuery`: herhangi bir satır `IN_PROGRESS` ise 2000 ms polling, `keepPreviousData`. `useConversationDetailQuery`: `inProgressTurn` doluyken polling. 4xx hatada ve veri yokken polling yok. `useConversationCountQuery`: `page=0&size=1` → `select: totalElements` |
| K2a, K2b, K3h (liste) | `components/ConversationTable.tsx`, `.module.css` | Lab `.tbl` + `tr.click`. Sütunlar: Konuşma (`cnv_<id>` `Link`), İlk soru (60), Tur, Durum (+ "Temsilcide" çipi), Kanal, Saat. Satıra tıklayınca detaya gider; klavyede yol `Link`'tir (SourceTable kalıbı) |
| K1b | `components/ConversationEmptyState.tsx`, `.module.css` | Lab'da karşılığı yok. Lab `.callout` diliyle başlık, metin ve "Önizleme'ye git" bağlantısı |
| K3a-K3f, K3h, G3 | `components/TurnCard.tsx`, `turnCard.module.css` | Lab `.turn` (`.q`, `.rq`, `.a`, `.nums`). Durum çipi, üretim, rehber (varsa), prompt/cevap token (null değilse), süre, geri bildirim (varsa), `INCOMPLETE` için sebep. Altında tablo ya da uyarı (`LLM_ONLY` ve RAG-parçasız) |
| K4 | `components/InProgressTurnCard.tsx` | `{turnNo}. {question}`, `…`, `işleniyor` çipi |
| K3e, K3g | `components/TurnChunkTable.tsx`, `.module.css` | Başlıklar Prompt, Parça, Vector #, Mesafe, [Rerank], Atıf. Rerank sütunu sadece `RERANK` turunda. Atıflı satırda yeşil `✓` (lab `.used`). Satıra ya da Parça düğmesine (klavye) tıklayınca parça penceresi açılır |
| K3h (aktarma) | `components/HandoffLine.tsx` | Callout (info): `Temsilciye aktarıldı · {queue} kuyruğu · {HH:mm} · sebep: cevap yok` |
| Liste sayfası | `pages/ConversationListPage.tsx` | PageHeader (mevcut metin). Yükleniyor / hata / boş durum / tablo; `totalPages > 1` ise sayfalama ("Konuşma sayfaları") |
| K3a, K6, G2 | `pages/ConversationDetailPage.tsx`, `pages/ConversationPages.module.css` | Id `^\d+$` değilse istek atılmadan bulunamadı sayfası gösterilir; `CONVERSATION_NOT_FOUND` de aynı sayfayı açar. Breadcrumb "Konuşmalar › cnv_<id>", `h1` `cnv_<id>`, alt başlık `{Kanal} · {saat} · {N} tur` (N = kayıtlı + süren). Ardından HandoffLine, turlar, süren tur; tur yoksa "Bu konuşmada henüz tur yok.". Alıntı ve tablo satırı için tek `ChunkDialog` |
| Rota | `app/App.tsx` | `konusmalar/:conversationId` |
| K2c | `app/MainNav.tsx`, `MainNav.module.css` | `/konusmalar` `end: false`; `NavItem.count?`; Konuşmalar yanında lab `.side .cnt` rozeti. Veri yokken ya da hatada rozet gösterilmez |
| Callout uyarı tonu | `shared/components/Callout.tsx`, `.module.css` | `tone?: 'info' \| 'warning'`. `warning` = lab `.callout.w` (lab'daki gibi ikonsuz). Varsayılan `info`, mevcut kullanım değişmez |
| Token'lar | `index.css` | `--color-callout-warning-line` (#ecd7a8), `--color-callout-warning-ink` (#5f4410), `--color-nav-count-bg` (#efe8eb), `--color-search-query-code-bg` (#f3eef0), `--text-nav-count` (10.5px), `--text-turn-answer` (13.5px), `--line-height-turn-answer` (1.5), `--size-turn-answer-rule` (3px), `--radius-nav-count` (9px). Hepsi lab'dan |
| `web/CLAUDE.md` | `web/CLAUDE.md` | Durum yorumu, klasör ağacı (conversation, shared/turn, shared/conversation, Callout tonu, formatListTime), rota ve `end` notu, sayaç |

## Değişen dosyalar
- **Yeni:** `web/src/shared/turn/AnswerText.tsx`, `web/src/shared/turn/AnswerText.module.css`, `web/src/shared/conversation/conversationKeys.ts`, `web/src/features/conversation/model/{types,conversationRules,turnStatus,turnLabels,conversationTexts}.ts`, `web/src/features/conversation/api/{conversationApi,conversationQueries}.ts`, `web/src/features/conversation/components/{ConversationTable.tsx,ConversationTable.module.css,ConversationEmptyState.tsx,ConversationEmptyState.module.css,TurnCard.tsx,turnCard.module.css,InProgressTurnCard.tsx,TurnChunkTable.tsx,TurnChunkTable.module.css,HandoffLine.tsx}`, `web/src/features/conversation/pages/{ConversationDetailPage.tsx,ConversationPages.module.css}`
- **Taşınan:** `web/src/features/preview/model/answerFormat.ts` → `web/src/shared/turn/answerFormat.ts`
- **Değişen:** `web/src/app/App.tsx`, `web/src/app/MainNav.tsx`, `web/src/app/MainNav.module.css`, `web/src/features/conversation/pages/ConversationListPage.tsx`, `web/src/features/preview/components/AnswerMessage.tsx`, `web/src/features/preview/components/AnswerMessage.module.css`, `web/src/features/preview/hooks/useAnswerStream.ts`, `web/src/index.css`, `web/src/shared/components/Callout.tsx`, `web/src/shared/components/Callout.module.css`, `web/src/shared/format/formatDateTime.ts`, `web/CLAUDE.md`
- **Paket:** yok.

## Ekran metinleri
- **Liste:** başlık `Konuşmalar`; sütunlar `Konuşma`, `İlk soru`, `Tur`, `Durum`, `Kanal`, `Saat`; hücre `cnv_<id>` (bağlantı); kanal `Önizleme`; çip `Temsilcide`; sayfalama `aria-label="Konuşma sayfaları"` (`Önceki` / `Sonraki`); `Yükleniyor…`.
- **Boş durum:** başlık (`h2`) `Henüz konuşma yok`; metin `Önizleme'de asistana bir soru sorduğunda konuşma burada görünür; her tur modele giden parçalarla birlikte saklanır.`; bağlantı `Önizleme'ye git` → `/onizleme`.
- **Durum çipleri:** `işleniyor`, `TAMAMLANMADI`, `DAYANAKSIZ`, `BİLMİYOR`, `HATALI`, `DAYANAKLI`.
- **Detay:** breadcrumb bağlantısı `Konuşmalar` › `cnv_<id>`; `h1` `cnv_<id>`; alt başlık `Önizleme · 13:31 · 2 tur`; tur sorusu `{n}. {soru}`; `Arama sorgusu: <code>…</code>`; bilgi satırı `üretim <b>RAG · rerank 20→5</b>`, `rehber <b>2026.10</b>`, `prompt <b>1.234</b> token`, `cevap <b>87</b> token`, `süre <b>3,42 sn</b>`, `geri bildirim <b>Evet|Hayır</b>`, `sebep <b>dil modeline ulaşılamadı|bilgi bankasına ulaşılamadı|cevap yarıda kesildi|beklenmeyen hata</b>`; üretim etiketleri `Sadece LLM`, `RAG · top-5`, `RAG · rerank 20→5`, `RAG · reranker DOWN`; cevap yoksa `Cevap üretilmedi.`; süren tur cevabı `…`.
- **Parça tablosu:** başlıklar `Prompt`, `Parça`, `Vector #`, `Mesafe`, `Rerank` (sadece `RERANK`), `Atıf`; hücreler `[1]`, `s. 19 · #31` (düğme, `title` = parça başlığı), `7`, `0,3124`, `5,360` (rerank null ise `–`), `✓`.
- **Uyarılar:** `Bu turda modele hiçbir parça gitmedi (Sadece LLM).`, `Bu turda modele hiçbir parça gitmedi.`, `Bu konuşmada henüz tur yok.`
- **Aktarma:** `Temsilciye aktarıldı · bireysel kuyruğu · 13:31 · sebep: cevap yok`.
- **Bulunamadı:** `h1` `Konuşma bulunamadı.`, bağlantı `Konuşmalar'a dön` → `/konusmalar`; sayfa başlığı `Konuşma bulunamadı · Raşit Bankası Dijital Asistan Yönetimi`.
- **Hatalar:** `ErrorAlert` → `messageFor(errorCode)` (500 → `Beklenmeyen bir hata oluştu. Tekrar dene.`). Parça penceresi `CHUNK_NOT_FOUND` → mevcut `Parça bulunamadı.`.
- **Yan menü:** `Konuşmalar` bağlantısının içinde sayı (`<span>`); erişilebilir ad `Konuşmalar 3`.

## Plan dışı karar ve sapmalar
- **`answerFormat.test.ts` (dosya sahipliği):** `tsconfig.app.json` `include: ["src"]` test dosyalarını da tip kontrolüne alıyor. Bu yüzden `answerFormat.ts` taşınınca eski yerdeki `features/preview/model/answerFormat.test.ts` (`./answerFormat.ts` import'u) `npm run build`'i kırıyor. Lead'e iki seçenek sundum: (A) test dosyasını içeriğine dokunmadan `git mv` ile ben taşırım, (B) tester taşıyana kadar build kırık kalır. Lead kararı **B** oldu: `answerFormat.ts` taşındı, test dosyasına dokunulmadı (dosya sahipliği kuralı istisnasız). Tester ilk iş olarak test dosyasını `src/shared/turn/answerFormat.test.ts`'e taşıyacak (göreli import `./answerFormat.ts` aynı kalır) ve `listConversations` için MSW varsayılan handler'ını ekleyecek. Lead bunu tester'a iletecek.
- **Saat (`now`) değeri:** `formatListTime`'ın "bugün mü?" karşılaştırması için `now` render sırasında `new Date()` ile değil, `useState(() => new Date())` ile mount anında alınıyor (oxlint `react(purity)` uyarısı yüzünden). Sayfa gece yarısını geçerek açık kalırsa saat biçimi yeniden mount olana kadar eski günü baz alır.
- **HandoffLine'ın yeri:** Plandaki sırayla sayfa başlığının altında, turlardan önce duruyor. Görünüm `Callout` (info); lab'da karşılığı yok.
- **Uyarı callout'u ikonsuz:** Lab `.callout.w` örneklerinin ikisi de ikonsuz, bu yüzden `Callout tone="warning"` ikon çizmez.
- **Boş durum:** Plandaki gibi `Callout` dili kullanıldı: brand-soft zemin, ikon, `h2` başlık, metin ve buton biçimli bağlantı. Başlık `span` içine giremediği için `Callout` bileşeni değil, aynı token'larla ayrı bir CSS Module kullanıldı.

## Doğrulama
- `cd web && npm run lint` → 0 uyarı, 0 hata (çıkış kodu 0).
- `cd web && npx vite build` → başarılı.
- **Özet (karar B, kullanıcı onaylı):** `npm run build` sadece eski yerdeki `answerFormat.test.ts` import'u yüzünden kırık; dosyayı tester taşıyacak.
- `cd web && npm run build` (`tsc -b && vite build`) → **kaldı**. `tsc -b` tek hata veriyor, bu yüzden `vite build` adımına geçilmiyor:
  ```
  src/features/preview/model/answerFormat.test.ts(2,30): error TS2307: Cannot find module './answerFormat.ts' or its corresponding type declarations.
  ```
  `npx tsc -b 2>&1 | grep -c "error TS"` → `1`. Sebep: test dosyası henüz taşınmadı (lead kararı B, yukarıya bakın). Tester dosyayı taşıyınca build geçmelidir.
- **Geri kalan kodda tip hatası olmadığını gösteren kontrol:** `tsconfig.app.json`'u extend eden geçici bir config ile (scratchpad'de; `include: web/src`, `exclude`'da sadece `features/preview/model/answerFormat.test.ts`) `npx tsc -p <geçici config>` → çıkış kodu 0, hata yok. Kapsam: diğer bütün uygulama ve test dosyaları.
- `cd web && npx vite build` → başarılı (`✓ built`).
- `cd web && npm test` → 30 dosya: 26 geçti, 4 kaldı. 496 test: 488 geçti, 8 kaldı. Hepsi bu task'ın beklenen etkisi ve tester'ın işi:
  1. `features/preview/model/answerFormat.test.ts`: dosya yeni yerine taşınmadı.
  2. `app/AdminLayout.test.tsx` (2 test): `MainNav` artık `useConversationCountQuery` kullandığı için `QueryClientProvider` istiyor ("No QueryClient set").
  3. `app/App.test.tsx` `givenUnknownPath_…` `/konusmalar/12` parametresi: bu adres artık gerçek bir rota, "Sayfa bulunamadı" beklentisi geçersiz.
  4. `features/source/pages/SourceDetailPage.test.tsx` `givenNonNumericId_…` (5 parametre): yan menüdeki sayaç `GET /api/assistant/conversations?page=0&size=1` isteği atıyor, bu da testin istek kaydında 1 istek olarak görünüyor (beklenen 0). Testin sadece `/api/workspaces` isteklerini sayması ya da MSW varsayılan handler'ı gerekiyor.
- `grep -rn "dangerouslySetInnerHTML\|innerHTML\|eval(\|new Function" web/src` → boş.

## Tester için notlar
- **İlk iş (lead kararı B):** `git mv web/src/features/preview/model/answerFormat.test.ts web/src/shared/turn/answerFormat.test.ts`. Dosyanın içeriği değişmez. Bunun ardından `npm run build` geçmelidir.
- **MainNav notu:** `app/MainNav.tsx` artık `useConversationCountQuery` ile `GET /api/assistant/conversations?page=0&size=1` çağırıyor. Bu yüzden `MainNav`'ı render eden her test (`App.test.tsx`, `AdminLayout.test.tsx`, `AdminLayout`'u kullanan sayfa testleri) hem `QueryClientProvider` hem de bu uç için bir MSW handler'ı ister; MSW `onUnhandledRequest: 'error'` ayarlı. İstek kaydı tutan testler (ör. `SourceDetailPage.test.tsx` `givenNonNumericId_…`) bu isteği de sayar. Bugün kırılan 7 test bu sebepten kırılıyor; 8.'si `App.test.tsx`'teki `/konusmalar/12` parametresidir (adres artık gerçek rota).
- **MSW varsayılan handler'ları (plan):** `GET /api/assistant/conversations` boş sayfa (`{content: [], page: 0, size: N, totalElements: 0, totalPages: 0}`); `GET /api/assistant/conversations/:conversationId` fixture. Sayaç ayrı bir istektir: `page=0&size=1`. Liste sayfası `page=0&size=20` ister. İkisi aynı yola gider; handler query'ye bakmazsa sayaç `totalElements`'i listeyle aynı olur.
- `MainNav` (yani `AdminLayout` ve `App`) artık `QueryClientProvider` gerektirir; `renderWithProviders` kullanılmalıdır.
- **Polling (F4):** Liste herhangi bir satır `lastTurnStatus: 'IN_PROGRESS'` iken, detay `inProgressTurn !== null` iken 2000 ms'de bir yeniden ister. İlk cevap `IN_PROGRESS`, ikincisi `GROUNDED` dönen bir handler ile ikinci cevaptan sonra yeni istek gelmez. Sahte zamanlayıcı yerine `findBy*` ile 2 sn beklenebilir; `CONVERSATION_POLL_INTERVAL_MS` `model/conversationRules.ts`'tedir.
- **Sayaç yenileme (F20):** `useAnswerStream` `done`/`error` olayında `conversationKeys.all`'ı invalidate eder; sayaç sorgusu (`['conversations','count']`) bu yüzden yeniden istek atar.
- **Saat (F1, F7):** `formatListTime` "bugün"ü `now` ile karşılaştırır; `now` bileşen mount olurken alınır, bu yüzden `vi.setSystemTime` render'dan önce çağrılmalıdır. Liste satırı, alt başlık ve aktarma satırı Europe/Istanbul saatidir (`2026-11-02T10:31:05Z` → `13:31`).
- **Satır tıklama (F3):** Liste satırının herhangi bir yerine tıklamak `/konusmalar/<id>`'ye gider; klavyede yol `cnv_<id>` bağlantısıdır (Enter). Parça tablosunda klavye yolu `s. N · #M` düğmesidir, satıra tıklamak da pencereyi açar.
- **Alıntı (F11):** Cevaptaki `Kaynak 1` düğmesi `contextChunks`'ta `n === 1` olan parçanın `chunkId`'siyle `GET /api/workspaces/42/chunks/{chunkId}` ister. Sadece LLM turunda alıntılar düğmeye çevrilmez (`contextSize null`).
- **Bulunamadı (F16):** `/konusmalar/abc` istek atmaz. Ancak yan menü sayacı yine `GET /api/assistant/conversations?page=0&size=1` atar; "istek yok" kontrolü detay yoluna bakmalıdır.
- **Hatalı tur (F14):** `TAMAMLANMADI` çipinin tonu `neutral`. `promptTokens`/`completionTokens` null ise `token` öğeleri hiç çizilmez. `RETRIEVAL_UNAVAILABLE` turunda (`answer: null`, `contextChunks: []`, `RAG`) `Cevap üretilmedi.` ve `Bu turda modele hiçbir parça gitmedi.` görünür.
- **Rehber sürümü:** Parçaların sürümleri tekrarsız ve virgülle birleştirilir. Parça yoksa `rehber` öğesi çizilmez.

## Düzeltme

STATUS: DONE
