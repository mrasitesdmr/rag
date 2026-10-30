<!--
AMAÇ: Claude (ve ekip üyeleri) retrieval-api/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
İçerik: harita, kapsam, komutlar, dosya sahipliği, Boot 4 tuzakları, iş → skill yönlendirmesi.
Kuralların ayrıntısı backend-conventions skill'inde, içe alma ve aramanın ayrıntısı rag-pipeline/references/indexing.md'de. Orada yazanı burada tekrar etme; iki kopya zamanla çelişir.
Durum: TASK-005: çalışma alanı, kaynak, sürüm, parça ve içe alma işi tabloları; içe alma hattı (PDFBox → parçalama → bge-m3 → pgvector); rehberin açılışta kendiliğinden içe alınması; GET .../ingestion-jobs/{jobId}. "Hedef" yazan satırlar henüz kurulmamış parçalardır; ilgili task'ta gerçek değerlerle değiştirilir. Yeni dependency, plugin ya da komut eklenince "Teknoloji" ve "Komutlar" bölümlerini güncelle.
-->

# retrieval-api (Spring Boot + pgvector)

## Teknoloji
- Java 26, Spring Boot 4.1.1 (`spring-boot-starter-parent`), Maven wrapper 3.3.4 → Maven 3.9.16 (`./mvnw` kullanılır, sistemdeki `mvn` değil). `com.rasitbank:retrieval-api:0.0.1-SNAPSHOT`. Bağımsız Maven projesidir; assistant-api ile ortak modül ya da paylaşılan kütüphane yoktur.
- Kök paket `com.rasitbank.retrieval`, uygulama sınıfı `RetrievalApplication`. Her pakette `package-info.java` (`@NullMarked`, JSpecify 1.0.1).
- Dependency'ler: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-data-jpa`, `-flyway`, `flyway-database-postgresql`, `hibernate-vector` (sürümü Boot yönetir), `pdfbox` 3.0.6 (`pdfbox.version`), `postgresql` (runtime), `jspecify`. Test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`.
- Plugin'ler: `spring-boot-maven-plugin`; `maven-dependency-plugin` (`properties`, Mockito agent yolu) + surefire `argLine` = `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`; JaCoCo 0.8.15 (prepare-agent, report, check: LINE COVEREDRATIO ≥ `jacoco.line.minimum` = 0.80); PIT 1.30.1 + `pitest-junit5-plugin` 1.2.3 (hedef `com.rasitbank.retrieval.*`).
- Hata yanıtları: `exception/GlobalExceptionHandler` (RFC 9457, `type` = `https://rag.example/errors/<error-code>`; framework hataları status adından `errorCode` alır, `@RequestParam` doğrulaması da `VALIDATION_FAILED` döner). Domain hataları `BaseException`'dan türeyen kategori sınıflarıyla fırlatılır: `NotFoundException` (404), `ConflictException` (409), `UnprocessableContentException` (422), `ServiceUnavailableException` (503); constructor `(errorCode, message)`, handler'a yeni metot eklenmez. İçe alma işinde bu sınıfların `errorCode`'u işin `errorCode`'u olur (HTTP'ye çıkmaz).
- PostgreSQL 18 + pgvector 0.8.1 (imaj `pgvector/pgvector:0.8.1-pg18`, lab; testlerde de aynısı), Flyway (`spring.flyway.locations=classpath:db/migration`, `validate-on-migrate=true`; `V1__enable_vector_extension.sql`, `V2__create_workspaces_table.sql` (42 "Raşit Bankası · Dijital Asistan", `bge-m3`, 1024), `V3__create_sources_and_ingestion_tables.sql` (`sources` + `sources_id_seq` 10001'den, `ingestion_jobs` + kaynak başına tek aktif iş partial unique index'i, `ingestion_job_files` (PDF baytları), `source_versions`), `V4__create_chunks_table.sql` (`chunks`, `vector(1024)`, HNSW `vector_cosine_ops`, `chunks_id_seq` INCREMENT 50)), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. Bağlantı ortam değişkenlerinden: `DATABASE_URL` (varsayılan `jdbc:postgresql://localhost:5432/retrieval`), `DATABASE_USER` (varsayılan `retrieval`), `DATABASE_PASSWORD` (varsayılanı yok). Port `server.port=8080`. Sağlık ucu `/actuator/health` (sadece `health` yayınlanır, `probes.enabled=true`). `spring.jpa.properties.hibernate.jdbc.batch_size=100`, `order_inserts=true` (parçalar toplu yazılır). Veritabanı `retrieval` (çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri; lab tabloları `workspaces`, `sources`, `chunks`, `ingestion_jobs`). retrieval-api kendi kullanıcısıyla sadece bu veritabanına bağlanır ve kendi Flyway migration'larını taşır; `assistant` veritabanını okumaz. Vektörler sadece `retrieval` veritabanındadır.
- Embedding lokal modeldir, anahtar yok: `EmbeddingClient` (`embedding/client`, `RestClient` + `JdkClientHttpRequestFactory`, bean `embeddingRestClient` `EmbeddingClientConfig`'te) Ollama'nın `/api/embed` ucuna toplu istek atar. Ayarlar `EmbeddingProperties` (`embedding.*`): `base-url=${OLLAMA_BASE_URL:http://localhost:11434}`, `model=${EMBEDDING_MODEL:bge-m3}`, `dimensions=${EMBEDDING_DIMENSIONS:1024}`, `connect-timeout=2s`, `read-timeout=60s`, `max-attempts=3`, `retry-backoff=1s` (deneme n'den sonra n × backoff), `max-concurrent-requests=2` (Semaphore). Bağlantı hatası, zaman aşımı ve 5xx yeniden denenir; 4xx, vektör sayısı ve boyut farkı denenmez. Hata kodları `EMBEDDING_UNAVAILABLE`, `EMBEDDING_DIMENSION_MISMATCH`.
- İçe alma ayarları `IngestionProperties` (`ingestion.*`): `target-chunk-chars=1600`, `max-chunk-chars=2000`, `overlap-chars=200`, `chars-per-token=4`, `embedding-batch-size=96`, `page-header-pattern`, `page-footer-pattern` (rehberin sayfa üst/alt bilgisi), `bootstrap.enabled=${INGESTION_BOOTSTRAP_ENABLED:true}`, `bootstrap.file=${INGESTION_BOOTSTRAP_FILE:../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf}` (varsayılan, `retrieval-api/` içinden çalıştırmaya göre), `bootstrap.source-name`, `bootstrap.version=2026.10`. Dosya yolu `PathConversionConfig`'teki converter ile `Path`'e çevrilir (Spring'in `PathEditor`'ü `../` ile başlayan yolu classpath kaynağı sanar).
- **`application.properties` saf ASCII'dir:** Türkçe karakterler `\uXXXX` kaçışıyla yazılır (Spring dosyayı ISO-8859-1 okur; UTF-8 karakter bozulur ve üst/alt bilgi kalıbı eşleşmez). Düzenledikten sonra `LC_ALL=C grep -c '[^ -~]' src/main/resources/application.properties` → `0` olmalı. Shell heredoc'u `\u` kaçışlarını çevirebilir; dosyayı editörle ya da Python ile yaz.
- Hedef: arama ayarları (lab): `search.default-ef-search=40`, `search.max-scan-tuples=20000` (TASK-006).
- PDF okuma: Apache PDFBox 3.0.6 (`ingestion/parsing/PdfDocumentParser`). Başlıklar PDF'in numaralı yer imlerinden tanınır; satır yer imi başlığına tam eşit ve yer iminin sayfasındaysa başlıktır. "Sık sorulanlar" satırı `<bölüm başlığı> · Sık sorulanlar` kısmını açar.
- Referans uygulama: `../embedding-pgvector/backend` okunur kalıp referansıdır (içe alma hattı, parçalama, HNSW sorguları); kod toplu kopyalanmaz, bu projenin ihtiyacına göre yeniden yazılır; oraya yazılmaz ve çalışma zamanında ona bağlanılmaz.
- Henüz olmayanlar: `spring-boot-starter-restclient`, springdoc, `OpenApiConfig`, `PageResponse`, arama ucu ve `search.*` ayarları, kaynak/sürüm/parça okuma uçları, yükleme ucu. Spring Security, WebSocket/STOMP, tracing, Spring AI de yok. Bunları varmış gibi kod yazma; eklenmeleri planda yazmalıdır. Skill'lerdeki örnekler kullansa bile önce `pom.xml`'e bak.

## Kapsam (lab)
retrieval-api'de tek bir çalışma alanı vardır: 42, "Raşit Bankası · Dijital Asistan". Migration ile gelir; oluşturma, listeleme ya da seçme ekranı ve ucu yoktur. Adreslerde (`/api/workspaces/42/...`) ve Bilgi kaynakları ekranındaki bilgi kutusunda adıyla görünür (lab); ekran adreslerinde (`/kaynaklar/...`) yoktur. Kaynaklar, parçalar ve içe alma işleri çalışma alanına bağlıdır; embedding modeli çalışma alanı düzeyinde tektir (`bge-m3@1024`).

- Kaynaklar ve sürümleri: lab'da kaynak 10001 "Raşit Bankası Ürün ve Hizmet Rehberi" (PDF; sürüm 2026.10, yeni sürüm 2026.11). Yeni sürüm yükleme `202 Accepted` + `Location` + içe alma işi döner.
- İçe alma işleri: aşamalar `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE`, hata olursa `FAILED` (`errorCode` ile); ilerleme polling ile izlenir. Bir kaynağın aynı anda tek aktif işi olur (aktif iş varken yeni sürüm → 409 `INGESTION_IN_PROGRESS`); farklı embedding modeliyle yükleme → 409 `EMBEDDING_MODEL_MISMATCH`.
- Parçalar: başlık, sayfa ve içerikle saklanır; embedding'i `vector(1024)` kolonundadır, vektörün kendisi yanıta konmaz.
- Vektör arama: `POST /api/workspaces/{workspaceId}/search`, `{query, mode: "VECTOR", limit}` (lab: `limit` 5 ya da 20). Çağıran assistant-api'dir. `KEYWORD`/`HYBRID` modlarının olup olmayacağına ilgili task karar verir.
- Yok: aranan sorgular retrieval-api'de saklanmaz ("hangi sorguyla hangi parça geldi" assistant-api'nin tur kaydındadır); embedding deneme ucu yoktur (embedding sadece içe almada ve aramada üretilir).
- `web`'in Bilgi kaynakları ekranı kaynak, sürüm, parça ve içe alma uçlarını aynı adres üzerinden (dev'de Vite proxy, Docker'da nginx) doğrudan çağırır.

## Paket yapısı
Kök paket `com.rasitbank.retrieval`. TASK-005 itibarıyla olanlar: `workspace/` (`Workspace`, `WorkspaceRepository`, `WorkspaceService`), `source/` (`Source`, `SourceKind`, `SourceVersion`, repository'leri), `chunk/` (`Chunk`, `ChunkRepository`), `ingestion/` (`controller/IngestionJobController`; `service/IngestionService`, `IngestionJobLifecycleService` (her aşama kendi transaction'ında), `IngestionJobRunner` (arka plan, tek thread'li `ingestionExecutor`), `GuideBootstrap` + `GuideBootstrapResult`, `IngestionStartupListener` (açılışta önce yarıda kalan işleri `FAILED` + `INGESTION_INTERRUPTED`, sonra rehberin ilk girişi); `domain/entity/IngestionJob`, `IngestionJobFile`, `IngestionStage`; `domain/dto/IngestionJobResponse`; `parsing/` (`PdfDocumentParser`, `ParsedDocument`, `ParsedSection`, `PageLine`); `chunking/` (`TextChunker`, `ChunkDraft`)), `embedding/client/` (`EmbeddingClient`, `OllamaEmbedRequest`, `OllamaEmbedResponse`), `config/` (`ClockConfig`, `EmbeddingProperties`, `IngestionProperties`, `IngestionExecutorConfig`, `EmbeddingClientConfig`, `PathConversionConfig`), `exception/`. Önce özellik, özelliğin içinde katman. Kökte katman paketi (`controllers`, `services`, `api`) ve `common` paketi açılmaz. Ayrıntı: `backend-conventions` bölüm 3 "Paketler".
```
com.rasitbank.retrieval
├── workspace/               Tek çalışma alanı (42): okuma ve varlık kontrolü
├── source/                  Kaynaklar ve sürümleri, yeni sürüm yükleme (ör. SourceController, SourceService, SourceRepository)
├── chunk/                   Parçalar ve listeleme
├── ingestion/               İçe alma işleri, aşamaları, arka plan çalıştırma (ör. IngestionService, IngestionJobRepository)
├── search/                  Vektör arama ucu ve native benzerlik sorgusu
├── embedding/               EmbeddingClient (Ollama /api/embed)
├── pagination/              PageResponse, PageParameters
├── config/                  Uygulama yapılandırması (properties sınıfları), ClockConfig, OpenApiConfig
└── exception/               GlobalExceptionHandler, BaseException ve kategori exception'ları
```
- Her özellik paketinin içi aynıdır: `controller/`, `service/`, `repository/`, `domain/entity/`, `domain/dto/` (ve gerekirse `domain/event/`). İhtiyacı olmayan alt paket boş olarak açılmaz.
- Katman sırası: controller → service → repository. Controller iş kuralı içermez; domain nesnesi controller'dan dışarı çıkmaz, DTO (`record`) döner.
- Exception kategori sınıfları ve `PageResponse` bu servisin kendi paketindedir; assistant-api ile paylaşılmaz.
- Testler aynı paket yapısıyla `src/test/java` altında.

## Komutlar
Hepsi `retrieval-api/` içinde çalıştırılır.
- Derle: `./mvnw -q compile`
- Derle + tüm testler + coverage eşiği: `./mvnw verify` (JaCoCo satır coverage %80'in altındaysa build kırılır; hiç test yoksa JaCoCo "missing execution data" diyerek atlar). Veritabanı ve arama testleri Testcontainers ile gerçek pgvector imajına (`pgvector/pgvector:0.8.1-pg18`) karşı çalışır; Docker açık olmalıdır.
- Testler Ollama kapalıyken de geçmelidir: `OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` kalıbı (embedding testte sahtelenir; `backend-conventions` bölüm 5 "Dış modeller testte", `rag-pipeline/references/indexing.md` bölüm 7).
- Testte rehberin açılış içe alması kapalıdır: `src/test/resources/config/application.properties` (`ingestion.bootstrap.enabled=false`, `embedding.base-url=http://127.0.0.1:9`, `embedding.retry-backoff=0ms`). Bu dosya ana `application.properties`'in üstüne yazar, onu gölgelemez (`src/test/resources/application.properties` açılmaz). Dosya yokken aynı etki ortam değişkeniyle: `INGESTION_BOOTSTRAP_ENABLED=false OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify`.
- Coverage eşiğini kırmadan çalıştır (yeni kodun testleri henüz yokken, backend-coder için): `./mvnw verify -Djacoco.haltOnFailure=false`.
- Mutation testi (PIT): `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` → rapor `target/pit-reports/index.html`
- Tek test sınıfı: `./mvnw test -Dtest=<Sınıf>`; tek metot: `./mvnw test -Dtest='<Sınıf>#<metot>'`
- Lokal veritabanı: depo kökünde `docker compose -f deployment/docker-compose.dev.yml up -d` (sadece postgres; dosya devops'undur).
- Çalıştır: `DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run` (terminali bloklar; arka planda başlat, iş bitince durdur) → `curl -s localhost:8080/actuator/health`. Port 8080. İçe alma ve arama için Ollama (11434) açık olmalıdır. Açılışta rehber tamamlanmış sürümü yoksa kendiliğinden içe alınır (logda `Guide bootstrap result=JOB_QUEUED`, ardından `Ingestion job finished`); durum: `curl -s localhost:8080/api/workspaces/42/ingestion-jobs/1`. Kapatmak için `INGESTION_BOOTSTRAP_ENABLED=false`.
- Geliştirirken tek sınıf ya da metot çalıştır. Handoff'a `STATUS: DONE` yazmadan önce `./mvnw verify` baştan sona geçmeli.

## Dosya sahipliği
| Yol | Sahibi |
|-----|--------|
| `src/main/**` (Flyway migration'ları dahil), `pom.xml`, `.gitignore`, `mvnw*`, `.mvn/**` | backend-coder |
| `src/test/**` | tester |
| `Dockerfile`, `.dockerignore` | devops |

- `pom.xml`'i sadece backend-coder değiştirir. Tester'a test dependency'si gerekiyorsa backend-coder'a mesaj atar.
- Tester testi geçirmek için `src/main` altında değişiklik yapmaz; bulduğu hatayı raporlar.
- Sözleşme dosyaları (`docs/api/**`) planner'ındır; backend-coder onları değiştirmez.

## Kurallar
- Bağlayıcı kurallar `backend-conventions` skill'indedir: dil, yorum politikası (her üst seviye tipte Türkçe amaç Javadoc'u, metot yorumu yok), isimlendirme, dış modeller, loglama sınırları, test adı ve AAA yapısı, pagination. retrieval-api/ altında kod ya da test yazmadan önce bu skill'i yükle. Başka bir skill'in örneği bununla çelişirse `backend-conventions` geçerlidir.
- Endpoint path'i, HTTP method'u, DTO alanları ve status kodları `docs/api/openapi.yaml`'ın `/api/workspaces/**` kısmıyla birebir aynıdır. Sözleşmede olmayan endpoint ya da alan eklenmez.
- Arama ucu assistant-api'nin `RetrievalClient`'ı tarafından kullanılır; şeklini değiştirmek iki servisi etkileyen bir sözleşme değişikliğidir (planner).
- Uzun süren işler (yeni sürüm yükleme, içe alma) `202 Accepted` + `Location` döner; durum ayrı bir `GET` ile sorgulanır.
- Her domain hatası kendi kategori exception'ı ve sabit bir `errorCode` ile RFC 9457 response'una dönüşür (`problem-details-rfc9457`; exception paketi `com.rasitbank.retrieval.exception`).
- Parça içeriği ve arama sorgusu loglanmaz; sadece kimlik, sayı ve süre (lab: `Search completed workspaceId={} mode={} hitCount={} durationMs={}`).
- Initializr'dan gelen dosyalar tab ile girintili olabilir. Dokunduğunda 4 boşluğa çevir.

## Spring Boot 4 tuzakları
Eğitim verisindeki eski API'ler burada derlenmez ya da yanlış çalışır.
- `@MockBean` yok → `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`)
- Controller testi `MockMvcTester` ile yazılır; eski `mockMvc.perform(...).andExpect(...)` zinciri değil
- Test slice anotasyonları modüler `*-test` starter'larından gelir; paketleri değişti (`spring-testing-fundamentals`)
- Jackson 3: `tools.jackson.databind.json.JsonMapper`. `com.fasterxml.jackson.databind.ObjectMapper` yok
- ProblemDetail varsayılan olarak kapalıdır: `spring.mvc.problemdetails.enabled=true`
- Yapılandırma `application.properties` ile yapılır; YAML kullanılmaz (dokümanlardaki YAML örnekleri properties'e çevrilir)
- `@DataJpaTest` testlerinde okuma assertion'ından önce `TestEntityManager.flush()` + `clear()`; yoksa test L1 cache'ten okur
- `IDENTITY` id'li entity'lerde Hibernate batch insert yapmaz; toplu yazılan `Chunk` id'si `SEQUENCE` ile üretilir (`rag-pipeline/references/indexing.md` bölüm 5)
- Mockito, surefire'da `-javaagent` ile yüklenir. JaCoCo/PIT eklenirse agent'ları `@{argLine}` üzerinden birleşir; `argLine` ezilmez
- `CREATE EXTENSION IF NOT EXISTS vector` ilk migration'dadır
- Vektör kolon boyutu (`vector(1024)`) `embedding.dimensions` ile aynıdır; model değişirse kolon ve index migration ile değişir

## Hangi iş → hangi skill
| İş | Skill |
|----|-------|
| Her kod ve test | `backend-conventions` (zorunlu) |
| Endpoint, service, katmanlar, dosya yükleme (multipart), dış servis istemcisi | `springboot-patterns`, `spring-boot-engineer`, `java-coding-standards` |
| İçe alma, parçalama, embedding, arama | `rag-pipeline/references/indexing.md`, `postgres-patterns`, `sql-optimization-patterns` |
| Arka plan içe alma işi, executor, embedding modeline sınırlı paralel istek | `java-concurrency`, `spring-transactions` |
| Hata response'u | `problem-details-rfc9457` |
| STOMP / WebSocket (planlı değil; içe alma ilerlemesi polling ile izlenir) | `spring-websocket`; testi `spring-websocket-testing` |
| Girdi doğrulama, dosya türü ve boyutu, güvenlik | `springboot-security` |
| Unit test | `spring-testing-fundamentals`, `writing-good-tests`, `springboot-tdd` |
| Controller testi | `spring-mvc-testing` |
| Transaction, kilitleme | `spring-transactions` |
| JPA, PostgreSQL, pgvector, migration | `jpa-patterns`, `spring-jpa-testing`, `postgres-patterns`, `flyway-migrations`, `sql-optimization-patterns` |
| Swagger / OpenAPI | `spring-boot-openapi-documentation` |
| Loglama, tracing | `observability-tracing` |
| Review ve doğrulama | `java-code-review`, `code-review-and-quality`, `springboot-verification` |
| Hata ayıklama | `systematic-debugging` |
