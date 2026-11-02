<!--
AMAÇ: Claude (ve ekip üyeleri) assistant-api/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
İçerik: harita, komutlar, dosya sahipliği, Boot 4 tuzakları, iş → skill yönlendirmesi.
Kuralların ayrıntısı backend-conventions skill'inde, RAG hattının ayrıntısı rag-pipeline'da. Orada yazanı burada tekrar etme; iki kopya zamanla çelişir.
Durum: TASK-008: konuşma açma ve soru gönderme (SSE cevap akışı), RAG hattı (retrieval-api araması, reranker, prompt, Spring AI + Ollama, alıntı, bulamadım tespiti), tur kaydı (V1 migration). TASK-010: konuşma geçmişi (son 3 tamamlanmış tur, cevap prompt'una da girer), takip sorusunu yeniden yazma (QueryRewriter), V2 migration. "Hedef" yazan satırlar henüz kurulmamış parçalardır; ilgili task'ta gerçek değerlerle değiştirilir. Yeni dependency, plugin ya da komut eklenince "Teknoloji" ve "Komutlar" bölümlerini güncelle.
-->

# assistant-api (Spring Boot + Spring AI)

## Teknoloji
- Java 26, Spring Boot 4.1.1 (`spring-boot-starter-parent`), Maven wrapper 3.3.4 → Maven 3.9.16 (`./mvnw` kullanılır, sistemdeki `mvn` değil). `com.rasitbank:assistant-api:0.0.1-SNAPSHOT`. Bağımsız Maven projesidir; retrieval-api ile ortak modül ya da paylaşılan kütüphane yoktur.
- Kök paket `com.rasitbank.assistant`, uygulama sınıfı `AssistantApplication` (lab logları `c.r.assistant.*`). Her pakette `package-info.java` (`@NullMarked`, JSpecify 1.0.1).
- Dependency'ler: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-data-jpa`, `-flyway`, `flyway-database-postgresql`, `spring-ai-starter-model-ollama` (Spring AI 2.0.2, `spring-ai-bom` `dependencyManagement`'ta, sürüm `spring-ai.version`; Spring Framework 7.0.x ile uyumlu), `postgresql` (runtime), `jspecify`. Spring AI starter'ı `spring-boot-starter-restclient` (`RestClient.Builder`) ve `spring-boot-starter-webclient` (Ollama akışı WebClient + reactor-netty ile okunur) getirir; ayrıca eklenmedi. Test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`.
- Plugin'ler: `spring-boot-maven-plugin`; `maven-dependency-plugin` (`properties`, Mockito agent yolu) + surefire `argLine` = `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`; JaCoCo 0.8.15 (prepare-agent, report, check: LINE COVEREDRATIO ≥ `jacoco.line.minimum` = 0.80); PIT 1.30.1 + `pitest-junit5-plugin` 1.2.3 (hedef `com.rasitbank.assistant.*`).
- Hata yanıtları: `exception/GlobalExceptionHandler` (RFC 9457, `type` = `https://rag.example/errors/<error-code>`; framework hataları status adından `errorCode` alır, `@RequestParam` doğrulaması da `VALIDATION_FAILED` döner). Domain hataları `BaseException`'dan türeyen kategori sınıflarıyla fırlatılır: `NotFoundException` (404), `ConflictException` (409), `ServiceUnavailableException` (503); constructor `(errorCode, message)`, handler `application/problem+json` yazar, yeni hata için metot eklenmez. Kodlar: `CONVERSATION_NOT_FOUND`, `TURN_IN_PROGRESS`, `RETRIEVAL_UNAVAILABLE`; `RERANKER_UNAVAILABLE` sadece içeride (yedek yola döner); akış içi `CHAT_MODEL_UNAVAILABLE`, `INTERNAL_ERROR` SSE `error` olayıdır. Tur kaydında ayrıca `STREAM_CANCELLED`.
- Dil modeli Spring AI `ChatModel` (`OllamaChatModel`, otomatik yapılandırma) ile `stream(Prompt)` çağrılır (cevap ve sorgu yeniden yazma); `ChatClient` kullanılmıyor. Sorgu yeniden yazma Spring AI `CompressionQueryTransformer` sınıfını kullanmaz (ayrı `spring-ai-rag` modülü, boş çıktıyı ayırt etmiyor); şablon metni onunkiyle aynıdır (`prompts/query-rewrite.st`). Ollama ayarları `spring.ai.ollama.*`: `base-url=${OLLAMA_BASE_URL:http://localhost:11434}`, `chat.model=${CHAT_MODEL:qwen3.5:9b}`, `chat.temperature=0`, `chat.num-ctx=8192`, `chat.think=false` (`OllamaThinkProperties` enum; Spring AI 2.0'da `chat.options.*` anahtarları deprecated), `init.pull-model-strategy=never`; `spring.ai.model.embedding=none` (embedding otomatik yapılandırması kapalı). Açılışta Ollama'ya gidilmez; model kapalıyken servis `UP` olur.
- PostgreSQL + Flyway (`spring.flyway.locations=classpath:db/migration`, `validate-on-migrate=true`; `V1__create_conversations_and_turns.sql` → `conversations`, `conversation_turns`, `turn_chunks`; `V2__add_query_rewrite_to_turns.sql`), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. Bağlantı ortam değişkenlerinden: `DATABASE_URL` (varsayılan `jdbc:postgresql://localhost:5432/assistant`), `DATABASE_USER` (varsayılan `assistant`), `DATABASE_PASSWORD` (varsayılanı yok). Port `server.port=8090`. Sağlık ucu `/actuator/health` (sadece `health` yayınlanır, `probes.enabled=true`). Veritabanı `assistant` (konuşmalar, turlar, değerlendirme çalıştırmaları; lab tabloları `conversations`, `conversation_turns`, `eval_runs`); vektör yok. assistant-api kendi kullanıcısıyla sadece bu veritabanına bağlanır ve kendi Flyway migration'larını taşır; `retrieval` veritabanını okumaz, retrieval-api ile REST ile konuşur.
- Lokal modeller: dil modeli Ollama `qwen3.5:9b`; reranker llama-server `bge-reranker-v2-m3` (`POST {assistant.reranker.base-url}/v1/rerank`, `{model, query, documents}`, `top_n` yok; belge = `heading + "\n\n" + content`). Embedding modeli retrieval-api'nindir; assistant-api embedding üretmez.
- retrieval-api'ye `retrieval/client/RetrievalClient` ile gider (`POST /api/workspaces/{workspaceId}/search`, `{query, topK}`); reranker'a `RerankClient` ile. İkisi de `RestClient` + `JdkClientHttpRequestFactory` (beanler `retrievalRestClient`, `rerankRestClient`, `config/RestClientConfig`; Boot'un `RestClient.Builder`'ı klonlanır). Ayarlar `config/AssistantProperties` (`assistant.*`, `@Validated`, `@ConfigurationPropertiesScan`):
  - `workspace-id=42`, `host-label=${ASSISTANT_HOST_LABEL:}` (boşsa `null`, `done.hostLabel`)
  - `retrieval-api.base-url=${RETRIEVAL_API_URL:http://localhost:8080}`, `retrieval-api.connect-timeout=2s`, `retrieval-api.read-timeout=15s`
  - `retrieval.strategy=${RETRIEVAL_STRATEGY:rerank}` (`top5` → `TOP_K`, `rerank` → `RERANK`; başka değer açılışı düşürür), `retrieval.candidates=20` (1-50), `retrieval.top-k=5` (`top-k > candidates` açılışı düşürür)
  - `reranker.base-url=${RERANKER_URL:http://localhost:8081}`, `reranker.model=${RERANKER_MODEL:bge-reranker-v2-m3}`, `reranker.connect-timeout=1s`, `reranker.read-timeout=5s`
  - `generation.not-found-answer=Bu konuda rehberimizde bilgi bulamad\u0131m.`, `generation.first-token-timeout=60s`, `generation.token-idle-timeout=30s`, `generation.stream-timeout=300s` (`SseEmitter` süresi)
  - `query-rewrite.enabled=${QUERY_REWRITE_ENABLED:true}` (takip sorusunu yeniden yazma; kapalıyken arama soruyla yapılır, geçmiş yine cevap prompt'una girer), `query-rewrite.timeout=20s` (toplam; dolunca `FAILED`, model aboneliği iptal edilir), `query-rewrite.max-length=1000` (1-1000; uzun sorgu `FAILED`, retrieval-api sınırı)
  - `history.max-turns=3` (1-10; geçmişe giren cevabı tamamlanmış son tur sayısı; `error_code` dolu turlar girmez, cevaplardaki `[n]` silinir)
- Bu ayarlar sunucu yapılandırmasıdır, ekranda düğme yoktur. Üretim modu seçimi (TASK-011) henüz yok; `GenerationMode` sadece `RAG`.
- **`application.properties` saf ASCII'dir:** Türkçe karakter `\uXXXX` kaçışıyla yazılır (Spring dosyayı ISO-8859-1 okur). Düzenledikten sonra `LC_ALL=C grep -c '[^ -~]' src/main/resources/application.properties` → `0`. Shell heredoc'u `\u` kaçışlarını çevirebilir; dosyayı editörle ya da Python ile yaz.
- Veritabanı: `V1__create_conversations_and_turns.sql` → `conversations`, `conversation_turns` (tur başına soru, arama sorgusu, mod, strateji, aday/top-k, model, cevap, `not_found_answer`, `cited int[]`, token sayıları, adım süreleri, `error_code`), `turn_chunks` (her aday bir satır, PK `(turn_id, vector_rank)`, `prompt_position` prompt'a girenlerde dolu, `rerank_score`). `V2__add_query_rewrite_to_turns.sql` → `conversation_turns`'e `query_rewrite_status` (NOT NULL, `DISABLED`/`NO_HISTORY`/`REWRITTEN`/`FAILED`/`EMPTY`; eski turlar `DISABLED`), `query_rewrite_latency_ms` (null), `history_turns` (NOT NULL DEFAULT 0, ≥ 0).
- Referans uygulama: `../embedding-pgvector/backend` (Spring Boot 4.1.1, Java 26): aynı yığının sürümleri ve test/kalite araçları için okunur kalıp referansı; değiştirilmez, kodu toplu kopyalanmaz, çalışma zamanında ona bağlanılmaz.
- Henüz olmayanlar: springdoc, `OpenApiConfig`, `PageResponse`, `ChatClient`, geri bildirim, temsilciye aktarma, değerlendirme. Spring Security, WebSocket/STOMP, tracing de yok. Bunları varmış gibi kod yazma; eklenmeleri planda yazmalıdır. Skill'lerdeki örnekler kullansa bile önce `pom.xml`'e bak.

## Paket yapısı
Kök paket `com.rasitbank.assistant`. TASK-010 itibarıyla olanlar: `conversation/` (`controller/ConversationController`; `service/ConversationService`, `AnswerService` (akış öncesi kontroller, retrieval, emitter), `AnswerStreamer` (model akışı → SSE olayları, iptal, zaman aşımı, tur kaydı tam bir kez), `TurnService` (`recordTurn`, tek transaction), `ActiveTurnRegistry` (konuşma başına tek tur, bellekte), `TurnDraft`, `TurnOutcome`; `repository/ConversationRepository`, `TurnRepository`; `domain/entity/Conversation`, `Turn`, `TurnChunk`, `TurnChunkId`, `TurnValues`, `TurnChunkValues`, `Channel`, `GenerationMode`, `RetrievalStrategy`, `QueryRewriteStatus`; `domain/dto/CreateConversationRequest`, `ConversationResponse`, `SendMessageRequest`; `domain/event/TokenEvent`, `SourcesEvent`, `ContextChunk`, `DoneEvent`, `StreamErrorEvent`), `retrieval/` (`client/RetrievalClient`, `RerankClient` + istek/yanıt record'ları; `service/ContextRetriever`, `RetrievedChunk`, `RetrievedContext`, `QueryRewriter`, `QueryRewriteResult`), `generation/` (`PromptBuilder`, `CitationParser`, `NotFoundAnswerDetector`, `HistoryTurn`), `config/` (`AssistantProperties`, `ClockConfig`, `RestClientConfig`), `exception/`. Prompt şablonları `prompts/rag-system.st`, `prompts/rag-user.st`, `prompts/query-rewrite.st` (İngilizce, lab metni; tek kullanıcı mesajı, `{history}` = `USER: …`/`ASSISTANT: …` satırları, `{query}`) (Spring AI `PromptTemplate`, `{}` ayraçları). Önce özellik, özelliğin içinde katman. Kökte katman paketi (`controllers`, `services`, `api`) ve `common` paketi açılmaz. Ayrıntı: `backend-conventions` bölüm 3 "Paketler".
```
com.rasitbank.assistant
├── conversation/            Konuşma, tur, mesaj gönderme, geri bildirim
│   ├── controller/          ConversationController (SSE endpoint'i dahil)
│   ├── service/             ConversationService, AnswerService
│   ├── repository/          ConversationRepository, TurnRepository
│   └── domain/
│       ├── entity/          Conversation, Turn, GenerationMode, RetrievalStrategy
│       ├── dto/             Request ve Response record'ları
│       └── event/           Akış olayları (TokenEvent, SourcesEvent, DoneEvent) ve domain event'leri
├── retrieval/
│   ├── client/              RetrievalClient (retrieval-api araması), RerankClient (llama-server /v1/rerank)
│   └── service/             ContextRetriever, QueryRewriter
├── generation/              PromptBuilder, sistem talimatları, dil modeli çağrısı (Spring AI ChatClient)
├── handoff/                 Temsilciye aktarma (HandoffController, HandoffService)
├── evaluation/              EvalRun, RetrievalEvaluator, golden set yükleme
├── pagination/              PageResponse, PageParameters
├── config/                  AssistantProperties, ClockConfig, OpenApiConfig
└── exception/               GlobalExceptionHandler, BaseException ve kategori exception'ları
```
- Katman sırası: controller → service → repository. Controller iş kuralı içermez; domain nesnesi controller'dan dışarı çıkmaz, DTO (`record`) döner.
- Kodda `handoff` her zaman "temsilciye aktarma"dır; ekip iş akışındaki handoff dosyalarıyla ilgisi yoktur.
- Prompt şablonları `src/main/resources/prompts/` altındadır (`<name>.st`).
- İhtiyacı olmayan alt paket boş olarak açılmaz.
- Testler aynı paket yapısıyla `src/test/java` altında.

## Komutlar
Hepsi `assistant-api/` içinde çalıştırılır.
- Derle: `./mvnw -q compile`
- Derle + tüm testler + coverage eşiği: `./mvnw verify` (JaCoCo satır coverage %80'in altındaysa build kırılır; hiç test yoksa JaCoCo "missing execution data" diyerek atlar). Veritabanı testleri Testcontainers ile çalışır; Docker açık olmalıdır. Test imajı production'la aynıdır: `pgvector/pgvector:0.8.1-pg18` (assistant veritabanında vektör olmasa da iki servis aynı imajı kullanır).
- Coverage eşiğini kırmadan çalıştır (yeni kodun testleri henüz yokken, backend-coder için): `./mvnw verify -Djacoco.haltOnFailure=false`.
- Mutation testi (PIT): `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` → rapor `target/pit-reports/index.html`
- Tek test sınıfı: `./mvnw test -Dtest=<Sınıf>`; tek metot: `./mvnw test -Dtest='<Sınıf>#<metot>'`
- Lokal veritabanı: depo kökünde `docker compose -f deployment/docker-compose.dev.yml up -d` (sadece postgres; dosya devops'undur).
- Çalıştır: `DATABASE_PASSWORD=assistant-local ./mvnw spring-boot:run` (terminali bloklar; arka planda başlat, iş bitince durdur) → `curl -s localhost:8090/actuator/health`. Port 8090. Gerçek cevap için retrieval-api (8080, en az bir belge `DONE`), Ollama (11434) ve reranker (8081) açık olmalıdır (başlatma: `ollama.md`).
- Cevap akışı duman testi: `curl -s -X POST localhost:8090/api/assistant/conversations -H 'Content-Type: application/json' -d '{"channel":"PREVIEW"}'` → `conversationId`; sonra `curl -sN -X POST localhost:8090/api/assistant/conversations/<id>/messages -H 'Content-Type: application/json' -H 'Accept: text/event-stream, application/problem+json' -d '{"text":"Kartımı kaybettim, ne yapmalıyım?"}'` → `token`… → `sources` → `done` (Spring olay satırlarını iki noktadan sonra boşluksuz yazar: `event:token`). Stratejiyi değiştirmek: `RETRIEVAL_STRATEGY=top5` ile başlat. Takip sorusu: aynı konuşmaya ikinci soruyu gönder → `sources.searchQuery`, `sources.queryRewriteStatus` (`REWRITTEN`), `done.queryRewriteLatencyMs`. Yeniden yazmayı kapatmak: `QUERY_REWRITE_ENABLED=false` ile başlat (→ `DISABLED`). Başarısızlık yolunu denemek: `ASSISTANT_QUERYREWRITE_TIMEOUT=50ms` ile başlat (→ `FAILED`, arama soruyla).
- Testler lokal modeller (Ollama, reranker) ve retrieval-api kapalıyken de geçmelidir: dil modeli, `RetrievalClient` ve `RerankClient` testte sahtelenir (`backend-conventions` bölüm 5 "Dış modeller testte", `rag-pipeline` bölüm 9).
- Geliştirirken tek sınıf ya da metot çalıştır. Handoff'a `STATUS: DONE` yazmadan önce `./mvnw verify` baştan sona geçmeli.

## Dosya sahipliği
| Yol | Sahibi |
|-----|--------|
| `src/main/**` (prompt şablonları ve Flyway migration'ları dahil), `pom.xml`, `.gitignore`, `mvnw*`, `.mvn/**` | backend-coder |
| `src/test/**` | tester |
| `Dockerfile`, `.dockerignore` | devops |

- `pom.xml`'i sadece backend-coder değiştirir. Tester'a test dependency'si gerekiyorsa backend-coder'a mesaj atar.
- Tester testi geçirmek için `src/main` altında değişiklik yapmaz; bulduğu hatayı raporlar.
- Sözleşme dosyaları (`docs/api/**`) planner'ındır; backend-coder onları değiştirmez.

## Kurallar
- Bağlayıcı kurallar `backend-conventions` skill'indedir: dil, yorum politikası (her üst seviye tipte Türkçe amaç Javadoc'u, metot yorumu yok), isimlendirme, dış modeller ve prompt, cevap akışı, loglama sınırları, test adı ve AAA yapısı, pagination. assistant-api/ altında kod ya da test yazmadan önce bu skill'i yükle. Başka bir skill'in örneği bununla çelişirse `backend-conventions` geçerlidir.
- Endpoint path'i, HTTP method'u, DTO alanları ve status kodları `docs/api/openapi.yaml`'ın `/api/assistant/**` kısmıyla, SSE olayları `docs/api/sse.md` ile (ilk akış task'ında oluşturulur) birebir aynıdır. Sözleşmede olmayan endpoint, alan ya da olay eklenmez.
- `RetrievalClient`'ın istek ve yanıt şekli aynı dosyanın `POST /api/workspaces/{workspaceId}/search` kısmına uyar. O kısım retrieval-api'nindir; assistant-api ona göre değiştirilmez, uyumsuzluk planner'a bildirilir.
- Her domain hatası kendi kategori exception'ı ve sabit bir `errorCode` ile RFC 9457 response'una dönüşür (`problem-details-rfc9457`). Akış başladıktan sonraki hata aynı `errorCode` ile SSE `error` olayıdır (`sse-streaming`).
- Her tur, cevabın neye dayandığını sonradan incelemek için modele giden parçaların kimliklerini, alıntıları, arama sorgusunu, stratejiyi, token sayılarını ve süreyi saklar (`backend-conventions` bölüm 3 "Veritabanı", `rag-pipeline`).
- Initializr'dan gelen dosyalar tab ile girintili olabilir. Dokunduğunda 4 boşluğa çevir.

## Spring Boot 4 tuzakları
Eğitim verisindeki eski API'ler burada derlenmez ya da yanlış çalışır.
- `@MockBean` yok → `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`)
- Controller testi `MockMvcTester` ile yazılır; eski `mockMvc.perform(...).andExpect(...)` zinciri değil
- Test slice anotasyonları modüler `*-test` starter'larından gelir; paketleri değişti (`spring-testing-fundamentals`)
- Jackson 3: `tools.jackson.databind.json.JsonMapper`. `com.fasterxml.jackson.databind.ObjectMapper` yok
- ProblemDetail varsayılan olarak kapalıdır: `spring.mvc.problemdetails.enabled=true`
- Yapılandırma `application.properties` ile yapılır; YAML kullanılmaz (Spring AI dokümanlarındaki ve lab'daki YAML örnekleri properties'e çevrilir)
- `@DataJpaTest` testlerinde okuma assertion'ından önce `TestEntityManager.flush()` + `clear()`; yoksa test L1 cache'ten okur
- `IDENTITY` id'li entity'lerde Hibernate batch insert yapmaz
- Mockito, surefire'da `-javaagent` ile yüklenir. JaCoCo/PIT eklenirse agent'ları `@{argLine}` üzerinden birleşir; `argLine` ezilmez
- Spring AI sürümleri Spring Boot sürümüne bağlıdır; Boot 3 için yazılmış Spring AI örnekleri (paket adları, auto-configuration anahtarları) Boot 4'te farklı olabilir. Önce `pom.xml`'deki sürümün dokümanına bak.

## Hangi iş → hangi skill
| İş | Skill |
|----|-------|
| Her kod ve test | `backend-conventions` (zorunlu) |
| Endpoint, service, katmanlar, dış servis istemcisi | `springboot-patterns`, `spring-boot-engineer`, `java-coding-standards` |
| Dil modeli, prompt, sorgu yeniden yazma, retrieval/rerank istemcisi, alıntı, tur kaydı | `rag-pipeline` |
| Cevap akışı (SSE), iptal, zaman aşımı | `sse-streaming`, `java-concurrency` |
| Değerlendirme (golden set, recall@k, MRR, `EvalRun`) | `rag-evaluation` |
| Hata response'u | `problem-details-rfc9457` |
| STOMP / WebSocket (planlı değil; gelirse) | `spring-websocket`; testi `spring-websocket-testing` |
| Girdi doğrulama, prompt injection, güvenlik | `springboot-security` |
| Unit test | `spring-testing-fundamentals`, `writing-good-tests`, `springboot-tdd` |
| Controller testi | `spring-mvc-testing` |
| Transaction, kilitleme | `spring-transactions` |
| JPA, PostgreSQL, migration | `jpa-patterns`, `spring-jpa-testing`, `postgres-patterns`, `flyway-migrations`, `sql-optimization-patterns` |
| Swagger / OpenAPI | `spring-boot-openapi-documentation` |
| Loglama, tracing | `observability-tracing` |
| Review ve doğrulama | `java-code-review`, `code-review-and-quality`, `springboot-verification` |
| Hata ayıklama | `systematic-debugging` |
