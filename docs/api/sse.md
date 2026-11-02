# Cevap akışı sözleşmesi (SSE)

> AMAÇ: assistant-api'nin cevap akışının tek doğru kaynağı. Planner yazar; backend-coder olayları bu adlarla ve bu
> alanlarla gönderir, frontend-coder bu şekilde ayrıştırır, qa uyumu kontrol eder. Endpoint'in kendisi ve akış
> öncesi hatalar `docs/api/openapi.yaml`'dadır (`sendMessage`).
> Durum: TASK-008 (ilk akış). TASK-010: `sources` olayına `searchQuery` ve `queryRewriteStatus`, `done` olayına
> `queryRewriteLatencyMs` eklendi (takip sorusunu yeniden yazma). TASK-011: üretim modu Sadece LLM (`LLM_ONLY`):
> `sources` olayına `generationMode`, `RetrievalStrategy`'ye `NONE`; `sources.candidates`, `sources.topK`,
> `sources.searchQuery` ve `done.retrievalLatencyMs` Sadece LLM'de `null` olabilir. TASK-016: `ContextChunk`'a
> `containsAnswer` eklendi (golden set'teki sorunun cevabının geçtiği parça; Önizleme'de sarı işaret).

## Endpoint

```
POST /api/assistant/conversations/{conversationId}/messages
Content-Type: application/json
Accept: text/event-stream, application/problem+json

{ "text": "Kartımı kaybettim, ne yapmalıyım?" }
```

- Başarılı yanıt: `200`, `Content-Type: text/event-stream`, `Cache-Control: no-cache`, `X-Accel-Buffering: no`.
- Akış, sorgu yeniden yazma (TASK-010) ve retrieval (ve rerank) adımları bittikten sonra açılır. Yeniden yazma hiçbir
  zaman hata döndürmez: başarısız olursa arama müşterinin yazdığı soruyla yapılır ve sonuç `sources.queryRewriteStatus`'ta görünür. Akış açılmadan oluşan hatalar (400 `VALIDATION_FAILED`,
  404 `CONVERSATION_NOT_FOUND`, 409 `TURN_IN_PROGRESS`, 503 `RETRIEVAL_UNAVAILABLE`, …) normal HTTP status +
  `application/problem+json` döner; hiç olay gönderilmez (`openapi.yaml`).
- `Accept` başlığında `application/problem+json` da bulunmalıdır; yoksa sunucu hata gövdesini yazamaz.
- İstemci bağlantıyı kapatırsa (sekme kapandı, sayfadan çıkıldı, `AbortController.abort()`) sunucu dil modeli
  çağrısını iptal eder; tur `STREAM_CANCELLED` hata koduyla, o ana kadar üretilen metinle kaydedilir. İstemciye olay gitmez.

## Biçim

Her olay iki satır ve bir boş satırdır:

```
event: <ad>
data: <tek satır JSON>

```

- `event` adları küçük harftir: `token`, `sources`, `done`, `error`. Başka olay gönderilmez; `id:` ve `retry:` alanları
  kullanılmaz. İstemci bilmediği olayı atlar.
- `data` her zaman tek satırlık JSON nesnesidir; alanlar camelCase. Metin içindeki satır sonları JSON içinde `\n` olarak kaçışlanır.
- Satır sonu `\n`'dir. İstemci `\r\n`'i de kabul eder.
- Alan adından sonraki iki noktadan sonra boşluk isteğe bağlıdır (SSE standardı: değerin başındaki tek boşluk atılır).
  Sunucu (Spring `SseEmitter`) boşluksuz yazar: `event:token`, `data:{"text":"…"}`. İstemci ikisini de kabul eder;
  bu dosyadaki örnekler okunurluk için boşlukludur.

## Sıra

Başarılı akış:

```
token (0 ya da daha fazla; model boş cevap verirse 0) → sources (1 kez) → done (1 kez) → bağlantı kapanır
```

Hatalı akış:

```
token (0 ya da daha fazla) → error (1 kez) → bağlantı kapanır
```

- `error`'dan sonra `sources` ve `done` gelmez. `error`'dan önce gelen token'lar yarım cevaptır.
- `sources` ve `done`, tur veritabanına yazılıp commit edildikten sonra gönderilir.
- Bilgi bankasında aranabilir parça yoksa (retrieval boş liste döndü) dil modeli çağrılmaz: tek bir `token` olayı sabit
  "bulamadım" cümlesini taşır, ardından `sources` (`contextChunks: []`, `citations: []`) ve `done` (`notFoundAnswer: true`) gelir.
  Bu kural sadece `RAG` modundadır.
- Sadece LLM (`generationMode` `LLM_ONLY`, TASK-011): sıra aynıdır (`token`* → `sources` → `done`). Yeniden yazma,
  retrieval ve rerank yapılmaz; bilgi bankası boş olsa da dil modeli çağrılır. `sources` olayı parçasız gelir (aşağıda).
- Akış `done` ya da `error` olmadan biterse (ağ koptu, proxy kesti) istemci bunu `STREAM_INTERRUPTED` sayar (bu kod
  sunucudan gelmez, sadece web'dedir).

## Olaylar

### `token` — `TokenEvent`
Cevabın bir sonraki metin parçası. Parçalar sırayla birleştirilir; sınırları kelime ya da karakter sınırı değildir.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `text` | string | Boş olmayan metin parçası |

```
event: token
data: {"text":"Kartını hemen "}
```

### `sources` — `SourcesEvent`
Modele giden parçalar ve cevaptaki alıntılar. Bir kez, son `token`'dan sonra.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `generationMode` | `GenerationMode` | Bu turun üretim modu: `RAG` ya da `LLM_ONLY` (Sadece LLM; TASK-011). Değerler `openapi.yaml` `GenerationMode` |
| `retrievalStrategy` | `RetrievalStrategy` | Bu turda gerçekten uygulanan strateji (aşağıda); `LLM_ONLY`'de her zaman `NONE` |
| `candidates` | integer \| null | Yapılandırmadaki aday sayısı (`assistant.retrieval.candidates`, varsayılan 20). Ekrandaki "rerank 20→5" metninin ilk sayısı. `LLM_ONLY`'de `null` |
| `topK` | integer \| null | Yapılandırmadaki prompt'a girecek parça sayısı (`assistant.retrieval.top-k`, varsayılan 5). "top-5" ve "20→5" metninin sayısı. `LLM_ONLY`'de `null` |
| `searchQuery` | string \| null | retrieval-api'ye (ve reranker'a) giden arama sorgusu. `queryRewriteStatus` `REWRITTEN` ise modelin yazdığı bağımsız sorgu; diğer bütün `RAG` durumlarında müşterinin yazdığı soru (baştaki/sondaki boşluk silinmiş). Ekranda `· arama: "…"` (TASK-010). `LLM_ONLY`'de `null` (arama yapılmadı) |
| `queryRewriteStatus` | `QueryRewriteStatus` | Bu turda sorgu yeniden yazmanın sonucu (aşağıda); `LLM_ONLY`'de her zaman `DISABLED` |
| `contextChunks` | `ContextChunk[]` | Prompt'a giren parçalar, prompt'taki sırayla (`n` = 1, 2, …). En fazla `topK`; bilgi bankasında daha az parça varsa daha az; boş bilgi bankasında `[]`; `LLM_ONLY`'de her zaman `[]` |
| `citations` | integer[] | Cevapta geçen geçerli alıntı numaraları, cevaptaki ilk geçiş sırasıyla, tekrarsız. Sadece 1 ile `contextChunks.length` arasındaki `[n]`'ler; aralık dışı (`[0]`, `[6]`) yok sayılır. `LLM_ONLY`'de alıntı ayrıştırılmaz, her zaman `[]` (cevapta `[1]` yazsa bile) |

`RetrievalStrategy`:
- `TOP_K`: yapılandırma `top5`; vector sırasının ilk `topK` parçası, reranker çağrılmadı. Ekran: "top-5".
- `RERANK`: yapılandırma `rerank`; `candidates` aday puanlandı, puanı en yüksek `topK` parça (eşitlikte vector sırası önce). Ekran: "rerank 20→5".
- `RERANK_FALLBACK`: yapılandırma `rerank` ama reranker zaman aşımına uğradı ya da hata döndü; vector sırasının ilk `topK` parçası. Ekran: "reranker DOWN · vector sırası".
- `NONE`: `generationMode` `LLM_ONLY`; retrieval yapılmadı (TASK-011). Sadece bu modda.

`QueryRewriteStatus` (TASK-010):
- `DISABLED`: `assistant.query-rewrite.enabled=false` ya da `generationMode` `LLM_ONLY` (arama olmadığı için yeniden yazma yapılmaz, TASK-011); dil modeli yeniden yazma için çağrılmadı. `RAG`'de `searchQuery` = soru, `LLM_ONLY`'de `null`.
- `NO_HISTORY`: konuşmada cevabı tamamlanmış (hata kodu olmayan) önceki tur yok; konuşmanın ilk turu her zaman böyledir. Model çağrılmadı. `searchQuery` = soru.
- `REWRITTEN`: model geçmişle birlikte bağımsız bir sorgu yazdı; arama onunla yapıldı. `searchQuery` = modelin sorgusu (model soruyu aynen tekrar ettiyse soruyla aynı olabilir).
- `FAILED`: model hata verdi, bağlanılamadı, `assistant.query-rewrite.timeout` içinde bitirmedi ya da sorgu `assistant.query-rewrite.max-length`'ten (1000) uzundu. `searchQuery` = soru.
- `EMPTY`: model boş (ya da sadece boşluk / etiket) döndü. `searchQuery` = soru.

`ContextChunk`:

| Alan | Tip | Açıklama |
|------|-----|----------|
| `n` | integer | Prompt'taki numara (1'den başlar); cevaptaki `[n]` bununla eşleşir |
| `chunkId` | integer (int64) | retrieval-api parça kimliği; parça penceresi `GET /api/workspaces/42/chunks/{chunkId}` ile açılır |
| `chunkNo` | integer | Parçanın kendi sürümündeki sıra numarası (0'dan başlar); ekranda `#31` |
| `pageNumber` | integer | Parçanın ilk satırının PDF sayfası |
| `heading` | string | Parçanın başlığı |
| `sourceId` | integer (int64) | Kaynak kimliği |
| `sourceName` | string | Kaynağın (belgenin) adı |
| `version` | string | Parçanın ait olduğu kaynak sürümünün etiketi (ör. `2026.10`) |
| `vectorRank` | integer | retrieval-api sonucundaki sıra (1'den başlar; mesafe artan) |
| `distance` | number | Cosine mesafesi (retrieval-api `distance`) |
| `rerankScore` | number \| null | Reranker'ın ham puanı (logit; 0-1 arası değildir, eksi olabilir). Sadece `RERANK`'te dolu; `TOP_K` ve `RERANK_FALLBACK`'te `null` |
| `containsAnswer` | boolean | TASK-016: müşterinin yazdığı soru (kırpılmış) normalize edilince güncel golden set'teki bir sorunun normalize metnine eşitse ve bu parça o sorunun `answerText`'ini içeriyorsa `true` (eşleşme kuralı `openapi.yaml` `TurnStatus` ve `findChunkMatches`). Soru golden set'te yoksa, golden set yüklenmemişse ya da retrieval-api'nin `findChunkMatches` çağrısı başarısızsa bütün parçalarda `false`; bu çağrının hatası cevabı etkilemez. Hesap retrieval'dan sonra, akış açılmadan yapılır |

```
event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Kartımı kaybettim, ne yapmalıyım?","queryRewriteStatus":"NO_HISTORY","contextChunks":[{"n":1,"chunkId":151,"chunkNo":31,"pageNumber":19,"heading":"2.16 Kayıp ya da çalıntı bildirimi","sourceId":10001,"sourceName":"Raşit Bankası Ürün ve Hizmet Rehberi","version":"2026.10","vectorRank":1,"distance":0.3124,"rerankScore":5.36,"containsAnswer":true}],"citations":[1]}
```

### `done` — `DoneEvent`
Tur kaydedildi; akış başarıyla bitti. Son olay.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `turnId` | integer (int64) | Kaydedilen turun kimliği |
| `notFoundAnswer` | boolean | Cevap sabit "Bu konuda rehberimizde bilgi bulamadım." cümlesini içeriyorsa `true` ("cevap yok" turu). `true` ise web "Rehber · s. N" düğmelerini göstermez. `LLM_ONLY`'de tespit yapılmaz, her zaman `false` |
| `chatModel` | string | Cevabı üreten dil modeli (yapılandırmadaki ad, ör. `qwen3.5:9b`) |
| `promptTokens` | integer \| null | Dil modelinin saydığı prompt token'ı; model çağrılmadıysa ya da sayı gelmediyse `null` |
| `completionTokens` | integer \| null | Dil modelinin saydığı cevap token'ı; aynı kural (yeniden yazma çağrısının token'ları dahil değildir) |
| `queryRewriteLatencyMs` | integer \| null | Sorgu yeniden yazma çağrısı (istek → son token ya da hata/zaman aşımı). `REWRITTEN`, `FAILED` ve `EMPTY`'de dolu; `DISABLED` ve `NO_HISTORY`'de `null` (TASK-010). Ekranda "yeniden yazma" süre satırı |
| `retrievalLatencyMs` | integer \| null | retrieval-api araması (istek gönderildi → yanıt okundu); `LLM_ONLY`'de `null` |
| `rerankLatencyMs` | integer \| null | Reranker çağrısı; `RERANK_FALLBACK`'te başarısız denemenin süresi; `TOP_K` ve `LLM_ONLY`'de `null` |
| `generationLatencyMs` | integer \| null | Dil modeli çağrısı (istek → son token); model çağrılmadıysa `null` |
| `timeToFirstTokenMs` | integer \| null | Dil modeli çağrısından ilk token'a kadar; model çağrılmadıysa `null` |
| `latencyMs` | integer | Toplam: isteğin assistant-api'ye gelişi → cevabın son token'ı (kayıt hariç; yeniden yazma dahil) |
| `hostLabel` | string \| null | Modellerin çalıştığı makinenin etiketi (`assistant.host-label`, ör. "Apple M5 · 16 GB"); ayar boşsa `null`. Onay kartı soru 3'e bağlıdır |

Süreler milisaniyedir, tam sayıya yuvarlanır.

```
event: done
data: {"turnId":12,"notFoundAnswer":false,"chatModel":"qwen3.5:9b","promptTokens":2154,"completionTokens":61,"queryRewriteLatencyMs":null,"retrievalLatencyMs":84,"rerankLatencyMs":712,"generationLatencyMs":3920,"timeToFirstTokenMs":1840,"latencyMs":4731,"hostLabel":"Apple M5 · 16 GB"}
```

### `error` — `StreamErrorEvent`
Akış açıldıktan sonraki hata. Son olay; ardından bağlantı kapanır. Tur bu `errorCode` ve o ana kadarki metinle kaydedilir.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `errorCode` | string | Aşağıdaki kodlardan biri |

- `CHAT_MODEL_UNAVAILABLE`: dil modeline bağlanılamadı, hata döndü, model bulunamadı ya da zamanında cevap vermedi
  (ilk token için `assistant.generation.first-token-timeout`, token'lar arası `assistant.generation.token-idle-timeout`).
  İlk token gelmeden de olabilir.
- `INTERNAL_ERROR`: beklenmeyen hata (ör. tur kaydı yazılamadı).

```
event: error
data: {"errorCode":"CHAT_MODEL_UNAVAILABLE"}
```

## Örnek akış

```
event: token
data: {"text":"Kartını hemen Raşit Mobil'den "}

event: token
data: {"text":"geçici olarak kapat [1]."}

event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Kartımı kaybettim, ne yapmalıyım?","queryRewriteStatus":"NO_HISTORY","contextChunks":[…5 parça…],"citations":[1]}

event: done
data: {"turnId":12,"notFoundAnswer":false,"chatModel":"qwen3.5:9b","promptTokens":2154,"completionTokens":61,"queryRewriteLatencyMs":null,"retrievalLatencyMs":84,"rerankLatencyMs":712,"generationLatencyMs":3920,"timeToFirstTokenMs":1840,"latencyMs":4731,"hostLabel":null}

```

Aynı konuşmanın ikinci sorusu ("Yenisi kaç günde gelir?"), yeniden yazma açık (TASK-010):

```
event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Kaybolan kartın yenisi kaç günde gelir?","queryRewriteStatus":"REWRITTEN","contextChunks":[…5 parça…],"citations":[1]}

event: done
data: {"turnId":13,"notFoundAnswer":false,"chatModel":"qwen3.5:9b","promptTokens":2410,"completionTokens":38,"queryRewriteLatencyMs":812,"retrievalLatencyMs":79,"rerankLatencyMs":690,"generationLatencyMs":2950,"timeToFirstTokenMs":1610,"latencyMs":4561,"hostLabel":null}

```

Sadece LLM (`assistant.generation.mode=llm`, TASK-011), "Kartımı kaybettim, ne yapmalıyım?". Cevapta `[1]` geçse bile
`citations` boştur:

```
event: token
data: {"text":"Kartını hemen bankanı arayarak kapattır [1]."}

event: sources
data: {"generationMode":"LLM_ONLY","retrievalStrategy":"NONE","candidates":null,"topK":null,"searchQuery":null,"queryRewriteStatus":"DISABLED","contextChunks":[],"citations":[]}

event: done
data: {"turnId":14,"notFoundAnswer":false,"chatModel":"qwen3.5:9b","promptTokens":58,"completionTokens":44,"queryRewriteLatencyMs":null,"retrievalLatencyMs":null,"rerankLatencyMs":null,"generationLatencyMs":2210,"timeToFirstTokenMs":640,"latencyMs":2236,"hostLabel":null}

```

## İsimler

| Kavram | SSE / JSON | Backend (Java record) | Web (TypeScript type) |
|--------|-----------|-----------------------|-----------------------|
| Token olayı | `token` | `TokenEvent` | `TokenEvent` |
| Kaynaklar olayı | `sources` | `SourcesEvent` | `SourcesEvent` |
| Modele giden parça | `contextChunks[]` | `ContextChunk` | `ContextChunk` |
| Yeniden yazmanın sonucu | `queryRewriteStatus` | `QueryRewriteStatus` (enum) | `QueryRewriteStatus` (`QUERY_REWRITE_STATUSES`) |
| Üretim modu | `generationMode` | `GenerationMode` (enum: `RAG`, `LLM_ONLY`) | `GenerationMode` (`GENERATION_MODES`, `shared/liveConfig/types.ts`) |
| Retrieval stratejisi | `retrievalStrategy` | `RetrievalStrategy` (enum: `TOP_K`, `RERANK`, `RERANK_FALLBACK`, `NONE`) | `RetrievalStrategy` (`RETRIEVAL_STRATEGIES`) |
| Bitti olayı | `done` | `DoneEvent` | `DoneEvent` |
| Akış içi hata | `error` | `StreamErrorEvent` | `StreamErrorEvent` |
| Olayların birleşimi | — | — | `AnswerStreamEvent` (ayrık birleşim, `type` alanı olay adı) |
