TASK: TASK-008
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline`, `sse-streaming`, `java-concurrency`. Spring AI 2.0.2'nin property anahtarları ve akış davranışı jar'ın `spring-configuration-metadata.json`'ından ve `OllamaChatModel` kaynağından doğrulandı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Dependency (kart soru 1) | `pom.xml` | `spring-ai-bom` 2.0.2 (`import`) + `spring-ai-starter-model-ollama`. 2.0.2 son kararlı sürüm, Spring Framework 7.0.10 tabanlı; Boot 4.1.1 de 7.0.9 kullanıyor. `spring-boot-starter-restclient`'i starter zaten getiriyor, ayrıca eklenmedi |
| Migration | `db/migration/V1__create_conversations_and_turns.sql` | Planla aynı: `conversations`, `conversation_turns`, `turn_chunks`. Hibernate `validate` geçti (`int[]` dahil) |
| Properties, K4c, K12, K14, K8e | `application.properties` | Plandaki `assistant.*` anahtarları. Ollama: `spring.ai.ollama.chat.model/temperature/num-ctx/think=false`, `init.pull-model-strategy=never`, `spring.ai.model.embedding=none`. Dosya ASCII (`ı` kaçışı) |
| config | `AssistantProperties`, `ClockConfig`, `RestClientConfig`; `AssistantApplication`'a `@ConfigurationPropertiesScan` | `strategy` `top5\|rerank` (`@Pattern`), `candidates` 1-50, `topK > candidates` → `@AssertTrue` ile açılış hatası; boş `host-label` → `null` |
| exception, K5b | `BaseException`, `NotFoundException`, `ConflictException`, `ServiceUnavailableException`, `GlobalExceptionHandler` (`@ExceptionHandler(BaseException)`) | `application/problem+json`; SSE ucunda da (`Accept: text/event-stream, application/problem+json`) doğrulandı |
| K1, K2 | `CreateConversationRequest`, `SendMessageRequest`, `ConversationService`, `ConversationController` | 201 + `Location`; `text` `@NotBlank` + `@Size(max=1000)`, soru `strip` edilir |
| K5a, K5c, K9d, K11 | `AnswerService`, `AnswerStreamer`, `ActiveTurnRegistry` | `SseEmitter`; `chatModel.stream(prompt)` + `Flux.timeout(first, idle)` + `publishOn(boundedElastic)`. İptal: `send` `IOException`'ı, `onError`, `onTimeout`, `onCompletion` → `dispose` + `STREAM_CANCELLED`. Kayıt ve `registry.finish` her turda bir kez (`AtomicBoolean`) |
| K4a, K4b | `PromptBuilder`, `prompts/rag-system.st`, `prompts/rag-user.st` | Spring AI `PromptTemplate` (`{}` ayraçları, `<parcalar>` ile çakışmıyor). Parça bloğu `[n] heading\ncontent`, bloklar arasında boş satır |
| K6a | `CitationParser` | `\[(\d{1,9})]`, 1..contextSize, tekrarsız, ilk geçiş sırası |
| K7 | `NotFoundAnswerDetector` | `tr` küçük harf + boşluklar teke, "içerir" kontrolü |
| K12-K15 | `ContextRetriever`, `RetrievalClient`, `RerankClient` | Sıralama: puan azalan, eşitlikte `vectorRank` artan. Reranker'ın her hatası ve bozuk yanıtı (eksik, tekrarlı, aralık dışı `index`, sayı farkı) → `RERANK_FALLBACK`. retrieval-api'nin her hatası → 503 `RETRIEVAL_UNAVAILABLE` |
| K9, K15 | `Turn`, `TurnChunk`, `TurnChunkId`, `TurnService` | Her aday bir `turn_chunks` satırı (`@EmbeddedId` + `@MapsId`), prompt'takilerde `prompt_position` |
| K10, K18 | Bütün loglar | Planın log satırları. Soru, cevap, parça, prompt ve reranker belgeleri loglanmıyor. Hata loglarında sadece exception sınıf adı var; Postgres "Failing row contains (...)" mesajı veri sızdırmasın diye mesaj loglanmıyor |
| S7 / Kararlar #7 | `AnswerService` → `streamFixedAnswer` | Arama 0 sonuç verirse model çağrılmaz; tek `token` + `sources` (boş) + `done` (`notFoundAnswer: true`) |
| `assistant-api/CLAUDE.md` | `CLAUDE.md` | Durum, Teknoloji (Spring AI, gerçek anahtarlar, ASCII notu, tablolar), Henüz olmayanlar, Paket yapısı, Komutlar (duman testi `curl`'leri) |

## Değişen dosyalar
- Değişen: `assistant-api/pom.xml`, `assistant-api/CLAUDE.md`, `src/main/resources/application.properties`, `AssistantApplication.java`, `exception/GlobalExceptionHandler.java`
- Yeni (`src/main/java/com/rasitbank/assistant/`):
  - `config/`: `package-info`, `AssistantProperties`, `ClockConfig`, `RestClientConfig`
  - `exception/`: `BaseException`, `NotFoundException`, `ConflictException`, `ServiceUnavailableException`
  - `conversation/controller/`: `ConversationController`
  - `conversation/service/`: `ConversationService`, `AnswerService`, `AnswerStreamer`, `TurnService`, `ActiveTurnRegistry`, `TurnDraft`, `TurnOutcome`
  - `conversation/repository/`: `ConversationRepository`, `TurnRepository`
  - `conversation/domain/entity/`: `Channel`, `GenerationMode`, `RetrievalStrategy`, `Conversation`, `Turn`, `TurnChunk`, `TurnChunkId`, `TurnValues`, `TurnChunkValues`
  - `conversation/domain/dto/`: `CreateConversationRequest`, `ConversationResponse`, `SendMessageRequest`
  - `conversation/domain/event/`: `TokenEvent`, `SourcesEvent`, `ContextChunk`, `DoneEvent`, `StreamErrorEvent`
  - `retrieval/client/`: `RetrievalClient`, `RerankClient`, `SearchChunksRequest`, `SearchResponse`, `SearchHit`, `RerankRequest`, `RerankResponse`, `RerankResult`
  - `retrieval/service/`: `ContextRetriever`, `RetrievedChunk`, `RetrievedContext`
  - `generation/`: `PromptBuilder`, `CitationParser`, `NotFoundAnswerDetector`
  - Her yeni pakette `package-info.java` (`@NullMarked`)
- Yeni kaynaklar: `db/migration/V1__create_conversations_and_turns.sql`, `prompts/rag-system.st`, `prompts/rag-user.st`
- Eklenen dependency'ler: `org.springframework.ai:spring-ai-bom:2.0.2` (BOM), `org.springframework.ai:spring-ai-starter-model-ollama`

## Plandan sapmalar
Davranışı ya da sözleşmeyi değiştiren bir sapma yok. Uygulama ayrıntıları:
1. **`TurnDraft`'a `chatModel` alanı eklendi.** Değer `@Value("${spring.ai.ollama.chat.model}")` ile `AnswerService`'e gelir; `OllamaChatModel`'in kullandığı property de budur. `ChatModel.getDefaultOptions()` kullanılmadı, çünkü sahte `ChatModel` bu metottan `null` döner ve testler zorlaşır.
2. **`Turn.record(...)` için iki yardımcı record:** `TurnValues` ve `TurnChunkValues` (`domain/entity`). Entity, service record'larına (`TurnDraft`, `TurnOutcome`) bağımlı olmasın diye eklendi.
3. **`RerankResult` alanları nullable:** `@Nullable Integer index`, `@Nullable Double relevanceScore`. Eksik alan sessizce 0 sayılmasın, hata sayılsın (Kararlar #12) diye böyle yazıldı.
4. **Durumu kapalı olan sürelerin kaydı:** Hatalı ya da iptal edilen turlarda `generation_latency_ms` ve `time_to_first_token_ms`, o ana kadar ölçülen değerle yazılır (kolonlar zaten nullable). Model hiç çağrılmadıysa ikisi de `null` olur.
5. **SSE satır biçimi (sözleşme notu, engel değil):** Spring `SseEmitter` satırları `event:token` / `data:{...}` şeklinde, iki noktadan sonra boşluk koymadan yazar. sse.md'nin örnekleri boşluklu. SSE standardına göre ikisi aynıdır. frontend-coder'a ayrıştırıcının ikisini de kabul etmesini, planner'a ve lead'e de sse.md'ye not eklemeyi mesajla önerdim.

## Doğrulama
- `cd assistant-api && ./mvnw -q compile` → başarılı.
- `cd assistant-api && ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, Tests run: 13, Failures: 1, Errors: 10**. Kırılan testlerin ikisi de `src/test`'te (tester'ın) ve yeni özellik yüzünden eskimiş; ana kodda hata yok:
  - `AssistantApplicationIntegrationTest.givenEmptyDatabase_whenContextStarts_thenFlywayRunsWithNoMigrations`: migration olmadığını doğruluyordu; artık plan gereği `V1` uygulanıyor. Aynı sınıfın diğer iki testi geçti: context Ollama, reranker ve retrieval-api kapalıyken açılıyor, `/actuator/health` UP dönüyor (Kararlar #21).
  - `GlobalExceptionHandlerTest` (10 test): `@WebMvcTest` controller vermeden yazıldığı için bütün controller'ları yüklüyor. `ConversationController`'ın service'leri slice'ta yok, bu yüzden context açılmıyor. Düzeltme: `@WebMvcTest(ErrorProbeController.class)` ya da `@MockitoBean ConversationService/AnswerService`.
  - JaCoCo eşiği komut satırından kapatıldı (yeni kodun testleri henüz yok).
- **M0 (lokal, gerçek modeller: Ollama `qwen3.5:9b`, llama-server `bge-reranker-v2-m3`, retrieval-api + rehber 2026.10, dev postgres):**
  - `POST /api/assistant/conversations` `{"channel":"PREVIEW"}` → 201, `Location: /api/assistant/conversations/1`.
  - "Kartımı kaybettim, ne yapmalıyım?":
    - 70 token, parça parça geldi: başka bir soruda 1. token 24,72 sn'de, 60. token 26,63 sn'de geldi (perl zaman damgası).
    - Cevap Türkçe, `[1]` `[3]` alıntılı; düşünme metni yok.
    - `sources` `RERANK`, prompt'taki vector sırası `1,9,2,3,4`.
    - `done`: `promptTokens` 2063, `completionTokens` 70, `retrievalLatencyMs` 1605, `rerankLatencyMs` 949, `timeToFirstTokenMs` 9517, `latencyMs` 14101.
  - "Bitcoin alıp satabilir miyim?" → `notFoundAnswer: true` (`completionTokens` 14).
  - "Ev kredisini erken kapatırsam ceza öder miyim?" → prompt'taki vector sırası `1,2,3,9,16`; ilk iki parça "4 Krediler · Sık sorulanlar" (puan 5,19) ve "4.10 Erken ödeme ve erken kapama tazminatı" (1,46). Gerçek rehberde erken ödeme parçası vector 2. sırada, lab'daki 7. sıra çıkmadı (plan risk 5; qa raporlar).
  - Veritabanı: `conversation_turns`'te `turn_no` 1..3, `question = search_query`, `cited` `{1,3}` / `{5}` / `{1}`. Her tur için `turn_chunks`'ta 20 satır: 5'inde `prompt_position`, 20'sinde `rerank_score` dolu.
  - Loglar: `Retrieval search finished topK=20 hits=20 durationMs=…`, `Reranked candidates=20 kept=5 durationMs=…`, `Turn completed conversationId=1 turnId=1 strategy=RERANK questionLength=33 answerLength=241 …`. Logda soru, cevap ya da parça metni yok.
  - Hatalar:
    - Bilinmeyen konuşma (SSE `Accept`) → 404 `application/problem+json` `CONVERSATION_NOT_FOUND`
    - `"   "` → 400 `text` `must not be blank`; 1001 karakter → 400 `size must be at most 1000 characters`
    - `{}` → 400 `channel` `must not be null`; `"WEB"` → 400 `BAD_REQUEST`; `abc` kimliği → 400 `BAD_REQUEST`; `text/plain` → 415
  - 409: akış sürerken aynı konuşmaya ikinci soru → `TURN_IN_PROGRESS`.
  - İptal: istemci ilk token'dan sonra bağlantıyı kapattı → tur `STREAM_CANCELLED`, sadece gönderilmiş metin (`answer` 32 karakter) kaydedildi, `Turn cancelled conversationId=1 turnId=4 answerLength=32`. Ollama tarafındaki erken kapanma logu bu makinede bulunamadı (`~/.ollama/logs` yok); M1'de devops doğrular.
  - Reranker durduruldu → `RERANK_FALLBACK`, vector `[1..5]`, puanlar `null`, `rerankLatencyMs` 27, cevap normal aktı. Log: `Reranker unavailable, falling back to vector order candidates=20 topK=5 reason=Reranker request failed: ResourceAccessException`.
  - Ollama'ya ulaşılamayan ikinci örnek (port 8091, `OLLAMA_BASE_URL=http://127.0.0.1:9`, `RETRIEVAL_STRATEGY=top5`):
    - Arama `topK=5` ile yapıldı, reranker çağrılmadı.
    - Tek olay `error` `{"errorCode":"CHAT_MODEL_UNAVAILABLE"}`.
    - Tur `CHAT_MODEL_UNAVAILABLE` ile kaydedildi.
  - retrieval-api durduruldu → 503 `application/problem+json` `RETRIEVAL_UNAVAILABLE`; tur `error_code = RETRIEVAL_UNAVAILABLE`, `answer` null, aday satırı 0.
  - Test bitince açtığım süreçleri (reranker, retrieval-api, iki assistant-api örneği) kapattım; başlangıçta da kapalıydılar. Dev `assistant` veritabanında V1 ve 7 deneme turu duruyor.
  - Not: macOS'ta Netty, `netty-resolver-dns-native-macos` olmadığı için bir uyarı ve `restricted method` uyarıları yazıyor. Kozmetik; Linux konteynerde çıkmaz.

## Tester için notlar
- **Önce iki eski test düzeltilmeli** (yukarıda): Flyway testi artık `V1`'i bekler; `GlobalExceptionHandlerTest`, `@WebMvcTest(controllers = ErrorProbeController.class)` olmalı ya da yeni service'leri `@MockitoBean` ile vermeli.
- **`AnswerStreamer` asenkron çalışır:** model akışı `publishOn(Schedulers.boundedElastic())` ile başka thread'de işlenir. `streamModelAnswer` hemen döner; olayları kaydeden sahte emitter, `complete()` ya da kayıt çağrısı beklenerek (`CountDownLatch`, Awaitility) doğrulanmalı (B15-B19, B21, B35). `streamFixedAnswer` ise senkrondur.
- **Kayıt sırası:** `TurnService.recordTurn` → `ActiveTurnRegistry.finish` → `sources` → `done` → `emitter.complete()`. Hata yolunda: `recordTurn` → `finish` → `error` → `complete()`. İptal yolunda `recordTurn` → `finish` → `complete()` sırası izlenir, olay gönderilmez.
- **İptal seam'i:** `emitter.send(...)` `IOException` fırlatırsa abonelik `dispose` edilir. `chatModel.stream` dönen `Flux`'a `doOnCancel` takılarak iptal görülebilir. Kaydedilen `answer`, sadece başarıyla gönderilmiş token'lardır (B18: "Kartını ").
- **Emitter callback'leri:** `onCompletion`, `onTimeout`, `onError` üçü de iptal yoluna gider; tur zaten bittiyse etkisizdir. Sahte emitter callback'leri kaydederse kopma da simüle edilebilir.
- **Zaman aşımı:** `assistant.generation.first-token-timeout` ve `token-idle-timeout` `Mono.delay` ile uygulanır (parallel scheduler). B19 için `Flux.never()` + 100 ms yeterlidir. Hata `TimeoutException` olur ve `CHAT_MODEL_UNAVAILABLE`'a çevrilir.
- **`ChatModel.stream` senkron hata fırlatırsa** da `CHAT_MODEL_UNAVAILABLE` olur. Akıştaki her hata (`Flux.error`) `CHAT_MODEL_UNAVAILABLE`'dır. `INTERNAL_ERROR` sadece bizim tarafımızdaki hatalarda çıkar: kayıt yazılamadı ya da `send` `IOException` dışı bir `RuntimeException` attı.
- **Usage:** sahte `ChatResponse`'larda `ChatResponseMetadata.builder().usage(new DefaultUsage(p, c))` kullanılır. 0/0 olan usage yok sayılır (Ollama ara parçalarda 0 yollar); son pozitif usage alınır, hiç gelmezse `null` kalır.
- **`AnswerService` constructor'ı** `String chatModelName` (`@Value("${spring.ai.ollama.chat.model}")`) ve `Clock` alır; unit testte doğrudan verilir.
- **İstemci seam'leri:** `RetrievalClient(RestClient, AssistantProperties)` ve `RerankClient(RestClient, AssistantProperties)`. `MockRestServiceServer.bindTo(RestClient.builder())` ile kurulan `RestClient` verilebilir. Zaman aşımı testi için `RestClientConfig`'teki gibi `JdkClientHttpRequestFactory` + JDK `HttpServer` kullanılabilir. Bean adları `retrievalRestClient` ve `rerankRestClient`.
- **`PromptBuilder(Resource system, Resource user, AssistantProperties)`:** testte `new ClassPathResource("prompts/rag-system.st")` verilir. Dönen `Prompt`'ta seçenek yoktur; seçenekler `OllamaChatModel` varsayılanından gelir (B34: `OllamaChatModel.getDefaultOptions()` → model, `temperature`, `numCtx`, `thinkOption`).
- **Entity:** `TurnChunk` kimliği `TurnChunkId(turnId, vectorRank)`, `@MapsId` ile. Yeni `Turn` kaydedilince adaylar cascade ile yazılır. `Turn.getCited()` `List<Integer>` döner.
- **Doğrulama mesajları:** `channel` `must not be null`; `text` `must not be blank` / `size must be at most 1000 characters`. `strategy` `must be one of top5, rerank`; `topK > candidates` için `top-k must not be greater than candidates` (`isTopKWithinCandidates`).
- **Integration testleri** için `@MockitoBean ChatModel` (ya da sahte bean), `RetrievalClient` ve `RerankClient` kullanılabilir. Context açılışında Ollama'ya gidilmez.

## Düzeltme
—

## Düzeltme turu 1
Kaynak: `handoff/05-qa-report.md` "Düzeltme" maddesi (backend-coder, küçük).

| Madde | Yapılan değişiklik |
|---|---|
| `assistant-api/CLAUDE.md:17` Flyway satırı "henüz migration yok" diyordu | Satır `V1__create_conversations_and_turns.sql` → `conversations`, `conversation_turns`, `turn_chunks` olarak güncellendi. Dosyada eski ifadeden başka kalıntı yok (`grep "No migrations\|henüz migration"` → boş) |

Sadece doküman değişti; kod değişmediği için `./mvnw verify` yeniden çalıştırılmadı.

STATUS: DONE
