TASK: TASK-027
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `spring-transactions`, `flyway-migrations`. Kalıp referansı: `../embedding-pgvector/backend` (`PageParameters` mesajları, controller'da `@Validated` olmaması); okundu, kopyalanmadı.

## Yapılanlar
| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| K1: otomatik yükleme yok | `IngestionStartupListener`; silinenler: `GuideBootstrap`, `GuideBootstrapResult`, `PathConversionConfig`, `ingestion.bootstrap.*`, `ingestion.page-*-pattern` | Açılışta sadece `failInterruptedJobs()` çalışır. `src/main`'de rehber yolu yok. |
| K2: belge ekleme ucu | `SourceController.createSource`, `IngestionService.createSource` | 202 + `Location` + `IngestionJobResponse`. İş commit'ten sonra `IngestionJobRunner.submit` ile başlar (TASK-005 hattı). |
| K3: doğrulama, ad tekilliği, sürüm | `SourceUploadValidator`, `V5__allow_user_uploaded_sources.sql`, `SourceRepository.findByLowerName`, multipart ayarları, `GlobalExceptionHandler` | İhlal sırası `file`, `name`, `version`; mesajlar sözleşmedeki gibi. Uzunluklar kod noktasıyla ölçülür. Yarışta `DataIntegrityViolationException` → 409 `SOURCE_NAME_TAKEN`. |
| K4: sadece PDF | `SourceUploadValidator` | `.pdf` uzantısı (harf farksız) ve ilk 1024 baytta `%PDF-` → değilse 415 `UNSUPPORTED_FILE_TYPE`. |
| K5a, Kararlar #9: genel okuyucu | `PdfDocumentParser` | Her yer imi başlıktır. Kontrol sırası açılma → metin → yer imi. Sayfasında satır olarak bulunamayan yer imi sayfanın ilk satırından önce açılır. Başlık 200 kod noktasını aşarsa son boşlukta kesilip `…` eklenir. SSS kuralı korundu. |
| K5b, Kararlar #11 (+ Değişiklik 1) | `PageMarginFilter`, `IngestionProperties` | Her kenarda en fazla `repeated-line-max-per-edge` (2) tur. Önce üst kenar, sonra alt kenar. Eşik paydası: başlangıçtaki metinli sayfa sayısı. |
| K5c: yükleme anında ret | `SourceUploadValidator` → `PdfDocumentParser` | 422 `PDF_UNREADABLE` / `PDF_NO_TEXT` / `PDF_NO_BOOKMARKS`; hiçbir şey yazılmaz. Log: `Upload rejected errorCode= fileSizeBytes=`. |
| K5d: rehber kalitesi | (değişmeyen `TextChunker`) | M0: 154 parça, ort. 285 token (TASK-005 ile aynı). |
| K6, Kararlar #14: yeniden dene | `SourceController.retryIngestion`, `IngestionService.retryIngestion`, `IngestionJobRepository.findFirstBySourceIdOrderByIdDesc` | Son işin `version`, `file_name`, `file_size_bytes` değerleri ve dosya baytları yeni işe kopyalanır. Kontroller sırayla: 404 çalışma alanı, 404 kaynak, 409 model, `SOURCE_ALREADY_INGESTED`, `INGESTION_IN_PROGRESS`. Yarışta partial index → `INGESTION_IN_PROGRESS`. |
| Kararlar #1: kaynak listesi | `SourceService.listSources`, `SourceResponse`, `pagination/PageResponse`, `PageParameters` | Sıra `id ASC`. Geçersiz `page`/`size` → 400 `VALIDATION_FAILED`. |
| Kararlar #3: hata kategorileri | `ValidationFailedException`, `Violation`, `UnsupportedMediaTypeException`, `GlobalExceptionHandler` | `violations` biçimi Bean Validation'la aynı. `handleMaxUploadSizeExceededException` → 413 `FILE_TOO_LARGE`, `title` "Content Too Large", `application/problem+json`. `parameterName` `@RequestPart` adını da okur. |
| Kararlar #8: model kontrolü istek anında | `WorkspaceService.ensureEmbeddingModelMatches`; `IngestionService` (iki uç), `IngestionJobRunner` | Runner'daki özel metot kaldırıldı, servisteki metodu çağırıyor. |
| Kararlar #15: yükleme sınırı | `application.properties` | `max-file-size=20MB`, `max-request-size=21MB`, `server.tomcat.max-swallow-size=-1`. Dosya saf ASCII (grep → 0). |
| Migration | `V5__allow_user_uploaded_sources.sql` | `sources_workspace_id_name_key` düşer, `(workspace_id, lower(name))` unique index gelir. `ingestion_jobs_error_code_check` yeniden kurulur. Postgres'in verdiği ad V3'le kurulmuş şemada doğrulandı (DROP başarılı, `\d ingestion_jobs` yeni CHECK'i gösteriyor). |
| Loglar | `SourceUploadValidator`, `IngestionService`, `IngestionJobRunner` | `Upload rejected`, `Source created`, `Ingestion job queued` (`version` çıkarıldı), `Ingestion retry queued`, `Document parsed … removedMarginLines`. |
| `retrieval-api/CLAUDE.md` | `retrieval-api/CLAUDE.md` | Durum, hata sınıfları, V5, içe alma ve multipart ayarları, PDF okuma paragrafı, paket yapısı, "Henüz olmayanlar", Komutlar (yükleme ve yeniden dene `curl`'leri, bootstrap cümleleri çıktı). |

## Değişen dosyalar
Tümü `retrieval-api/` altında.
- **Yeni:** `exception/ValidationFailedException.java`, `exception/Violation.java`, `exception/UnsupportedMediaTypeException.java`, `pagination/{PageResponse,PageParameters,package-info}.java`, `source/controller/{SourceController,package-info}.java`, `source/service/{SourceService,SourceUploadValidator,package-info}.java`, `source/domain/dto/{SourceResponse,SourceUpload,package-info}.java`, `ingestion/parsing/PageMarginFilter.java`, `resources/db/migration/V5__allow_user_uploaded_sources.sql`
- **Değişen:** `config/IngestionProperties.java`, `exception/GlobalExceptionHandler.java`, `exception/NotFoundException.java` ve `UnprocessableContentException.java` (sadece sınıf yorumu), `ingestion/parsing/PdfDocumentParser.java`, `ingestion/parsing/ParsedDocument.java`, `ingestion/repository/IngestionJobRepository.java`, `ingestion/service/{IngestionService,IngestionJobRunner,IngestionStartupListener}.java`, `source/repository/SourceRepository.java`, `workspace/service/WorkspaceService.java`, `resources/application.properties`, `CLAUDE.md`
- **Silinen:** `config/PathConversionConfig.java`, `ingestion/service/GuideBootstrap.java`, `ingestion/service/GuideBootstrapResult.java`
- **Dependency:** yok. `pom.xml` değişmedi.

## Plandan sapmalar
1. **Değişiklik 1 (planner'la ve lead'le konuşuldu, kullanıcı yeniden onayladı):** M0'da gördüm ki Chrome her sayfanın sonuna art arda iki satır yazıyor: tarih + başlık, sonra adres + `n/51`. Tek satır kuralında tarih + başlık satırı 66 parçanın 48'inde kalıyordu. Planner Kararlar #11'i her kenarda en fazla 2 tur olacak şekilde güncelledi, kart yeniden onaylandı, ben de öyle uyguladım.
2. **`SourceController`'da `@Validated` yok (plan "`@Validated`" diyordu).** Sınıf düzeyinde `@Validated` olunca Spring MVC'nin kendi metot doğrulaması devre dışı kalıyor; doğrulamayı AOP proxy'si yapıp `ConstraintViolationException` fırlatıyor. `GlobalExceptionHandler` bu hatayı karşılamadığı için 500 dönerdi. Anotasyon olmadan `page=-1` → 400 `VALIDATION_FAILED` (`field` `page`), `size=101` → 400 `size` geliyor (elle denendi). `../embedding-pgvector`'deki controller da aynı şekilde `@Validated` kullanmıyor. Sözleşme davranışı değişmiyor.
3. **`ParsedDocument`'e dördüncü alan `removedMarginLines` eklendi.** Plandaki `Document parsed … removedMarginLines={}` logunu yazabilmek için gerekiyordu. `parse(byte[])` imzası aynı kaldı. Testlerde `new ParsedDocument(70, 11, sections)` → `new ParsedDocument(70, 11, sections, 0)` olmalı.
4. **Bölüm (`chapterCount`) tanımında bir ayrıntı.** Kullanılabilir (başlığı ve sayfası olan) bir üst yer imi olmayan yer imi bölüm sayılır. Sayfası çözülemeyen bir üst yer iminin çocukları böylece bölüme yükselir ve `chapter_count > 0` CHECK'i hiçbir durumda kırılmaz. Normal PDF'te bu, plandaki "en üst düzeydekiler" tanımıyla aynı sonucu verir: rehberde 11, Belge B'de 1.
5. **M0'da `down -v` kullanılamadı:** izin reddedildi, mevcut dev volume'u silinmedi. Bunun yerine 5433 portunda geçici ve boş bir `pgvector/pgvector:0.8.1-pg18` konteyneri açtım. Değişiklik 1'den sonraki M0'yu aynı konteynerde yeni bir veritabanında (`retrieval_m0b`) baştan koştum. İş bitince konteyneri kapattım; dev volume'una hiç dokunulmadı.

## Doğrulama
- `cd retrieval-api && ./mvnw -q compile` → başarılı.
- `cd retrieval-api && ./mvnw clean verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, test derleme hatası.** Bu 9 test dosyası plan gereği silinen ya da değişen API'leri kullanıyor: `fixture/IngestionFixtures`, `integration/AbstractIngestionIntegrationTest`, `BootstrapPropertiesIntegrationTest`, `GuideBootstrapIntegrationTest`, `parsing/PdfDocumentParserTest`, `PdfDocumentParserGuideTest`, `service/IngestionJobRunnerTest`, `IngestionServiceTest`, `IngestionStartupListenerTest`. Plan bunları tester'a veriyor ("Silinen testler", "Mevcut testler yeni imzalara göre güncellenir"). `src/test`'e dokunmadım.
- Kalan testlerin durumunu görmek için projenin kopyasında (scratchpad) bu 9 dosyayı ve onlara bağlı 5 testi (`EmbeddingClientTest`, `TextChunkerTest`, `ActiveJobConstraintIntegrationTest`, `InterruptedJobsIntegrationTest`, `WorkspaceMigrationIntegrationTest`; `IngestionFixtures`/`AbstractIngestionIntegrationTest` kullanıyorlar) çıkardım. `./mvnw clean verify -Djacoco.haltOnFailure=false` → **Tests run: 34, Failures: 1, Errors: 0**. Tek hata `RetrievalApplicationIntegrationTest.givenFreshDatabase_whenContextStarts_thenFlywayAppliedVersions1To4`: V5 eklendiği için beklenen sonuç, testi tester güncelleyecek. `GlobalExceptionHandlerTest` (13), `IngestionJobControllerTest` (5), `IngestionJobTest` (10), `ResponseAndDraftTest` (2) ve diğer 3 bağlam testi geçti. Coverage eşiği kapalı çalıştırıldı; eşiği tester ve qa tam `./mvnw verify` ile doğrular.
- Elle denemeler (servis açık, gerçek `bge-m3`): 409 `SOURCE_NAME_TAKEN` (`" raşit bankası ürün ve hizmet rehberi "`), 400 sürüm yok, 400 üç ihlal sırayla, 415 `.txt` ve imzasız `.pdf`, 422 `PDF_UNREADABLE`, 415 `UNSUPPORTED_MEDIA_TYPE` (JSON gövde), 404 çalışma alanı 43 (yükleme, liste, yeniden dene), 404 `SOURCE_NOT_FOUND`, 409 `SOURCE_ALREADY_INGESTED`, 413 `FILE_TOO_LARGE` `application/problem+json` (20 MiB + 1 bayt), tam 20 MiB → 413 değil 422. Türkçe ad multipart `name` parçasından bozulmadan kaydediliyor.

### M0 (gerçek `bge-m3`, Ollama lokal, Değişiklik 1 ile, boş veritabanı)
Açılışta `ingestion_jobs` 0, `sources` 0, `chunks` 0. `GET .../sources` → `content: []`, `totalElements: 0`.

| Belge | Sonuç | Sayfa | Bölüm | Parça | Ort. token | Süre | `removedMarginLines` |
|---|---|---|---|---|---|---|---|
| Rehber 2026.10 (kaynak 10001) | 202 → `DONE` | 70 | 11 | 154 | 285 | 8,83 sn | 138 |
| Belge B, `rag.html` yer imli Chrome baskısı (10002) | 202 → `DONE` | 51 | 1 | 63 | 277 | 3,21 sn | 102 |
| Belge C, `rag.html` yer imsiz Chrome baskısı | 422 `PDF_NO_BOOKMARKS`, listede yok, kaynak/iş yok; aynı adla Belge B sonra kabul edildi | — | — | — | — | — | — |

- **Rehberde TASK-005'le fark:** parça sayısı (154), ortalama token (285) ve ilk parçanın sayfası (5) aynı. Süre 8,83 sn (TASK-005: 8,96 sn), fark ölçüm gürültüsü. Parçalarda `Sayfa n / 70` ya da `Rehberi · Sürüm` kalmadı (0 / 0).
- **Belge B'de tarayıcı üst ve alt bilgisi:** `content ~ 'file:///'` → 0, `content ~ '\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}'` → 0, `content ~ '\d+/\d+\s*$'` → 0. Tek satır kuralıyla ilk denemede: 66 parça, ort. 280 token, `removedMarginLines=51`, tarih + başlık 48 parçada vardı.
- **Belge B'nin yer imleri:** Chrome yer imi başlıklarındaki satır sonlarını boşluksuz birleştiriyor ("KendiVerinle", "nasılhazırlandı"). Bu başlıklar metindeki satırla eşleşmediği için sayfanın ilk satırından önce açılıyor (Kararlar #9 kural 4). Parça başlıkları yer iminin metnini taşıyor (38 kısım).
- **S8 / S9 (devops'un M3/M2'sinden önce kendi kodum için):** Ollama'ya ulaşılamayan adresle yüklenen belgenin işi `FAILED` (`EMBEDDING` aşaması, `EMBEDDING_UNAVAILABLE`) oldu. Servis normal adresle yeniden açılınca sayılar değişmedi (`sources` 4, `ingestion_jobs` 4, `chunks` 374; log `Interrupted ingestion jobs marked failed count=0`). `POST .../sources/10004/ingestion-jobs` → 202, iş 5 `DONE`; kaynak tek, `version`, `file_name` ve dosya md5'i iş 4'le aynı. İkinci yeniden dene → 409 `SOURCE_ALREADY_INGESTED`.
- **Log sınırı:** dört servis logunda kaynak adı, dosya adı, sürüm etiketi aranınca 0 satır çıktı.
- **İz araması (M5, `retrieval-api/src/main`):** iki satır çıkıyor, ikisi de V3'te: `START WITH 10001` (planda zaten istisna) ve V3'ün eski CHECK'indeki `'PDF_NO_SECTIONS'`. V3 çalışmış bir migration olduğu için değiştirilemez; CHECK'i V5 düşürüp yeniden kuruyor. qa bu satırı da istisna saymalı.

## Tester için notlar
- **Silinecek ya da güncellenecek testler:** "Doğrulama"daki 9 dosya ve `src/test/resources/config/application.properties`'teki `ingestion.bootstrap.enabled=false` satırı (artık bağlanan bir property yok, satır zararsız ama plan siliyor). `RetrievalApplicationIntegrationTest` artık V1-V5 görmeli.
- **Yeni imzalar:**
  - `PdfDocumentParser(PageMarginFilter)`, `PageMarginFilter(IngestionProperties)`
  - `IngestionProperties(targetChunkChars, maxChunkChars, overlapChars, charsPerToken, embeddingBatchSize, repeatedLineMinPages, repeatedLineMinShare, repeatedLineMaxPerEdge)`
  - `ParsedDocument(pageCount, chapterCount, sections, removedMarginLines)`
  - `IngestionService(WorkspaceService, SourceRepository, IngestionJobRepository, IngestionJobFileRepository, IngestionJobRunner, EmbeddingProperties, Clock)`
  - `IngestionStartupListener(IngestionService)`
  - `SourceUploadValidator(PdfDocumentParser)`, `SourceService(WorkspaceService, SourceRepository)`, `SourceController(SourceService, SourceUploadValidator, IngestionService)`
- **Sahtelenecek noktalar:** embedding `@MockitoBean EmbeddingClient` (TASK-005'teki gibi). `IngestionJobRunner.submit` commit'ten sonra çağrılır (`TransactionSynchronization.afterCommit`); unit testte `TransactionSynchronizationManager` aktif değilse `registerSynchronization` `IllegalStateException` fırlatır. `IngestionServiceTest`'te bunun için `TransactionSynchronizationManager.initSynchronization()` / `clearSynchronization()` gerekir (TASK-005 testinde nasıl yapıldıysa).
- **T27 / T56 (yarış):** `sourceRepository.saveAndFlush` ya da `ingestionJobRepository.saveAndFlush` `DataIntegrityViolationException` fırlatacak şekilde stub'lanır. Yeniden dene yolunda job'ı yazan `saveAndFlush` `ingestionJobRepository`'dedir.
- **T29 (model farkı yazmaz):** sıra `ensureWorkspaceExists` → `ensureEmbeddingModelMatches` → `findByLowerName`. `WorkspaceService` mock'lanırsa `ensureEmbeddingModelMatches`'in `ConflictException("EMBEDDING_MODEL_MISMATCH")` fırlatması stub'lanır.
- **Validator sırası:** önce alan ihlalleri (dosya baytları okunmaz), sonra `getBytes()`, sonra tür (415), sonra `parser.parse` (422). 415 ve 422'de `Upload rejected errorCode=… fileSizeBytes=…` INFO logu atılır (T51). Ad türetme: `name` boşsa dosya adının son `.`'sından öncesi; 80 kod noktasından uzunsa ilk 80, sonra `strip()`. Kısaltma sadece türetilmiş ada uygulanır; verilen 81 karakterlik ad ihlaldir.
- **`PageMarginFilter`:** eşik paydası başlangıçtaki metinli sayfa sayısıdır, turlar arasında değişmez. Önce üst kenar (≤ 2 tur), sonra alt kenar (≤ 2 tur). Bir turda hiç satır atılmazsa o kenar durur. Tek satırlı bir sayfada üst kenar satırı atarsa alt kenarda o sayfa artık metinsiz sayılır.
- **Okuyucu:** satır eşleşmesinde hem yer imi başlığı hem satır `strip()` + iç boşluklar tek boşluk yapılarak karşılaştırılır. Başlık metni yer iminin normalleştirilmiş hâlidir. SSS başlığı da 200 kod noktası sınırından geçer.
- **T20 (413):** 20 × 1024 × 1024 + 1 baytlık gövde, `RANDOM_PORT` + gerçek Tomcat ile 413 `application/problem+json` `FILE_TOO_LARGE` döndü. Tam 20 MiB, `%PDF-` + dolgu → 422 `PDF_UNREADABLE`. `@WebMvcTest`/MockMvc'de multipart sınırı uygulanmaz; T20 integration olmalı (planda öyle).
- **Türkçe I/ı:** "RAG ANLATIMI" ile "RAG Anlatımı" `lower()` ile farklı ad sayılıyor (elle görüldü). Bu planın risk #6'sı; bunu hata gibi doğrulayan bir test yazılmamalı.

## Düzeltme turu 1
Kaynak: `handoff/03-test-report.md` `## Düzeltme` (tester, T49). Kullanılan skill'ler: `backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `rag-pipeline`.

| Madde | Yapılan değişiklik |
|-------|--------------------|
| Plan Kararlar #9 kural 6 yer imi olan "Sık sorulanlar" satırında uygulanmıyordu (8 kısım yalın `Sık sorulanlar` başlığı alıyordu; T49 kırık) | `PdfDocumentParser.SectionCollector.startHeading`: yer imi bölüm düzeyinde değilse, başlığı (normalleştirilmiş) `Sık sorulanlar` ise ve üstte bir bölüm açıksa `startFaq()` çağrılır; başlık `<bölüm> · Sık sorulanlar` olur (200 karakter sınırından geçer). Satır olarak bulunan ve sayfasında satır olarak bulunamayan yer imleri aynı yoldan geçtiği için ikisinde de geçerli. En üst düzeydeki (bölüm) "Sık sorulanlar" yer imi ve bölümsüz durum değişmedi: başlık yalın kalır. Yer imi olmayan "Sık sorulanlar" satırı eskisi gibi `FAQ_HEADING` dalına düşer. |

- Değişen dosya: `retrieval-api/src/main/java/com/rasitbank/retrieval/ingestion/parsing/PdfDocumentParser.java` (tek dosya). Yeni dependency yok, `CLAUDE.md`'de değişiklik gerekmedi (okuyucu paragrafı zaten bu kuralı anlatıyor). Plandan sapma yok. Testlere dokunulmadı.
- **Doğrulama:**
  - `cd retrieval-api && ./mvnw -q test -Dtest='PdfDocumentParserGuideTest,PdfDocumentParserTest' -Djacoco.skip=true` → geçti (T49 dahil; GuideTest 6, ParserTest 14 test).
  - `cd retrieval-api && OLLAMA_BASE_URL=http://127.0.0.1:9 ./mvnw verify` (eşik kontrolü **açık**) → BUILD SUCCESS, Tests run: 185, Failures: 0, Errors: 0, Skipped: 0; "All coverage checks have been met".
  - Gerçek rehber, geçici boş konteyner (`pgvector/pgvector:0.8.1-pg18`, ad `rag-fix1-pg`, port 55432, volume yok; dev volume'una dokunulmadı), servis 8090'da, Ollama açık: `POST .../sources` (rehber 2026.10) → 202, iş 1 `DONE`, kaynak 10001. `chunks`: 154 parça, ort. 285 token (TASK-005 ile aynı); `source_versions`: `page_count` 70, `chapter_count` 11, `chunk_count` 154; süre 8,78 sn. `% · Sık sorulanlar` başlıklı farklı başlık 8: `2 Kartlar`, `3 Hesaplar`, `4 Krediler`, `5 Para transferi ve ödemeler`, `6 Dijital bankacılık`, `7 Yatırım`, `8 Şube, ATM ve müşteri hizmetleri`, `9 Şikâyet, itiraz ve kişisel veriler` + ` · Sık sorulanlar`. Yalın `Sık sorulanlar` başlıklı parça 0. Ardından servis durduruldu (`pkill`, çıkış kodu 144 bundan) ve konteyner `docker stop` ile kaldırıldı (`--rm`).
- **Tester için:** rehberde bu 8 "Sık sorulanlar" hem yer imi (bölümün derinlik 1 çocuğu) hem satır. Sentetik `PdfFixtures`'ta bu durum için alt düzeyde `Sık sorulanlar` yer imi olan bir PDF ile birim test yazılabilir (yer imi satırla eşleşen ve eşleşmeyen iki hâli; ayrıca en üst düzey `Sık sorulanlar` yer iminin yalın kaldığı hâl).

STATUS: DONE
