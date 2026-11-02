TASK: TASK-011
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `react-state-management` (yoklama, invalidate), `frontend-a11y` (şerit `role="group"`), `frontend-design` (lab `.cfg` / `.side2>.cfg` stillerinin token'a aktarılması).

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Kararlar #16, K3 – `shared/liveConfig/` | `shared/liveConfig/types.ts`, `liveConfigApi.ts`, `liveConfigQueries.ts`, `liveConfigRules.ts` | `GENERATION_MODES`, `RERANKER_STATUSES`, `KNOWLEDGE_BASE_STATUSES` + type'lar; `KnowledgeBaseSource`, `KnowledgeBaseResponse`, `LiveConfigResponse` (`retrievalStrategy: 'TOP_K' \| 'RERANK'`) openapi ile birebir. `getLiveConfig()` → `GET /api/assistant/live-config`. `liveConfigKeys.all = ['liveConfig']`, `useLiveConfigQuery()` `refetchInterval: LIVE_CONFIG_POLL_INTERVAL_MS` (15 000; kart soru 2 = C) |
| SSE tipleri (sse.md) | `features/preview/model/types.ts` | `RETRIEVAL_STRATEGIES`'e `'NONE'`; `SourcesEvent` ilk alan `generationMode: GenerationMode` (shared'dan), `candidates`/`topK: number \| null`, `searchQuery: string \| null`; `DoneEvent.retrievalLatencyMs: number \| null` |
| Olay doğrulama (F8) | `model/answerStreamEvents.ts` | `isSourcesEvent` `generationMode`'u bilinen değer, `candidates`/`topK` nullable integer, `searchQuery` nullable string olarak kontrol eder; `isDoneEvent` `retrievalLatencyMs` nullable. `generationMode` yoksa ya da bilinmiyorsa olay düşer |
| Strateji etiketi | `model/strategyLabels.ts` | `NONE` → `RETRIEVAL_OFF_LABEL = 'kapalı (Sadece LLM)'`; `REWRITTEN` + `searchQuery === null` → `null` (bkz. Plandan sapmalar: null daraltması) |
| Süre satırı (F11) | `model/latencyRows.ts` | `retrievalLatencyMs === null` → "retrieval-api araması" satırı yok |
| K4, K3c, kart soru 1 = A (F1) | `model/liveConfigItems.ts` | Beş öğe sırayla: Model / Embedding (`?? '—'`) / Retrieval / Soru yeniden yazma / Bilgi kaynakları. Tonlar `normal`/`warning`/`danger`. `KNOWLEDGE_BASE_NAME_SHORT_LENGTH = 40` (`shortenText`: 39 harf + `…`). `READY`+1 → `ad · sürüm`; `READY`+≥2 → `{searchableSourceCount} belge`; `EMPTY` → `belge yok` warning; `UNAVAILABLE` → `ulaşılamıyor` warning |
| K6, Kararlar #10-12 (F2) | `model/howItWorks.ts` | Üç varyant; `emb` = `embeddingModel === null ? 'pgvector' : '{embeddingModel} + pgvector'`. LLM_ONLY metni lab'ın aynısı; RERANK (DOWN değil) ve TOP_K/RERANK+DOWN metinleri karar 4'teki ek'siz biçim |
| K4 – şerit | `components/LiveConfigStrip.tsx`, `LiveConfigStrip.module.css`, `src/index.css` | `<div role="group" aria-label="Canlı yapılandırma">`, her öğe `<span data-tone={tone}>{label} <b>{value}</b></span>`. CSS lab `.cfg` + `.side2>.cfg span` (padding 2px 6px, 11px, 500, `#5f5258` = `--color-neutral-ink`), `.w` → `[data-tone='warning']`, `.r` → `[data-tone='danger']`. Yeni token'lar: `--color-config-warning-line: #ecd3a0`, `--color-config-danger-line: #f0c6c1` (lab `.cfg span.w/.r` kenarları), `--text-config: 11px`, `--radius-config: 6px` |
| K6, G5 | `components/HowItWorksCard.tsx` | props `{ config: LiveConfigResponse \| undefined }`; ilk alt satır sadece `config` varken `howItWorksText(config)`; ikinci satır aynı. Sabit model adı/sayı kaldırıldı |
| Kararlar #11, G5 (F3, F4, F7) | `pages/PreviewPage.tsx` | `useLiveConfigQuery()` bir kez. Yan sütunun başında: veri varsa `LiveConfigStrip`; veri yok ve hata varsa `ErrorAlert` (`ApiError.errorCode`, değilse `UNEXPECTED_ERROR`); yüklenirken hiçbir şey. Kart: son tur `done` + `sources.generationMode === 'RAG'` → `ContextChunksCard`; diğer her durumda `HowItWorksCard config={data}` |
| K2b, G3, G4 (F5) | `components/AnswerMessage.tsx` | `isEmptyKnowledgeBase` → `generationMode === 'RAG' && contextChunks.length === 0`. Sadece LLM cevabında `[n]` düz metin kalır: `formatAnswer(..., contextChunks.length = 0)` hiçbir numarayı düğmeye çevirmez; `citations` `[]` olduğundan "Rehber · s. N" düğmesi çıkmaz (mevcut kod, değişiklik gerekmedi) |
| Kararlar #15 (F6) | `hooks/useAnswerStream.ts` | `useQueryClient()`; `done` ya da `error` olayında ve akış öncesi `ApiError`'da (`createConversation` dahil) `invalidateQueries({ queryKey: liveConfigKeys.all })`. Abort'ta yapılmaz |
| AppHeader, errorMessages | — | Değişmedi (plan). Yeni `errorCode` yok |
| `web/CLAUDE.md` | `web/CLAUDE.md` | Durum yorumu, klasör yapısı (`shared/liveConfig/`, yeni model/component dosyaları). Yeni komut ve paket yok |

## Değişen dosyalar
Yeni:
- `web/src/shared/liveConfig/types.ts`
- `web/src/shared/liveConfig/liveConfigApi.ts`
- `web/src/shared/liveConfig/liveConfigQueries.ts`
- `web/src/shared/liveConfig/liveConfigRules.ts`
- `web/src/features/preview/model/liveConfigItems.ts`
- `web/src/features/preview/model/howItWorks.ts`
- `web/src/features/preview/components/LiveConfigStrip.tsx`
- `web/src/features/preview/components/LiveConfigStrip.module.css`

Değişen:
- `web/src/index.css` (4 token)
- `web/src/features/preview/model/types.ts`, `answerStreamEvents.ts`, `strategyLabels.ts`, `latencyRows.ts`
- `web/src/features/preview/components/HowItWorksCard.tsx`, `AnswerMessage.tsx`
- `web/src/features/preview/hooks/useAnswerStream.ts`
- `web/src/features/preview/pages/PreviewPage.tsx`
- `web/CLAUDE.md`

Silinen: yok. Eklenen paket: yok.

## Ekran metinleri
Şerit (`role="group"`, `aria-label="Canlı yapılandırma"`); her öğenin erişilebilir metni `"{label} {value}"`:
- `Model {chatModel}` — normal
- `Embedding {embeddingModel}` / `Embedding —` — normal
- `Retrieval rerank {candidates}→{topK}` normal · `Retrieval top-{topK}` normal · `Retrieval reranker DOWN` warning · `Retrieval kapalı (Sadece LLM)` danger
- `Soru yeniden yazma açık` normal · `Soru yeniden yazma kapalı` warning
- `Bilgi kaynakları {ad (40'ta kısalır)} · {sürüm}` normal · `Bilgi kaynakları {n} belge` normal · `Bilgi kaynakları belge yok` warning · `Bilgi kaynakları ulaşılamıyor` warning
- Ton `data-tone` niteliğindedir: `normal` / `warning` / `danger`.

"Asistan nasıl cevaplıyor?" kartı (başlık `h2`):
- RERANK (reranker DOWN değil): `Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5 parça qwen3.5:9b modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.`
- TOP_K ya da RERANK + DOWN: `Soru önce rehberde aranır (bge-m3 + pgvector), en yakın 5 parça qwen3.5:9b modeline gider. Cevaptaki numaralar rehberin parçalarını gösterir.`
- LLM_ONLY: `Soru önce rehberde aranır (bge-m3 + pgvector), bu modda arama kapalı; soru doğrudan modele gider.`
- embedding `null` iken parantez içi sadece `pgvector`.
- İkinci satır her zaman: `Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek.`
- (Örneklerdeki model adı ve sayılar yapılandırmadan gelir; kodda sabit yok.)

Yapılandırma okunamazsa: `role="alert"` `messageFor(errorCode)` → `INTERNAL_ERROR`/`UNEXPECTED_ERROR` için `Beklenmeyen bir hata oluştu. Tekrar dene.`; `NETWORK_ERROR` için mevcut eşleme.

Parça kartının alt başlığı Sadece LLM'de gösterilmez (kart çıkmaz); `strategyLabel` `NONE` için yine de `kapalı (Sadece LLM)` döner.

## Plandan sapmalar
Yok. Planın bıraktığı iki küçük ayrıntı (kimseyle konuşmayı gerektirmedi; davranış sözleşmeyle aynı):
- `strategyLabel`: `TOP_K`/`RERANK` turunda `topK`/`candidates` sözleşmeye göre null olmaz; tip daraltması için null gelirse `top-k` / `rerank` döner (ulaşılamayan dal).
- Şerit verisi bir kez yüklendikten sonra arka plandaki bir yenileme hata verirse TanStack Query son başarılı veriyi tutar; şerit son bilinen değerleri göstermeye devam eder, `ErrorAlert` sadece hiç veri yokken çıkar (G5 "uç okunamaz" = ilk yükleme).

## Doğrulama
- `cd web && npm run lint` → exit 0, uyarı/hata yok.
- `cd web && npm run build` → **`tsc -b` 1 hatayla kaldı**, hata uygulama kodunda değil, tester'ın dosyasında: `src/test/previewFixtures.ts(33,3)`: `sourcesEvent()` fixture'ı `generationMode` alanını vermiyor, artık zorunlu olan `SourcesEvent` tipine uymuyor. Plan bunu tester'a yazıyor ("bütün SSE `sources` fixture'ları `generationMode: 'RAG'` alır"); test dosyasına dokunmadım.
  - Uygulama kodunun kendisi: `npx tsc -b` çıktısında test dışı dosyada hata yok; `npx vite build` → `✓ built` (351.5 kB JS). Çıktı (`dist/`) silindi.
- `cd web && npm test` → 21 dosya, 371 test: 335 geçti, 36 kaldı (`answerStreamEvents.test.ts` 2, `PreviewPage.test.tsx` 34). Nedeni sözleşme değişikliği: fixture'larda `generationMode` yok (olay doğrulamada düşüyor) ve `GET /api/assistant/live-config` için MSW varsayılan handler'ı yok (`onUnhandledRequest: 'error'`); ayrıca eski `HowItWorksCard` metnini arayan test sabit metne bakıyor. Üçü de planda tester'ın maddesi.
- Model fonksiyonları scratch betikle F1/F2 değerlerine karşı elle kontrol edildi (varsayılan, LLM_ONLY, RERANK+DOWN, TOP_K + rewrite kapalı, 3 belge, EMPTY, UNAVAILABLE, 45 harflik ad, `30/3/llama3:8b`): hepsi plandaki beklenen metin ve tonla aynı.
- M5: `grep -rnE "qwen|bge-|llama|nomic" web/src --include='*.ts' --include='*.tsx' | grep -v -E "\.test\.|src/test/"` → boş.

## Tester için notlar
- **MSW:** `GET /api/assistant/live-config` → `LiveConfigResponse` (openapi örneği tam şema). Bütün Önizleme testlerinde gerekir (sayfa açılınca istek atılır). `src/test/previewFixtures.ts` `sourcesEvent()` → `generationMode: 'RAG'` eklenmeli; Sadece LLM fixture'ı: `{ generationMode: 'LLM_ONLY', retrievalStrategy: 'NONE', candidates: null, topK: null, searchQuery: null, queryRewriteStatus: 'DISABLED', contextChunks: [], citations: [] }`, `done.retrievalLatencyMs: null`, `rerankLatencyMs: null`.
- **Şerit sorgusu:** `within(screen.getByRole('group', { name: 'Canlı yapılandırma' }))`; öğe metni `"Retrieval kapalı (Sadece LLM)"` gibi tek string (`getByText` için `{label} ` + `<b>` ayrı düğüm; `toHaveTextContent` ya da `closest('span')` ile `data-tone` kontrolü daha güvenli).
- **F6 (cevaptan sonra yenileme):** `done` olayından sonra `liveConfigKeys.all` invalidate edilir → ikinci GET. Ayrıca `error` olayında ve akış öncesi `ApiError`'da (ör. 503 `RETRIEVAL_UNAVAILABLE`, 409) da yenilenir; istek sayısını sayan testlerde bunu hesaba kat.
- **Yoklama:** `refetchInterval` 15 000 ms; sayan testler sahte zamanlayıcı kullanmıyorsa 15 sn içinde ek istek gelmez. `renderWithProviders` `retry: false` olduğu için F7'de 500 tek istekte hataya düşer.
- **F7:** hata durumunda şerit yok, `alert` `Beklenmeyen bir hata oluştu. Tekrar dene.`; kartta sadece "Bir soru sor; …" satırı. Soru gönderme etkilenmez.
- **F5:** Sadece LLM `done`'dan sonra yan kart "Asistan nasıl cevaplıyor?" kalır (ContextChunksCard çıkmaz); `Kaynak 1` düğmesi, "Rehber · s." düğmesi ve "Bilgi bankasında henüz belge yok." yoktur; `[1]` metin olarak görünür.
- **F10:** `LiveConfigStrip` tek başına `QueryClientProvider` istemez (`config` prop'u alır).

## Düzeltme

STATUS: DONE
