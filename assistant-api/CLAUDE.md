<!--
AMAÇ: Claude (ve ekip üyeleri) assistant-api/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
İçerik: harita, komutlar, dosya sahipliği, Boot 4 tuzakları, iş → skill yönlendirmesi.
Kuralların ayrıntısı backend-conventions skill'inde, RAG hattının ayrıntısı rag-pipeline'da. Orada yazanı burada tekrar etme; iki kopya zamanla çelişir.
Durum: iskelet kuruldu (TASK-002): uygulama sınıfı, GlobalExceptionHandler, Actuator sağlık ucu, Flyway (migration yok), datasource. "Hedef" yazan satırlar henüz kurulmamış parçalardır; ilgili task'ta gerçek değerlerle değiştirilir. Yeni dependency, plugin ya da komut eklenince "Teknoloji" ve "Komutlar" bölümlerini güncelle.
-->

# assistant-api (Spring Boot + Spring AI)

## Teknoloji
- Java 26, Spring Boot 4.1.1 (`spring-boot-starter-parent`), Maven wrapper 3.3.4 → Maven 3.9.16 (`./mvnw` kullanılır, sistemdeki `mvn` değil). `com.rasitbank:assistant-api:0.0.1-SNAPSHOT`. Bağımsız Maven projesidir; retrieval-api ile ortak modül ya da paylaşılan kütüphane yoktur.
- Kök paket `com.rasitbank.assistant`, uygulama sınıfı `AssistantApplication` (lab logları `c.r.assistant.*`). Her pakette `package-info.java` (`@NullMarked`, JSpecify 1.0.1).
- Dependency'ler: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-data-jpa`, `-flyway`, `flyway-database-postgresql`, `postgresql` (runtime), `jspecify`. Test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`.
- Plugin'ler: `spring-boot-maven-plugin`; `maven-dependency-plugin` (`properties`, Mockito agent yolu) + surefire `argLine` = `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`; JaCoCo 0.8.15 (prepare-agent, report, check: LINE COVEREDRATIO ≥ `jacoco.line.minimum` = 0.80); PIT 1.30.1 + `pitest-junit5-plugin` 1.2.3 (hedef `com.rasitbank.assistant.*`).
- Hata yanıtları: `exception/GlobalExceptionHandler` (RFC 9457, `type` = `https://rag.example/errors/<error-code>`; framework hataları status adından `errorCode` alır, `@RequestParam` doğrulaması da `VALIDATION_FAILED` döner). `BaseException` ve kategori sınıfları ilk domain hatasını getiren task'ta eklenir.
- Hedef: Spring AI (lab: `spring-ai-starter-model-ollama`, `ChatClient`, `CompressionQueryTransformer`). Sürüm, Spring Boot 4 ile uyumlu olan sürüme göre iskelet ya da ilk model task'ında plan kararıyla seçilir.
- PostgreSQL + Flyway (`spring.flyway.locations=classpath:db/migration`, `validate-on-migrate=true`; henüz migration yok, Flyway "No migrations found" uyarısıyla çalışır), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. Bağlantı ortam değişkenlerinden: `DATABASE_URL` (varsayılan `jdbc:postgresql://localhost:5432/assistant`), `DATABASE_USER` (varsayılan `assistant`), `DATABASE_PASSWORD` (varsayılanı yok). Port `server.port=8090`. Sağlık ucu `/actuator/health` (sadece `health` yayınlanır, `probes.enabled=true`). Veritabanı `assistant` (konuşmalar, turlar, değerlendirme çalıştırmaları; lab tabloları `conversations`, `conversation_turns`, `eval_runs`); vektör yok. assistant-api kendi kullanıcısıyla sadece bu veritabanına bağlanır ve kendi Flyway migration'larını taşır; `retrieval` veritabanını okumaz, retrieval-api ile REST ile konuşur.
- Hedef: lokal modeller (lab: dil modeli Ollama `http://localhost:11434` üzerinde `qwen3.5:9b`, `temperature: 0`, `num_ctx: 8192`, `think: false`; reranker llama-server `http://localhost:8081` üzerinde `bge-reranker-v2-m3`, `POST /v1/rerank`). Embedding modeli retrieval-api'nindir; assistant-api embedding üretmez. Adresler ve model adları properties + ortam değişkeni ile değiştirilir (`backend-conventions` bölüm 7, `rag-pipeline` bölüm 4 ve 6).
- Hedef (lab): retrieval-api'ye `RetrievalClient` ile gider (`POST /api/workspaces/{workspaceId}/search`, `{query, mode: "VECTOR", limit}`); reranker'a `RerankClient` ile. İkisi de `RestClient`, bağlantı ve okuma zaman aşımı yapılandırmadan gelir. Lab'daki ayar anahtarları: `assistant.generation` (`rag` / `llm-only`), `assistant.retrieval` (`top-k: 5`, `candidates: 20`, `rerank: true`), `assistant.query-rewrite`, `assistant.retrieval-api.url` (hedef `http://localhost:8080`), `assistant.reranker.url` (hedef `http://localhost:8081`). Çalışma alanı kimliği tek ve sabittir (42); o da yapılandırmadan gelir, kullanıcıdan alınmaz. Lab ayarları YAML'dır; burada `application.properties` anahtarlarına çevrilir. Kesin anahtar adları ilk assistant-api task'ının planında belirlenir ve bu satır onlarla değiştirilir.
- Hedef: bu ayarlar sunucu yapılandırmasıdır, ekranda düğme yoktur (lab'daki "Canlı yapılandırma" sağ yarıdadır). Değerlendirme ekranının gösterdiği etkin strateji ("rerank 20→5") değerini assistant-api verir.
- Referans uygulama: `../embedding-pgvector/backend` (Spring Boot 4.1.1, Java 26): aynı yığının sürümleri ve test/kalite araçları için okunur kalıp referansı; değiştirilmez, kodu toplu kopyalanmaz, çalışma zamanında ona bağlanılmaz.
- Henüz olmayanlar: Spring AI, `spring-boot-starter-restclient`, springdoc, `ClockConfig`, `OpenApiConfig`, `AssistantProperties`, `PageResponse`, `BaseException` ve kategori exception'ları, Flyway migration'ları. Spring Security, WebSocket/STOMP, tracing de yok. Bunları varmış gibi kod yazma; eklenmeleri planda yazmalıdır. Skill'lerdeki örnekler kullansa bile önce `pom.xml`'e bak.

## Paket yapısı
Kök paket `com.rasitbank.assistant` (iskelette sadece `exception/` var). Önce özellik, özelliğin içinde katman. Kökte katman paketi (`controllers`, `services`, `api`) ve `common` paketi açılmaz. Ayrıntı: `backend-conventions` bölüm 3 "Paketler".
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
- Çalıştır: `DATABASE_PASSWORD=assistant-local ./mvnw spring-boot:run` (terminali bloklar; arka planda başlat, iş bitince durdur) → `curl -s localhost:8090/actuator/health`. Port 8090. RAG modunda gerçek cevap için retrieval-api (8080), Ollama (11434) ve reranker (8081) açık olmalıdır.
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
