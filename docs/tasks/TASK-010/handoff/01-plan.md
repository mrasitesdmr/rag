TASK: TASK-010
# Plan

## Özet
**Mevcut durum (TASK-008):** `AnswerService.sendMessage` soruyu olduğu gibi arar (`TurnDraft.searchQuery` = soru), prompt sistem + tek kullanıcı mesajıdır (geçmiş yok, TASK-008 Kararlar #9). `ConversationService` sadece konuşma açar/getirir. SSE `sources` arama sorgusunu, `done` yeniden yazma süresini taşımaz. Yan kartın alt başlığı `"soru" · rerank 20→5`; süre çubukları retrieval, rerank, model.

**Yeni durum:**
- **assistant-api (değişen servis):**
  1. Konuşmanın son `assistant.history.max-turns` (kart soru 1, öneri 3) **cevabı tamamlanmış** turu (`error_code` null, `answer` dolu) geçmiş olarak okunur.
  2. `assistant.query-rewrite.enabled=true` ve geçmiş boş değilse `QueryRewriter` lab şablonuyla (`prompts/query-rewrite.st`, İngilizce, `CompressionQueryTransformer` metni) dil modelinden bağımsız bir arama sorgusu ister. Hata, zaman aşımı, boş ya da 1000 karakterden uzun sonuç → arama özgün soruyla; akış etkilenmez.
  3. retrieval-api araması ve reranker bu `searchQuery` ile yapılır.
  4. Cevap prompt'u: sistem + geçmiş (`user` soru / `assistant` cevap çiftleri, alıntı numaraları silinmiş) + `<parcalar>` ve **müşterinin yazdığı soru** (lab `promptMessages`, `backend-conventions` §7). Geçmiş boşsa prompt TASK-008'dekiyle birebir aynı.
  5. Tura `query_rewrite_status`, `query_rewrite_latency_ms`, `history_turns` eklenir (`search_query` zaten var). Migration `V2`.
  6. SSE: `sources.searchQuery`, `sources.queryRewriteStatus`, `done.queryRewriteLatencyMs`.
- **web:** yan kart alt başlığına `· arama: "…"` (sadece `REWRITTEN`, 40 karakter) ve başarısızlıkta kart soru 3'e göre `· yeniden yazma başarısız`; süre çubuklarının başına "yeniden yazma" satırı.
- **retrieval-api:** değişmez.
- **Docker:** `QUERY_REWRITE_ENABLED` ortam değişkeni Compose'a (qa'nın açık/kapalı karşılaştırması için).

**Silinen/değişen mevcut parçalar:** `PromptBuilder.buildPrompt(question, contextChunks)` → `buildPrompt(question, history, contextChunks)`. `TurnDraft`, `TurnValues`, `Turn`, `SourcesEvent`, `DoneEvent` yeni alanlar alır (aşağıda sıra). İsim değişikliği yok.

**Uygulama sırası:** backend-coder ve frontend-coder paralel; ikisi de sadece `sse.md`'ye bakar. Kart soruları 1, 3, 4'ün cevabı sabit değerleri değiştirir, yapıyı değiştirmez.

**Kart cevapları (2026-11-01, kullanıcı):** 1 B (`assistant.history.max-turns=3`), 2 A (lab şablonu; denemede İngilizce sorgu çıkarsa tanımlı tek satır eklenir), 3 B (`· yeniden yazma başarısız` görünür), 4 B (`assistant.query-rewrite.timeout=20s`). Hepsi planın önerdiği seçenek; plan değişmedi.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/sse.md`, `docs/api/openapi.yaml` (sadece `sendMessage` açıklaması).
- **backend-coder:** `assistant-api/src/main/**`, `assistant-api/CLAUDE.md`. M0'ı gerçek modelle çalıştırır. Rapor `handoff/02-backend-implementation.md`.
- **frontend-coder:** `web/src/**` (testler hariç), `web/CLAUDE.md` gerekiyorsa. Yeni paket yok. Rapor `handoff/02-frontend-implementation.md`.
- **tester:** `assistant-api/src/test/**`, `web/**/*.test.ts(x)`, `web/src/test/**` (SSE fixture'larına yeni alanlar), `web/e2e/**`. Rapor `handoff/03-test-report.md`.
- **devops:** `deployment/docker-compose.yml` (tek ortam değişkeni), `deployment/CLAUDE.md`, `ollama.md` (değişken tablosu + açma/kapama). TASK'ta Docker işaretli değil ama kabul kriteri 1'deki "açıkken ve kapalıyken" karşılaştırması çalışan sistemde ayarın değiştirilebilmesini gerektiriyor. Rapor `handoff/04-deploy-report.md`.
- **qa:** M1-M4, kabul kriterleri, sözleşme uyumu. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** yok. retrieval-api'ye kimse dokunmaz.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Yeniden yazma `assistant.*` ile açılır/kapanır; varsayılan açık | `AssistantProperties.QueryRewrite.enabled` (`assistant.query-rewrite.enabled=${QUERY_REWRITE_ENABLED:true}`), `QueryRewriter` | B1, B18, M2 |
| K2 | İlk turda yeniden yazma yok; arama sorgusu = soru | `QueryRewriter` (geçmiş boş → `NO_HISTORY`, model çağrılmaz) | B2, B16 |
| K3a | Talimat lab şablonu (İngilizce, `CompressionQueryTransformer` metni) | `prompts/query-rewrite.st`, `QueryRewriter` | B3 |
| K3b | Üretilen sorgu Türkçe; gerçek modelle doğrulanır, gerekirse kartta değişiklik önerilir | Kart soru 2; şablon değişikliği (A seçilirse) önceden tanımlı tek satır | M0, M1 |
| K4 | Geçmişe kaç tur girer: kartta | `assistant.history.max-turns` (kart soru 1), `ConversationService.getHistory` | B4, B18 |
| K5 | Hata/boş → özgün soruyla arama; tur kaydeder; cevap akar | `QueryRewriter` (`FAILED`, `EMPTY`), `AnswerService` | B5, B6, B7, B9, B11, B21 |
| K6 | Tur kaydına arama sorgusu ve yeniden yazma süresi | `conversation_turns.search_query` (var), `query_rewrite_latency_ms`, `query_rewrite_status`, `history_turns` (V2) | B14, B21 |
| K7a | Yan kart alt başlığı `· arama: "…"` | `queryRewriteLabel` (`strategyLabels.ts`), `ContextChunksCard` | F3, F4 |
| K7b | Süre çubuklarına "yeniden yazma" satırı | `latencyRows.ts`, `DoneEvent.queryRewriteLatencyMs` | F2, F5 |
| K8 | Yeniden yazılmış sorgu loglanmaz; uzunluk ve süre loglanır | `AnswerService` / `QueryRewriter` logları | B17 |
| S1 | "Kartımı kaybettim…" → "Yenisi kaç günde gelir?": arama teslim süresini sorar, cevap teslim süresi parçasına dayanır | K2, K5 | B10, B16, M1 |
| S2 | Kapalıyken "Yenisi kaç günde gelir?" olduğu gibi aranır; fark raporda | K1 | B1, M1 |
| AC1 | Gerçek modelle ≥5 takip sorusu; arama sorgusu ve parçalar açık/kapalı karşılaştırılmış | qa | M1 |
| AC2 | İlk turda atlama, hata durumunda dönüş, geçmiş sınırı testli; build'ler geçer | tester, coder'lar | B2, B5-B7, B4, raporlar |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Önceki turlar hata almış ya da yarıda kesilmiş (`error_code` dolu) | Geçmişe girmez; hiç tamamlanmış tur yoksa ilk tur gibi (`NO_HISTORY`, yeniden yazma yok) | B4, B2 |
| G2 | Dil modeli kapalı | Yeniden yazma hemen `FAILED`, arama özgün soruyla yapılır; cevap adımında TASK-008'deki `CHAT_MODEL_UNAVAILABLE` `error` olayı gelir; turda ikisi de görünür | B5, B21 |
| G3 | Model sorguyu `Standalone query: "…"` etiketiyle, tırnakla ya da birkaç satır yazar | Etiket, çevreleyen tırnaklar silinir, satır sonları ve çoklu boşluk teke iner; geriye bir şey kalmazsa `EMPTY` | B8, B7 |
| G4 | Model 1000 karakterden uzun sorgu yazar (retrieval-api sınırı 1000) | Kullanılmaz, `FAILED`; arama özgün soruyla (aksi halde retrieval-api 400 → kullanıcı `RETRIEVAL_UNAVAILABLE` görürdü) | B9 |
| G5 | Kullanıcı yeniden yazma sürerken aynı konuşmaya ikinci soruyu gönderir ya da sayfadan çıkar | İkinci istek 409 `TURN_IN_PROGRESS` (kilit yeniden yazmadan önce alınır); sayfadan çıkışta akış açılınca ilk gönderim başarısız olur, tur `STREAM_CANCELLED` (TASK-008 yolu) | B19 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Geçmiş cevap prompt'una da girer** (sistem → `user` q / `assistant` a çiftleri, eskiden yeniye → son `user` mesajı `<parcalar>` + `Soru: {müşterinin sorusu}`). Yeniden yazma kapalıyken de girer. Sınır yeniden yazmayla aynı (`assistant.history.max-turns`) | `backend-conventions` §7 ("prompt'ta müşterinin kendi sorusu ve geçmiş kalır"), lab `promptMessages`; TASK-008 Kararlar #9 bunu TASK-010'a bırakmıştı | Geçmişi sadece yeniden yazmaya vermek: model "Yenisi" sorusunu bağlamsız cevaplar, lab'dan ayrışır |
| 2 | **Geçmiş = cevabı tamamlanmış turlar** (`error_code is null and answer is not null`), `turn_no` azalan ilk N, sonra eskiden yeniye. "Bulamadım" turları dahil | Yarım ya da hatalı cevap modeli yanıltır; "bulamadım" gerçek bir cevaptır | Son N turu koşulsuz almak: `answer` null olan tur `ASSISTANT:` satırını boş bırakır |
| 3 | **Geçmiş cevaplardaki `[n]` silinir** (`\s*\[\d+]` → ""), hem yeniden yazma hem cevap prompt'unda | Önceki numaralar bu turun parçalarını göstermez; model eski numarayı yeniden kullanabilir | Lab gibi olduğu gibi göndermek: yanlış alıntı riski |
| 4 | **Kendi `QueryRewriter`'ımız**, Spring AI `CompressionQueryTransformer` sınıfı değil; şablon metni birebir onun (lab) | Sınıf ayrı `spring-ai-rag` modülünde (yeni dependency); model boş dönünce uyarı loglayıp özgün sorguyu döndürür → `EMPTY` ayırt edilemez (K5 "tur bunu kaydeder"); zaman aşımı kontrolü yok | Sınıfı kullanmak: yeni dependency + K5 kaydı yapılamaz |
| 5 | **Yeniden yazma çağrısı `chatModel.stream(prompt)`**, parçalar birleştirilir, toplam `assistant.query-rewrite.timeout` (`Flux.timeout` + `reduce` + `block`) | Zaman aşımında abonelik iptal edilir → Ollama isteği kapanır; varsayılan seçenekler (`temperature 0`, `think false`) aynı | `chatModel.call`: bloklayan çağrı zaman aşımında arka planda sürer (lab `stream:false` kullanıyordu; sağ yarı ipucudur) |
| 6 | **Tek kullanıcı mesajı**: şablon `{history}` = `USER: q\nASSISTANT: a` satırları `\n` ile; `{query}` = soru. Sistem mesajı yok | Lab Network sekmesi (`messages:[{role:'user',content:tpl}]`), `CompressionQueryTransformer` biçimi | — |
| 7 | **Çıktı normalizasyonu** (`QueryRewriter`): strip → baştaki `Standalone query:` (büyük/küçük harf duyarsız) silinir → strip → baştaki ve sondaki eşleşen `"…"` ya da `“…”` silinir → her boşluk dizisi (satır sonu dahil) tek boşluk → strip. Boşsa `EMPTY`; `assistant.query-rewrite.max-length` (1000) üstü `FAILED` | G3, G4; retrieval-api `SearchChunksRequest.query` ≤ 1000 | Çıktıyı ham kullanmak: etiketli/tırnaklı sorgu aramayı bozar |
| 8 | **`QueryRewriteStatus { DISABLED, NO_HISTORY, REWRITTEN, FAILED, EMPTY }`** turda ve `sources`'ta | K5 kaydı ve yan kart (`REWRITTEN` → arama; `FAILED`/`EMPTY` → kart soru 3) aynı alandan | Sadece `searchQuery != question` karşılaştırması: model soruyu aynen yazarsa ayırt edilemez |
| 9 | **Kilit ve sıra:** `getConversation` → `tryStart` → `getHistory` (kısa readOnly tx) → `rewriteQuery` (tx yok) → `retrieveContext(searchQuery)` → akış. Yeniden yazma akış açılmadan yapılır | Yeniden yazma retrieval'ın girdisi; dış çağrı tx dışında (`spring-transactions`); G5 | Akışı önce açmak: `RETRIEVAL_UNAVAILABLE` artık Problem Details olamaz (sözleşme değişir) |
| 10 | **Arama sorgusu SSE'de** (`sources.searchQuery` + `queryRewriteStatus`, `done.queryRewriteLatencyMs`) | K7 yan kartı akan turdan çizer; tur okuma ucu TASK-014'te | Ayrı `GET` tur ucu açmak: TASK-014 kapsamı |
| 11 | **Migration V2** eski turlara `query_rewrite_status = 'DISABLED'`, `history_turns = 0` yazar | TASK-008 turlarında özellik yoktu; değer doğru | Kolonu nullable bırakmak: TASK-014 iki durumu ayrıca ele alır |
| 12 | **Rerank sorgusu da `searchQuery`** | `rag-pipeline` §4 (`"query": "<standaloneQuery>"`); `ContextRetriever.retrieveContext(query)` zaten tek sorgu alır | — |
| 13 | **Yeniden yazmanın token sayıları kaydedilmez**; `done.promptTokens/completionTokens` sadece cevap çağrısıdır | TASK sadece sorgu ve süre istiyor | Yeni kolonlar: kapsam dışı |

## Sözleşme değişiklikleri
`docs/api/sse.md`:
- Durum satırı; "Endpoint": akış yeniden yazma + retrieval bittikten sonra açılır, yeniden yazma hiç hata döndürmez.
- `sources` (`SourcesEvent`): yeni `searchQuery` (string), `queryRewriteStatus` (`QueryRewriteStatus`); `QueryRewriteStatus` değerleri ve anlamları.
- `done` (`DoneEvent`): yeni `queryRewriteLatencyMs` (integer | null); `latencyMs` yeniden yazmayı içerir; `completionTokens` yeniden yazmayı içermez.
- Örnekler güncellendi; ikinci tur (`REWRITTEN`) örneği eklendi; isim tablosuna `QueryRewriteStatus`.

`docs/api/openapi.yaml`: başlık yorumu (TASK-010) ve `sendMessage` açıklaması (yeniden yazma adımı, kontrol sırası, `RETRIEVAL_UNAVAILABLE` turunda arama sorgusu). Şema, status ve `errorCode` değişmedi. `websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline`, `sse-streaming`, `java-concurrency`, `spring-transactions`, `jpa-patterns`, `flyway-migrations`, `springboot-security` (prompt injection: geçmiş ve soru veridir).

**Yeni dependency yok.**

### Migration `V2__add_query_rewrite_to_turns.sql`
`conversation_turns`'e: `query_rewrite_status text` → `UPDATE … SET query_rewrite_status = 'DISABLED'` → `SET NOT NULL` + `CHECK (query_rewrite_status IN ('DISABLED','NO_HISTORY','REWRITTEN','FAILED','EMPTY'))`; `query_rewrite_latency_ms int` (null); `history_turns int NOT NULL DEFAULT 0 CHECK (history_turns >= 0)`.

### Properties (ASCII)
```
assistant.query-rewrite.enabled=${QUERY_REWRITE_ENABLED:true}
assistant.query-rewrite.timeout=20s            # kart soru 4
assistant.query-rewrite.max-length=1000
assistant.history.max-turns=3                  # kart soru 1
```
`AssistantProperties` yeni bileşenler: `@Valid @NotNull QueryRewrite queryRewrite` (`boolean enabled`, `@NotNull Duration timeout`, `@Min(1) @Max(1000) int maxLength`), `@Valid @NotNull History history` (`@Min(1) @Max(10) int maxTurns`). `assistant-api/CLAUDE.md` ayar listesi ve "Henüz olmayanlar" (sorgu yeniden yazma çıkar) güncellenir.

### Sınıflar
**conversation/domain/entity/**
- `QueryRewriteStatus { DISABLED, NO_HISTORY, REWRITTEN, FAILED, EMPTY }`.
- `Turn`: `queryRewriteStatus` (`@Enumerated(STRING)`, `query_rewrite_status`), `@Nullable Integer queryRewriteLatencyMs`, `int historyTurns` + getter'lar. `TurnValues`'a aynı üç alan `searchQuery`'den hemen sonra: `(question, searchQuery, queryRewriteStatus, queryRewriteLatencyMs, historyTurns, generationMode, …)`.

**conversation/domain/event/**
- `SourcesEvent(RetrievalStrategy retrievalStrategy, int candidates, int topK, String searchQuery, QueryRewriteStatus queryRewriteStatus, List<ContextChunk> contextChunks, List<Integer> citations)`.
- `DoneEvent(long turnId, boolean notFoundAnswer, String chatModel, @Nullable Integer promptTokens, @Nullable Integer completionTokens, @Nullable Long queryRewriteLatencyMs, long retrievalLatencyMs, @Nullable Long rerankLatencyMs, @Nullable Long generationLatencyMs, @Nullable Long timeToFirstTokenMs, long latencyMs, @Nullable String hostLabel)`.

**conversation/repository/TurnRepository**
- `@Query("select t from Turn t where t.conversation.id = :conversationId and t.errorCode is null and t.answer is not null order by t.turnNo desc") List<Turn> findAnsweredTurns(@Param("conversationId") long conversationId, Pageable pageable)`.

**conversation/service/**
- `ConversationService`: yeni `@Transactional(readOnly = true) List<HistoryTurn> getHistory(long conversationId)` → `findAnsweredTurns(id, PageRequest.of(0, maxTurns))`, ters çevrilir (eskiden yeniye), her biri `new HistoryTurn(question, citationParser.removeCitations(answer))`. Constructor'a `TurnRepository`, `CitationParser`, `AssistantProperties` eklenir.
- `TurnDraft(long conversationId, String question, String searchQuery, QueryRewriteStatus queryRewriteStatus, @Nullable Long queryRewriteLatencyMs, int historyTurns, String chatModel, Instant receivedAt, long startNanos, @Nullable RetrievedContext context)`.
- `AnswerService.sendMessage`: Kararlar #9 sırası. `history = conversationService.getHistory(id)`, `rewrite = queryRewriter.rewriteQuery(question, history)`, `retrieveContext(rewrite.searchQuery())` (hata kaydı da yeni `TurnDraft` alanlarıyla), `promptBuilder.buildPrompt(question, history, contextChunks)`. Log: `INFO Query rewrite conversationId={} status={} historyTurns={} questionLength={} queryLength={} durationMs={}` (sorgu metni yok).
- `AnswerStreamer`: `sourcesEvent` `draft.searchQuery()` ve `draft.queryRewriteStatus()`'u, `doneEvent` `draft.queryRewriteLatencyMs()`'yi yazar.
- `TurnService.recordTurn`: yeni üç alanı `TurnValues`'a geçirir.

**retrieval/service/**
- `QueryRewriteResult(QueryRewriteStatus status, String searchQuery, @Nullable Long latencyMs)`.
- `QueryRewriter` (`@Service`; `ChatModel`, `@Value("classpath:prompts/query-rewrite.st") Resource`, `AssistantProperties`): `QueryRewriteResult rewriteQuery(String question, List<HistoryTurn> history)`.
  - `enabled == false` → `(DISABLED, question, null)`; `history` boş → `(NO_HISTORY, question, null)`; ikisinde model çağrılmaz.
  - Aksi halde Kararlar #5-7; her hata (`RuntimeException`, zaman aşımı) → `FAILED`; süre her denemede dolu (`System.nanoTime()`).
  - `WARN Query rewrite failed, searching with the original question status={} reason={} durationMs={}` (reason = exception sınıf adı ya da `TooLong`/`Empty`).

**generation/**
- `HistoryTurn(String question, String answer)` (record).
- `CitationParser.removeCitations(String answer): String` (Kararlar #3; sonra strip).
- `PromptBuilder.buildPrompt(String question, List<HistoryTurn> history, List<RetrievedChunk> contextChunks)` → `SystemMessage`, her geçmiş tur için `UserMessage(question)` + `AssistantMessage(answer)`, son `UserMessage` (rag-user.st, değişmedi).

**prompts/query-rewrite.st** (lab metni, birebir; `{history}`, `{query}`):
```
Given the following conversation history and a follow-up query, your task is to synthesize
a concise, standalone query that incorporates the context from the history.
Ensure the standalone query is clear, specific, and maintains the user's intent.

Conversation history:
{history}

Follow-up query:
{query}

Standalone query:
```
Kart soru 2 = A ve M0/M1'de İngilizce sorgu çıkarsa `Ensure…` satırının altına tek satır eklenir: `Write the standalone query in the same language as the follow-up query.` (B3 beklenen metni de güncellenir; değişiklik raporda yazılır).

### M0 (backend-coder, gerçek model)
Lab'ın 8 takip çifti (aşağıda) ile `curl`: her ikinci soruda `sources.searchQuery` Türkçe mi, `queryRewriteStatus` `REWRITTEN` mi; sonuçlar raporda tablo.

## Frontend planı
Ek skill'ler: `sse-streaming`, `frontend-a11y`.

**Yeni paket yok.**
- `model/types.ts`: `QUERY_REWRITE_STATUSES = ['DISABLED', 'NO_HISTORY', 'REWRITTEN', 'FAILED', 'EMPTY'] as const` + `QueryRewriteStatus`; `SourcesEvent`'e `searchQuery: string`, `queryRewriteStatus: QueryRewriteStatus`; `DoneEvent`'e `queryRewriteLatencyMs: number | null`.
- `model/answerStreamEvents.ts`: `isSourcesEvent` iki yeni alanı (string, bilinen durum), `isDoneEvent` `isNullableInteger(queryRewriteLatencyMs)` kontrol eder; eksik alan → `null` (olay atlanır).
- `model/strategyLabels.ts`: `SEARCH_QUERY_SHORT_LENGTH = 40`, `QUERY_REWRITE_FAILED_LABEL = 'yeniden yazma başarısız'` (kart soru 3 = B ise), `queryRewriteLabel(sources: SourcesEvent): string | null` → `REWRITTEN` → `arama: "${shortenText(searchQuery, 40)}"`; `FAILED`/`EMPTY` → `QUERY_REWRITE_FAILED_LABEL` (soru 3 = A ise `null`); diğerleri `null`.
- `components/ContextChunksCard.tsx`: alt başlık `"${shortenText(question, 60)}" · ${strategyLabel(sources)}` + etiket varsa ` · ${queryRewriteLabel(sources)}`.
- `model/latencyRows.ts`: `queryRewriteLatencyMs !== null` ise ilk satır `{ label: 'yeniden yazma', ms, isModel: false }` (lab sırası: yeniden yazma, retrieval-api araması, rerank, model).
- `HowItWorksCard` metni değişmez (lab metninde yeniden yazma yok).

## Test senaryoları
Backend sahteleri TASK-008'deki gibi (sahte `ChatModel`, `RetrievalClient`, `RerankClient`, `RecordingSseEmitter`). Yeniden yazma için sahte `ChatModel`'in `stream`'i ilk çağrıda yeniden yazma metnini, ikincide cevabı döner; ya da `QueryRewriter` unit testinde ayrı sahte. Web SSE fixture'larının tamamı yeni alanları taşır (eksik alan artık olayı düşürür).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| B1 | Kapalı | unit `QueryRewriterTest` | `givenRewriteDisabled_whenRewriteQuery_thenDisabledWithoutModelCall` | 1 geçmiş turla bile `(DISABLED, "Yenisi kaç günde gelir?", null)`; `chatModel` etkileşimsiz |
| B2 | İlk tur | unit | `givenFirstTurn_whenRewriteQuery_thenNoHistoryAndSearchesWithQuestion` | geçmiş `[]` → `(NO_HISTORY, soru, null)`; model çağrılmadı |
| B3 | Şablon | unit | `givenOneHistoryTurn_whenRewriteQuery_thenSendsLabTemplateAsSingleUserMessage` | Yakalanan prompt 1 `UserMessage`; metin = şablon literal'i + `USER: Kartımı kaybettim, ne yapmalıyım?\nASSISTANT: Kartını hemen kapat.` + `Follow-up query:\nYenisi kaç günde gelir?` (testte tam literal) |
| B4 | Geçmiş sınırı | integration `ConversationServiceIntegrationTest` (Testcontainers) | `givenFiveAnsweredTurnsAndOneFailed_whenGetHistory_thenLastThreeAnsweredOldestFirst` | turlar 1-5 cevaplı, 6 `CHAT_MODEL_UNAVAILABLE`; `max-turns` 3 → soruları tur 3, 4, 5 sırasıyla; cevaplardan `[1]` silinmiş |
| B5 | Model hatası | unit `QueryRewriterTest` | `givenModelFails_whenRewriteQuery_thenFailedAndOriginalQuestion` | `Flux.error` → `(FAILED, soru, latency ≥ 0)` |
| B6 | Zaman aşımı | unit | `givenModelSilentLongerThanTimeout_whenRewriteQuery_thenFailedWithinTimeout` | timeout 100 ms, `Flux.never()` → `FAILED`; test 5 sn dolmadan biter; iptal bayrağı true |
| B7 | Boş | unit | `givenBlankOrLabelOnlyOutput_whenRewriteQuery_thenEmpty` | `"  \n"` ve `"Standalone query:"` → `EMPTY`, soru |
| B8 | Normalizasyon | unit (`@ParameterizedTest`) | `givenDecoratedOutput_whenRewriteQuery_thenCleanQuery` | `"Standalone query: \"Kaybolan kartın yenisi kaç günde gelir?\"\n"` ve `"“Kaybolan kartın\nyenisi kaç günde gelir?”"` → `Kaybolan kartın yenisi kaç günde gelir?`, `REWRITTEN` |
| B9 | Çok uzun | unit | `givenOutputLongerThanMaxLength_whenRewriteQuery_thenFailed` | 1001 karakter → `FAILED`, soru; 1000 karakter → `REWRITTEN` |
| B10 | Arama standalone, prompt özgün soru | unit `AnswerServiceTest` (`ArgumentCaptor`) | `givenFollowUp_whenSendMessage_thenSearchesWithStandaloneAndPromptsCustomerQuestion` | `retrieveContext("Kaybolan kartın yenisi kaç günde gelir?")`; prompt son mesajı `Soru: Yenisi kaç günde gelir?` ile biter; mesaj sırası System, User(q1), Assistant(a1), User |
| B11 | Hata → akış sürer | unit | `givenRewriteFailed_whenSendMessage_thenSearchesWithQuestionAndStreams` | `retrieveContext("Yenisi kaç günde gelir?")`; `streamModelAnswer` 1 kez; draft `FAILED` |
| B12 | Prompt geçmişi | unit `PromptBuilderTest` | `givenTwoHistoryTurns_whenBuildPrompt_thenHistoryPairsBeforeChunkMessage` / `givenNoHistory_whenBuildPrompt_thenSameAsTask008` | 6 mesaj: System, User q1, Assistant a1, User q2, Assistant a2, User (`<parcalar>…Soru: …`); geçmişsiz 2 mesaj, TASK-008 B4 literal'i |
| B13 | Alıntı silme | unit `CitationParserTest` | `givenAnswerWithCitations_whenRemoveCitations_thenPlainText` | `"Kartını kapat [1]. Bak [2][3]."` → `"Kartını kapat. Bak."` |
| B14 | Tur kaydı | integration `TurnServiceIntegrationTest` | `givenRewrittenDraft_whenRecordTurn_thenStoresSearchQueryStatusLatencyAndHistoryTurns` | `search_query` "Kaybolan kartın yenisi kaç günde gelir?", `question` "Yenisi kaç günde gelir?", `query_rewrite_status` REWRITTEN, `query_rewrite_latency_ms` 812, `history_turns` 1 |
| B15 | Migration | integration (Flyway, boş veritabanı) | `givenMigrations_whenApplied_thenRewriteColumnsHaveConstraints` | `query_rewrite_status = 'FOO'` insert'i check ihlali; `history_turns = -1` ihlal |
| B16 | SSE uçtan uca | integration `SendMessageIntegrationTest` | `givenTwoQuestions_whenSecondSent_thenSourcesHaveStandaloneQueryAndDoneHasRewriteLatency` | 1. akış `sources.queryRewriteStatus` NO_HISTORY, `searchQuery` = soru, `done.queryRewriteLatencyMs` null; 2. akış REWRITTEN, `searchQuery` sahte modelin sorgusu, latency ≥ 0 |
| B17 | Log sınırı | integration (`OutputCaptureExtension`) | `givenRewrittenTurn_whenLogsCaptured_thenLengthsButNoQueryText` | `Query rewrite conversationId=` ve `queryLength=` var; "Kaybolan kartın" logda yok |
| B18 | Ayarlar | unit `AssistantPropertiesTest` | `givenDefaults_whenBound_thenRewriteOnTwentySecondsThreeTurns` / `givenMaxTurnsZero_whenBound_thenContextFails` / `givenEnabledFalse_whenBound_thenDisabled` | `enabled` true, `timeout` 20s, `max-length` 1000, `max-turns` 3; 0 açılışı düşürür |
| B19 | Kilit önce | unit `AnswerServiceTest` | `givenTurnInProgress_whenSendMessage_thenNoHistoryReadAndNoRewrite` | `TURN_IN_PROGRESS`; `queryRewriter` ve `getHistory` etkileşimsiz |
| B20 | Transaction dışında | unit `QueryRewriterTest` | `givenRewrite_whenModelCalled_thenNoTransactionActive` | sahte model içinde `isActualTransactionActive()` false |
| B21 | Retrieval hatası kaydı | unit `AnswerServiceTest` | `givenRetrievalUnavailableAfterRewrite_whenSendMessage_thenTurnKeepsSearchQueryAndStatus` | 503; kaydedilen draft `searchQuery` standalone, `REWRITTEN`, latency dolu |
| F1 | Olay doğrulama | unit `answerStreamEvents.test.ts` | `givenSourcesWithoutSearchQueryOrUnknownStatus_whenToAnswerStreamEvent_thenNull` | `searchQuery` yok → null; `queryRewriteStatus: "FOO"` → null; `done` `queryRewriteLatencyMs: null` geçerli |
| F2 | Süre satırları | unit `previewModel.test.ts` | `givenRewriteLatency_whenLatencyRows_thenRewriteRowFirst` / `givenNullRewriteLatency_whenLatencyRows_thenNoRewriteRow` | 812 → ilk satır "yeniden yazma" 812; null → satır yok |
| F3 | Alt başlık | component `PreviewPage.test.tsx` | `givenRewrittenSecondTurn_whenDone_thenSubtitleShowsSearchQuery` | `"Yenisi kaç günde gelir?" · rerank 20→5 · arama: "Kaybolan kartın yenisi kaç günde gelir?"` |
| F4 | Etiket durumları | unit `strategyLabels.test.ts` (`it.each`) | `givenRewriteStatuses_whenQueryRewriteLabel_thenLabelOnlyForRewrittenAndFailures` | NO_HISTORY/DISABLED → null; REWRITTEN 45 karakterlik sorgu → `arama: "` + 39 karakter + `…"`; FAILED/EMPTY → "yeniden yazma başarısız" (kart soru 3 = B) |
| F5 | Süre kartı | component | `givenRewrittenDone_whenRendered_thenRewriteLatencyRow` | "yeniden yazma 812 ms" satırı "retrieval-api araması"ndan önce |
| E1 | Takip sorusu (gerçek sistem) | e2e `web/e2e/preview.spec.ts` | `givenLostCardAnswered_whenFollowUpAsked_thenSideCardShowsSearchQueryAndRewriteRow` | "Kartımı kaybettim, ne yapmalıyım?" sonra "Yenisi kaç günde gelir?" → 120 sn içinde "Gönder" etkin; alt başlıkta `· arama: "`; "yeniden yazma" satırı |

**Elle çalıştırılanlar:**
- **M0 (backend-coder):** bkz. Backend planı.
- **M1 (qa, AC1):** lab'ın takip çiftleriyle (en az 5): "Kartımı kaybettim, ne yapmalıyım?" → "Yenisi kaç günde gelir?"; "Gold kartın yıllık aidatı ne kadar?" → "Peki Platinum'da?"; "FAST ile en fazla ne kadar gönderebilirim?" → "Daha fazlasını göndermem gerekirse?"; "İhtiyaç kredisi faizi ne kadar?" → "Erken kapatırsam ücret öder miyim?"; "Konut kredisi faizi ne kadar?" → "Ekspertiz ücreti ne kadar?"; "Kumbara hesabının faizi ne kadar?" → "Ayda kaç kez para çekebilirim?"; "Mobil şifremi unuttum, ne yapmalıyım?" → "Kartım yoksa ne yapacağım?"; "Almanya'daki oğluma para göndermek istiyorum." → "Ücreti ne kadar?". Her çift yeniden yazma açık ve `QUERY_REWRITE_ENABLED=false` ile yeni konuşmada: `search_query`, `query_rewrite_status`, `turn_chunks` (prompt'taki 5 parça başlığı), cevabın dayandığı parça, süre. Sorguların dili (K3b).
- **M2 (devops):** `QUERY_REWRITE_ENABLED=false docker compose -f deployment/docker-compose.yml up -d assistant-api` → ikinci soruda `sources.queryRewriteStatus` `DISABLED`; değişkensiz aynı komutla geri.
- **M3 (qa):** yan kart lab'la yan yana: alt başlık biçimi, "yeniden yazma" satırının yeri.
- **M4 (qa):** `sse.md` ile gerçek akış alanlarının karşılaştırması.

## Deployment planı
- `deployment/docker-compose.yml` assistant-api ortamına `QUERY_REWRITE_ENABLED: ${QUERY_REWRITE_ENABLED:-true}`.
- `deployment/CLAUDE.md`: değişken ve M2 komutu; `ollama.md` §8 değişken tablosuna satır.
- Yeni lokal model yok (yeniden yazma `qwen3.5:9b` ile).

## Açık sorular ve riskler
1. **Kart soruları:** 1 → `assistant.history.max-turns` (B4, B18 değerleri); 2 → şablona ek satır koşulu (B3); 3 → `queryRewriteLabel` (F4); 4 → `assistant.query-rewrite.timeout` (B18).
2. **Risk – süre:** takip sorularında cevaptan önce bir model çağrısı daha var (~1 sn sıcak). Geçmiş cevap prompt'unu da büyütür (3 tur ≈ +300-600 token; `num_ctx` 8192 yeterli).
3. **Risk – dil:** lab kaydında model bu şablonla Türkçe sorgu yazdı; gerçek sistemde farklı çıkabilir (kart soru 2).
4. **Kapsam dışı not:** `rag-pipeline` §2'deki `assistant.query-rewrite` tek anahtarı burada `assistant.query-rewrite.enabled` oldu (properties'te hem değer hem alt anahtar olamaz); kesin ad `assistant-api/CLAUDE.md`'ye yazılır.

STATUS: DONE
