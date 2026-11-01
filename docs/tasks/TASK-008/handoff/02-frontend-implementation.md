TASK: TASK-008
# Frontend uygulama raporu

## Kullanılan skill'ler
`frontend-conventions`, `react-patterns`, `sse-streaming`, `frontend-a11y`, `frontend-security`. Lab'dan aktarılan değerler doğrudan `kaynaklar/rag-lab.html`'in Önizleme CSS'inden ve `pPreview`/`bubbleHtml`/`fmtAnswer`/`scrollMsgs` fonksiyonlarından alındı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| shared `eventStream.ts` (K5a, K5b) | `shared/api/eventStream.ts` | `parseEventStream`: TextDecoder `stream: true`, satır tamponu, `\r\n` → `\n`, boş satırda olay, `event:` yoksa `message`, çok satırlı `data` `\n` ile birleşir, `:` yorum atlanır, `alan:değer` ve `alan: değer` ikisi de kabul (sse.md'nin son hâli). `postEventStream`: `Content-Type: application/json`, `Accept: text/event-stream, application/problem+json`; `fetch` reddi → `ApiError(0,'NETWORK_ERROR')` (abort olduğu gibi fırlar); `!ok` → `toApiError`; gövde yok → `UNEXPECTED_ERROR`. Akış açıldıktan sonra okuma hatası (ağ koptu) → `ApiError(0,'STREAM_INTERRUPTED')`. Bitince/erken çıkışta `reader.cancel()` |
| shared `errorMessages.ts` (K11, F24) | `shared/api/errorMessages.ts` | 5 yeni kod, metinler planla birebir. `INTERNAL_ERROR` → fallback (eşleme yok) |
| shared `formatDecimal`, `formatTime` | `shared/format/formatNumber.ts`, `shared/format/formatDateTime.ts` | `formatDecimal(0.3124,3)` "0,312", `(5.359,2)` "5,36", `(-11.04,2)` "-11,04" (ASCII eksi). `formatTime` Europe/Istanbul "HH:mm" |
| `model/types.ts` | `features/preview/model/types.ts` | Sözleşme adlarıyla birebir: `Channel`, `CreateConversationRequest`, `ConversationResponse`, `SendMessageRequest`, `RetrievalStrategy`, `ContextChunk`, `TokenEvent`, `SourcesEvent`, `DoneEvent`, `StreamErrorEvent`, `AnswerStreamEvent`, `TurnPhase`, `PreviewTurn` |
| `answerStreamEvents.ts` (F4) | `features/preview/model/answerStreamEvents.ts` | JSON parse `try`, her alan tipi kontrol (int, nullable, enum); bilinmeyen olay/bozuk şekil → `null` |
| `messageRules.ts` (K1, G4) | `features/preview/model/messageRules.ts` | `MESSAGE_MAX_LENGTH = 1000`, `normalizeMessage` = `trim()`, `canSendMessage`, `MESSAGE_RULE_MESSAGE` "Soru 1-1000 karakter olmalı." |
| `answerFormat.ts` (Kararlar #17, K6a) | `features/preview/model/answerFormat.ts` | Lab `fmtAnswer` karşılığı: satırlar, `- `/`* ` madde, `**x**` kalın, `[n]` sadece `contextSize` doluysa ve 1..n aralığındaysa alıntı; HTML üretmez |
| `citations.ts` (K6b, kart soru 2, Kararlar #11) | `features/preview/model/citations.ts` | `SOURCE_NAME_SHORT_LENGTH = 24`, `citedChunks` (alıntı sırası, aynı `chunkId` bir kez), `sourceButtonLabel` (`shortenText` + " · s. N"), `sourceButtonTitle` (tam ad · sayfa N), `contextChunkOf` |
| `strategyLabels.ts` (K16) | `features/preview/model/strategyLabels.ts` | `top-${topK}`, `rerank ${candidates}→${topK}`, "reranker DOWN · vector sırası" |
| `latencyRows.ts` (K8c, K17, Kararlar #20) | `features/preview/model/latencyRows.ts` | "retrieval-api araması"; `rerankLatencyMs !== null` → "rerank"; `generationLatencyMs !== null` → `${modelShortName} cevabı` (`isModel`) |
| `previewApi.ts` (Kararlar #1) | `features/preview/api/previewApi.ts` | `createConversation()` gövde `{"channel":"PREVIEW"}`; `sendMessage(id, {text}, signal)` `postEventStream` + daraltma, `null`'lar atlanır |
| `useAnswerStream` (K2, K5c, K5d, G1, G5, Kararlar #16) | `features/preview/hooks/useAnswerStream.ts` | Akış handler'dan başlar; istek başına `AbortController`, unmount'ta `abort()`; akış sürerken ikinci `sendQuestion` yok sayılır; `conversationId` yoksa önce `createConversation`; `token` updater ile eklenir; `done`/`error` sonrası döngü biter; `done`'suz bitiş → `STREAM_INTERRUPTED`; `ApiError` → `errorCode`, `CONVERSATION_NOT_FOUND`'da kimlik `null` (sonraki soru yeni konuşma açar); abort sessiz. `isStreaming` turlardan türetilir. `startNewConversation` akışta çalışmaz; mevcut turlar boş değilse `previousTurns` olur. Token'lar Query cache'ine yazılmaz (bu task'ta invalidate edilecek konuşma sorgusu yok) |
| `useClock` | `features/preview/hooks/useClock.ts` | 30 sn'de bir |
| `PreviewPage` (Kararlar #18, F28) | `features/preview/pages/PreviewPage.tsx` (+ `.module.css`) | `PageHeader` metinleri aynı; `.pv` ızgarası 322 px + kalan, `@container (max-width: 640px)` tek sütun (lab). Yan kart: son tur `done` ve `sources`/`done` dolu → `ContextChunksCard`, değilse `HowItWorksCard`. Açık `chunkId` sayfa state'inde → `ChunkDialog`. Görünmez `aria-live="polite"` bölge (bkz. sapma 1) |
| `PhoneFrame` (K2, K5d) | `components/PhoneFrame.tsx` (+ css) | Lab `.phone/.scr/.sbar/.abar`; durum çubuğu (saat, "5G", pil) `aria-hidden`; avatar SVG renkleri CSS token'ından; "Yeni konuşma" `disabled={isStreaming}` |
| `MessageList` (K3) | `components/MessageList.tsx` (+ css), `components/message.module.css` | `WELCOME_MESSAGE` lab metni; `previousTurns` + ayraç "Yeni konuşma" / "Yeni konuşma · {id}"; lab `scrollMsgs` algoritması birebir (`.msp` boşluğu dahil) `useLayoutEffect` ile her tur/token değişiminde. Kap `role="region"` `aria-label="Mesajlar"` `tabIndex=0` (kaydırılabilir bölge klavyeyle erişilir) |
| `UserMessage` | `components/UserMessage.tsx` | Soru balonu + `formatTime(askedAt)`; `data-message="question"` (kaydırma için) |
| `AnswerMessage` (K6a, K6b, K7, K11, kart soru 4) | `components/AnswerMessage.tsx` (+ css) | `waiting` ve metin yok → üç noktalı gösterge (`prefers-reduced-motion`'da animasyonsuz). Alıntı `<button aria-label="Kaynak n">n</button>`. `done` ve `notFoundAnswer === false` → kaynak düğmeleri (bkz. sapma 2). `done` ve `contextChunks` boş → "Bilgi bankasında henüz belge yok." + `/kaynaklar` bağlantısı. `failed` → yarım metin + balon içinde `role="alert"` mesaj (`VALIDATION_FAILED`'da `MESSAGE_RULE_MESSAGE`), `.bad` kenar |
| `MessageComposer` (K1, K5d, F25) | `components/MessageComposer.tsx` (+ css) | `onSubmit` + kontrollü input (`aria-label="Soru"`, `placeholder="Sorunu yaz…"`, `maxLength=1000`, akışta `disabled`), `Gönder` (`aria-label`, lab ok SVG'si) `disabled={isStreaming \|\| !canSendMessage(value)}`; gönderince alan boşalır. "Kayıtlı sorular" düğmesi yok |
| `HowItWorksCard` (K8a, Kararlar #19) | `components/HowItWorksCard.tsx`, `components/sideCard.module.css` | İki `.sub` metni plandaki gibi |
| `ContextChunksCard` (K8b, K8d, K16) | `components/ContextChunksCard.tsx` (+ css) | Başlık, `"${shortenText(soru,60)}" · ${strategyLabel}`, her parça düğme: `[n]` + atıflıda yeşil " ✓" (gerçek metin, CSS `::after` değil), `s. N · başlık · #chunkNo`, `RERANK`'te "rerank x,xx · d x,xxx", diğerlerinde "d x,xxx"; "✓ atıf yapılan" (sarı yok) |
| `LatencyBars` (K8c, K8e, K17) | `components/LatencyBars.tsx` (+ css) | "Süre · {toplam}" + `hostLabel` doluysa " · {hostLabel}" (lab `.sub` stiliyle); satır genişliği `max(1, ms/total×100)%` (inline style, en fazla %100); model satırı `--color-accent` |
| `index.css` | `src/index.css` | Lab `--s-*` ve Önizleme CSS'inden telefon, balon, alıntı, kaynak çipi, yazıyor, composer, yan kart, süre çubuğu token'ları; component CSS'inde ham hex yok |
| `web/CLAUDE.md` | `web/CLAUDE.md` | Durum yorumu (TASK-008), `eventStream.ts` "Henüz olmayanlar"dan çıktı, klasör yapısına `features/preview` içeriği |

## Değişen dosyalar
Yeni:
- `web/src/shared/api/eventStream.ts`
- `web/src/features/preview/api/previewApi.ts`
- `web/src/features/preview/hooks/useAnswerStream.ts`, `useClock.ts`
- `web/src/features/preview/model/types.ts`, `answerStreamEvents.ts`, `messageRules.ts`, `answerFormat.ts`, `citations.ts`, `strategyLabels.ts`, `latencyRows.ts`
- `web/src/features/preview/components/PhoneFrame.tsx` (+ `.module.css`), `MessageList.tsx` (+ `.module.css`), `message.module.css`, `UserMessage.tsx`, `AnswerMessage.tsx` (+ `.module.css`), `MessageComposer.tsx` (+ `.module.css`), `HowItWorksCard.tsx`, `sideCard.module.css`, `ContextChunksCard.tsx` (+ `.module.css`), `LatencyBars.tsx` (+ `.module.css`)
- `web/src/features/preview/pages/PreviewPage.module.css`

Değişen: `web/src/features/preview/pages/PreviewPage.tsx`, `web/src/index.css`, `web/src/shared/api/errorMessages.ts`, `web/src/shared/format/formatNumber.ts`, `web/src/shared/format/formatDateTime.ts`, `web/CLAUDE.md`.

Silinen: yok. Eklenen paket: yok.

## Ekran metinleri
- Sayfa: "Önizleme" (breadcrumb), başlık "Asistan önizleme", açıklama "Raşit Mobil'deki asistanı müşterinin gördüğü hâliyle dene; önizleme konuşmaları da Konuşmalar'a düşer."
- Telefon: "Dijital Asistan", "Raşit Mobil · 7/24", düğme "Yeni konuşma", durum çubuğunda "5G" ve saat (`aria-hidden`).
- Mesaj alanı: `role="region"` adı "Mesajlar".
- Karşılama: "Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılık sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplarım. Bakiyen gibi sana özel bilgileri göremem."
- Ayraç: "Yeni konuşma" (kimlik gelmeden) / "Yeni konuşma · {conversationId}".
- Soru kutusu: `aria-label="Soru"`, placeholder "Sorunu yaz…". Düğme: `aria-label="Gönder"` (sadece ikon).
- Alıntı düğmesi: adı "Kaynak {n}", görünen metin "{n}".
- Kaynak düğmesi: adı ve metni "{kısa ad} · s. {sayfa}" (ör. "Raşit Bankası Ürün ve H… · s. 19"), `title` "{tam ad} · sayfa {sayfa}".
- Boş bilgi bankası ipucu: "Bilgi bankasında henüz belge yok." + bağlantı "Bilgi kaynakları'ndan belge ekle." (`/kaynaklar`).
- Hata (balon içinde `role="alert"`): "Konuşma bulunamadı. Yeni konuşma başlat.", "Bu konuşmada bir cevap hâlâ yazılıyor. Bitince yeniden sor.", "Bilgi bankasına şu an ulaşılamıyor. Biraz sonra tekrar dene.", "Dil modeline şu an ulaşılamıyor. Biraz sonra tekrar dene.", "Cevap yarıda kesildi. Soruyu yeniden gönder.", "Sunucuya ulaşılamıyor. Bağlantını kontrol et." (`NETWORK_ERROR`), "Soru 1-1000 karakter olmalı." (`VALIDATION_FAILED`), diğerleri "Beklenmeyen bir hata oluştu. Tekrar dene."
- Duyuru (görünmez `aria-live="polite"`): "Asistan yazıyor…" (bekliyor/akıyor), "Cevap tamamlandı." (done); hata ve başlangıçta boş.
- Yan kart (önce): başlık "Asistan nasıl cevaplıyor?"; "Soru önce rehberde aranır (bge-m3 + pgvector), 20 aday reranker ile puanlanıp en iyi 5'i qwen3.5:9b'ye gider. Cevaptaki numaralar rehberin parçalarını gösterir."; "Bir soru sor; modele hangi parçaların gittiği, sıraları ve süreler burada görünecek."
- Yan kart (sonra): başlık "Son cevapta modele giden parçalar"; alt başlık `"{soru, 60 karakter}" · rerank 20→5` / `· top-5` / `· reranker DOWN · vector sırası`; satır düğmesi `[n]` (+ " ✓"), "s. 19 · 2.16 Kayıp ya da çalıntı bildirimi · #31", "rerank 5,36 · d 0,312" ya da "d 0,312"; "✓ atıf yapılan"; "Süre · 4,73 sn" + " · Apple M5 · 16 GB" (sadece `hostLabel` doluysa); satırlar "retrieval-api araması", "rerank", "qwen3.5 cevabı" + süre ("84 ms", "712 ms", "3,92 sn").
- Yan kart başlıkları `h2` (sayfa `h1`'inden sonra seviye atlamasın diye; görünüm lab'ın `h3`'üyle aynı); "Süre · …" `h3`.

## Plandan sapmalar
1. **Durum duyurusu hata hâlinde boş** (plan: hata → `messageFor(errorCode)`). Aynı metin balonda `role="alert"` ile zaten duyuruluyor; ikisi birlikte olunca ekran okuyucu iki kez okur ve testte aynı metin iki yerde bulunur. Lead onayladı (kartı değiştirmiyor).
2. **Kaynak düğmesinde `aria-label` yok, sadece `title`** (plan: ikisi de `${sourceName} · sayfa ${pageNumber}`). `aria-label` görünen metinden farklı bir ad verir (WCAG 2.5.3) ve planın F13 ("Raşit Bankası Ürün ve H… · s. 19" düğmesi) ile E1 ("· s." düğmesi) testlerini kırar. Tam ad üstüne gelince `title` ile görünür (kart soru 2). Lead onayladı.
3. **`ContextChunksCard` props'u** `{ turn }` yerine `{ question, sources, done, onChunkOpen }`: sayfa sadece üçü de doluyken kartı gösteriyor; tip `null` kontrolü bileşen içinde tekrarlanmıyor. Davranış aynı.
4. **Akış ortasında ağ kopması** (`reader.read()` reddi) `STREAM_INTERRUPTED` sayılıyor (plan sadece "`done`/`error` olmadan biterse" diyor; sse.md "ağ koptu"yu da bu koda bağlıyor). Abort yine sessiz.

## Doğrulama
- `cd web && npm run build` → geçti (`tsc -b` + `vite build`, hata yok).
- `cd web && npm run lint` (oxlint) → geçti, 0 uyarı.
- `cd web && npm test` → 14 dosya, 238 test: 237 geçti, **1 kaldı**: `src/app/App.test.tsx` › `givenScreenPath_whenRendered_thenShowsLabBreadcrumbTitleAndDescription /onizleme`. Sebep: test `getByText('Dijital Asistan')` ile breadcrumb'ı arıyor; Önizleme artık lab'daki gibi telefonda da "Dijital Asistan" yazıyor (plan `PhoneFrame`), metin iki yerde. Uygulama hatası değil; test güncellenmeli (tester, ör. `getByText('Dijital Asistan', { selector: 'span' })`).
- `grep -rn "dangerouslySetInnerHTML\|innerHTML\|eval(\|new Function" web/src` → boş. Preview CSS'lerinde ham hex yok.
- Elle (Node 26 ile scratch betik, test dosyası değil): bölünmüş olay, CRLF + yorum + boşluksuz `event:done`, bozuk/bilinmeyen olay → `null`, F5/F6/F7 örnekleri plandaki sonuçları verdi.
- Elle (Playwright ile dev server, `page.route` ile sahte SSE): akan cevap, alıntı → `ChunkDialog` açıldı, Esc sonrası odak "Kaynak 1"de; kaynak düğmeleri, rerank kartı ve süre çubukları lab görünümünde; akış içi `error`, 503, `done`'suz bitiş, "Yeni konuşma" ayracı ("Yeni konuşma · 2"), boş bilgi bankası ipucu; iki konuşma için 2 `POST /conversations`, 4 mesaj isteği; 360 px'te yatay taşma yok; konsolda hata yok.

## Tester için notlar
- **MSW handler'ları:** `POST /api/assistant/conversations` (201 `ConversationResponse`), `POST /api/assistant/conversations/:conversationId/messages` (200 `text/event-stream` ya da `problem(...)`), alıntı/kart satırı için `GET /api/workspaces/42/chunks/:chunkId` (mevcut `chunkDetail` fixture'ı).
- **SSE sahteleme:** `ReadableStream` gövdeli `HttpResponse` (`Content-Type: text/event-stream`). Ayrıştırıcı `event: x` ve `event:x` biçimini, `\r\n`'i ve satır ortasından bölünen parçaları kabul eder. Akış sürerken kilit testi (F9) ve iptal testi (F18) için akışı kapatmayan (`controllableSseStream`) gövde gerekir; unmount'ta `request.signal.aborted` true olur.
- **Olay şekli sıkı kontrol ediliyor:** `sources`/`done` fixture'larında sse.md'deki tüm alanlar olmalı (`ContextChunk`'ta `rerankScore` dahil, `null` olabilir; `DoneEvent`'te `hostLabel`, `timeToFirstTokenMs` vb.). Eksik alanlı olay `null` sayılıp atlanır; ardından `done` gelmezse tur "Cevap yarıda kesildi." olur.
- **Konuşma açma:** ilk soruda (ve `CONVERSATION_NOT_FOUND`'dan ya da "Yeni konuşma"dan sonra) önce `POST /conversations`, sonra mesaj. Mesaj gövdesi trim'lenmiş metin.
- **Hata metni tek yerde:** balondaki `role="alert"` (`findByRole('alert')`). Görünmez duyuru bölgesi hata hâlinde boş; "Asistan yazıyor…" ve "Cevap tamamlandı." metinleri o bölgede (`aria-live="polite"`; görünmez ama DOM'da, `getByText` bulur).
- **"Gönder" düğmesi** kutu boşken/sadece boşlukken de pasif (`canSendMessage`); F8'deki "etkin" kontrolü için önce metin yazılmalı.
- **Kaynak düğmesinin adı** görünen metindir ("Raşit Bankası Ürün ve H… · s. 19"); tam ad `title`'da.
- **Yan kart satır düğmesinin adı** tüm metnidir ("[1] ✓ s. 19 · … · #31 rerank 5,36 · d 0,312" gibi); satır seçmek için `getAllByRole('button', { name: /^\[3\]/ })` gibi bir kalıp işe yarar. Süre satırları ayrı `span`'larda (etiket, çubuk, değer).
- **`App.test.tsx`'teki /onizleme durumu** yukarıdaki sebeple güncellenmeli.
- **Saat:** telefonun durum çubuğu `aria-hidden`; sorunun altındaki saat `formatTime(askedAt)` (Istanbul). Saati sabitlemek için `vi.setSystemTime` kullanılabilir.
- **Kaydırma:** `MessageList` `useLayoutEffect` içinde `scrollHeight`/`offsetTop` kullanır; jsdom'da hepsi 0 olduğu için sadece `scrollTop = 0` olur, test etkilenmez.

## Düzeltme
Yok.

STATUS: DONE
