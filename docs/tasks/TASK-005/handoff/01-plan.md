TASK: TASK-005
# Plan

## Özet
**Mevcut durum:** retrieval-api iskelet hâlinde. Uygulama sınıfı, `GlobalExceptionHandler`, sağlık ucu ve `V1__enable_vector_extension.sql` var. Domain tablosu, uç, `EmbeddingClient`, PDF kütüphanesi ve `BaseException` yok. `docs/api/openapi.yaml`'da hiç uç yoktu. Rehberin iki PDF'i `docs/rehber/pdf/` altında duruyor (TASK-004). 2026.10: 70 sayfa, 11 bölüm, 110 numaralı başlık (11 bölüm + 99 alt başlık), 8 "Sık sorulanlar". Bütün numaralı başlıklar PDF yer imi olarak doğru sayfada (D6).

**Yeni durum:**
- **Tablolar (`retrieval` veritabanı):** `workspaces` (42 migration ile gelir), `sources`, `ingestion_jobs`, `ingestion_job_files`, `source_versions`, `chunks` (`vector(1024)` + HNSW cosine).
- **İçe alma hattı:** PDF (PDFBox) → başlıklara göre kısımlar → parçalar → `bge-m3` (Ollama `/api/embed`, 96'lık gruplar) → tek transaction'da yazma. İş arka planda yürür, her aşama kendi kısa transaction'ında yazılır.
- **Uç:** `GET /api/workspaces/{workspaceId}/ingestion-jobs/{jobId}`.
- **Açılış:** retrieval-api açılınca önce yarıda kalmış işleri `FAILED` + `INGESTION_INTERRUPTED` yapar (kart sorusu 2, önerilen). Sonra rehberin ilk girişini yapar (kart sorusu 1, önerilen). Bilgi bankasında rehber kaynağının tamamlanmış sürümü yoksa yapılandırılmış PDF'ten bir iş başlatır. Kaynak yoksa oluşturur (ilk kaynak 10001).
- **Değişen servis:** sadece retrieval-api. assistant-api ve web değişmez. Docker: retrieval-api'ye PDF klasörü salt okunur bağlanır ve bir ortam değişkeni eklenir.
- **Silinen ya da yeniden adlandırılan parça yok.** `retrieval-api/CLAUDE.md`'deki `EMBEDDING_BASE_URL` adı, Compose'daki ve `ollama.md`'deki gerçek adla değiştirilir: `EMBEDDING_BASE_URL` → `OLLAMA_BASE_URL` (Kararlar #14).

**Kart sorularının başka bir cevabı gelirse:**
- **Soru 1 = B ya da C:** `POST /api/workspaces/{workspaceId}/sources` (multipart `name`, `version`, `file`; 202 + `Location`) sözleşmeye eklenir. B'de `GuideBootstrap` kalkar. Plan güncellenir.
- **Soru 2 = B:** `failInterruptedJobs` yerine `requeueInterruptedJobs` yazılır. İş sıfırlanır (`QUEUED`, progress 0, `startedAt` null) ve dosyası `ingestion_job_files`'tan okunup yeniden kuyruğa alınır. `INGESTION_INTERRUPTED` sözleşmeden çıkar.
- **Soru 3 = B:** `hibernate-vector` eklenmez. `Chunk.embedding` entity'de eşlenmez. Parçalar `JdbcTemplate.batchUpdate` + `cast(? as vector)` ile yazılır.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** retrieval-api `src/main/**`, `pom.xml`, `retrieval-api/CLAUDE.md`. Rapor: `handoff/02-backend-implementation.md`. Lokal makinede (Ollama açık) gerçek PDF ve gerçek `bge-m3` ile bir uçtan uca çalıştırma yapar ve sonuçları raporuna yazar (M0).
- **tester:** `retrieval-api/src/test/**`. Rapor: `handoff/03-test-report.md`.
- **devops:** `deployment/docker-compose.yml` (PDF bağlama, ortam değişkeni), `deployment/CLAUDE.md`, `ollama.md` (bölüm 8'deki "Okuyacak task" sütunu artık gerçek), kök `README.md` (ilk açılışta rehberin kendiliğinden girdiği). Docker'da M1-M3'ü çalıştırır. Rapor: `handoff/04-deploy-report.md`.
- **qa:** M4-M5, kabul kriterleri, sözleşme uyumu. Rapor: `handoff/05-qa-report.md`.
- **Atlanan:** frontend-coder. Ekran bu task'ta yok (TASK.md "Ekran bu task'ta yok"; Bilgi kaynakları TASK-007).

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Tek çalışma alanı 42 "Raşit Bankası · Dijital Asistan", migration ile; embedding modeli çalışma alanı düzeyinde `bge-m3@1024` | `V2__create_workspaces_table.sql`; `Workspace.embeddingModelLabel()`; `IngestionJobRunner` model kontrolü | T30, T27, T43 |
| K2 | Aşamalar `QUEUED → PARSING → CHUNKING → EMBEDDING → WRITING → DONE`, hata `FAILED` + `errorCode`; `GET .../ingestion-jobs/{id}` aşama, ilerleme (embed edilen/toplam), parça sayısı, zaman damgaları | `IngestionJob`, `IngestionJobLifecycleService`, `IngestionJobController`, `IngestionJobResponse`; openapi | T23, T24, T25, T29, T37, T40, T41, T42 |
| K3 | İş arka planda; istek hemen döner; servis yeniden başlarsa yarıda kalan iş unutulmaz (kart sorusu 2) | `IngestionService.queueIngestion` (commit sonrası executor), `IngestionService.failInterruptedJobs`, `IngestionStartupListener` | T35, T45, M2 |
| K4 | Parçalama: hedef 1.600, en fazla 2.000, 200 örtüşme; başlık + sayfa; mümkünse bölüm sınırında biter; sıra no, sayfa no, başlık, metin, token; tablo satırı bölünmez | `TextChunker`, `ChunkDraft`, `PdfDocumentParser`; `chunks` CHECK'leri | T1-T8, T9-T12, T15, T31, M4 |
| K5 | 96'lık gruplar; eşzamanlı istek sınırlı; 1024 boyut, farklı boyut → `FAILED` | `IngestionJobRunner` (gruplar), `EmbeddingClient` (Semaphore, boyut kontrolü) | T19, T22, T26, T29 |
| K6 | pgvector, HNSW, cosine | `V4__create_chunks_table.sql` | T38 |
| K7 | İlk giriş gerçek hattan geçer, elle/seed parça yok (kart sorusu 1) | `GuideBootstrap.bootstrapGuide` → `IngestionService.queueIngestion` → `IngestionJobRunner` (TASK-015'teki yükleme de aynı hattı kullanır) | T31, M1 |
| K8 | Yeniden açılışta ikinci kaynak ya da mükerrer parça yok; kaynak kimliği sequence'ten, ilk 10001 | `sources_id_seq START 10001`; `GuideBootstrap` (ad ile arar); `sources (workspace_id, name)` unique; `chunks (source_version_id, chunk_no)` unique | T31, T32, T34, M2 |
| K9 | PDF ve özellikleri (dosya adı, bayt, sayfa sayısı, bölüm sayısı) saklanır | `ingestion_job_files.content`; `source_versions` (`file_name`, `file_size_bytes`, `page_count`, `chapter_count`) | T31 |
| K10 | Her içe alma sürüm etiketiyle; "son senkron" = işin bitiş zamanı | `ingestion_jobs.version` NOT NULL; `source_versions.synced_at = ingestion_jobs.finished_at` | T31 |
| K11 | Parça içeriği loglanmaz; iş kimliği, aşama, parça sayısı, süre loglanır | Backend planı "Loglar" | T39, M5 |
| S1 | İlk açılış → iş `DONE`; kaynak 10001, sürüm `2026.10`, 1024 boyutlu parçalar | `GuideBootstrap` + hat | T31, M1 |
| S2 | Embedding modeli kapalı → iş EMBEDDING'de `FAILED`, `EMBEDDING_UNAVAILABLE`; yarım parça yok | `EmbeddingClient` retry → `ServiceUnavailableException`; runner `fail`; yazma yalnız WRITING'de | T18, T26, T33, M3 |
| S3 | Yeniden başlatma → kaynak sayısı 1 | `GuideBootstrap` | T32, T34, M2 |
| AC1 | Gerçek PDF + gerçek `bge-m3` uçtan uca; parça sayısı, ortalama token, süre raporda | backend-coder M0, devops M1 | M0, M1 |
| AC2 | Rastgele parçaların sayfa ve başlığı PDF ile elle karşılaştırılmış | qa | M4 |
| AC3 | Parçalama, durum geçişleri, hata durumları testli | tester | T1-T29, T33-T36, T40-T44 |
| AC4 | `./mvnw verify` geçer | backend-coder, tester | tester raporu |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | retrieval-api, Ollama kapalıyken ilk kez açılır | Kaynak 10001 oluşur. İş EMBEDDING'de `FAILED` olur (`EMBEDDING_UNAVAILABLE`). Parça ve sürüm yazılmaz. Servis `healthy` kalır. Sonraki açılışta aynı kaynak için yeni bir iş başlar (kart kararı) | T33, T34, M3 |
| G2 | Yapılandırılmış rehber dosyası yok ya da okunamıyor | Hiçbir şey yazılmaz. `ERROR` log (sadece yol). Servis açık kalır. Sonraki açılışta yeniden bakılır | T36 |
| G3 | Servis iş sürerken kapanır (ör. EMBEDDING'de) | Açılışta iş `FAILED` + `INGESTION_INTERRUPTED`, `failedStage` = kaldığı aşama. Ardından rehber hiç tamamlanmadıysa yeni iş başlar (kart sorusu 2) | Soru 2, T35, T45 |
| G4 | Dosya bozuk ya da PDF değil; ya da PDF'te yer imi yok | İş PARSING'de `FAILED`: `PDF_UNREADABLE` ya da `PDF_NO_SECTIONS` | T13, T14 |
| G5 | Var olmayan iş, 42 dışındaki çalışma alanı, sayı olmayan kimlik | 404 `INGESTION_JOB_NOT_FOUND`, 404 `WORKSPACE_NOT_FOUND`, 400 `BAD_REQUEST` | T42, T43, T44 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Başlıklar PDF yer imlerinden tanınır.** Numaralı yer imleri (`^\d+(\.\d+)? .+`) okunur. Bir satır, yer iminin başlığına **tam eşit** ve yer iminin sayfasındaysa başlıktır. Noktasız numara bölümdür, noktalı numara alt başlıktır. "Sık sorulanlar" satırı (tam eşit) içinde bulunduğu bölümün SSS kısmını açar: başlığı `<bölüm başlığı> · Sık sorulanlar`, ör. `2 Kartlar · Sık sorulanlar`. Bir bölüm başlığıyla ilk alt başlığı arasındaki satırlar bölüm başlığını alır (ör. `11 Terimler sözlüğü`). İçerik, ilk bölüm yer iminin sayfasından başlar. Kapak ve içindekiler atlanır. Numaralı yer imi yoksa ya da yer imlerinin altında hiç satır yoksa sonuç `PDF_NO_SECTIONS` olur | D6 her numaralı başlığın yer imi olarak, tek satırda ve doğru sayfada olduğunu garanti ediyor. Metin regex'i içindekiler satırlarını ("2.16 … 16") ve "1 Kasım 2026 …" gibi sayıyla başlayan gövde satırlarını başlık sanar | Satır regex'i: yanlış başlık. Yazı tipi boyutu/kalınlığı ile tanıma (TASK-004 `kisimlar`): özel `PDFTextStripper` alt sınıfı gerekir, daha kırılgan |
| 2 | **Metin çıkarma:** `PDFTextStripper`, varsayılan `setSortByPosition(false)`, `setLineSeparator("\n")`, sayfa sayfa. Her sayfada ilk satır `ingestion.page-header-pattern`'e, son satır `ingestion.page-footer-pattern`'e uyuyorsa atılır. Satırlar `strip()` edilir, boş satır atılır | TASK-004 README "İçe alma için notlar": bu ayarla tablo satırı tek satır gelir. D4 ve D5 bunu doğruluyor | Konum sıralı çıkarma: tablo satırlarının bütünlüğü doğrulanmadı |
| 3 | **Parçalama birimi satırdır; parça satır ortasında kesilmez.** Algoritma Backend planında ("TextChunker"). Örtüşme, önceki parçanın son tam satırlarıdır (toplam ≤ 200 karakter) | Tablo satırı tek satır olarak geldiği için "satır bölünmez" kuralı kendiliğinden sağlanır | Lab'daki cümle bazlı birim: tablo satırlarında cümle sonu yok. Karakter bazlı kesme: satır böler |
| 4 | **Parçanın sayfası**, parçadaki ilk "yeni" satırın (önceki parçadan örtüşmeyle gelmeyen) sayfasıdır | Örtüşme önceki sayfadan geldiğinde parçanın sayfası yanlış görünmesin | `page_start/page_end` iki kolon: TASK tek sayfa no istiyor; alıntı "s. N" gösteriyor |
| 5 | **PDF baytları veritabanında**, ayrı tabloda tutulur: `ingestion_job_files (ingestion_job_id PK/FK, content bytea)`. Her işin kendi dosyası olur | Ek volume ve dosya yolu senkronu yok. Yedek ve `down -v` veritabanıyla birlikte. Ayrı tablo, iş satırı okunurken baytların yüklenmesini önler | Disk volume: devops işi artar, kayıtla dosya ayrışabilir. `sources`/`ingestion_jobs` içinde kolon: her okumada 1,1 MB yüklenir (Hibernate lazy basic, bytecode enhancement ister) |
| 6 | **`source_versions` satırı sadece iş `DONE` olunca** (WRITING transaction'ında) yazılır: `ingestion_job_id` (unique), `version`, `file_name`, `file_size_bytes`, `page_count`, `chapter_count`, `chunk_count`, `synced_at`. `sources.active_version_id` bu satırı gösterir. Başarısız işin sürümü yoktur | "Sürüm" bilgi bankasındaki bir hâldir. TASK-007'deki "PDF'i aç", `source_versions.ingestion_job_id` → `ingestion_job_files` yolunu izler | Sürüm satırını kabul anında açmak: başarısız denemeler sürüm listesinde görünür, `(source_id, version)` tekilliği yeniden denemeyi engeller |
| 7 | **Entity'ler arası JPA ilişkisi yok.** `IngestionJob.sourceId`, `Chunk.sourceId`, `Chunk.sourceVersionId`, `SourceVersion.sourceId`, `Source.activeVersionId` düz `Long`/`long` alanlardır. FK'lar veritabanındadır | Lazy loading, `open-in-view=false` ve türetilmiş getter'lı derived query tuzakları (`backend-conventions` bölüm 3) yaşanmaz | `@ManyToOne`: bu task'ta gezinme gerekmiyor |
| 8 | **`EmbeddingClient` tek sınıftır**, interface değildir. `RestClient` + `JdkClientHttpRequestFactory` (connect/read timeout) `EmbeddingClientConfig`'te bean olarak (`embeddingRestClient`) kurulur. `spring-boot-starter-restclient` eklenmez | `RestClient` `spring-web`'de zaten var (webmvc starter). Test `MockRestServiceServer.bindTo(builder)` ile kurulur | Restclient starter: bir dependency daha. Spring AI: retrieval-api'de yok, sadece embedding için ağır |
| 9 | **Retry ve sınır istemcide:** `embedding.max-attempts=3`, `embedding.retry-backoff=1s` (deneme n'den sonra n × backoff bekler). Bağlantı hatası, zaman aşımı ve 5xx yeniden denenir. 4xx, boyut farkı ve vektör sayısı farkı denenmez. `Semaphore(embedding.max-concurrent-requests=2)` her HTTP isteğini sarar | Lokal model paralel istekte yavaşlar (`indexing.md` §4). Bir işin grupları sırayla gider. Sınır, ileride aramanın (TASK-006) aynı istemciyi kullanacağı içindir | Spring Retry: yeni dependency. Grupları paralel göndermek: ilerleme sırası bozulur, lokal modelde hız kazancı yok |
| 10 | **Executor:** tek thread'li `ExecutorService` bean'i `ingestionExecutor`, thread adı `ingestion-N` (lab logu). Context kapanırken `shutdownNow()` | Aynı anda tek iş yeterli: kaynak tek, yükleme TASK-015'te | Virtual thread: iki iş aynı anda modele yüklenir. `@Async`: yapılandırması dağınık |
| 11 | **Açılış sırası tek dinleyicide:** `IngestionStartupListener` `ApplicationReadyEvent`'te önce `failInterruptedJobs()`, sonra `bootstrapGuide()` çağırır | Kurtarma bootstrap'tan önce olmazsa yarıda kalmış aktif iş bootstrap'ı "iş zaten var" diye durdurur | İki ayrı listener: sıra garanti değil |
| 12 | **İşin hata kodu `String`'dir** ve `BaseException.getErrorCode()`'dan gelir. Parser `UnprocessableContentException` (422), istemci `ServiceUnavailableException` (503), model farkı `ConflictException` (409) fırlatır. Runner `BaseException` → `fail(errorCode)`, diğer `RuntimeException` → `fail("INTERNAL_ERROR")` + `ERROR` log | Aynı kategori sınıfları TASK-006 (arama 503) ve TASK-015 (yükleme 409/422) uçlarında HTTP'ye çıkacak. İçeride ayrı exception hiyerarşisi açılmaz (`problem-details-rfc9457`) | `IngestionFailedException`: hata başına sınıf kuralına aykırı |
| 13 | **Yanıtta `failedStage` alanı** (lab'da yok): `FAILED` işin hangi aşamada durduğu | S2 "EMBEDDING aşamasında `FAILED`" ancak böyle doğrulanır. TASK-007 ekranında da gerekir | Sadece log: API'den görülemez |
| 14 | **Ortam değişkenleri Compose ile aynı adda:** `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS` (TASK-003 Compose ve `ollama.md` §8). Yeni: `INGESTION_BOOTSTRAP_FILE`, `INGESTION_BOOTSTRAP_ENABLED`. backend-coder `retrieval-api/CLAUDE.md`'deki `EMBEDDING_BASE_URL`'ları düzeltir | Compose zaten bu adları veriyor | `EMBEDDING_BASE_URL`: Compose'u değiştirmek gerekir |
| 15 | **Türkçe karakterli property değerleri** `application.properties`'te `\uXXXX` kaçışıyla yazılır (kaynak adı, başlık/altbilgi kalıpları) | Properties dosyaları ISO-8859-1 okunabilir. T31 kaynak adını literal olarak karşılaştırır | Adı kodda sabit tutmak: yapılandırma olmaktan çıkar |
| 16 | **Embedding girdisi** `heading + "\n\n" + content`. **Token sayısı** `ceil(girdi uzunluğu / ingestion.chars-per-token)`. `chunks.embedding_model` = `bge-m3@1024` | Lab ve `indexing.md` §3 | Model tokenizer'ı: Ollama embed ucu token sayısı dönmüyor |
| 17 | **`chunk_no` 0'dan başlar**, belge boyunca kesintisiz artar. **Bölüm sayısı** (`chapter_count`) noktasız numaralı yer imi sayısıdır: 2026.10'da 11 | Lab'da `chunk_no` 0'dan. Rehberde "bölüm" 11 ana bölümdür (TASK-004 kartı) | Lab'daki "sayfa = bölüm" (31/31): bizim parser sayfaya göre değil başlığa göre ayırıyor |
| 18 | **Testte bootstrap kapalı:** tester `src/test/resources/config/application.properties` açar: `ingestion.bootstrap.enabled=false`, `embedding.base-url=http://127.0.0.1:9`, `embedding.retry-backoff=0ms`. Bu dosya ana dosyanın üstüne yazar, onu gölgelemez | Testte gerçek modele gidilmez. Mevcut `RetrievalApplicationIntegrationTest` de açılışta Ollama'ya gitmemeli | `src/test/resources/application.properties`: ana dosyayı tamamen gölgeler, datasource ve Flyway ayarları kaybolur |
| 19 | **springdoc bu task'ta eklenmez.** Tek GET ucu var. Sözleşme uyumunu qa elle kontrol eder (yol, alanlar, status, errorCode) | Bir dependency daha; kart sorusunu büyütür | Eklemek: ilk çok uçlu task'ta (TASK-007) değerlendirilir |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu: TASK-005 durumu. İş başlatan REST ucunun olmadığı, rehberin açılışta girdiği yazıldı.
- `tags`: `Ingestion jobs`.
- `paths./api/workspaces/{workspaceId}/ingestion-jobs/{jobId}.get` (`operationId: getIngestionJob`): 200 `IngestionJobResponse`; 400 `BAD_REQUEST`; 404 `WORKSPACE_NOT_FOUND` / `INGESTION_JOB_NOT_FOUND`; 500.
- `components.parameters.WorkspaceId`.
- `components.schemas`: `IngestionStage`, `IngestionErrorCode`, `IngestionJobResponse`. Alanlar: `jobId, workspaceId, sourceId, version, fileName, stage, failedStage, progress, embeddedChunks, totalChunks, errorCode, createdAt, startedAt, finishedAt, statusUrl`. Hepsi `required`, null olabilenler `'null'` tipiyle.
- `ProblemDetail.errorCode` enum'una `WORKSPACE_NOT_FOUND`, `INGESTION_JOB_NOT_FOUND` eklendi.
- `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline/references/indexing.md`, `postgres-patterns`, `flyway-migrations`, `jpa-patterns`, `spring-transactions`, `java-concurrency`, `springboot-patterns`, `spring-boot-engineer`.

### Dependency (`pom.xml`)
- `org.apache.pdfbox:pdfbox:${pdfbox.version}`, `pdfbox.version=3.0.6` (kart sorusu 3).
- `org.hibernate.orm:hibernate-vector` (sürümü Boot yönetir; kart sorusu 3).
- Başka dependency yok. Awaitility test için `spring-boot-starter-test` içinde geliyor.

### Migration'lar (`src/main/resources/db/migration`)
- **`V2__create_workspaces_table.sql`:** `workspaces(id bigint PK, name text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 80), embedding_model text NOT NULL, embedding_dimensions int NOT NULL CHECK (embedding_dimensions > 0), created_at timestamptz NOT NULL DEFAULT now())`. `INSERT (42, 'Raşit Bankası · Dijital Asistan', 'bge-m3', 1024)`.
- **`V3__create_sources_and_ingestion_tables.sql`:**
  - `CREATE SEQUENCE sources_id_seq START WITH 10001 INCREMENT BY 1`.
  - `sources(id bigint PK DEFAULT nextval('sources_id_seq'), workspace_id bigint NOT NULL FK, name text NOT NULL CHECK 2-80, kind text NOT NULL CHECK (kind IN ('PDF')), active_version_id bigint NULL, created_at timestamptz NOT NULL)`, `UNIQUE (workspace_id, name)`.
  - `ingestion_jobs(id bigint GENERATED ALWAYS AS IDENTITY PK, workspace_id bigint NOT NULL FK, source_id bigint NOT NULL FK, version text NOT NULL CHECK 1-40, file_name text NOT NULL CHECK 1-255, file_size_bytes bigint NOT NULL CHECK (> 0), stage text NOT NULL CHECK (7 değer), failed_stage text NULL CHECK (QUEUED..WRITING), progress double precision NOT NULL CHECK (0..1), embedded_chunks int NOT NULL DEFAULT 0, total_chunks int NULL, error_code text NULL CHECK (IngestionErrorCode'daki 7 değer), created_at, started_at, finished_at timestamptz)`.
  - CHECK: `(stage = 'FAILED') = (error_code IS NOT NULL AND failed_stage IS NOT NULL)`.
  - Partial unique index: `ingestion_jobs_one_active_per_source ON ingestion_jobs (source_id) WHERE stage NOT IN ('DONE','FAILED')`.
  - `ingestion_job_files(ingestion_job_id bigint PK FK → ingestion_jobs, content bytea NOT NULL)`.
  - `source_versions(id bigint IDENTITY PK, source_id bigint NOT NULL FK, ingestion_job_id bigint NOT NULL UNIQUE FK, version text NOT NULL, file_name text NOT NULL, file_size_bytes bigint NOT NULL, page_count int NOT NULL CHECK (> 0), chapter_count int NOT NULL CHECK (> 0), chunk_count int NOT NULL CHECK (> 0), synced_at timestamptz NOT NULL, created_at timestamptz NOT NULL)`.
  - `ALTER TABLE sources ADD FOREIGN KEY (active_version_id) REFERENCES source_versions(id)`.
- **`V4__create_chunks_table.sql`:**
  - `CREATE SEQUENCE chunks_id_seq START WITH 1 INCREMENT BY 50`.
  - `chunks(id bigint PK, workspace_id bigint NOT NULL FK, source_id bigint NOT NULL FK, source_version_id bigint NOT NULL FK, chunk_no int NOT NULL CHECK (>= 0), page_number int NOT NULL CHECK (>= 1), heading text NOT NULL CHECK 1-200, content text NOT NULL CHECK (char_length BETWEEN 1 AND 2000), token_count int NOT NULL CHECK (>= 1), embedding vector(1024) NOT NULL, embedding_model text NOT NULL, created_at timestamptz NOT NULL)`, `UNIQUE (source_version_id, chunk_no)`.
  - `CREATE INDEX chunks_embedding_hnsw_idx ON chunks USING hnsw (embedding vector_cosine_ops)`, `CREATE INDEX chunks_source_id_idx ON chunks (source_id)`.

### Properties (`application.properties`'e eklenir)
```
spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true
embedding.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
embedding.model=${EMBEDDING_MODEL:bge-m3}
embedding.dimensions=${EMBEDDING_DIMENSIONS:1024}
embedding.connect-timeout=2s
embedding.read-timeout=60s
embedding.max-attempts=3
embedding.retry-backoff=1s
embedding.max-concurrent-requests=2
ingestion.target-chunk-chars=1600
ingestion.max-chunk-chars=2000
ingestion.overlap-chars=200
ingestion.chars-per-token=4
ingestion.embedding-batch-size=96
ingestion.page-header-pattern=^Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm .+$
ingestion.page-footer-pattern=^Yürürlük: .+ · Sayfa \\d+ / \\d+$
ingestion.bootstrap.enabled=${INGESTION_BOOTSTRAP_ENABLED:true}
ingestion.bootstrap.file=${INGESTION_BOOTSTRAP_FILE:../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf}
ingestion.bootstrap.source-name=Raşit Bankası Ürün ve Hizmet Rehberi
ingestion.bootstrap.version=2026.10
```
Varsayılan dosya yolu, `retrieval-api/` içinden `./mvnw spring-boot:run` ile çalışmaya göredir. Docker'da `INGESTION_BOOTSTRAP_FILE` verilir.

### Sınıflar (kök `com.rasitbank.retrieval`; her yeni pakette `package-info.java` `@NullMarked`)
**exception/**
- `BaseException` (abstract; `getErrorCode()`, `getStatus()`), `NotFoundException` (404), `ConflictException` (409), `ServiceUnavailableException` (503), `UnprocessableContentException` (422, `HttpStatus.UNPROCESSABLE_CONTENT`). Constructor `(String errorCode, String message)`.
- `GlobalExceptionHandler`: `@ExceptionHandler(BaseException.class)`, `type` = `typeUri(errorCode)`, `title` = status phrase, `errorCode` property, `instance` = path.

**config/**
- `ClockConfig` (`Clock.systemUTC()`).
- `EmbeddingProperties` (`@ConfigurationProperties("embedding")`, record: `baseUrl, model, dimensions, connectTimeout (Duration), readTimeout, maxAttempts, retryBackoff, maxConcurrentRequests`; `String modelLabel()` → `model + "@" + dimensions`).
- `IngestionProperties` (`"ingestion"`, record: `targetChunkChars, maxChunkChars, overlapChars, charsPerToken, embeddingBatchSize, pageHeaderPattern, pageFooterPattern, Bootstrap bootstrap`). `Bootstrap` record: `enabled, file (Path), sourceName, version`.
- `IngestionExecutorConfig` (`ExecutorService ingestionExecutor`, `destroyMethod = "shutdownNow"`).
- `EmbeddingClientConfig` (`RestClient embeddingRestClient(EmbeddingProperties)`).

**workspace/**
- `domain/entity/Workspace`: `id, name, embeddingModel, embeddingDimensions, createdAt`; `String embeddingModelLabel()`.
- `repository/WorkspaceRepository`.
- `service/WorkspaceService`: `Workspace getWorkspace(long workspaceId)` → yoksa `NotFoundException("WORKSPACE_NOT_FOUND", …)`; `void ensureWorkspaceExists(long workspaceId)`.

**source/**
- `domain/entity/SourceKind` (enum: `PDF`).
- `domain/entity/Source`: `@SequenceGenerator(sequenceName = "sources_id_seq", allocationSize = 1)`; alanlar `workspaceId, name, kind, activeVersionId (@Nullable Long), createdAt`; `static Source create(long workspaceId, String name, SourceKind kind, Instant now)`; `void activate(long sourceVersionId)`; `boolean hasActiveVersion()`.
- `domain/entity/SourceVersion`: `static SourceVersion of(long sourceId, long ingestionJobId, String version, String fileName, long fileSizeBytes, int pageCount, int chapterCount, int chunkCount, Instant syncedAt)`.
- `repository/SourceRepository` (`Optional<Source> findByWorkspaceIdAndName(long, String)`), `repository/SourceVersionRepository`.

**ingestion/**
- `domain/entity/IngestionStage` (enum, `boolean isActive()`).
- `domain/entity/IngestionJob`:
  - Fabrika: `static IngestionJob queued(long workspaceId, long sourceId, String version, String fileName, long fileSizeBytes, Instant now)`.
  - Domain metotları: `start(Instant)` → PARSING 0.05; `moveTo(IngestionStage)` → CHUNKING 0.1 / EMBEDDING 0.1 / WRITING 0.9, başka aşama `IllegalArgumentException`; `recordTotalChunks(int)`; `recordEmbeddingProgress(int embedded)` → `0.1 + 0.8 × embedded / total`; `complete(Instant)` → DONE 1.0; `fail(String errorCode, Instant)` → `failedStage` = mevcut aşama, `stage` = FAILED, progress değişmez.
  - Bitmiş işte her domain metodu `IllegalStateException`. İlerleme sabitleri entity'de.
- `domain/entity/IngestionJobFile` (`ingestionJobId` PK, `byte[] content`).
- `domain/dto/IngestionJobResponse` (openapi'deki 15 alan; `static from(IngestionJob)`; `statusUrl = "/api/workspaces/" + workspaceId + "/ingestion-jobs/" + jobId`).
- `repository/IngestionJobRepository`: `findByIdAndWorkspaceId`, `findByStageIn(Collection<IngestionStage>)`, `existsBySourceIdAndStageIn`.
- `repository/IngestionJobFileRepository`.
- `controller/IngestionJobController`: `@GetMapping("/api/workspaces/{workspaceId}/ingestion-jobs/{jobId}") IngestionJobResponse getIngestionJob(@PathVariable long workspaceId, @PathVariable long jobId)`.
- `service/IngestionService`:
  - `@Transactional(readOnly = true) IngestionJobResponse getIngestionJob(long workspaceId, long jobId)`: önce çalışma alanı kontrolü; iş yoksa `NotFoundException("INGESTION_JOB_NOT_FOUND", …)`.
  - `@Transactional IngestionJobResponse queueIngestion(long workspaceId, String sourceName, String version, String fileName, byte[] content)`: aynı adda kaynak yoksa `Source.create(workspaceId, sourceName, PDF, now)` ile oluşturur. Sonra iş (`QUEUED`) ve dosyayı yazar. Commit sonrası `IngestionJobRunner.submit(jobId, content)` çağrılır (`TransactionSynchronization.afterCommit`). Transaction bu metottadır; `GuideBootstrap` kendi içinde transaction açmaz (self-invocation proxy'yi atlar).
  - `@Transactional int failInterruptedJobs()`: aktif her iş `fail("INGESTION_INTERRUPTED", now)`.
- `service/GuideBootstrap`: `GuideBootstrapResult bootstrapGuide()`. Enum `GuideBootstrapResult`: `DISABLED, ALREADY_INGESTED, JOB_ACTIVE, FILE_MISSING, JOB_QUEUED`. Sıra:
  1. Kapalıysa → DISABLED.
  2. `findByWorkspaceIdAndName(42, sourceName)`. Kaynak var ve `hasActiveVersion()` → ALREADY_INGESTED. Kaynağın aktif işi var → JOB_ACTIVE.
  3. Dosyayı oku (`Files.readAllBytes`). Yoksa ya da okunamıyorsa `ERROR` log (yol ve hata sınıfı) → FILE_MISSING. Bu durumda hiçbir şey yazılmaz.
  4. `ingestionService.queueIngestion(42, sourceName, version, dosya adı, bayt)` → JOB_QUEUED.
  - Çalışma alanı kimliği 42 bir sabittir: `GuideBootstrap.WORKSPACE_ID`.
- `service/IngestionStartupListener` (`@Component`): `@EventListener(ApplicationReadyEvent.class) void onApplicationReady()` → `failInterruptedJobs()`, sonra `bootstrapGuide()` (Kararlar #11).
- `service/IngestionJobRunner` (`@Component`): `void submit(long jobId, byte[] content)` → `ingestionExecutor`; `void run(long jobId, byte[] content)` (public, testte senkron çağrılır). Adımlar:
  1. `lifecycle.start`.
  2. Model kontrolü: `workspace.embeddingModelLabel()` ≠ `embeddingProperties.modelLabel()` → `ConflictException("EMBEDDING_MODEL_MISMATCH")`.
  3. `parser.parse` → `moveTo(CHUNKING)` → `chunker.chunk` → `recordTotalChunks` → `moveTo(EMBEDDING)`.
  4. `embeddingBatchSize`'lık gruplar sırayla `embeddingClient.embed`; her gruptan sonra `recordEmbeddingProgress`.
  5. `moveTo(WRITING)` → `lifecycle.complete(...)`.
  - Hata yakalama Kararlar #12'deki gibi. Runner hiçbir transaction açmaz.
- `service/IngestionJobLifecycleService`: her metot kendi `@Transactional`'ında.
  - `start(long jobId)`, `moveTo(long jobId, IngestionStage)`, `recordTotalChunks(long jobId, int)`, `recordEmbeddingProgress(long jobId, int)`, `fail(long jobId, String errorCode)`.
  - `complete(long jobId, ParsedDocument document, List<ChunkDraft> drafts, List<float[]> embeddings)`, tek transaction: `SourceVersion` kaydı, `Chunk`'lar (batch), `source.activate(versionId)`, `job.complete(now)`. `syncedAt` = job'un `finishedAt`'iyle aynı `Instant`.
- `parsing/PdfDocumentParser` (`@Component`): `ParsedDocument parse(byte[] content)`. Açılamayan, şifreli ya da PDF olmayan dosya → `UnprocessableContentException("PDF_UNREADABLE")`. Bölüm bulunamazsa → `"PDF_NO_SECTIONS"`. Kurallar Kararlar #1, #2.
- `parsing/ParsedDocument(int pageCount, int chapterCount, List<ParsedSection> sections)`, `parsing/ParsedSection(String heading, List<PageLine> lines)`, `parsing/PageLine(int pageNumber, String text)`.
- `chunking/ChunkDraft(int chunkNo, int pageNumber, String heading, String content, int tokenCount)`, `String embeddingInput()`.
- `chunking/TextChunker` (`@Component`), `List<ChunkDraft> chunk(List<ParsedSection> sections)`:
  1. Her kısım ayrı parçalanır. Parça kısım sınırını geçmez. Kısımlar arası örtüşme yoktur.
  2. **Bloklar:** kısmın satırları sırayla bloklara toplanır. Bir blok, `.`, `!`, `?` ya da `:` ile biten satırdan sonra ya da `•` ile başlayan satırdan önce kapanır.
  3. **Uzun blok:** `max-chunk-chars`'ı aşan blok, satır sınırlarında, her parça `target-chunk-chars`'ı aşmayacak şekilde bölünür. Tek başına `max`'ı aşan satır, `max`'tan önceki son boşlukta bölünür.
  4. **Parça kurma:** bloklar parçaya eklenir. Satırlar `\n` ile birleşir, uzunluk ayırıcılarla sayılır. Bir blok eklenince toplam (örtüşme dahil) hedefi aşacaksa parça kapanır.
  5. **Örtüşme:** yeni parça, öncekinin son tam satırlarıyla (toplam ≤ `overlap-chars`) başlar. Örtüşme + blok `max`'ı aşarsa örtüşme satırları baştan atılır.
  6. Tek başına hedefi aşan blok kendi parçası olur (≤ `max`).
  7. `pageNumber` Kararlar #4'teki gibidir. `chunkNo` 0'dan, belge boyunca artar. `tokenCount` Kararlar #16'daki gibidir.

**chunk/**
- `domain/entity/Chunk`: `@SequenceGenerator(sequenceName = "chunks_id_seq", allocationSize = 50)`; `@JdbcTypeCode(SqlTypes.VECTOR) @Array(length = 1024) float[] embedding`; `static Chunk of(long workspaceId, long sourceId, long sourceVersionId, ChunkDraft draft, float[] embedding, String embeddingModel, Instant now)`.
- `repository/ChunkRepository`.

**embedding/**
- `client/EmbeddingClient`:
  - Constructor `EmbeddingClient(RestClient embeddingRestClient, EmbeddingProperties properties)`.
  - `List<float[]> embed(List<String> inputs)`: `POST /api/embed` gövdesi `{"model": <embedding.model>, "input": [..]}`. Dönen vektörler giriş sırasıyla gelir.
  - Vektör sayısı girdiden farklıysa → `ServiceUnavailableException("EMBEDDING_UNAVAILABLE")`. Bir vektörün boyutu `dimensions`'tan farklıysa → `ServiceUnavailableException("EMBEDDING_DIMENSION_MISMATCH")`. Retry ve sınır Kararlar #9.
  - `String modelLabel()`.
- `client/OllamaEmbedRequest(String model, List<String> input)`, `client/OllamaEmbedResponse(List<float[]> embeddings)`.

### Loglar (İngilizce, `key=value`; parça metni, başlık metni ve PDF içeriği loglanmaz)
- `INFO Ingestion job queued jobId={} sourceId={} version={} fileSizeBytes={}`
- `INFO Ingestion job started jobId={} workspaceId={} sourceId={}`
- `INFO Ingestion stage changed jobId={} stage={}`
- `INFO Document parsed jobId={} pageCount={} chapterCount={} sectionCount={}`
- `INFO Document chunked jobId={} chunkCount={} avgTokens={}`
- `INFO Embedding batch done jobId={} batch={}/{} chunkCount={} durationMs={}`
- `INFO Ingestion job finished jobId={} sourceId={} sourceVersionId={} chunkCount={} durationMs={}`
- `WARN Ingestion job failed jobId={} failedStage={} errorCode={} durationMs={}` (`INTERNAL_ERROR`'da `ERROR` + exception)
- `WARN Embedding request failed attempt={}/{} status={}`
- `INFO Interrupted ingestion jobs marked failed count={}`
- `INFO Guide bootstrap result={} sourceId={} jobId={}`
- `ERROR Guide bootstrap file not readable path={} error={}` (error = exception sınıf adı)

### `retrieval-api/CLAUDE.md` (backend-coder)
- "Teknoloji"ye PDFBox ve `hibernate-vector`.
- Gerçek property'ler.
- `EMBEDDING_BASE_URL` → `OLLAMA_BASE_URL`.
- Paket yapısındaki gerçek sınıflar.
- Testte bootstrap'ın kapalı olduğu (Kararlar #18).
- "Henüz olmayanlar" listesinden eklenenler çıkar.

## Frontend planı
Yok (frontend-coder atlandı).

## Test senaryoları
Fixture'lar: `ChunkFixtures`, `IngestionJobFixtures`. Sahte embedding: `@MockitoBean EmbeddingClient`. Girdiden deterministik 1024 boyutlu vektör döner: `v[i] = (hash(text) + i) % 7 / 7f`. PDF fixture'ları testte PDFBox ile, yer imleriyle üretilir. Gerçek rehber: `../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` (surefire çalışma dizini `retrieval-api/`).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Uzun kısım | unit `TextChunkerTest` | `givenSectionOf40SentenceLines_whenChunk_thenEveryChunkAtMost2000AndMultiBlockChunksAtMost1600` | 40 × 100 karakterlik, noktayla biten satır (4.039 karakter). Her parça ≤ 2.000. Birden fazla blok içeren her parça ≤ 1.600. Parça sayısı ≥ 3 |
| T2 | Örtüşme | unit | `givenConsecutiveChunksInOneSection_whenChunk_thenNextChunkStartsWithLastLinesOfPreviousUpTo200Chars` | T1 fixture'ında parça n+1'in ilk satırları parça n'in son satırlarıdır. Toplamları ≤ 200 ve > 0 (100 karakterlik satırla 1 satır) |
| T3 | Tablo satırı bölünmez | unit | `givenTableRowsWithoutSentenceEnd_whenChunk_thenEveryRowAppearsWholeInAChunk` | 30 satırlık, noktasız `Raşit Gold 1.450 TL Yıllık 150.000 TL harcama Harcamanın binde 5'i` benzeri satırlar. Her satır en az bir parçada tam satır olarak var. Hiçbir parça satır ortasında başlamıyor ya da bitmiyor |
| T4 | Kısım sınırı | unit | `givenTwoSections_whenChunk_thenNoChunkSpansSectionsAndEachCarriesItsHeading` | `2.3 Ek kart` ve `2.4 Yıllık üyelik ücreti, muafiyet ve iade`. Hiçbir parça iki kısmın satırını birlikte içermiyor. İkinci kısmın ilk parçası örtüşmesiz başlıyor |
| T5 | Sayfa numarası | unit | `givenSectionCrossingFromPage7To8_whenChunk_thenPageNumberIsPageOfFirstNewLine` | İlk parça s. 7. Yeni satırı s. 8'de başlayan parça 8 (örtüşme satırları s. 7'den gelse de) |
| T6 | Token sayısı | unit | `givenHeadingAndContent_whenChunk_thenTokenCountIsCeilOfEmbeddingInputOver4` | Başlık `2.3 Ek kart` (11) + `\n\n` (2) + 85 karakterlik içerik = 98 → 25 |
| T7 | Çok uzun satır | unit | `givenLineOf4500CharsWithSpaces_whenChunk_thenSplitAtWhitespaceIntoPiecesAtMost2000` | 3 parça, her biri ≤ 2.000, birleşimi boşluklar dışında orijinal satır |
| T8 | Sıra no | unit | `givenThreeSections_whenChunk_thenChunkNumbersRunFrom0WithoutGaps` | `chunkNo` = 0, 1, …, n-1 |
| T9 | Üst/alt bilgi | unit `PdfDocumentParserTest` | `givenPagesWithHeaderAndFooter_whenParse_thenHeaderAndFooterLinesAreDropped` | Üretilmiş PDF'te `Raşit Bankası Ürün ve Hizmet Rehberi · Sürüm 2026.10` ve `Yürürlük: 1 Ekim 2026 · Sayfa 2 / 3` hiçbir `PageLine`'da yok. Gövde satırı korunuyor |
| T10 | Kapak ve içindekiler | unit | `givenCoverAndTocBeforeFirstChapterBookmark_whenParse_thenThosePagesAreSkipped` | Kapak (s. 1) ve `İçindekiler` (s. 2) satırları yok. İlk kısım s. 3'te |
| T11 | Yanlış başlık yok | unit | `givenBodyLinesStartingWithNumbers_whenParse_thenOnlyBookmarkedLinesBecomeHeadings` | `1 Kasım 2026 tarihinden itibaren …` ve `1. Kartınızı …` gövdede kalır. Kısım başlıkları sadece yer imli `5.1 Havale`, `5.2 EFT` |
| T12 | SSS ve bölüm başı | unit | `givenFaqLineAndChapterWithoutSubsections_whenParse_thenHeadingsAreChapterDotFaqAndChapterTitle` | `2 Kartlar · Sık sorulanlar` ve `11 Terimler sözlüğü` kısımları var. `chapterCount` = yer imli bölüm sayısı |
| T13 | PDF değil | unit | `givenBytesThatAreNotAPdf_whenParse_thenThrowsPdfUnreadable` | `UnprocessableContentException`, `errorCode` `PDF_UNREADABLE` |
| T14 | Yer imi yok | unit | `givenPdfWithoutBookmarks_whenParse_thenThrowsPdfNoSections` | `UnprocessableContentException`, `PDF_NO_SECTIONS` |
| T15 | Gerçek rehber | unit `PdfDocumentParserGuideTest` | `givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection` | `pageCount` 70, `chapterCount` 11. İçindekiler'deki 99 alt başlığın her biri bir kısım başlığı. 8 `· Sık sorulanlar` kısmı. `2.16 Kayıp ya da çalıntı bildirimi`'nin ilk satırı s. 16'da, `4.10 Erken ödeme ve erken kapama tazminatı`'nın s. 35'te. Hiçbir satır `İçindekiler` ya da `Sayfa N / 70` içermiyor. `TextChunker` çıktısında her parça ≤ 2.000. 2.2'deki Raşit Gold tablo satırı tek satır olarak bir parçada var |
| T16 | İstek biçimi | unit `EmbeddingClientTest` (`MockRestServiceServer`) | `givenTwoInputs_whenEmbed_thenPostsModelAndInputsAndReturnsVectorsInOrder` | `POST /api/embed`, gövde `{"model":"bge-m3","input":["a","b"]}`. Dönen 2 vektör sırayla |
| T17 | 5xx sonra başarı | unit | `givenServerErrorThenSuccess_whenEmbed_thenRetriesOnceAndReturnsVectors` | 2 istek, sonuç vektörler |
| T18 | Hep 5xx | unit | `givenServerErrorOnEveryAttempt_whenEmbed_thenThrowsEmbeddingUnavailableAfterThreeAttempts` | Tam 3 istek. `ServiceUnavailableException`, `EMBEDDING_UNAVAILABLE` |
| T19 | Yanlış boyut | unit | `given512DimensionVector_whenEmbed_thenThrowsEmbeddingDimensionMismatchWithoutRetry` | 1 istek, `EMBEDDING_DIMENSION_MISMATCH` |
| T20 | Eksik vektör | unit | `givenOneVectorForTwoInputs_whenEmbed_thenThrowsEmbeddingUnavailable` | `EMBEDDING_UNAVAILABLE` |
| T21 | Bağlantı reddi | unit | `givenNothingListeningOnPort9_whenEmbed_thenThrowsEmbeddingUnavailable` | Gerçek `RestClient`, `http://127.0.0.1:9`, backoff 0 → `EMBEDDING_UNAVAILABLE` |
| T22 | Eşzamanlılık sınırı | unit | `givenFourConcurrentCalls_whenEmbed_thenAtMostTwoRequestsInFlight` | `max-concurrent-requests=2`. Latch'le bekleyen sahte istek fabrikasında aynı anda gözlenen en çok istek 2 |
| T23 | İlerleme | unit `IngestionJobTest` | `givenJobAdvancingThroughStages_whenProgressRead_thenMatchesStageValues` | PARSING 0.05, CHUNKING 0.1, EMBEDDING başı 0.1, 96/150 → 0.612, WRITING 0.9, DONE 1.0 (`isCloseTo`, `within(1e-6)`) |
| T24 | Bitmiş iş değişmez | unit | `givenDoneOrFailedJob_whenMoveTo_thenThrowsIllegalState` | DONE ve FAILED için `IllegalStateException` |
| T25 | Hata kaydı | unit | `givenJobInEmbedding_whenFail_thenFailedWithFailedStageEmbeddingAndProgressKept` | `stage` FAILED, `failedStage` EMBEDDING, `errorCode` verilen, `finishedAt` = sabit saat, progress önceki değer |
| T26 | İkinci grupta model düşer | unit `IngestionJobRunnerTest` | `givenEmbeddingUnavailableOnSecondBatch_whenRun_thenFailsWithEmbeddingUnavailableAndNeverCompletes` | 150 taslak. 1. grup başarılı, 2.'de `EMBEDDING_UNAVAILABLE`. `lifecycle.fail(jobId, "EMBEDDING_UNAVAILABLE")`. `complete` hiç çağrılmıyor. `recordEmbeddingProgress(jobId, 96)` çağrıldı |
| T27 | Model farkı | unit | `givenConfiguredModelDiffersFromWorkspace_whenRun_thenFailsWithEmbeddingModelMismatchBeforeParsing` | Yapılandırma `nomic-embed-text@768`, çalışma alanı `bge-m3@1024` → `fail(…, "EMBEDDING_MODEL_MISMATCH")`. Parser çağrılmıyor |
| T28 | Beklenmeyen hata | unit | `givenParserThrowsRuntimeException_whenRun_thenFailsWithInternalError` | `fail(…, "INTERNAL_ERROR")` |
| T29 | Gruplar | unit | `given150Chunks_whenRun_thenEmbedsInBatchesOf96And54InOrderAndRecordsProgress96Then150` | `embed` çağrı boyutları [96, 54]. İlk grubun girdileri `embeddingInput()` sırasıyla. İlerleme 96, 150. `complete` 150 vektörle |
| T30 | Çalışma alanı | integration `WorkspaceMigrationIntegrationTest` (Testcontainers) | `givenFreshDatabase_whenMigrated_thenWorkspace42WithBgeM3At1024Exists` | `workspaces`'ta tek satır: 42, `Raşit Bankası · Dijital Asistan`, `bge-m3`, 1024 |
| T31 | İlk giriş uçtan uca | integration `GuideBootstrapIntegrationTest` (sahte embedding, bootstrap testte elle çağrılır, iş Awaitility ile beklenir) | `givenFreshDatabase_whenBootstrapGuide_thenJobDoneSource10001Version202610AndChunksWith1024Dims` | Sonuç `JOB_QUEUED`, iş DONE. `sources`: 1 satır, id 10001, ad `Raşit Bankası Ürün ve Hizmet Rehberi`. `source_versions`: `version` `2026.10`, `page_count` 70, `chapter_count` 11, `file_size_bytes` = dosya boyu, `chunk_count` = `chunks` satır sayısı, `synced_at` = işin `finished_at`. `ingestion_job_files.content` dosyayla bayt bayt aynı. `sources.active_version_id` bu sürüm. Her parçada `vector_dims(embedding)` 1024, `embedding_model` `bge-m3@1024`, `page_number` 5-70 arası, başlık ve metin dolu |
| T32 | Yeniden açılış | integration | `givenGuideAlreadyIngested_whenBootstrapAgain_thenAlreadyIngestedAndNoNewRows` | Sonuç `ALREADY_INGESTED`. `sources` 1, `ingestion_jobs` 1, `chunks` sayısı değişmedi |
| T33 | Model kapalı | integration | `givenEmbeddingUnavailable_whenBootstrapGuide_thenJobFailedAtEmbeddingAndNothingWritten` | Sahte istemci `EMBEDDING_UNAVAILABLE` fırlatır. İş FAILED, `failedStage` EMBEDDING, `errorCode` `EMBEDDING_UNAVAILABLE`. `chunks` 0, `source_versions` 0, `active_version_id` null. `sources` 1 (10001) |
| T34 | Başarısızlık sonrası açılış | integration | `givenPreviousBootstrapFailed_whenBootstrapAgain_thenSameSourceNewJobDone` | `sources` 1 (10001). `ingestion_jobs` 2: ilki FAILED, ikincisi DONE. `chunks` sadece ikinci işin sürümüne ait |
| T35 | Yarıda kalan iş | integration | `givenJobsInEmbeddingAndDone_whenFailInterruptedJobs_thenActiveOneFailedInterruptedAndDoneUntouched` | Dönen sayı 1. EMBEDDING'deki iş FAILED, `failedStage` EMBEDDING, `INGESTION_INTERRUPTED`. DONE iş aynı |
| T36 | Dosya yok | integration | `givenBootstrapFileMissing_whenBootstrapGuide_thenFileMissingAndNothingWritten` | Sonuç `FILE_MISSING`. `sources`, `ingestion_jobs` 0 |
| T37 | Tek aktif iş | integration (`@DataJpaTest` + Testcontainers) | `givenActiveJobForSource_whenSecondActiveJobSaved_thenUniqueIndexRejects` | `DataIntegrityViolationException`. DONE iş varken yeni QUEUED kabul edilir |
| T38 | Index | integration | `givenMigratedDatabase_whenIndexesRead_thenChunksEmbeddingHasHnswCosineIndex` | `pg_indexes.indexdef` `USING hnsw (embedding vector_cosine_ops)` içeriyor |
| T39 | Log sınırı | integration (`OutputCaptureExtension`) | `givenGuideIngested_whenLogsCaptured_thenStageAndCountsLoggedButNoChunkText` | Log `Document chunked jobId=` ve `Ingestion job finished jobId=` içeriyor. `Raşit Banka Kartı, vadesiz Türk lirası hesabınıza bağlı` ve `2.16 Kayıp ya da çalıntı bildirimi` içermiyor |
| T40 | GET başarılı | controller `IngestionJobControllerTest` (`@WebMvcTest`, `MockMvcTester`) | `givenRunningJob_whenGetIngestionJob_thenReturns200WithAllFields` | 200. `jobId` 1, `workspaceId` 42, `sourceId` 10001, `version` `2026.10`, `stage` `EMBEDDING`, `failedStage` null, `progress` 0.612, `embeddedChunks` 96, `totalChunks` 150, `errorCode` null, `statusUrl` `/api/workspaces/42/ingestion-jobs/1`. 15 alanın hepsi gövdede |
| T41 | GET başarısız iş | controller | `givenFailedJob_whenGetIngestionJob_thenReturnsErrorCodeAndFailedStage` | `stage` FAILED, `failedStage` EMBEDDING, `errorCode` `EMBEDDING_UNAVAILABLE`, `finishedAt` dolu |
| T42 | İş yok | controller | `givenUnknownJobId_whenGetIngestionJob_thenReturns404IngestionJobNotFound` | 404, `application/problem+json`, `errorCode` `INGESTION_JOB_NOT_FOUND` |
| T43 | Çalışma alanı yok | controller | `givenWorkspace43_whenGetIngestionJob_thenReturns404WorkspaceNotFound` | 404, `WORKSPACE_NOT_FOUND` |
| T44 | Sayı olmayan kimlik | controller | `givenNonNumericJobId_whenGetIngestionJob_thenReturns400BadRequest` | 400, `BAD_REQUEST` |
| T45 | Açılış sırası | unit `IngestionStartupListenerTest` | `givenApplicationReady_whenOnApplicationReady_thenFailsInterruptedJobsBeforeBootstrapping` | `InOrder`: `failInterruptedJobs` sonra `bootstrapGuide` |

**Elle çalıştırılanlar (gerçek PDF + gerçek `bge-m3`):**
- **M0 (backend-coder, lokal):** `docker compose -f deployment/docker-compose.dev.yml up -d`, Ollama açık, `DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run`. İş DONE. Raporda: iş süresi (`finished_at - started_at`), `chunk_count`, `avg(token_count)`, min/maks `char_length(content)`, `count(*) where vector_dims(embedding) <> 1024` = 0.
- **M1 (devops, Docker):** `down -v` + `up --build -d`. `curl localhost:8080/api/workspaces/42/ingestion-jobs/1` → DONE. M0'daki sayılar raporda. `select id from sources` → `10001`.
- **M2 (devops):** `docker compose … restart retrieval-api` ya da `down` + `up` (volume korunur). `sources` 1, `ingestion_jobs` 1, `chunks` sayısı aynı. Logda `Guide bootstrap result=ALREADY_INGESTED`.
- **M3 (devops):** `brew services stop ollama`, `down -v`, `up`. İş 1 FAILED, `failedStage` EMBEDDING, `EMBEDDING_UNAVAILABLE`, `chunks` 0, servis `healthy`. Sonra `brew services start ollama` ve `restart retrieval-api`: iş 2 DONE, `sources` hâlâ 1 (10001).
- **M4 (qa):** `select chunk_no, page_number, heading, left(content, 80) from chunks order by random() limit 5`. Her birinin başlığı ve sayfası PDF görüntüleyicide elle karşılaştırılır. Örtüşmeyle başlayan parçada ilk yeni satırın sayfası esas alınır. Bir tablo içeren parçada satırların tam olduğu görülür.
- **M5 (qa):** `docker compose … logs retrieval-api` içinde parça metni, başlık metni geçmiyor (ör. `Kayıp ya da çalıntı` aranır: 0).

## Deployment planı
- **`deployment/docker-compose.yml`, `retrieval-api`:**
  - `volumes: - ../docs/rehber/pdf:/app/rehber:ro`
  - `environment: INGESTION_BOOTSTRAP_FILE: /app/rehber/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf`
  - Mevcut `OLLAMA_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS` aynı kalır.
  - Dockerfile değişmez. `app` kullanıcısı salt okunur dosyayı okuyabilmeli (host dosyası 644); devops bunu doğrular.
- **`docker-compose.dev.yml`:** değişmez. Servis bilgisayarda çalışır, varsayılan göreli yol kullanılır.
- **Lokal model:** yeni model yok. `bge-m3` TASK-003'te indirildi.
- **Healthcheck:** değişmez. Model kapalıyken de `healthy` (M3).
- **`deployment/CLAUDE.md`, `ollama.md` §8, kök `README.md`:** ilk açılışta rehberin kendiliğinden içe alındığı, Ollama'nın Compose'dan önce açılması gerektiği, kapalıysa yeniden başlatma adımı. Durum sorgusu: `curl localhost:8080/api/workspaces/42/ingestion-jobs/1`.

## Açık sorular ve riskler
1. **Kart soruları 1-3:** Plan önerilen seçeneklere göre yazıldı. Başka cevapta değişecekler Özet'te.
2. **Yer imine bağımlılık (risk):** Rehber üretimi yer imlerini kaybederse içe alma `PDF_NO_SECTIONS` ile durur. D6 bunu bugün koruyor. TASK-015'teki 2026.11 aynı üretimden çıkıyor.
3. **Üst/alt bilgi kalıbı rehbere özgü (risk):** Başka biçimde bir PDF'te üst/alt bilgi atılmaz ve parçalara karışır. Yalnızca bu rehber içe alındığı için kabul edildi.
4. **Kapsam dışı not:** `rag.md`, `.claude/skills/rag-pipeline/references/indexing.md` ve skill'ler hâlâ lab'daki "31 sayfa, 61 parça" ve `EMBEDDING_BASE_URL` kalıbını anıyor. `.claude/**` ve `rag.md` kullanıcının. TASK-004'ün "doküman güncellemesi" risk task'ına eklenebilir.
5. **Kapsam dışı not:** `IngestionJobResponse.totalChunks` lab'daki adla aynı kaldı. TASK'taki "parça sayısı" bu alandır. Kaynak/sürüm okuma uçları TASK-007'de.

STATUS: DONE
