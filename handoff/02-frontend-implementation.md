TASK: TASK-032
# Frontend uygulama raporu

Bu task'ta frontend-coder sadece düzeltme turu 1'de çalışır (plan "## Düzeltme turu 1" → D4). İlk turda web'de değişiklik
yoktu ("## Frontend planı": Yok).

## Kullanılan skill'ler
- `frontend-conventions`, `react-patterns` (zorunlu)
- `sse-streaming` (D4: `done` olayının daraltılması)
- `react-testing` planda tester için yazılmış; frontend-coder test yazmadığı için yüklenmedi.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|------------------------|------------|-----|
| D4: `ANSWER_SOURCES`'a `NO_WORDS` | `web/src/features/preview/model/types.ts` | `['MODEL', 'EMPTY_KNOWLEDGE_BASE', 'BELOW_RERANK_THRESHOLD', 'SMALL_TALK', 'NO_WORDS'] as const`. `answerStreamEvents.ts` `isAnswerSource` bu listeyi okur; `NO_WORDS`'lü `done` artık atlanmaz (akış "yarıda kesildi" görünmez). `OTHER` gibi bilinmeyen değer yine atlanır |
| D4: Önizleme sağ kart | `web/src/features/preview/pages/PreviewPage.tsx` | `ContextChunksCard` koşuluna `lastTurn.done.answerSource !== 'NO_WORDS'` eklendi; `SMALL_TALK` ya da `NO_WORDS` ise yerine `HowItWorksCard` ("Asistan nasıl cevaplıyor?") görünür |
| D4: `AnswerMessage` değişmez | — | `done.notFoundAnswer` true olduğu için "Temsilciye bağlan" bugünkü kuralla görünür; boş bilgi bankası notu sadece `EMPTY_KNOWLEDGE_BASE`'te; `citations` [] olduğu için kaynak düğmesi yok |
| D4: Konuşmalar değişmez | — | Konuşma detayı `answerSource` okumuyor; `NONE` → "RAG", parça yok → "Bu turda modele hiçbir parça gitmedi.", durum `DONT_KNOW` → "BİLMİYOR" (backend `TurnStatusResolver`) |
| D4: `web/CLAUDE.md` | `web/CLAUDE.md` | Üst yorumdaki "Durum" satırında `answerSource` değerlerine `NO_WORDS` ve "harf/rakamsız mesaj turunda yan parça kartı yok" eklendi |
| D4: yeni metin, paket, `errorCode` | — | Yok |

## Değişen dosyalar
- Değişen: `web/src/features/preview/model/types.ts`, `web/src/features/preview/pages/PreviewPage.tsx`, `web/CLAUDE.md`
- Yeni / silinen dosya yok. Paket eklenmedi.

## Ekran metinleri
Yeni metin yok. W2 için kullanılacak mevcut metinler:
- Balon (backend `done.answer`): "Bu konuda rehberimizde bilgi bulamadım."
- Gösterilmemesi gereken kart başlığı: "Son cevapta modele giden parçalar"
- Gösterilmemesi gereken not: "Bilgi bankasında henüz belge yok."
- Görünmesi gereken düğme: "Temsilciye bağlan"
- Yerine görünen kart: "Asistan nasıl cevaplıyor?" (`HowItWorksCard`)

## Plandan sapmalar
Yok.

## Doğrulama
- `cd web && npm run build` → geçti (tsc -b + vite build, hata yok).
- `cd web && npm run lint` → geçti (oxlint, uyarı ve hata yok).
- `cd web && npm test` → geçti: 46 test dosyası, 748 test.

## Tester için notlar
- W1 (`answerStreamEvents.test.ts`): `done` payload'ında `answerSource: 'NO_WORDS'` daraltılır; `'OTHER'` atlanır.
- W2 (`PreviewPage.test.tsx`): mesaj endpoint'i `POST /api/assistant/conversations/:conversationId/messages`, `src/test/sse.ts`
  `sseResponse` ile şu olaylar: tek `token` ("Bu konuda rehberimizde bilgi bulamadım."), `sources` (`previewFixtures.sourcesEvent`
  ile `retrievalStrategy: 'NONE'`, `searchQuery: null`, `candidates: null`, `topK: null`, `queryRewriteStatus: 'DISABLED'`,
  `contextChunks: []`, `citations: []`), `done` (`previewFixtures.doneEvent` ile `notFoundAnswer: true`,
  `answerSource: 'NO_WORDS'`, `answer` aynı cümle, model süreleri ve token'lar `null`). Diğer gerekli handler'lar (konuşma
  oluşturma, live-config, suggestions) mevcut Önizleme testlerindeki gibidir.
- Sağ sütunda kart yerine "Asistan nasıl cevaplıyor?" kartı beklenir (live-config handler'ı varsa içeriği dolar).

## Düzeltme
Yok.

STATUS: DONE
