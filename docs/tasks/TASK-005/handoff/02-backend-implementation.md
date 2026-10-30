TASK: TASK-005
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline` (+ `references/indexing.md`), `java-concurrency`, `spring-transactions`. Planın yazdığı `postgres-patterns`, `flyway-migrations`, `jpa-patterns`, `spring-boot-engineer` kuralları plandaki SQL ve sınıf listesine işlenmiş hâliyle uygulandı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Dependency | `pom.xml` | `org.hibernate.orm:hibernate-vector` (Boot yönetir: 7.4.5.Final), `org.apache.pdfbox:pdfbox:${pdfbox.version}` = 3.0.6. Başka dependency yok |
| K1 | `V2__create_workspaces_table.sql`, `workspace/**` | 42, "Raşit Bankası · Dijital Asistan", `bge-m3`, 1024. `Workspace.embeddingModelLabel()`, `WorkspaceService.getWorkspace` / `ensureWorkspaceExists` → `WORKSPACE_NOT_FOUND` |
| K2 | `IngestionJob`, `IngestionStage`, `IngestionJobLifecycleService`, `IngestionJobController`, `IngestionJobResponse` | İlerleme sabitleri entity'de (QUEUED 0, PARSING 0.05, CHUNKING 0.1, EMBEDDING 0.1 + 0.8 × e/t, WRITING 0.9, DONE 1.0). `fail` → `failedStage` = mevcut aşama, progress korunur. Bitmiş işte her domain metodu `IllegalStateException`. Yanıt 15 alan, null'lar JSON'da `null` olarak var |
| K3 | `IngestionService.queueIngestion` (commit sonrası `TransactionSynchronization.afterCommit` → `IngestionJobRunner.submit`), `IngestionService.failInterruptedJobs`, `IngestionStartupListener`, `IngestionExecutorConfig` | Kart kararı 2 = A: aktif işler `FAILED` + `INGESTION_INTERRUPTED`. Executor tek thread, ad `ingestion-1`, `destroyMethod = shutdownNow` |
| K4 | `ingestion/chunking/TextChunker`, `ChunkDraft`, `ingestion/parsing/PdfDocumentParser`, `V4` CHECK'leri | Plan algoritması (bloklar, uzun blok/satır bölme, örtüşme ≤ 200 tam satır, örtüşme + blok > max ise örtüşme baştan atılır, sayfa = ilk yeni satırın sayfası). Parçalayıcının değişken durumu çağrı başına iç sınıfta (`ChunkAssembler`); bean thread-safe |
| K5 | `IngestionJobRunner` (96'lık gruplar, sırayla), `EmbeddingClient` (Semaphore(2, fair) + `tryAcquire(connect+read timeout)`, 3 deneme, n × 1 sn bekleme, boyut/sayı kontrolü) | Bağlantı hatası/zaman aşımı/5xx yeniden denenir; 4xx, JSON hatası, sayı farkı (`EMBEDDING_UNAVAILABLE`) ve boyut farkı (`EMBEDDING_DIMENSION_MISMATCH`) denenmez |
| K6 | `V4__create_chunks_table.sql` | `CREATE INDEX chunks_embedding_hnsw_idx ON chunks USING hnsw (embedding vector_cosine_ops)` (DB'de doğrulandı) |
| K7 | `GuideBootstrap` → `IngestionService.queueIngestion` → `IngestionJobRunner` | Kart kararı 1 = A. Elle/seed parça yok |
| K8 | `V3` (`sources_id_seq START 10001`, `UNIQUE (workspace_id, name)`, partial unique `ingestion_jobs_one_active_per_source`), `V4` (`UNIQUE (source_version_id, chunk_no)`), `GuideBootstrap` (ada göre arar) | Yeniden açılışta `ALREADY_INGESTED` (M0'da denendi) |
| K9 | `ingestion_job_files.content` (bytea), `source_versions` | Dosya adı, bayt, sayfa, bölüm sayısı |
| K10 | `ingestion_jobs.version` NOT NULL, `source_versions.synced_at` = işin `finished_at` (aynı `Instant`) | M0'da eşit olduğu görüldü |
| K11 | Loglar (plandaki 12 satır) | Parça metni, başlık metni, PDF içeriği loglanmaz. M0 logunda `Kayıp ya da çalıntı` / `Raşit Banka Kartı, vadesiz` = 0 |
| Kararlar #1, #2 | `PdfDocumentParser` | Numaralı yer imleri (`^\d+(\.\d+)? .+`), satır = yer imi başlığı ve aynı sayfa (her yer imi bir kez eşleşir). "Sık sorulanlar" satırı → `<bölüm> · Sık sorulanlar`. İçerik ilk bölüm yer iminin sayfasından başlar. `setSortByPosition(false)`, `setLineSeparator("\n")`, satırlar `strip`, boşlar atılır, ilk satır üst bilgi / son satır alt bilgi kalıbına uyuyorsa atılır. Açılamayan ya da şifreli PDF → `PDF_UNREADABLE`; bölüm yer imi yok ya da altında satır yok → `PDF_NO_SECTIONS` |
| Kararlar #12 | `exception/BaseException`, `NotFoundException`, `ConflictException`, `ServiceUnavailableException`, `UnprocessableContentException` (`HttpStatus.UNPROCESSABLE_CONTENT`), `GlobalExceptionHandler.handleBaseException` | Runner: `BaseException` → `fail(errorCode)`, diğer `RuntimeException` → `ERROR` log + `fail("INTERNAL_ERROR")` |
| Kararlar #14, #15 | `application.properties` | Plandaki anahtarların hepsi. Türkçe değerler `\uXXXX` kaçışlı; dosya saf ASCII |
| Kararlar #16, #17 | `ChunkDraft.embeddingInput()` = `heading + "\n\n" + content`; token = `ceilDiv(len, 4)`; `chunkNo` 0'dan; `chapter_count` = noktasız yer imi sayısı | |
| Sözleşme | `GET /api/workspaces/{workspaceId}/ingestion-jobs/{jobId}` | 200 / 404 `WORKSPACE_NOT_FOUND` / 404 `INGESTION_JOB_NOT_FOUND` / 400 `BAD_REQUEST` canlı denendi (aşağıda) |
| `retrieval-api/CLAUDE.md` | | Teknoloji (dependency'ler, gerçek property'ler, migration'lar), `EMBEDDING_BASE_URL` → `OLLAMA_BASE_URL`, paket yapısındaki gerçek sınıflar, testte bootstrap'ın kapalı olduğu (Kararlar #18), "henüz olmayanlar" güncellendi, properties'in ASCII kuralı |

## Değişen dosyalar
**Değişen:** `retrieval-api/pom.xml`, `retrieval-api/CLAUDE.md`, `src/main/resources/application.properties`, `RetrievalApplication.java` (`@ConfigurationPropertiesScan`), `exception/GlobalExceptionHandler.java` (`BaseException` handler'ı).

**Yeni (hepsi `retrieval-api/src/main/` altında; her yeni pakette `package-info.java` `@NullMarked`):**
- `resources/db/migration/V2__create_workspaces_table.sql`, `V3__create_sources_and_ingestion_tables.sql`, `V4__create_chunks_table.sql`
- `exception/`: `BaseException`, `NotFoundException`, `ConflictException`, `ServiceUnavailableException`, `UnprocessableContentException`
- `config/`: `ClockConfig`, `EmbeddingProperties`, `IngestionProperties`, `IngestionExecutorConfig`, `EmbeddingClientConfig`, `PathConversionConfig`
- `workspace/`: `domain/entity/Workspace`, `repository/WorkspaceRepository`, `service/WorkspaceService`
- `source/`: `domain/entity/Source`, `SourceKind`, `SourceVersion`; `repository/SourceRepository`, `SourceVersionRepository`
- `chunk/`: `domain/entity/Chunk`, `repository/ChunkRepository`
- `embedding/client/`: `EmbeddingClient`, `OllamaEmbedRequest`, `OllamaEmbedResponse`
- `ingestion/`: `controller/IngestionJobController`; `domain/entity/IngestionJob`, `IngestionJobFile`, `IngestionStage`; `domain/dto/IngestionJobResponse`; `repository/IngestionJobRepository`, `IngestionJobFileRepository`; `service/IngestionService`, `IngestionJobLifecycleService`, `IngestionJobRunner`, `GuideBootstrap`, `GuideBootstrapResult`, `IngestionStartupListener`; `parsing/PdfDocumentParser`, `ParsedDocument`, `ParsedSection`, `PageLine`; `chunking/TextChunker`, `ChunkDraft`

**Silinen:** yok. **Eklenen dependency'ler:** `hibernate-vector`, `pdfbox` 3.0.6 (kart kararı 3 = A).

## Plandan sapmalar
Davranışı ya da sözleşmeyi değiştiren bir sapma yok. Uygulamada netleşen küçük ekler (lead'e rapor mesajıyla bildirildi):
1. **`config/PathConversionConfig` (yeni sınıf, plan listesinde yok):** `@ConfigurationPropertiesBinding` `Converter<String, Path>` (`Path::of`). Neden: Spring'in varsayılan `PathEditor`'ü `../docs/...` yolunu classpath kaynağı sanıyor ve servis açılmıyordu (`The resource path [/../docs/...] has been normalized to [null]`). Planın `Bootstrap.file (Path)` tipi korunmuş oldu.
2. **`IngestionJobLifecycleService.start` `IngestionJob` döner** (plan dönüş tipini yazmıyordu). Runner, model kontrolü için işin `workspaceId`'sini buradan alır.
3. **Aşama sırası sıkı:** `start` sadece `QUEUED`'dan, `moveTo(X)` sadece X'ten önceki aşamadan, `recordEmbeddingProgress` sadece `EMBEDDING`'de, `complete` sadece `WRITING`'de geçer; aksi `IllegalStateException`. `moveTo`'ya CHUNKING/EMBEDDING/WRITING dışı aşama `IllegalArgumentException` (önce "bitmiş iş" kontrolü yapılır, yani DONE/FAILED işte her zaman `IllegalStateException`).
4. **Loglar:** "Ingestion job finished/failed" lifecycle servisinde yazılır; `durationMs` = `finished_at - started_at`. `INTERNAL_ERROR`'da lifecycle `ERROR` seviyesinde yazar, exception'ın stack trace'i runner'daki `Ingestion job crashed jobId={}` satırındadır. Ek iki log: `Ingestion job not submitted, executor is shut down jobId={}`, `Ingestion job interrupted by shutdown jobId={}`.
5. **Kapanışta yarıda kalan iş:** thread kesilmişse (`shutdownNow`) runner işi `FAILED` yapmaz; iş aktif kalır ve sonraki açılışta `INGESTION_INTERRUPTED` olur (kart kararı 2'nin amacı).
6. **`GuideBootstrap`:** 0 baytlık dosya da `FILE_MISSING` sayılır (`ERROR ... error=EmptyFile`); aksi hâlde `file_size_bytes > 0` CHECK'i servis açılışında exception verirdi. `IngestionStartupListener` iki çağrıyı ayrı `try/catch`'le sarar; hata loglanır, servis açık kalır (G1, G2).
7. **Migration ekleri:** `ALTER SEQUENCE … OWNED BY` (iki sequence), constraint'lere açık ad. Ek index ya da CHECK yok.

## Doğrulama
**M0 — gerçek PDF + gerçek `bge-m3` (Ollama, lokal), `docker compose -f deployment/docker-compose.dev.yml up -d`, temiz `retrieval` veritabanı, `cd retrieval-api && DATABASE_PASSWORD=retrieval-local ./mvnw spring-boot:run`:**
- Log sırası: `Interrupted ingestion jobs marked failed count=0` → `Ingestion job queued jobId=1 sourceId=10001 version=2026.10 fileSizeBytes=1120236` → `Guide bootstrap result=JOB_QUEUED` → PARSING → `Document parsed pageCount=70 chapterCount=11 sectionCount=109` → CHUNKING → `Document chunked chunkCount=154 avgTokens=285` → EMBEDDING → `batch=1/2 chunkCount=96 durationMs=4454`, `batch=2/2 chunkCount=58 durationMs=2435` → WRITING → `Ingestion job finished jobId=1 sourceId=10001 sourceVersionId=1 chunkCount=154 durationMs=7236`.
- **İş süresi** (`finished_at - started_at`): **7.236 ms**. İş DONE, progress 1.0, embedded/total 154/154.
- **`chunk_count` = 154**, **`avg(token_count)` = 285,4** (min 60, maks 408).
- `char_length(content)`: **min 205, maks 1.599**, ortalama 1.108.
- `count(*) where vector_dims(embedding) <> 1024` = **0**. `embedding_model` tek değer: `bge-m3@1024`. `page_number` 5-70.
- Üst/alt bilgi sızıntısı (`Sayfa % / 70`, `Rehberi · Sürüm`): **0** parça.
- `sources`: `10001 | Raşit Bankası Ürün ve Hizmet Rehberi | active_version_id 1`. `source_versions`: `2026.10 | Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf | 1120236 | 70 sayfa | 11 bölüm | 154 parça | synced_at = finished_at`. `ingestion_job_files` 1.120.236 bayt.
- `workspaces`: `42 | Raşit Bankası · Dijital Asistan | bge-m3 | 1024`. HNSW ve partial unique index'ler `pg_indexes`'te.
- Rastgele 5 parçanın başlık/sayfası yer imleriyle uyumlu (ör. `3.2 Vadeli mevduat` s. 20, `2.13 Temassız ödeme` s. 15, `2.17 Harcama itirazı` s. 16, `8.4 ATM nakit çekme ve yatırma limitleri` s. 58, `3.10 Hesap kapatma` s. 25). PDF görüntüleyicide elle karşılaştırma qa'nındır (M4).
- Uç: `GET /api/workspaces/42/ingestion-jobs/1` → 200, 15 alan (`"stage":"DONE","failedStage":null,"progress":1.0,"embeddedChunks":154,"totalChunks":154,"errorCode":null,...,"statusUrl":"/api/workspaces/42/ingestion-jobs/1"`). `/42/ingestion-jobs/99` → 404 `INGESTION_JOB_NOT_FOUND`; `/43/ingestion-jobs/1` → 404 `WORKSPACE_NOT_FOUND`; `/42/ingestion-jobs/abc` → 400 `application/problem+json` `BAD_REQUEST`.
- **Yeniden açılış:** servis durdurulup yeniden başlatıldı → `Guide bootstrap result=ALREADY_INGESTED sourceId=10001`; `sources` 1, `ingestion_jobs` 1, `chunks` 154 (değişmedi).
- Dev veritabanında `retrieval` şu an bu içe alınmış hâlde duruyor; servis durduruldu, postgres konteyneri açık.

**Build:** `cd retrieval-api && INGESTION_BOOTSTRAP_ENABLED=false OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE: Tests run: 14, Failures: 1, Errors: 10.** Derleme temiz. Kırılan iki test sınıfı tester'ındır ve plan gereği eskimiştir (kodda hata yok; düzeltilmesi tester'ın işi, aşağıda):
- `RetrievalApplicationIntegrationTest.givenFreshDatabase_whenContextStarts_thenFlywayAppliedVersion1`: `applied` artık 4 (V1-V4); test 1 bekliyor. Aynı sınıfın diğer 3 testi geçti. Bu da tam context'in gerçek pgvector'e karşı açıldığını ve `ddl-auto=validate`'in (vector kolonu dahil) geçtiğini gösteriyor.
- `GlobalExceptionHandlerTest` (10 test, context açılmıyor): `@WebMvcTest` controller listesi vermediği için yeni `IngestionJobController`'ı da yüklüyor ve `IngestionService` bean'i bulunamıyor.
- Coverage eşiği kapalı çalıştırıldı (yeni kodun testleri yok); eşiği tester ve qa tam `./mvnw verify` ile doğrular.

## Tester için notlar
- **Önce iki eski testi güncelle:** `RetrievalApplicationIntegrationTest` (uygulanan migration sayısı 4, son sürüm `4`); `GlobalExceptionHandlerTest` → `@WebMvcTest(controllers = ErrorProbeController.class)` ya da `@MockitoBean IngestionService`.
- **Test yapılandırması (Kararlar #18):** `src/test/resources/config/application.properties` (`ingestion.bootstrap.enabled=false`, `embedding.base-url=http://127.0.0.1:9`, `embedding.retry-backoff=0ms`). Dosya yokken `RetrievalApplicationIntegrationTest` açılışta gerçek rehberi Ollama'ya göndermeye çalışır. **Dosya ASCII olmalı**; Türkçe değer gerekiyorsa `\uXXXX` (Spring properties'i ISO-8859-1 okur; bu task'ta UTF-8 yazılan kalıp sessizce eşleşmedi).
- **Seam'ler:** `IngestionJobRunner.run(jobId, content)` public ve senkron (executor'suz test). `EmbeddingClient` somut sınıf → `@MockitoBean EmbeddingClient`; ya da `new EmbeddingClient(RestClient, EmbeddingProperties)` + `MockRestServiceServer.bindTo(RestClient.builder().baseUrl(...))`. `PdfDocumentParser(IngestionProperties)` ve `TextChunker(IngestionProperties)` düz constructor'la kurulur (`new IngestionProperties(1600, 2000, 200, 4, 96, header, footer, new IngestionProperties.Bootstrap(...))`). `Clock` bean'i `ClockConfig`'te.
- **`IngestionJobLifecycleService.start` `IngestionJob` döner:** runner testinde mock'u bir `IngestionJob` fixture'ı (`workspaceId` 42) dönecek şekilde kur; null dönerse runner `INTERNAL_ERROR`'a düşer. Model kontrolü `WorkspaceService.getWorkspace(42).embeddingModelLabel()` ile `EmbeddingProperties.modelLabel()`'ı karşılaştırır (T27: `new EmbeddingProperties(..., "nomic-embed-text", 768, ...)`).
- **Entity id'leri:** `IngestionJob.id` IDENTITY, birim testte `null` kalır (getter `Long`); `IngestionJobResponse.from` `long`'a çevirirken NPE verir → controller testinde `IngestionJobResponse`'u doğrudan kur ya da `ReflectionTestUtils.setField(job, "id", 1L)`.
- **Aşama sırası sıkı** (sapma #3): T23'te `start → moveTo(CHUNKING) → recordTotalChunks(150) → moveTo(EMBEDDING) → recordEmbeddingProgress(96) → moveTo(WRITING) → complete`. `recordEmbeddingProgress` `totalChunks` kaydedilmeden çağrılırsa `IllegalArgumentException`.
- **T15 (gerçek rehber):** gerçek sayılar `pageCount` 70, `chapterCount` 11, kısım sayısı 109 (99 alt başlık + 8 SSS + kendi satırı olan iki bölüm başı: `10 Ücret tarifesi` (4 satırlık giriş) ve `11 Terimler sözlüğü`), parça 154. **Dikkat:** s. 5'teki gövde satırı `• Başlıklar: Aradığınız konuyu başlangıçtaki İçindekiler sayfasından bulabilirsiniz. …` "İçindekiler" kelimesini içerir; içindekiler sayfasının atıldığını `contains("İçindekiler")` ile değil, `İçindekiler` satırına **eşit** satır olmamasıyla ya da `2.16 … 16` gibi TOC satırlarının olmamasıyla doğrula.
- **T21 (bağlantı reddi):** `EmbeddingClientConfig.embeddingRestClient(properties)` gerçek `JdkClientHttpRequestFactory` kurar; `127.0.0.1:9` → `ResourceAccessException` → 3 deneme → `EMBEDDING_UNAVAILABLE`.
- **T22 (eşzamanlılık):** Semaphore her HTTP isteğini sarar, bekleme süresi `connectTimeout + readTimeout`; latch'li sahte istek fabrikasında bu süreyi aşmayan bekleme kullan.
- **T31-T36:** iş commit sonrası tek thread'li `ingestionExecutor`'da yürür → Awaitility ile `ingestion_jobs.stage` DONE/FAILED beklenir. `@SpringBootTest`'te `@Transactional` kullanma (afterCommit tetiklenmez). `GuideBootstrap.WORKSPACE_ID` = 42. Kaynak adı property'si `ingestion.bootstrap.source-name`; T36 için `ingestion.bootstrap.file` var olmayan bir yola ayarlanır (`@TestPropertySource` ya da `@DynamicPropertySource`).
- **T39 (log):** `Document chunked jobId=` runner'da, `Ingestion job finished jobId=` lifecycle servisinde yazılır.
- Docker açık olmalı (Testcontainers, `pgvector/pgvector:0.8.1-pg18`).

STATUS: DONE
