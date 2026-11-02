TASK: TASK-010
# Frontend uygulama raporu

## Kullanılan skill'ler
- `frontend-conventions`, `react-patterns` (zorunlu)
- `sse-streaming` (SSE olay tipleri ve doğrulama), `frontend-a11y` (planın ek skill'leri)

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Tipler (sse.md `QueryRewriteStatus`, `SourcesEvent`, `DoneEvent`) | `web/src/features/preview/model/types.ts` | `QUERY_REWRITE_STATUSES = ['DISABLED', 'NO_HISTORY', 'REWRITTEN', 'FAILED', 'EMPTY'] as const` + `QueryRewriteStatus`; `SourcesEvent.searchQuery: string`, `SourcesEvent.queryRewriteStatus: QueryRewriteStatus`; `DoneEvent.queryRewriteLatencyMs: number \| null` |
| Olay doğrulama (F1) | `web/src/features/preview/model/answerStreamEvents.ts` | `isSourcesEvent`: `searchQuery` string olmalı, `queryRewriteStatus` bilinen durumlardan biri olmalı (`isQueryRewriteStatus`). `isDoneEvent`: `isNullableInteger(queryRewriteLatencyMs)`. Eksik ya da geçersiz alan → `null` (olay atlanır) |
| K7a: alt başlık `· arama: "…"` ve kart soru 3 = B (F3, F4) | `web/src/features/preview/model/strategyLabels.ts`, `web/src/features/preview/components/ContextChunksCard.tsx` | `SEARCH_QUERY_SHORT_LENGTH = 40`, `QUERY_REWRITE_FAILED_LABEL = 'yeniden yazma başarısız'`, `queryRewriteLabel(sources)`: `REWRITTEN` → `arama: "${shortenText(searchQuery, 40)}"`; `FAILED`/`EMPTY` → `yeniden yazma başarısız`; `DISABLED`/`NO_HISTORY` → `null`. Kartta alt başlık `subtitleText`: `"<soru, 60>" · <strateji>` + etiket varsa ` · <etiket>` (lab satır 1061 ile aynı biçim) |
| K7b: "yeniden yazma" süre satırı (F2, F5) | `web/src/features/preview/model/latencyRows.ts` | `queryRewriteLatencyMs !== null` ise ilk satır `{ label: 'yeniden yazma', ms, isModel: false }`; sonra retrieval-api araması, rerank, model (lab satır 1064 sırası). `LatencyBars.tsx` değişmedi, satırları zaten `latencyRows`'tan çizer |
| `HowItWorksCard` metni değişmez | — | Dokunulmadı |

## Değişen dosyalar
- Değişen: `web/src/features/preview/model/types.ts`, `web/src/features/preview/model/answerStreamEvents.ts`, `web/src/features/preview/model/strategyLabels.ts`, `web/src/features/preview/model/latencyRows.ts`, `web/src/features/preview/components/ContextChunksCard.tsx`
- Yeni / silinen dosya yok. Yeni paket yok. `web/CLAUDE.md` değişmedi (yeni komut ve paket yok).

## Ekran metinleri
- Yan kart alt başlığı (yeniden yazıldı): `"Yenisi kaç günde gelir?" · rerank 20→5 · arama: "Kaybolan kartın yenisi kaç günde gelir?"`
- Arama sorgusu 40 karakterden uzunsa: ilk 39 karakter + `…` (ör. `arama: "<39 karakter>…"`)
- Yeniden yazma başarısız (`FAILED`, `EMPTY`): `"<soru>" · rerank 20→5 · yeniden yazma başarısız`
- `DISABLED`, `NO_HISTORY`: alt başlık TASK-008'deki gibi (`"<soru>" · rerank 20→5`), ek yok
- Süre kartı satır etiketi: `yeniden yazma` (ör. `yeniden yazma` · `812 ms`), `retrieval-api araması` satırından önce
- Yeni hata metni ya da `errorCode` yok (yeniden yazma hiçbir zaman hata döndürmez, sse.md)

## Plandan sapmalar
Yok.

## Doğrulama
- `cd web && npm run lint` → geçti (oxlint, uyarı/hata yok).
- `cd web && npm run build` → **kaldı, sadece tester'ın fixture dosyasında**: `src/test/previewFixtures.ts(33,3)` `sourcesEvent()` `searchQuery`/`queryRewriteStatus` vermiyor; `(44,3)` `doneEvent()` `queryRewriteLatencyMs` vermiyor (2 hata, ikisi de bu dosyada). Uygulama kodunda tip hatası yok; `npx vite build` geçti. Bu dosya `src/test/**` olduğu için tester'ındır; plan "Web SSE fixture'larının tamamı yeni alanları taşır" diyor.
- `cd web && npm test` → aynı nedenle kaldı: fixture'daki `sources`/`done` olayları yeni zorunlu alanları taşımadığı için doğrulamada düşüyor; `answerStreamEvents`, `previewModel` ve `PreviewPage` testlerinden 30+ test kırık (beklenen davranış: eksik alan → olay atlanır).
- Kontrol (repoya yazılmadı): `web/` scratchpad'e kopyalandı (`/private/tmp/claude-501/-Users-rasitesdmr-Documents-rag/de4b15ca-6323-4a8e-b859-89b3826baef4/scratchpad/webcheck`), sadece kopyadaki `previewFixtures.ts`'e `searchQuery: 'Kartımı kaybettim, ne yapmalıyım?'`, `queryRewriteStatus: 'NO_HISTORY'`, `queryRewriteLatencyMs: null` eklendi → `npx tsc -b` geçti (0 hata), `npx vitest run` → 21 dosya, 350 test geçti.

## Tester için notlar
- **Fixture güncellemesi şart (build için):** `src/test/previewFixtures.ts` → `sourcesEvent()` varsayılanlarına `searchQuery` (ör. `'Kartımı kaybettim, ne yapmalıyım?'`) ve `queryRewriteStatus: 'NO_HISTORY'`; `doneEvent()` varsayılanlarına `queryRewriteLatencyMs: null`. Bu üç alan eklenince mevcut 350 test geçiyor (yukarıdaki kontrol). Elle yazılmış başka SSE JSON'u varsa (ör. `answerStreamEvents.test.ts` içindeki literal'ler) onlar da yeni alanları taşımalı.
- Endpoint değişmedi: `POST /api/assistant/conversations/:conversationId/messages` (SSE). F3 için MSW handler'ı ikinci istekte `sourcesEvent({ searchQuery: 'Kaybolan kartın yenisi kaç günde gelir?', queryRewriteStatus: 'REWRITTEN' })` + `doneEvent({ queryRewriteLatencyMs: 812 })` dönebilir; soru metni "Yenisi kaç günde gelir?".
- Alt başlık tek bir `div` içinde tek metin düğümüdür; `getByText('"Yenisi kaç günde gelir?" · rerank 20→5 · arama: "Kaybolan kartın yenisi kaç günde gelir?"')` ile tam eşleşir.
- `queryRewriteLabel` ve `QUERY_REWRITE_FAILED_LABEL`, `SEARCH_QUERY_SHORT_LENGTH` `model/strategyLabels.ts`'ten export edilir (F4 `it.each`). F4'te beklenen değeri sabitten türetme; literal `'yeniden yazma başarısız'` kullan.
- F2: `latencyRows(doneEvent({ queryRewriteLatencyMs: 812 }))[0]` → `{ label: 'yeniden yazma', ms: 812, isModel: false }`; `null` → hiçbir satırın etiketi `yeniden yazma` değil.
- F1: `queryRewriteLatencyMs` alanı hiç yoksa `done` olayı `null` döner (`undefined` geçersiz); `null` geçerli. `queryRewriteStatus: 'FOO'` → `null`; `searchQuery` yok → `null`.
- Zor durum: `REWRITTEN` iken model soruyu aynen tekrarlarsa `searchQuery` soruyla aynı olabilir; etiket yine görünür (sse.md).

## Düzeltme

STATUS: DONE
