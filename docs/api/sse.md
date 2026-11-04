# Cevap akışı sözleşmesi (SSE)

> AMAÇ: assistant-api'nin cevap akışının tek doğru kaynağı. Planner yazar; backend-coder olayları bu adlarla ve bu
> alanlarla gönderir, frontend-coder bu şekilde ayrıştırır, qa uyumu kontrol eder. Endpoint'in kendisi ve akış
> öncesi hatalar `docs/api/openapi.yaml`'dadır (`sendMessage`).
> Durum: TASK-008 (ilk akış). TASK-010: `sources` olayına `searchQuery` ve `queryRewriteStatus`, `done` olayına
> `queryRewriteLatencyMs` eklendi (takip sorusunu yeniden yazma). TASK-011: üretim modu Sadece LLM (`LLM_ONLY`):
> `sources` olayına `generationMode`, `RetrievalStrategy`'ye `NONE`; `sources.candidates`, `sources.topK`,
> `sources.searchQuery` ve `done.retrievalLatencyMs` Sadece LLM'de `null` olabilir. TASK-016: `ContextChunk`'a
> `containsAnswer` eklendi (golden set'teki sorunun cevabının geçtiği parça; Önizleme'de sarı işaret).
> TASK-031: `done` olayına `answer` (kaydedilen son cevap metni; web akan metnin yerine bunu gösterir) ve
> `answerSource` (`AnswerSource`: cevabın hangi yoldan geldiği) eklendi. Selamlaşma turu (`SMALL_TALK`; kalıp listesi
> ya da eşik altında etiket) ve reranker puan eşiği (`BELOW_RERANK_THRESHOLD`) yolları; "bulamadım" cevabında `citations` her zaman `[]`; cevap "bulamadım"
> cümlesiyle başlıyorsa cümle tamamlanana kadar `token` gönderilmez. TASK-031 düzeltme turu 1: "bulamadım"
> tespiti "rehberimizde … bulamadım" kalıbını da kapsar; bu kalıpta metin akarken görünür, `done.answer` sabit cümledir.
> TASK-032: selamlaşma kalıp listesi kaldırıldı. Kısa mesaj (tanım "Sıra"da) konuşmanın ilk mesajı da olsa aramadan
> önce etiketlenir; selamlaşma turu artık arama öncesi etiketle ya da (uzun mesajda) eşik altında etiketle tanınır.
> Olay adları ve alanlar değişmedi. TASK-032 düzeltme turu 1: `AnswerSource`'a `NO_WORDS` eklendi (harf ya da rakam
> içermeyen mesaj; arama ve dil modeli yok, sabit "bulamadım").

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
  Bu kural sadece `RAG` modundadır. `done.answerSource` `EMPTY_KNOWLEDGE_BASE`.
- Harf ya da rakam içermeyen mesaj (TASK-032 düzeltme turu 1, sadece `RAG`; ör. "???", "🙂"): öneri metinleriyle aynı
  normalizasyondan sonra metin boşsa etiket, yeniden yazma, retrieval, rerank ve dil modeli çağrılmaz: tek `token` sabit
  "bulamadım" cümlesi, `sources` (`generationMode` `RAG`, `retrievalStrategy` `NONE`, `candidates`/`topK`/`searchQuery`
  `null`, `queryRewriteStatus` `DISABLED`, `contextChunks: []`, `citations: []`), `done` (`notFoundAnswer: true`,
  `answerSource` `NO_WORDS`, model süreleri ve token'ları `null`). retrieval-api kapalıyken de böyledir. Bu kontrol
  aşağıdaki etiket adımından önce yapılır.
- Selamlaşma, arama öncesi etiket (TASK-032, sadece `RAG`; TASK-031'deki kalıp listesinin yerine): mesaj kısaysa,
  konuşmanın ilk mesajı olup olmamasından ve eşikten bağımsız olarak, yeniden yazmadan ve aramadan önce dil modeline
  müşterinin mesajı tek başına verilip sadece etiket sorulur (selam / teşekkür / veda / diğer; parça ve geçmiş verilmez,
  model cevap yazmaz). Kısa mesaj: normalize edilmiş metin (öneri metinleriyle aynı normalizasyon) en az 1 kelime ve
  `?` içermiyorsa en fazla 10 kelime, `?` içeriyorsa en fazla 5 kelime (onay kartı sorusu 1'e bağlı). Etiket selam,
  teşekkür ya da vedaysa yeniden yazma, retrieval, rerank ve cevap modeli çağrılmaz: tek `token` hazır karşılık metni
  (`assistant.small-talk.*-answer`), `sources` (`generationMode` `RAG`, `retrievalStrategy` `NONE`,
  `candidates`/`topK`/`searchQuery` `null`, `queryRewriteStatus` `DISABLED`, `contextChunks: []`, `citations: []`),
  `done` (`notFoundAnswer: false`, `answerSource` `SMALL_TALK`, cevap modelinin süreleri ve token'ları `null`; etiket
  çağrısının süresi `latencyMs`'e dahildir). retrieval-api kapalıyken de böyledir. Etiket diğer, hata, zaman aşımı ya
  da tanımsız çıktıysa akış normal yoldan sürer (yeniden yazma, retrieval, eşik); eşik altında etiket yeniden
  sorulmaz, "bulamadım" verilir.
- Reranker puan eşiği (TASK-031, sadece `RAG` + `retrievalStrategy` `RERANK`; değer TASK-032'de ölçümle yeniden seçildi):
  `assistant.retrieval.min-rerank-score` doluysa ve prompt'a girecek ilk parçanın (`rerankScore` en yüksek) puanı bu
  değerden küçükse cevap için dil modeli çağrılmaz. Mesaj kısaysa etiket aramadan önce zaten sorulmuştur ve diğer
  çıkmıştır: "bulamadım" verilir. Uzun mesajda önce dil modeline müşterinin mesajı tek başına verilip sadece etiket sorulur (selam / teşekkür / veda /
  diğer; parça ve geçmiş verilmez, model cevap yazmaz). Etiket selam, teşekkür ya da vedaysa tek `token` hazır karşılık
  metni ve `done.answerSource` `SMALL_TALK` (`notFoundAnswer: false`); diğer, hata, zaman aşımı ya da tanımsız çıktıda
  tek `token` sabit "bulamadım" cümlesi ve `done.answerSource` `BELOW_RERANK_THRESHOLD` (`notFoundAnswer: true`). İki
  durumda da `sources` strateji ve arama alanları normal dolu, `contextChunks: []`, `citations: []`; `done`'da cevap
  modelinin süre ve token alanları `null` (etiket çağrısının süresi `latencyMs`'e dahildir). `TOP_K` ve
  `RERANK_FALLBACK`'te eşik ve etiket yoktur (reranker puanı yok).
- "Bulamadım" cümlesinin tutulması (TASK-031, sadece `RAG`, dil modeli yolu): modelin metni (baştaki boşluk atlanarak,
  harf büyüklüğü ve boşluk farkı yok sayılarak) "bulamadım" cümlesinin başı olduğu sürece `token` gönderilmez. Metin
  cümleden ayrılırsa tutulan metin tek `token`'da gönderilir ve akış normal sürer. Metin cümleyi tamamlarsa cümle
  (yapılandırmadaki yazımıyla) tek `token`'da gönderilir; modelin sonrasında yazdığı her şey (`[5]` gibi numaralar
  dahil) gönderilmez. Model cümle bitmeden durursa tutulan metin `sources`'tan önce gönderilir. Tutma sadece sabit
  cümlenin başına bakar; "Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]" gibi başka kelimelerle yazılmış
  "bulamadım" cevabı akarken olduğu gibi gider, akış bitince `done.answer` sabit cümle, `citations` `[]` olur
  (düzeltme turu 1).
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
| `retrievalStrategy` | `RetrievalStrategy` | Bu turda gerçekten uygulanan strateji (aşağıda); `LLM_ONLY`'de ve arama yapılmadan tanınan selamlaşma turunda (arama öncesi etiket) ve harf/rakamsız mesaj turunda (`NO_WORDS`) (TASK-031) her zaman `NONE` |
| `candidates` | integer \| null | Yapılandırmadaki aday sayısı (`assistant.retrieval.candidates`, varsayılan 20). Ekrandaki "rerank 20→5" metninin ilk sayısı. `LLM_ONLY`'de ve arama yapılmadan tanınan selamlaşma turunda (arama öncesi etiket) ve harf/rakamsız mesaj turunda (`NO_WORDS`) `null` |
| `topK` | integer \| null | Yapılandırmadaki prompt'a girecek parça sayısı (`assistant.retrieval.top-k`, varsayılan 5). "top-5" ve "20→5" metninin sayısı. `LLM_ONLY`'de ve arama yapılmadan tanınan selamlaşma turunda (arama öncesi etiket) ve harf/rakamsız mesaj turunda (`NO_WORDS`) `null` |
| `searchQuery` | string \| null | retrieval-api'ye (ve reranker'a) giden arama sorgusu. `queryRewriteStatus` `REWRITTEN` ise modelin yazdığı bağımsız sorgu; diğer bütün `RAG` durumlarında müşterinin yazdığı soru (baştaki/sondaki boşluk silinmiş). Ekranda `· arama: "…"` (TASK-010). `LLM_ONLY`'de ve arama yapılmadan tanınan selamlaşma turunda (arama öncesi etiket) ve harf/rakamsız mesaj turunda (`NO_WORDS`) `null` (arama yapılmadı) |
| `queryRewriteStatus` | `QueryRewriteStatus` | Bu turda sorgu yeniden yazmanın sonucu (aşağıda); `LLM_ONLY`'de ve arama yapılmadan tanınan selamlaşma turunda (arama öncesi etiket) ve harf/rakamsız mesaj turunda (`NO_WORDS`) her zaman `DISABLED` |
| `contextChunks` | `ContextChunk[]` | Prompt'a giren parçalar, prompt'taki sırayla (`n` = 1, 2, …). En fazla `topK`; bilgi bankasında daha az parça varsa daha az; boş bilgi bankasında, eşik altında kalan turda (`BELOW_RERANK_THRESHOLD`), selamlaşma turunda (iki yoldan da), harf/rakamsız mesaj turunda (`NO_WORDS`) ve `LLM_ONLY`'de `[]` |
| `citations` | integer[] | Cevapta geçen geçerli alıntı numaraları, cevaptaki ilk geçiş sırasıyla, tekrarsız. Sadece 1 ile `contextChunks.length` arasındaki `[n]`'ler; aralık dışı (`[0]`, `[6]`) yok sayılır. "Bulamadım" cevabında (`done.notFoundAnswer` `true`) model numara yazmış olsa da her zaman `[]` (TASK-031). `LLM_ONLY`'de alıntı ayrıştırılmaz, her zaman `[]` (cevapta `[1]` yazsa bile) |

`RetrievalStrategy`:
- `TOP_K`: yapılandırma `top5`; vector sırasının ilk `topK` parçası, reranker çağrılmadı. Ekran: "top-5".
- `RERANK`: yapılandırma `rerank`; `candidates` aday puanlandı, puanı en yüksek `topK` parça (eşitlikte vector sırası önce). Ekran: "rerank 20→5".
- `RERANK_FALLBACK`: yapılandırma `rerank` ama reranker zaman aşımına uğradı ya da hata döndü; vector sırasının ilk `topK` parçası. Ekran: "reranker DOWN · vector sırası".
- `NONE`: `generationMode` `LLM_ONLY`; retrieval yapılmadı (TASK-011). Sadece bu modda.

`QueryRewriteStatus` (TASK-010):
- `DISABLED`: `assistant.query-rewrite.enabled=false`, `generationMode` `LLM_ONLY` (arama olmadığı için yeniden yazma yapılmaz, TASK-011) , arama yapılmadan tanınan selamlaşma turu (TASK-031, TASK-032: arama öncesi etiket) ya da harf/rakamsız mesaj turu (`NO_WORDS`); dil modeli yeniden yazma için çağrılmadı. `RAG`'de `searchQuery` = soru (arama yapılmayan selamlaşmada ve `NO_WORDS`'te `null`), `LLM_ONLY`'de `null`.
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
| `notFoundAnswer` | boolean | Modelin cevabı sabit "Bu konuda rehberimizde bilgi bulamadım." cümlesini içeriyorsa (harf büyüklüğü ve boşluk farkı yok sayılır) ya da aynı cümle içinde önce "rehberimizde", sonra "bulamadım" geçiyorsa (ör. "Diğer bankaların EFT ücretlerini rehberimizde bulamadım. [3]"; TASK-031 düzeltme turu 1), eşik altında kalan turda, boş bilgi bankasında ve harf/rakamsız mesajda (`NO_WORDS`, TASK-032) `true` ("cevap yok" turu). `true` ise `answer` tam olarak bu cümledir, `sources.citations` `[]`; web "Rehber · s. N" düğmelerini göstermez. Selamlaşma turunda `false`. `LLM_ONLY`'de tespit yapılmaz, her zaman `false` |
| `answer` | string | TASK-031. Turun kaydedilen son cevap metni. "Bulamadım" turunda sadece cümle (yapılandırmadaki yazımıyla); `RAG` model cevabında aralık dışı alıntı işaretleri (`[0]`, `[6]`; önündeki boşlukla) silinmiş ve baştaki/sondaki boşluk atılmış metin. Cevap üç sabit giriş cümlesinden biriyle başlıyorsa (`assistant.generation.*-opener`; kişisel hesap bilgisi, başka bankalar, yatırım tavsiyesi) o cümlenin içindeki ve hemen ardından gelen (arada sadece boşluk olan, bir ya da daha fazla) `[n]` işaretleri de silinir; `sources.citations` bu temizlenmiş metinden hesaplanır (TASK-031 düzeltme turu 1). Bu üç giriş cümlesinden biri cevabın ilk cümlesi değilse (cevabın ortasında ya da sonunda), o cümle arkasındaki `[n]` işaretleriyle birlikte silinir; `sources.citations` silinen cümlenin numaralarını içermez (TASK-032). Silinen numaralar ve cümleler akış sırasında `token`'larda görünmüş olabilir; `LLM_ONLY`'de akan metnin aynısı. Web, `done` gelince ekrandaki akan metni bununla değiştirir. Model boş cevap verdiyse `""` |
| `answerSource` | `AnswerSource` | TASK-031. Cevabın hangi yoldan geldiği (aşağıda) |
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

`AnswerSource` (TASK-031; aynı değerler tur kaydında `answer_source` kolonunda):
- `MODEL`: cevabı dil modeli yazdı (`RAG` ya da `LLM_ONLY`). Model cevabı "bulamadım" cümlesi olsa da `MODEL`'dir.
- `EMPTY_KNOWLEDGE_BASE`: `RAG`, retrieval boş liste döndü; model çağrılmadı, cevap sabit "bulamadım" cümlesi. Web
  sadece bu değerde "Bilgi bankasında henüz belge yok." notunu gösterir.
- `BELOW_RERANK_THRESHOLD`: `RAG` + `RERANK`, ilk parçanın reranker puanı `assistant.retrieval.min-rerank-score`'dan
  küçük ve etiket "diğer" (ya da etiket alınamadı); cevap modeli çağrılmadı, cevap sabit "bulamadım" cümlesi,
  `contextChunks` `[]`.
- `SMALL_TALK`: `RAG`, mesaj selamlaşma / teşekkür / veda; cevap hazır karşılık metni, `contextChunks` `[]`. Kısa
  mesajda arama öncesi etiketle tanındıysa arama yapılmadı (`retrievalStrategy` `NONE`); uzun mesajda eşik altında
  etiketle tanındıysa arama yapıldı (`retrievalStrategy` `RERANK`, `searchQuery` dolu). TASK-031'deki kalıp listesi
  yolu TASK-032'de kaldırıldı; eski kayıtlarda o yoldan gelen turlar da `NONE` + `SMALL_TALK`'tır.
- `NO_WORDS` (TASK-032 düzeltme turu 1): `RAG`, mesajda harf ya da rakam yok; etiket, arama ve dil modeli çağrılmadı,
  cevap sabit "bulamadım" cümlesi (`notFoundAnswer: true`), `retrievalStrategy` `NONE`, `contextChunks` `[]`. Web
  Önizleme'de bu turda "Son cevapta modele giden parçalar" kartını göstermez.

```
event: done
data: {"turnId":12,"notFoundAnswer":false,"answer":"Kartını hemen Raşit Mobil'den geçici olarak kapat [1].","answerSource":"MODEL","chatModel":"qwen3.5:9b","promptTokens":2154,"completionTokens":61,"queryRewriteLatencyMs":null,"retrievalLatencyMs":84,"rerankLatencyMs":712,"generationLatencyMs":3920,"timeToFirstTokenMs":1840,"latencyMs":4731,"hostLabel":"Apple M5 · 16 GB"}
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
data: {"turnId":12,"notFoundAnswer":false,"answer":"Kartını hemen Raşit Mobil'den geçici olarak kapat [1].","answerSource":"MODEL","chatModel":"qwen3.5:9b","promptTokens":2154,"completionTokens":61,"queryRewriteLatencyMs":null,"retrievalLatencyMs":84,"rerankLatencyMs":712,"generationLatencyMs":3920,"timeToFirstTokenMs":1840,"latencyMs":4731,"hostLabel":null}

```

Aynı konuşmanın ikinci sorusu ("Yenisi kaç günde gelir?"), yeniden yazma açık (TASK-010):

```
event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Kaybolan kartın yenisi kaç günde gelir?","queryRewriteStatus":"REWRITTEN","contextChunks":[…5 parça…],"citations":[1]}

event: done
data: {"turnId":13,"notFoundAnswer":false,"answer":"Yeni kartın 5 iş günü içinde adresine gelir [1].","answerSource":"MODEL","chatModel":"qwen3.5:9b","promptTokens":2410,"completionTokens":38,"queryRewriteLatencyMs":812,"retrievalLatencyMs":79,"rerankLatencyMs":690,"generationLatencyMs":2950,"timeToFirstTokenMs":1610,"latencyMs":4561,"hostLabel":null}

```

Sadece LLM (`assistant.generation.mode=llm`, TASK-011), "Kartımı kaybettim, ne yapmalıyım?". Cevapta `[1]` geçse bile
`citations` boştur:

```
event: token
data: {"text":"Kartını hemen bankanı arayarak kapattır [1]."}

event: sources
data: {"generationMode":"LLM_ONLY","retrievalStrategy":"NONE","candidates":null,"topK":null,"searchQuery":null,"queryRewriteStatus":"DISABLED","contextChunks":[],"citations":[]}

event: done
data: {"turnId":14,"notFoundAnswer":false,"answer":"Kartını hemen bankanı arayarak kapattır [1].","answerSource":"MODEL","chatModel":"qwen3.5:9b","promptTokens":58,"completionTokens":44,"queryRewriteLatencyMs":null,"retrievalLatencyMs":null,"rerankLatencyMs":null,"generationLatencyMs":2210,"timeToFirstTokenMs":640,"latencyMs":2236,"hostLabel":null}

```

Modelin "bulamadım" cevabına numara eklemesi (TASK-031). Model `Bu konuda rehberimizde bilgi bulamadım. [5]` yazar;
cümle tamamlanana kadar `token` gitmez, sonra cümle tek `token`'da gider, `[5]` hiç gitmez:

```
event: token
data: {"text":"Bu konuda rehberimizde bilgi bulamadım."}

event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Bitcoin alıp satabilir miyim?","queryRewriteStatus":"NO_HISTORY","contextChunks":[…5 parça…],"citations":[]}

event: done
data: {"turnId":15,"notFoundAnswer":true,"answer":"Bu konuda rehberimizde bilgi bulamadım.","answerSource":"MODEL","chatModel":"qwen3.5:9b","promptTokens":2088,"completionTokens":14,"queryRewriteLatencyMs":null,"retrievalLatencyMs":81,"rerankLatencyMs":705,"generationLatencyMs":1450,"timeToFirstTokenMs":1320,"latencyMs":2240,"hostLabel":null}

```

Eşik altında kalan soru (TASK-031; ör. "Bana bir şiir yaz", ilk parçanın puanı eşikten küçük):

```
event: token
data: {"text":"Bu konuda rehberimizde bilgi bulamadım."}

event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Bana bir şiir yaz","queryRewriteStatus":"NO_HISTORY","contextChunks":[],"citations":[]}

event: done
data: {"turnId":16,"notFoundAnswer":true,"answer":"Bu konuda rehberimizde bilgi bulamadım.","answerSource":"BELOW_RERANK_THRESHOLD","chatModel":"qwen3.5:9b","promptTokens":null,"completionTokens":null,"queryRewriteLatencyMs":null,"retrievalLatencyMs":79,"rerankLatencyMs":690,"generationLatencyMs":null,"timeToFirstTokenMs":null,"latencyMs":775,"hostLabel":null}

```

Eşik altında etiketle tanınan selamlaşma (TASK-031; uzun mesaj, kısa mesaj tanımına girmediği için aramadan önce
etiket sorulmadı, puanı eşikten küçük, etiket selam):

```
event: token
data: {"text":"Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?"}

event: sources
data: {"generationMode":"RAG","retrievalStrategy":"RERANK","candidates":20,"topK":5,"searchQuery":"Merhabalar, bugün sizinle sadece biraz tanışmak ve sohbet etmek istedim efendim","queryRewriteStatus":"NO_HISTORY","contextChunks":[],"citations":[]}

event: done
data: {"turnId":18,"notFoundAnswer":false,"answer":"Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?","answerSource":"SMALL_TALK","chatModel":"qwen3.5:9b","promptTokens":null,"completionTokens":null,"queryRewriteLatencyMs":null,"retrievalLatencyMs":80,"rerankLatencyMs":702,"generationLatencyMs":null,"timeToFirstTokenMs":null,"latencyMs":1910,"hostLabel":null}

```

Selamlaşma, arama öncesi etiket (TASK-032; "Selam", konuşmanın ilk mesajı; `latencyMs` etiket çağrısını kapsar):

```
event: token
data: {"text":"Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?"}

event: sources
data: {"generationMode":"RAG","retrievalStrategy":"NONE","candidates":null,"topK":null,"searchQuery":null,"queryRewriteStatus":"DISABLED","contextChunks":[],"citations":[]}

event: done
data: {"turnId":17,"notFoundAnswer":false,"answer":"Merhaba, ben Raşit Bankası'nın dijital asistanıyım. Kart, hesap, kredi, transfer ve dijital bankacılıkla ilgili sorularını Ürün ve Hizmet Rehberi'ne dayanarak cevaplayabilirim. Ne sormak istersin?","answerSource":"SMALL_TALK","chatModel":"qwen3.5:9b","promptTokens":null,"completionTokens":null,"queryRewriteLatencyMs":null,"retrievalLatencyMs":null,"rerankLatencyMs":null,"generationLatencyMs":null,"timeToFirstTokenMs":null,"latencyMs":840,"hostLabel":null}

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
| Cevabın geldiği yol | `answerSource` | `AnswerSource` (enum: `MODEL`, `EMPTY_KNOWLEDGE_BASE`, `BELOW_RERANK_THRESHOLD`, `SMALL_TALK`, `NO_WORDS`) | `AnswerSource` (`ANSWER_SOURCES`) |
| Akış içi hata | `error` | `StreamErrorEvent` | `StreamErrorEvent` |
| Olayların birleşimi | — | — | `AnswerStreamEvent` (ayrık birleşim, `type` alanı olay adı) |
