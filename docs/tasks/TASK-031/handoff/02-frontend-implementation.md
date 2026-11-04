TASK: TASK-031
# Frontend uygulama raporu

## Kullanılan skill'ler
- `frontend-conventions`, `react-patterns` (zorunlu)
- Planın ek skill'leri (`sse-streaming`, `frontend-a11y`) yüklenmedi; değişiklik mevcut akış ve kart bileşenlerinde alan eklemekten ibaret. Kalıplar (`unknown` → daraltma, updater biçimi, `aria-live` bölgesi) olduğu gibi kaldı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| `ANSWER_SOURCES`, `AnswerSource`; `DoneEvent.answer`, `DoneEvent.answerSource` (K1, K2, Kararlar #13) | `features/preview/model/types.ts` | Alan sırası sse.md ile aynı (`notFoundAnswer`'dan sonra) |
| `done` daraltması `answer` (string) ve `answerSource` (listede) ister; yoksa olay atlanır (W5) | `features/preview/model/answerStreamEvents.ts` | Yeni `isAnswerSource` |
| `done` gelince akan metin `done.answer` ile değiştirilir (G3, W1) | `features/preview/hooks/useAnswerStream.ts` (`applyEvent`) | "Maalesef, bu konuda … bulamadım. [2]" akmış olsa bile ekranda sadece cümle kalır; `[2]` düğmesi çıkmaz |
| Boş bilgi bankası notu sadece `answerSource === 'EMPTY_KNOWLEDGE_BASE'` (W2, W4) | `features/preview/components/AnswerMessage.tsx` | Eski `isEmptyKnowledgeBase(sources)` (RAG + `contextChunks` boş) kaldırıldı; artık eşik altı ve selamlaşma turunda da `contextChunks` boş olduğu için yanlış not çıkardı |
| Selamlaşma turunda yan kart gösterilmez (Kararlar #11, W2) | `features/preview/pages/PreviewPage.tsx` | `lastTurn.done.answerSource !== 'SMALL_TALK'`; yerine "Asistan nasıl cevaplıyor?" kartı kalır |
| Eşik altı turda liste yerine sabit satır, `LatencyBars` kalır (Kararlar #10, W3) | `features/preview/components/ContextChunksCard.tsx` | `BELOW_THRESHOLD_NOTE`, `card.sub` stilinde; parça listesi + lejant top-level `ContextChunkList` bileşenine ayrıldı (görünüm aynı) |
| `TURN_STATUSES`'a `SMALL_TALK` (`UNGROUNDED`'dan sonra, openapi sırası); etiket `SOHBET`, ton `neutral` (gri) (K3, W6) | `features/conversation/model/types.ts`, `features/conversation/model/turnStatus.ts` | `TurnCard` ve `ConversationTable` aynı haritayı okur; Konuşmalar detayında parçasız tur için mevcut "Bu turda modele hiçbir parça gitmedi." satırı görünür |
| `web/CLAUDE.md` güncellemesi (done alanları, SOHBET) | `web/CLAUDE.md` | Durum yorumuna TASK-031; "Teknoloji"deki akış maddesine `done.answer` / `answerSource` notu |

## Değişen dosyalar
Değişen:
- `web/src/features/preview/model/types.ts`
- `web/src/features/preview/model/answerStreamEvents.ts`
- `web/src/features/preview/hooks/useAnswerStream.ts`
- `web/src/features/preview/components/AnswerMessage.tsx`
- `web/src/features/preview/components/ContextChunksCard.tsx`
- `web/src/features/preview/pages/PreviewPage.tsx`
- `web/src/features/conversation/model/types.ts`
- `web/src/features/conversation/model/turnStatus.ts`
- `web/CLAUDE.md`

Yeni / silinen dosya yok. Yeni paket yok. Yeni `errorCode` yok.

## Ekran metinleri
- Önizleme yan kartı, eşik altı turda (`answerSource` `BELOW_RERANK_THRESHOLD`): **"Bu turda modele hiçbir parça gitmedi: en iyi parçanın rerank puanı eşiğin altında kaldı."** Kart başlığı "Son cevapta modele giden parçalar" ve alt satırdaki soru/strateji metni (ör. `"Bitcoin alıp satabilir miyim?" · rerank 20→5`) aynen kalır; parça düğmeleri ve "✓ atıf yapılan" lejantı yoktur; süre çubukları vardır.
- Konuşmalar durum çipi (liste ve detay): **"SOHBET"** (ton `neutral`).
- Değişmeyen ama koşulu değişen metinler: "Bilgi bankasında henüz belge yok." + "Bilgi kaynakları'ndan belge ekle." (sadece `EMPTY_KNOWLEDGE_BASE`), "Temsilciye bağlan" (`notFoundAnswer` true iken; selamlaşmada yok), "Son cevapta modele giden parçalar" (selamlaşma turunda yok).

## Plandan sapmalar
Yok. Tek yapısal not: `ContextChunksCard` içinde liste + lejant top-level bir yardımcı bileşene (`ContextChunkList`, aynı dosya, export edilmez) ayrıldı; DOM ve stil aynı.

## Doğrulama
- `cd web && npm run lint` → geçti (oxlint, 0 uyarı, exit 0).
- `cd web && npm run build` → **kaldı, tek hata tester dosyasında:** `src/test/previewFixtures.ts(48,3): TS2322` — `doneEvent()` fixture'ı yeni zorunlu `answer` / `answerSource` alanlarını içermiyor. Bu dosya tester'ın; dokunmadım.
  - Üretim kodu ayrıca doğrulandı: `src/test` ve `*.test.ts(x)` hariç tutularak `tsc --noEmit` → exit 0; `npx vite build` → geçti.
- `cd web && npm test` → 664 geçti / 64 kaldı (728). Hepsi aynı nedenden: fixture'lardaki `done` olayında `answer` / `answerSource` yok, daraltma olayı atlıyor.
  - Kontrol: web'in scratchpad'deki bir kopyasında (asıl dosyalara dokunmadan) `doneEvent()`'e `answer: ''`, `answerSource: 'MODEL'` eklenince `tsc -b` temiz, 710/728 geçti. Kalan 18 başarısızlık beklenen davranış değişikliği: testler `done.answer`'ı akan metinle eşleştirmiyor (ekran `done.answer`'ı gösterir) ve boş bilgi bankası testleri `answerSource: 'EMPTY_KNOWLEDGE_BASE'` vermiyor. Kopya silindi.

## Tester için notlar
- **Fixture güncellemesi şart (build için de):** `src/test/previewFixtures.ts` `doneEvent()` → `answer` ve `answerSource` (varsayılan `'MODEL'`). `done` gelince ekran metni `done.answer` olur; akış testlerinde `answer` akan token'ların birleşimiyle aynı verilmeli, yoksa cevap balonu boşalır/değişir. MSW SSE handler'larındaki `done` gövdeleri de (`src/test/sse.ts` / handler'lar) aynı iki alanı içermeli; `answerStreamEvents.test.ts` ve `.validation.test.ts`'teki geçerli `done` örneklerine iki alan eklenmeli.
- Boş bilgi bankası testleri: `answerSource: 'EMPTY_KNOWLEDGE_BASE'`, `notFoundAnswer: true`, `sources.contextChunks: []`. `LLM_ONLY` + boş bilgi bankası: `answerSource: 'MODEL'` → not çıkmaz.
- W1 (bulamadım + numara): token "Maalesef, bu konuda rehberimizde bilgi bulamadım. [2]", sources `citations: []` ve 5 parça, done `notFoundAnswer: true`, `answer: 'Bu konuda rehberimizde bilgi bulamadım.'` → balonda sadece cümle, "Kaynak 2" düğmesi ve "Rehber · s." yok, "Temsilciye bağlan" var.
- W2 (selamlaşma) iki fixture: kalıp listesi yolu sources `retrievalStrategy: 'NONE'`, `candidates`/`topK`/`searchQuery` `null`, `queryRewriteStatus: 'DISABLED'`, `contextChunks: []`, `citations: []`; etiket yolu `retrievalStrategy: 'RERANK'`, `candidates: 20`, `topK: 5`, `searchQuery` dolu, `contextChunks: []`. İkisinde de done `answerSource: 'SMALL_TALK'`, `notFoundAnswer: false` → "Bilgi bankasında henüz belge yok." yok, "Son cevapta modele giden parçalar" yok ("Asistan nasıl cevaplıyor?" görünür), "Temsilciye bağlan" yok, kaynak düğmesi yok. Örnek gövdeler `docs/api/sse.md` (turnId 17 ve 18).
- W3 (eşik altı): `ContextChunksCard`'a `done.answerSource: 'BELOW_RERANK_THRESHOLD'`, `sources.retrievalStrategy: 'RERANK'`, `contextChunks: []` → not metni görünür, `role="button"` parça yok, `LatencyBars` var. PreviewPage üzerinden de denenebilir (sse.md turnId 16 örneği).
- W5: `done`'da `answer` eksik, `answer` string değil ya da `answerSource: 'OTHER'` → `toAnswerStreamEvent` `null` döner.
- W6: `TurnResponse.status: 'SMALL_TALK'` ve liste satırında `lastTurnStatus: 'SMALL_TALK'` → "SOHBET" çipi, `turnStatusTone('SMALL_TALK') === 'neutral'`.
- Yeni MSW endpoint'i yok; mevcut `POST /api/assistant/conversations/:conversationId/messages` SSE handler'ı yeterli.

## Düzeltme
Yok (bkz. Doğrulama: build ve test başarısızlıkları sözleşme değişikliğinin tester fixture'larına yansıtılmasıyla kapanır; bu tester'ın bu task'taki işidir).

STATUS: DONE
