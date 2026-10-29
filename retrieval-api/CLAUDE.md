<!--
AMAÇ: Claude (ve ekip üyeleri) retrieval-api/ altında bir dosyayı okuyunca ya da düzenleyince yüklenir.
İçerik: harita, kapsam, komutlar, dosya sahipliği, Boot 4 tuzakları, iş → skill yönlendirmesi.
Kuralların ayrıntısı backend-conventions skill'inde, içe alma ve aramanın ayrıntısı rag-pipeline/references/indexing.md'de. Orada yazanı burada tekrar etme; iki kopya zamanla çelişir.
Durum: iskelet kuruldu (TASK-002): uygulama sınıfı, GlobalExceptionHandler, Actuator sağlık ucu, Flyway (`V1__enable_vector_extension.sql`), datasource. "Hedef" yazan satırlar henüz kurulmamış parçalardır; ilgili task'ta gerçek değerlerle değiştirilir. Yeni dependency, plugin ya da komut eklenince "Teknoloji" ve "Komutlar" bölümlerini güncelle.
-->

# retrieval-api (Spring Boot + pgvector)

## Teknoloji
- Java 26, Spring Boot 4.1.1 (`spring-boot-starter-parent`), Maven wrapper 3.3.4 → Maven 3.9.16 (`./mvnw` kullanılır, sistemdeki `mvn` değil). `com.rasitbank:retrieval-api:0.0.1-SNAPSHOT`. Bağımsız Maven projesidir; assistant-api ile ortak modül ya da paylaşılan kütüphane yoktur.
- Kök paket `com.rasitbank.retrieval`, uygulama sınıfı `RetrievalApplication`. Her pakette `package-info.java` (`@NullMarked`, JSpecify 1.0.1).
- Dependency'ler: `spring-boot-starter-webmvc`, `-validation`, `-actuator`, `-data-jpa`, `-flyway`, `flyway-database-postgresql`, `postgresql` (runtime), `jspecify`. Test: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`.
- Plugin'ler: `spring-boot-maven-plugin`; `maven-dependency-plugin` (`properties`, Mockito agent yolu) + surefire `argLine` = `@{argLine} -javaagent:${org.mockito:mockito-core:jar}`; JaCoCo 0.8.15 (prepare-agent, report, check: LINE COVEREDRATIO ≥ `jacoco.line.minimum` = 0.80); PIT 1.30.1 + `pitest-junit5-plugin` 1.2.3 (hedef `com.rasitbank.retrieval.*`).
- Hata yanıtları: `exception/GlobalExceptionHandler` (RFC 9457, `type` = `https://rag.example/errors/<error-code>`; framework hataları status adından `errorCode` alır, `@RequestParam` doğrulaması da `VALIDATION_FAILED` döner). `BaseException` ve kategori sınıfları ilk domain hatasını getiren task'ta eklenir.
- PostgreSQL 18 + pgvector 0.8.1 (imaj `pgvector/pgvector:0.8.1-pg18`, lab; testlerde de aynısı), Flyway (`spring.flyway.locations=classpath:db/migration`, `validate-on-migrate=true`; `V1__enable_vector_extension.sql`), `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. Bağlantı ortam değişkenlerinden: `DATABASE_URL` (varsayılan `jdbc:postgresql://localhost:5432/retrieval`), `DATABASE_USER` (varsayılan `retrieval`), `DATABASE_PASSWORD` (varsayılanı yok). Port `server.port=8080`. Sağlık ucu `/actuator/health` (sadece `health` yayınlanır, `probes.enabled=true`). Hedef: `hibernate-vector` (ilk vektör kolonuyla). Veritabanı `retrieval` (çalışma alanı, kaynaklar, parçalar ve embedding'leri, içe alma işleri; lab tabloları `workspaces`, `sources`, `chunks`, `ingestion_jobs`). retrieval-api kendi kullanıcısıyla sadece bu veritabanına bağlanır ve kendi Flyway migration'larını taşır; `assistant` veritabanını okumaz. Vektörler sadece `retrieval` veritabanındadır.
- Hedef: embedding lokal modeldir, anahtar yok: Ollama `http://localhost:11434`, `bge-m3`, 1024 boyut (`embedding.base-url`, `embedding.model`, `embedding.dimensions`; adres ortam değişkeniyle `EMBEDDING_BASE_URL`). İstemci `EmbeddingClient` Ollama'nın `/api/embed` ucuna toplu istek atar; hangi kütüphaneyle yazılacağı ilk retrieval-api task'ının planındadır.
- Hedef: arama ve içe alma ayarları (lab): `search.default-ef-search=40`, `search.max-scan-tuples=20000`, `ingestion.embedding-batch-size=96`, `ingestion.target-chunk-chars=1600`, `ingestion.max-chunk-chars=2000`, `ingestion.overlap-chars=200`, `ingestion.chars-per-token=4`. Kesin değerler ilk retrieval-api task'ının planında belirlenir.
- Hedef: PDF okuma kütüphanesi plan kararıdır (referans uygulamada Apache PDFBox).
- Referans uygulama: `../embedding-pgvector/backend` okunur kalıp referansıdır (içe alma hattı, parçalama, HNSW sorguları); kod toplu kopyalanmaz, bu projenin ihtiyacına göre yeniden yazılır; oraya yazılmaz ve çalışma zamanında ona bağlanılmaz.
- Henüz olmayanlar: `hibernate-vector`, `spring-boot-starter-restclient`, PDF kütüphanesi, springdoc, `EmbeddingClient`, `ClockConfig`, `OpenApiConfig`, `PageResponse`, `BaseException` ve kategori exception'ları, domain tabloları. Spring Security, WebSocket/STOMP, tracing, Spring AI de yok. Bunları varmış gibi kod yazma; eklenmeleri planda yazmalıdır. Skill'lerdeki örnekler kullansa bile önce `pom.xml`'e bak.

## Kapsam (lab)
retrieval-api'de tek bir çalışma alanı vardır: 42, "Raşit Bankası · Dijital Asistan". Migration ile gelir; oluşturma, listeleme ya da seçme ekranı ve ucu yoktur. Adreslerde (`/api/workspaces/42/...`) ve Bilgi kaynakları ekranındaki bilgi kutusunda adıyla görünür (lab); ekran adreslerinde (`/kaynaklar/...`) yoktur. Kaynaklar, parçalar ve içe alma işleri çalışma alanına bağlıdır; embedding modeli çalışma alanı düzeyinde tektir (`bge-m3@1024`).

- Kaynaklar ve sürümleri: lab'da kaynak 10001 "Raşit Bankası Ürün ve Hizmet Rehberi" (PDF; sürüm 2026.10, yeni sürüm 2026.11). Yeni sürüm yükleme `202 Accepted` + `Location` + içe alma işi döner.
- İçe alma işleri: aşamalar `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE`, hata olursa `FAILED` (`errorCode` ile); ilerleme polling ile izlenir. Bir kaynağın aynı anda tek aktif işi olur (aktif iş varken yeni sürüm → 409 `INGESTION_IN_PROGRESS`); farklı embedding modeliyle yükleme → 409 `EMBEDDING_MODEL_MISMATCH`.
- Parçalar: başlık, sayfa ve içerikle saklanır; embedding'i `vector(1024)` kolonundadır, vektörün kendisi yanıta konmaz.
- Vektör arama: `POST /api/workspaces/{workspaceId}/search`, `{query, mode: "VECTOR", limit}` (lab: `limit` 5 ya da 20). Çağıran assistant-api'dir. `KEYWORD`/`HYBRID` modlarının olup olmayacağına ilgili task karar verir.
- Yok: aranan sorgular retrieval-api'de saklanmaz ("hangi sorguyla hangi parça geldi" assistant-api'nin tur kaydındadır); embedding deneme ucu yoktur (embedding sadece içe almada ve aramada üretilir).
- `web`'in Bilgi kaynakları ekranı kaynak, sürüm, parça ve içe alma uçlarını aynı adres üzerinden (dev'de Vite proxy, Docker'da nginx) doğrudan çağırır.

## Paket yapısı
Kök paket `com.rasitbank.retrieval` (iskelette sadece `exception/` var). Önce özellik, özelliğin içinde katman. Kökte katman paketi (`controllers`, `services`, `api`) ve `common` paketi açılmaz. Ayrıntı: `backend-conventions` bölüm 3 "Paketler".
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
- Testler Ollama kapalıyken de geçmelidir: `EMBEDDING_BASE_URL=http://127.0.0.1:9 ./mvnw verify` kalıbı (embedding testte sahtelenir; `backend-conventions` bölüm 5 "Dış modeller testte", `rag-pipeline/references/indexing.md` bölüm 7).
- Coverage eşiğini kırmadan çalıştır (yeni kodun testleri henüz yokken, backend-coder için): `./mvnw verify -Djacoco.haltOnFailure=false`.
- Mutation testi (PIT): `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` → rapor `target/pit-reports/index.html`
- Tek test sınıfı: `./mvnw test -Dtest=<Sınıf>`; tek metot: `./mvnw test -Dtest='<Sınıf>#<metot>'`
- Lokal veritabanı: depo kökünde `docker compose -f deployment/docker-compose.dev.yml up -d` (sadece postgres; dosya devops'undur).
- Çalıştır: `DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run` (terminali bloklar; arka planda başlat, iş bitince durdur) → `curl -s localhost:8080/actuator/health`. Port 8080. İçe alma ve arama için Ollama (11434) açık olmalıdır.
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
