TASK: TASK-005
# Test raporu

## Kullanılan skill'ler
`writing-good-tests`, `backend-conventions`, `spring-testing-fundamentals`, `spring-mvc-testing`, `spring-jpa-testing` (Testcontainers), `rag-pipeline` (`references/indexing.md`).

## Senaryo → test eşlemesi
Sınıf yolları `retrieval-api/src/test/java/com/rasitbank/retrieval/` altındadır. Test adı kısaltılmıştır; tam ad plandaki (given_when_then) addır.

| # | Senaryo / Kural | Test (dosya#metot) | Sonuç |
|---|-----------------|--------------------|-------|
| T1 | Parça ≤ 2.000, çok bloklu parça ≤ 1.600, ≥ 3 parça | `ingestion/chunking/TextChunkerTest#givenSectionOf40SentenceLines_…` | Geçti |
| T2 | Örtüşme: sonraki parça öncekinin son satırıyla başlar, ≤ 200 | `TextChunkerTest#givenConsecutiveChunksInOneSection_…` | Geçti |
| T3 | Tablo satırı bölünmez | `TextChunkerTest#givenTableRowsWithoutSentenceEnd_…` | Geçti |
| T4 | Kısım sınırı, başlık, örtüşmesiz başlangıç | `TextChunkerTest#givenTwoSections_whenChunk_thenNoChunkSpansSections…`, `#givenTwoSections_…SecondSectionFirstChunkStartsWithoutOverlap` | Geçti |
| T5 | Sayfa = ilk yeni satırın sayfası (örtüşme s. 7'den, yeni satır s. 8) | `TextChunkerTest#givenSectionCrossingFromPage7To8_…` | Geçti |
| T6 | Token = ceil((başlık + `\n\n` + içerik) / 4) = 25 | `TextChunkerTest#givenHeadingAndContent_…` | Geçti |
| T7 | 4.500 karakterlik satır boşlukta bölünür, her parça ≤ 2.000 | `TextChunkerTest#givenLineOf4500CharsWithSpaces_…` | Geçti |
| T8 | `chunkNo` 0'dan boşluksuz | `TextChunkerTest#givenThreeSections_…` | Geçti |
| Ek | Cümle sonu işaretleri `. ! ? :` blok kapatır, virgül kapatmaz, `•` yeni blok açar | `TextChunkerTest#givenLineEndingWithSentenceMark_…` (4 değer), `#givenLineEndingWithComma_…`, `#givenBulletLine_…` | Geçti |
| Ek | Sınırlar: toplam tam 1.600 → 1 parça, 1.601 → 2; satır 2.000 bölünmez, 2.001 bölünür; örtüşme tam 200 dahil; örtüşme + blok > 2.000 ise baştan kırpılır | `TextChunkerTest#givenTwoBlocksTotalling1600_…`, `#givenLineOf2000And2001Chars_…`, `#givenOverlapCandidatesTotalling200Chars_…`, `#givenOverlapPlusBlockAboveMax_…` | Geçti |
| T9 | Üst/alt bilgi atılır, gövde korunur | `ingestion/parsing/PdfDocumentParserTest#givenPagesWithHeaderAndFooter_…` | Geçti |
| T10 | Kapak ve içindekiler atlanır, ilk kısım s. 3 | `PdfDocumentParserTest#givenCoverAndTocBeforeFirstChapterBookmark_…` | Geçti |
| T11 | Sayıyla başlayan gövde satırı başlık olmaz (`1 Kasım 2026…`, `1. Kartınızı…`) | `PdfDocumentParserTest#givenBodyLinesStartingWithNumbers_…` | Geçti |
| T12 | `<bölüm> · Sık sorulanlar`, alt başlıksız bölüm başı, `chapterCount` | `PdfDocumentParserTest#givenFaqLineAndChapterWithoutSubsections_…` | Geçti |
| Ek | SSS satırı bölümden önce başlık sayılmaz | `PdfDocumentParserTest#givenFaqLineBeforeAnyChapter_…` | Geçti |
| T13 | PDF değil → `PDF_UNREADABLE` | `PdfDocumentParserTest#givenBytesThatAreNotAPdf_…` | Geçti |
| T14 | Yer imi yok → `PDF_NO_SECTIONS` | `PdfDocumentParserTest#givenPdfWithoutBookmarks_…` | Geçti |
| T15 | Gerçek rehber: 70 sayfa, 11 bölüm, 99 alt başlık, 8 SSS, 2.16 s. 16, 4.10 s. 35, TOC/alt bilgi yok, parça ≤ 2.000, Raşit Gold satırı tek satır | `ingestion/parsing/PdfDocumentParserGuideTest` (4 test) | Geçti |
| T16 | İstek `POST /api/embed` gövdesi, vektörler sırayla | `embedding/EmbeddingClientTest#givenTwoInputs_…` | Geçti |
| T17 | 5xx sonra başarı: 1 yeniden deneme | `EmbeddingClientTest#givenServerErrorThenSuccess_…` | Geçti |
| T18 | Hep 5xx: tam 3 istek, `EMBEDDING_UNAVAILABLE` | `EmbeddingClientTest#givenServerErrorOnEveryAttempt_…` | Geçti |
| T19 | 512 boyut: 1 istek, `EMBEDDING_DIMENSION_MISMATCH` | `EmbeddingClientTest#given512DimensionVector_…` | Geçti |
| T20 | Eksik vektör → `EMBEDDING_UNAVAILABLE` | `EmbeddingClientTest#givenOneVectorForTwoInputs_…` | Geçti |
| T21 | Bağlantı reddi (gerçek `RestClient`, 127.0.0.1:9) | `EmbeddingClientTest#givenNothingListeningOnPort9_…` | Geçti |
| T22 | 4 eşzamanlı çağrı, gerçek yerel HTTP sunucusunda en çok 2 istek aynı anda | `EmbeddingClientTest#givenFourConcurrentCalls_…` | Geçti |
| Ek | 4xx denenmez; boş girdi istek atmaz; bekleme n × backoff (60 ms ile ≥ 180 ms) | `EmbeddingClientTest#givenClientError_…`, `#givenEmptyInputList_…`, `#givenBackoffOf60ms_…` | Geçti |
| T23 | İlerleme 0.05 / 0.1 / 0.612 / 0.9 / 1.0 | `ingestion/domain/IngestionJobTest#givenJobAdvancingThroughStages_…` | Geçti |
| T24 | Bitmiş iş değişmez | `IngestionJobTest#givenDoneOrFailedJob_whenMoveTo_…`, `#givenFailedJob_whenRecordingOrCompleting_…` | Geçti |
| T25 | `fail`: FAILED, `failedStage` EMBEDDING, ilerleme korunur | `IngestionJobTest#givenJobInEmbedding_whenFail_…` | Geçti |
| Ek | Aşama atlanamaz, EMBEDDING'den `complete` olmaz, sınır değerler (0, toplam, negatif) | `IngestionJobTest` (5 ek test) | Geçti |
| T26 | 2. grupta model düşer: `fail(EMBEDDING_UNAVAILABLE)`, ilerleme 96, `complete`/WRITING hiç yok | `ingestion/service/IngestionJobRunnerTest#givenEmbeddingUnavailableOnSecondBatch_…` | Geçti |
| T27 | Model farkı: `EMBEDDING_MODEL_MISMATCH`, parser çağrılmaz | `IngestionJobRunnerTest#givenConfiguredModelDiffersFromWorkspace_…` | Geçti |
| T28 | Beklenmeyen hata → `INTERNAL_ERROR` | `IngestionJobRunnerTest#givenParserThrowsRuntimeException_…` | Geçti |
| T29 | 150 parça → gruplar [96, 54], ilerleme 96 → 150, sıra, `complete` 150 vektörle | `IngestionJobRunnerTest#given150Chunks_…` | Geçti |
| Ek | 1024 dışı boyut işi `FAILED` yapar; `PDF_UNREADABLE` aşamayı ilerletmez | `IngestionJobRunnerTest#givenDimensionMismatchFromClient_…`, `#givenParserThrowsPdfUnreadable_…` | Geçti |
| T30 | Çalışma alanı 42, `bge-m3`, 1024 | `ingestion/integration/WorkspaceMigrationIntegrationTest#givenFreshDatabase_whenMigrated_…` | Geçti |
| T31 | İlk giriş uçtan uca (gerçek PDF, PDFBox, gerçek pgvector, sahte embedding): kaynak 10001, sürüm 2026.10, 70/11, bayt bayt PDF, `synced_at` = `finished_at`, 1024 boyut, `bge-m3@1024`, sayfa 5-70 | `ingestion/integration/GuideBootstrapIntegrationTest#givenFreshDatabase_whenBootstrapGuide_…` | Geçti |
| T32 | Yeniden açılış: kaynak 1, iş 1, parça sayısı aynı | `GuideBootstrapIntegrationTest#givenGuideAlreadyIngested_…` | Geçti |
| T33 | Model kapalı: EMBEDDING'de FAILED, `chunks` 0, `source_versions` 0, `active_version_id` null, kaynak 10001 | `GuideBootstrapIntegrationTest#givenEmbeddingUnavailable_…` | Geçti |
| Ek | Yanlış boyut: iş `EMBEDDING_DIMENSION_MISMATCH` ile FAILED, hiçbir şey yazılmaz | `GuideBootstrapIntegrationTest#givenEmbeddingReturnsWrongDimension_…` | Geçti |
| T34 | Başarısızlık sonrası açılış: aynı kaynak, iş 2 (FAILED, DONE), tek sürüm | `GuideBootstrapIntegrationTest#givenPreviousBootstrapFailed_…` | Geçti |
| T35 | Yarıda kalan iş: `INGESTION_INTERRUPTED`, `failedStage` EMBEDDING, DONE iş değişmez | `ingestion/integration/InterruptedJobsIntegrationTest` (2 test) | Geçti |
| T36 | Dosya yok: `FILE_MISSING`, hiçbir şey yazılmaz | `GuideBootstrapIntegrationTest#givenBootstrapFileMissing_…` | Geçti |
| T37 | Kaynak başına tek aktif iş (partial unique index); FAILED iş varken yeni iş kabul | `ingestion/integration/ActiveJobConstraintIntegrationTest` (2 test) | Geçti |
| T38 | HNSW `vector_cosine_ops` index'i | `WorkspaceMigrationIntegrationTest#givenMigratedDatabase_whenIndexesRead_…` | Geçti |
| T39 | Log: `Document chunked`, `Ingestion job finished` var; parça/başlık metni yok | `GuideBootstrapIntegrationTest#givenGuideIngested_whenLogsCaptured_…` | Geçti |
| T40-T44 | GET 200 (15 alan), başarısız iş, 404 `INGESTION_JOB_NOT_FOUND`, 404 `WORKSPACE_NOT_FOUND`, 400 `BAD_REQUEST` | `ingestion/controller/IngestionJobControllerTest` (5 test) | Geçti |
| Ek | Servis: iş okuma çalışma alanı kontrolünden sonra; olmayan çalışma alanında kuyruğa alma hiçbir şey yazmaz | `ingestion/service/IngestionServiceTest` (4 test) | Geçti |
| T45 | Açılış sırası: önce `failInterruptedJobs`, sonra `bootstrapGuide`; biri patlasa da servis açık kalır | `ingestion/service/IngestionStartupListenerTest` (3 test) | Geçti |
| Ek | `BaseException` → RFC 9457 (status, title, errorCode, type, instance) 503/409/422 | `exception/GlobalExceptionHandlerTest#givenDomainException_…` (3 değer) | Geçti |
| Ek | Bağlanan yapılandırma (1600/2000/200/96, kalıplar, kaynak adı, göreli rehber yolu, executor adı `ingestion-`) | `ingestion/integration/BootstrapPropertiesIntegrationTest` (3 test) | Geçti |
| Ek | `embeddingInput()` ve `statusUrl` biçimi | `ingestion/ResponseAndDraftTest` (2 test) | Geçti |
| — | Eski testler güncellendi: migration sayısı 4, son sürüm 4; `@WebMvcTest(controllers = ErrorProbeController.class)` | `RetrievalApplicationIntegrationTest#givenFreshDatabase_whenContextStarts_thenFlywayAppliedVersions1To4`, `GlobalExceptionHandlerTest` | Geçti |

Planda testi olmayan kural: yok. Plandan ayrıntı farkları:
- T37 `@DataJpaTest` yerine ortak `@SpringBootTest` tabanıyla (Testcontainers) yazıldı; aynı veritabanı kısıtını doğrular.
- T31-T36, T39: `ingestion.bootstrap.enabled=false` olduğu için `GuideBootstrap` testte, açık ayarlı `IngestionProperties` ile elle kurulur; gerçek `IngestionService`, runner, lifecycle, parser, chunker ve veritabanı kullanılır, yalnızca `EmbeddingClient` sahtedir.
- PDF fixture'ları test kaynağındaki `fonts/LiberationSans-Regular.ttf` (SIL OFL) ile üretilir; Türkçe karakter (ş, ı, İ) standart PDF fontlarıyla yazılamıyor.

## Mutation
PIT 1.30.1 (`./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`), hedef `com.rasitbank.retrieval.*`.
- İlk koşu: 353 mutant, 242 öldü (%69), test gücü %79.
- Son koşu: 353 mutant, 272 öldü (**%77**), test gücü **%88**, satır coverage (mutasyona uğrayan sınıflar) %91.
- Kapatılan task kuralları: cümle sonu işaretleri ve `•` blok sınırı, 1.600/2.000 sınırları, tam 200 karakter örtüşme ve örtüşme kırpma, `n × backoff` bekleme, `IngestionJob` sınır değerleri ve geçersiz aşama çağrıları, `IngestionService` çalışma alanı/iş kontrolleri, `BaseException` yanıt alanları, `embeddingInput`, `statusUrl`.
- Hayatta kalan 71 mutant task kuralı değildir: JPA entity getter'ları (`Chunk`, `SourceVersion`, `Source`, `Workspace`, `IngestionJobFile`; bu task'ta okuma ucu yok, TASK-006/007'de okunacak ve orada ölür), log metni/seviyesi (`averageTokens`, `durationMs`, `INTERNAL_ERROR` log seviyesi), `Thread.interrupt()` ve `closeQuietly` çağrıları, `@Bean` dönüşleri (`ClockConfig`, `IngestionExecutorConfig`, `PathConversionConfig`; bağlama başka testlerce dolaylı doğrulanıyor), `PDFTextStripper` ayarları (`setSortByPosition`, `setLineSeparator`; üretilen tek sütunlu fixture'larda fark yaratmıyor, gerçek rehberde T15 ile korunuyor), `GlobalExceptionHandler`'ın bu task'ta değişmeyen eski kolları.

## Komutlar ve sonuçları
| Komut | Sonuç |
|-------|-------|
| `cd retrieval-api && ./mvnw verify` (JaCoCo eşiği %80 açık, `haltOnFailure` varsayılan) | **BUILD SUCCESS**, 104 test, 0 başarısız, 0 atlanan, "All coverage checks have been met". Satır coverage **%90,8** (697/768) |
| `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | Yukarıdaki skor, ~7 dk |
| Tek tek sınıf koşuları | Hepsi geçti |

Testler gerçek Ollama'ya gitmez (`src/test/resources/config/application.properties`: `ingestion.bootstrap.enabled=false`, `embedding.base-url=http://127.0.0.1:9`, `embedding.retry-backoff=0ms`; embedding sahte). Docker açıktı (Testcontainers `pgvector/pgvector:0.8.1-pg18`).

## Çalıştırılamayanlar
- Gerçek `bge-m3` ile uçtan uca ve M1-M5 (Docker, yeniden başlatma, Ollama kapalı senaryosu, elle sayfa/başlık karşılaştırma): çalışan sistem gerekir; backend-coder M0'ı çalıştırdı, devops M1-M3'ü, qa M4-M5'i çalıştırır.
- Web testi: bu task'ta ekran yok.

## Düzeltme
Gerek yok. Uygulama hatası bulunmadı.

STATUS: DONE
