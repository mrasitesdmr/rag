TASK: TASK-027
# Plan

## Özet
**Mevcut durum (TASK-005):** retrieval-api açılışta `IngestionStartupListener` ile önce yarıda kalan işleri `FAILED` + `INGESTION_INTERRUPTED` yapıyor, sonra `GuideBootstrap` ile `ingestion.bootstrap.file`'daki rehberi kendiliğinden içe alıyor (kaynak 10001). `PdfDocumentParser` sadece numaralı yer imlerini okuyor; yoksa iş `PDF_NO_SECTIONS` ile `FAILED` oluyor. Üst/alt bilgi rehbere özgü iki regex'le (`ingestion.page-header-pattern`, `ingestion.page-footer-pattern`) atılıyor. Compose'da `../docs/rehber/pdf:/app/rehber:ro` bağlı. İş başlatan bir REST ucu yok.

**Yeni durum:**
- **Açılış:** sadece `failInterruptedJobs()`. Hiçbir kaynak, sürüm, parça ya da iş oluşturulmaz.
- **Belge ekleme ucu:** `POST /api/workspaces/{workspaceId}/sources` (multipart `file`, `name`, `version`; sürüm zorunlu). PDF istek sırasında bir kez okunur. Açılamayan, metni olmayan ve yer imi olmayan PDF 422 ile hemen reddedilir; hiçbir şey yazılmaz. Geçen dosya için 202 + `Location` + `IngestionJobResponse`; iş TASK-005'teki hattan geçer.
- **Yeniden deneme ucu:** `POST /api/workspaces/{workspaceId}/sources/{sourceId}/ingestion-jobs`. Hiç tamamlanmış sürümü olmayan ve işi sürmeyen kaynak için, son işin saklanan PDF'inden yeni iş başlatır.
- **Kaynak listesi:** `GET /api/workspaces/{workspaceId}/sources` (sayfalı, sade). TASK.md'nin 1. senaryosu bu adresi kullanıyor (Kararlar #1).
- **Genel PDF okuyucu:** her yer imini (numaralı olsun olmasın) başlık sayar. Başlık tahmin etmez. Üst/alt bilgiyi sayfalar arası tekrardan tanır.
- **Değişen servis:** sadece retrieval-api. assistant-api ve web değişmez. Docker: rehber klasörü bağlantısı ve `INGESTION_BOOTSTRAP_FILE` kalkar; nginx'e yükleme sınırı eklenir.

**Silinenler:** `GuideBootstrap`, `GuideBootstrapResult`, `IngestionProperties.Bootstrap`, `PathConversionConfig` (sadece bootstrap yolu içindi), `ingestion.bootstrap.*`, `ingestion.page-header-pattern`, `ingestion.page-footer-pattern`, `SourceRepository.findByWorkspaceIdAndName`, `IngestionService.queueIngestion` (yerini `createSource` alır), `PdfDocumentParser.NUMBERED_HEADING`, `PDF_NO_SECTIONS` (yerine `PDF_NO_BOOKMARKS`). Testlerden: `GuideBootstrapIntegrationTest`, `BootstrapPropertiesIntegrationTest`, `AbstractIngestionIntegrationTest.bootstrapFor`, `src/test/resources/config/application.properties`'teki `ingestion.bootstrap.enabled=false` satırı.

**Yeniden adlandırma:** `PDF_NO_SECTIONS` → `PDF_NO_BOOKMARKS`. `IngestionJobRunner.submit/run` ve `PdfDocumentParser.parse(byte[])` imzaları değişmez.

**Kullanıcı kararları (2026-10-31, kart sorularının cevabı):** yer imsiz PDF reddedilir (soru 1'deki seçeneklerin hiçbiri değil). Dosya kaynaklı hatalar yükleme anında yakalanır. Ad her durumda tekildir, başarısız ilk içe alma "yeniden dene" ucuyla tekrarlanır (soru 2 = B + yeni uç). Sürüm zorunludur (soru 3 = B). Sadece PDF kabul edilir (soru 4 = A). Çalışma alanı migration'la kalır (soru 5 = A). TASK.md lead tarafından buna göre güncellendi.

## Çalışacak roller
- **planner:** bu plan, onay kartı, `docs/api/openapi.yaml`.
- **backend-coder:** retrieval-api `src/main/**`, `pom.xml` (değişmez), `retrieval-api/CLAUDE.md`. M0'ı lokal çalıştırır. Rapor `handoff/02-backend-implementation.md`.
- **tester:** `retrieval-api/src/test/**`. Rapor `handoff/03-test-report.md`.
- **devops:** `deployment/docker-compose.yml`, `deployment/CLAUDE.md`, `web/nginx.conf` (yükleme sınırı), `ollama.md` §8, kök `README.md`. M1-M3. Rapor `handoff/04-deploy-report.md`.
- **qa:** M4-M5, kabul kriterleri, sözleşme uyumu, iz araması. Rapor `handoff/05-qa-report.md`.
- **Atlanan:** frontend-coder. TASK.md "bu task'ta ekran yok"; "Belge ekle" penceresi, "Yeniden dene" düğmesi ve yeni `errorCode`'ların Türkçe mesajları TASK-007'de.

## Kural eşlemesi
| # | Kural (TASK.md) | Nerede uygulanır | Hangi testle doğrulanır |
|---|-----------------|------------------|-------------------------|
| K1 | Otomatik yükleme yok; kod, ayar ve Docker bağlantısı kalkar; açılışta sadece yarıda kalan işler kapanır | `IngestionStartupListener` (tek çağrı), silinenler (Özet), Compose | T31, T32, T33, M1, M5 |
| K2 | Belge ekleme ucu: dosya + ad, 202 + `Location`, iş TASK-005 aşamalarından geçer | `SourceController.createSource`, `IngestionService.createSource`, `IngestionJobRunner` | T11, T21, T22, M0, M1 |
| K3 | Dosya boş değil, ≤ 20 MB; ad 2-80, harf farksız tekil, her durumda ikinci yükleme reddedilir; boş ad → dosya adı (uzantısız); sürüm zorunlu 1-40; her ihlal sabit `errorCode` | `SourceUploadValidator`, V5 index, `SourceRepository.findByLowerName`, multipart ayarları, `GlobalExceptionHandler` | T1-T7, T10, T12, T13, T16, T17, T20, T24, T26, T27, T29, T34 |
| K4 | Sadece PDF | `SourceUploadValidator` (uzantı + `%PDF-` imzası) | T8, T9, T14, T15 |
| K5a | Başlıklar yer imlerinden; yer imsiz PDF reddedilir; kalın/büyük ama yer imsiz satır başlık değildir | `PdfDocumentParser` (Kararlar #9) | T35-T40, T45b, T46, T23b |
| K5b | Rehbere özgü üst/alt bilgi kalıbı ve "içerik 5. sayfadan" kuralı kalkar; tekrar eden üst/alt bilgi genel yöntemle ayıklanır | `PageMarginFilter`, `IngestionProperties` | T41-T44 |
| K5c | Dosyadan kaynaklanan hatalar (yer imi yok, metin yok, açılamıyor) yükleme anında sabit `errorCode` ile reddedilir; kaynak yok, iş yok, ad boş kalır | `SourceUploadValidator` → `PdfDocumentParser` (422) | T23, T23b, T23c, T19, T45, T45b, T47 |
| K5d | Rehber bu okuyucuyla yüklenince TASK-005 kalitesi korunur | `PdfDocumentParser` + `TextChunker` (değişmez) | T49, T50, M0, M4 |
| K6 | Yeniden deneme ucu: tamamlanmış sürümü olmayan ve işi sürmeyen kaynak için saklanan PDF'ten yeni iş | `SourceController.retryIngestion`, `IngestionService.retryIngestion` | T52-T58, M3 |
| K7 | Çalışma alanı migration'la kalır, açılışta yazılmaz | `V2` (değişmez) | T30, T31 |
| K8 | Eski rehber verisinin temizliği `deployment/CLAUDE.md`'de; kodda sabit 10001 yok | devops dokümanı; Kararlar #12 | M1, M5 |
| K9 | Dosya içeriği ve parça metni loglanmaz; iş no, dosya boyu, sayfa, parça sayısı, süre loglanır | Backend planı "Loglar" | T51, M5 |
| K10 | Repodaki rehber PDF'leri kalır; servis onlara bakmaz | Compose bağlantısı kalkar; `src/main`'de yol yok | M5 |
| S1 | Temiz açılış: `GET .../sources` boş, iş yok | `SourceController.listSources`, K1 | T31, T18, M1 |
| S2 | Rehber 2026.10 `curl -F … -F version=2026.10` → 202, iş birkaç saniyede `DONE` | K2 | T21, M0, M1 |
| S3 | Yer imli başka bir PDF → `DONE`, parçalar kendi başlıkları ve sayfa numaralarıyla | K5a | T22, T35, M0 |
| S4 | Yer imsiz PDF → hemen ret, listede yeni kayıt yok, aynı adla düzeltilmiş belge yüklenebilir | K5c | T23b, M0, M1 |
| S5 | Sürüm etiketsiz yükleme → 400 `VALIDATION_FAILED` | K3 | T10, T13 |
| S6 | Aynı adla ikinci belge → 409 sabit `errorCode` | `SOURCE_NAME_TAKEN` | T24, T26, T12 |
| S7 | Taranmış PDF → hemen ret, kaynak ve iş yok | K5c | T23 |
| S8 | Ollama kapalıyken yüklenen belge `FAILED`; açılınca yeniden dene → `DONE`, kaynak tek | K6 | T52, M3 |
| S9 | Yeniden başlatma: kaynak ve parça sayısı aynı, yeni iş yok | K1 | T32, M2 |
| AC1 | Temiz Docker açılışında boş; kodda, ayarda, Compose'da iz yok (aramayla) | K1 | M1, M5 |
| AC2 | Gerçek `bge-m3` ile iki belge (rehber + yer imli başka PDF); yer imsiz PDF'in reddi gerçek dosyayla; sayılar ve TASK-005 farkı raporda | backend-coder M0, devops M1 | M0, M1 |
| AC3 | qa iki belgeden rastgele parçaları PDF'le karşılaştırır | qa | M4 |
| AC4 | Doğrulama, ad çakışması, yükleme anında ret, yeniden deneme, yeniden başlatma testli; `./mvnw verify` geçer | tester | Test senaryoları tablosu |

## Gözden kaçabilecek durumlar
| # | Durum | Beklenen davranış | Karşılayan test # ya da karttaki soru # |
|---|-------|-------------------|------------------------------------------|
| G1 | Ad sadece harf büyüklüğü ya da baştaki/sondaki boşlukla farklı ("Rehber" / " REHBER ") | Aynı ad sayılır → 409 `SOURCE_NAME_TAKEN` | T24, T34 |
| G2 | Aynı adla iki yükleme aynı anda | Biri 202; diğeri unique index'e takılır → 409 `SOURCE_NAME_TAKEN`; kaynak 1 | T27 |
| G3 | Türkçe karakterli ya da uzun dosya adı, ad boş ("Şirket El Kitabı.PDF", 90 karakterlik ad, "a.pdf") | "Şirket El Kitabı" olduğu gibi; 90 karakter → ilk 80; "a" → 400 `name` | T3, T4, T5, T6 |
| G4 | İş sürerken yeniden dene; iki yeniden dene aynı anda | 409 `INGESTION_IN_PROGRESS`; yarışta partial unique index aynı kodu verir | T55, T56 |
| G5 | İçe alma sürerken ikinci yükleme | İkinci iş `QUEUED` bekler (tek thread'li executor), ilki bitince başlar; ikisi de `DONE` | T28 |

## Kararlar
| # | Karar | Gerekçe | Elenen alternatif ve nedeni |
|---|-------|---------|-----------------------------|
| 1 | **Sade kaynak listesi bu task'ta eklenir:** `GET .../sources`, `PageResponse<SourceResponse>`, alanlar `sourceId, workspaceId, name, kind, createdAt`, sıra `id ASC`. TASK-007 alan ekler | TASK.md 1. ve 4. senaryolar ("listede yeni kayıt yok") bu adresi kullanıyor | Sadece veritabanı sorgusu: senaryo yazıldığı gibi denenemez |
| 2 | **Yükleme kontrolleri tek yerde:** `SourceUploadValidator` (`source/service`) ham parçaları alır. Sırayla: alan doğrulaması (`ValidationFailedException`, 400, bütün ihlaller birlikte), tür (`UnsupportedMediaTypeException`, 415), içerik (`PdfDocumentParser.parse`, `UnprocessableContentException`, 422). Geçerse `SourceUpload` döner. Controller sırayla validator'ı ve servisi çağırır | Türetilmiş ad Bean Validation'la denetlenemez. Okuma veritabanı transaction'ı dışında kalır. Sözleşmedeki sıra (400 → 415 → 422 → 404 → 409) tek sınıfta | Bean Validation + serviste ek kontrol: aynı alan için iki kaynak. Okumayı servis transaction'ında yapmak: bağlantı okuma süresince tutulur |
| 3 | **Yeni kategori sınıfları:** `ValidationFailedException` (400, sabit `VALIDATION_FAILED`, `List<Violation>`), `UnsupportedMediaTypeException` (415). `GlobalExceptionHandler.handleBaseException`, `ValidationFailedException`'da `violations` ekler. `handleMaxUploadSizeExceededException` override → 413 `FILE_TOO_LARGE` (`HttpStatus.CONTENT_TOO_LARGE`). 422 için mevcut `UnprocessableContentException` | `problem-details-rfc9457`: kategori sınıfı ihtiyaç duyan task'ta; `errorCode` başına sınıf yok | Hata başına sınıf: kurala aykırı |
| 4 | **Ad kuralı:** `strip()`; boşsa dosya adının son `.`'ya kadar olan kısmı `strip()`; 80 kod noktasından uzunsa ilk 80 kod noktası, yeniden `strip()`. Sonuç 2-80 kod noktası değilse `VALIDATION_FAILED` `name`. Uzunluk `codePointCount` ile (DB `char_length` ile aynı) | TASK kural 3; kısaltma kullanıcının uzun dosya adını reddetmemek için | Uzun türetilmiş adı reddetmek: kullanıcı adı kendisi vermemişti |
| 5 | **Dosya adı ve tür:** `getOriginalFilename()`'ın son `/` ya da `\`'tan sonrası; 255 karakterden uzunsa `VALIDATION_FAILED` `file`. Ad `.pdf` ile biter (harf farksız) ve ilk 1024 baytta `%PDF-` → değilse 415 `UNSUPPORTED_FILE_TYPE` | Kural 4; imza kontrolü yanlış uzantılı dosyayı okumadan reddeder | Sadece uzantı: `.pdf` adlı düz metin 422'ye kadar ilerler |
| 6 | **Sürüm:** `strip()`; eksik ya da boş → `must not be blank`; 40 kod noktasından uzun → `size must be between 1 and 40` (field `version`). DB'de `NOT NULL` ve `CHECK 1-40` zaten var | Kural 3 (kullanıcı kararı) | — |
| 7 | **Tekillik:** V5'te `UNIQUE (workspace_id, name)` kaldırılır, `CREATE UNIQUE INDEX sources_workspace_id_lower_name_key ON sources (workspace_id, lower(name))`. Ön kontrol JPQL `lower()` ile (`findByLowerName`); yarış `DataIntegrityViolationException` → `ConflictException("SOURCE_NAME_TAKEN")`. Ad hangi durumdaki kaynağa ait olursa olsun 409 | Kural 3; index ile sorgu aynı fonksiyonu kullanır. Spring Data `IgnoreCase` `upper()` kullanır, index'le ayrışır | `IgnoreCase` derived query: farklı fonksiyon |
| 8 | **Model kontrolü istek anında da yapılır** (yükleme ve yeniden dene): yapılandırılmış model ≠ çalışma alanının modeli → 409 `EMBEDDING_MODEL_MISMATCH`, hiçbir şey yazılmaz. Karşılaştırma `WorkspaceService.ensureEmbeddingModelMatches`'te; runner da bunu çağırır | Boş kaynak ve tutulmuş ad oluşmaz | Sadece runner: kaynak oluşur, ad kilitlenir |
| 9 | **Okuyucu kuralları:** (1) Açılamayan ya da şifreli → `PDF_UNREADABLE`. (2) Bütün sayfaların satırları → `PageMarginFilter`. Hiçbir sayfada satır yoksa → `PDF_NO_TEXT`. (3) Yer imleri: başlığı boş olmayan ve sayfası çözülebilen her yer imi, derinlik öncelikli sırayla; en üst düzeydekiler bölümdür (`chapterCount`). Hiç yoksa → `PDF_NO_BOOKMARKS`. (4) Bir satır yer iminin başlığına (iç boşluklar tek boşluğa indirilip) tam eşit ve yer iminin sayfasındaysa başlıktır. Sayfasında satır olarak bulunamayan yer imi o sayfanın ilk satırından önce açılır. (5) İçerik ilk yer iminin sayfasından başlar; öncesi (kapak, içindekiler) atlanır. (6) "Sık sorulanlar" satırı kuralı korunur (`<bölüm> · Sık sorulanlar`). (7) Başlık 200 karakteri aşarsa 200'den önceki son boşlukta kesilir, sonuna `…`. (8) Hiçbir kısım satır taşımıyorsa → `PDF_NO_BOOKMARKS`. Kontrol sırası: açılma → metin → yer imi | Kural 5. Rehberin yer imleri numaralı ve en üst düzeyde 11 tane; TASK-005 sonucu aynı çıkar. Sıradan PDF'lerin yer imleri numarasızdır. Taranmış ve yer imsiz bir dosyada kullanıcıya önce "metin yok" söylenir | Sadece numaralı yer imi: Word'den gelen yer imleri kaybolur. SSS kuralını kaldırmak: rehberde SSS satırları son alt başlığa yazılır |
| 10 | **Okuma iki kez yapılır:** istek sırasında ret için, iş içinde PARSING aşamasında yeniden. İş içinde okuyucu hata verirse işin `errorCode`'u aynı PDF kodu olur (normalde olmaz) | İş aşamaları TASK-005'teki gibi kalır; yeniden dene saklanan dosyadan aynı yolu izler; istekten işe nesne taşınmaz | Okunmuş belgeyi işe taşımak: kuyruk bellekte büyük nesne tutar, yeniden denemede yine okuma gerekir |
| 11 | **Tekrar eden üst/alt bilgi:** `PageMarginFilter`. Metinli sayfa sayısı ≥ `ingestion.repeated-line-min-pages` (3) ise: her sayfanın ilk satırının anahtarı (rakam dizileri `#`, boşluklar tek boşluk) metinli sayfaların ≥ `ingestion.repeated-line-min-share` (0.5) kadarında ilk satırsa, o anahtarı taşıyan her ilk satır atılır. Son satır için aynısı. Bu tur her kenarda en fazla `ingestion.repeated-line-max-per-edge` (2) kez uygulanır: bir tur sonunda sayfaların kalan ilk (ya da son) satırları üzerinden aynı eşikle yeniden denenir; turda hiç satır atılmazsa o kenar durur. Böylece sayfa başına en fazla 2 üst + 2 alt satır atılır (Değişiklik 1) | Kural 5; rehberin "Sayfa 7 / 70" alt bilgisi sayı farkına rağmen yakalanır. Chrome baskısı sayfa sonuna iki satır (tarih + başlık, adres + `n/51`) yazar | Sabit regex: rehbere özgü. Sayfa konumu (y koordinatı): `PDFTextStripper` alt sınıfı gerekir |
| 12 | **Kimlikler 10001'den başlar** (`sources_id_seq`, V3, değişmez). Kodda 10001'e bağlı hiçbir şey kalmaz | Lab'daki numara; kural 8 sağlanır | Sequence'i 1'den başlatmak: lab'la ayrışır |
| 13 | **Eski veri kendiliğinden silinmez.** V5 eski rehber satırlarıyla da çalışır (`PDF_NO_SECTIONS` taşıyan iş yok). Temizlik `down -v` ile, dokümanda | Veri silen migration geri alınamaz | Migration'da `DELETE` |
| 14 | **Yeniden dene adresi** `POST .../sources/{sourceId}/ingestion-jobs` (gövdesiz): kaynağa yeni bir içe alma işi oluşturur. Yeni iş son işin (`id` en büyük) `version`, `file_name`, `file_size_bytes` değerlerini ve dosya baytlarını kopyalar (`ingestion_job_files`'a yeni satır). Kaynak `active_version_id` doluysa `SOURCE_ALREADY_INGESTED`, aktif iş varsa ya da yarışta partial index'e takılırsa `INGESTION_IN_PROGRESS` | REST: fiil path'te değil. Her işin kendi dosyası kuralı (TASK-005 Kararlar #5) korunur | `.../retry`: fiil path'te. Dosyayı paylaşmak: şema değişir, iş-dosya ilişkisi bozulur |
| 15 | **Yükleme sınırı:** `spring.servlet.multipart.max-file-size=20MB`, `max-request-size=21MB`, `server.tomcat.max-swallow-size=-1` (20MB = 20 × 1024 × 1024). nginx `/api/workspaces/` location'ında `client_max_body_size 21m` | `-1` olmadan Tomcat büyük gövdede bağlantıyı keser, istemci 413 yerine "connection reset" görür (embedding-pgvector'deki ayar) | nginx varsayılanı 1 MB: web üzerinden yükleme nginx'in HTML 413'üne düşer |
| 16 | **springdoc bu task'ta da eklenmez.** Sözleşme uyumunu qa elle kontrol eder | Yeni dependency yok; TASK-005 Kararlar #19 | Eklemek: TASK-007'de değerlendirilir |

## Sözleşme değişiklikleri
`docs/api/openapi.yaml`:
- Başlık yorumu: TASK-027 durumu; açılışta hiçbir şey içe alınmaz.
- `tags`: `Sources`.
- `GET .../sources` (`listSources`): 200 `SourceResponsePage`; 400 `VALIDATION_FAILED` (`page`/`size`), `BAD_REQUEST`; 404 `WORKSPACE_NOT_FOUND`; 500.
- `POST .../sources` (`createSource`): multipart `CreateSourceRequest` (`file`, `version` zorunlu; `name` isteğe bağlı); 202 + `Location` + `IngestionJobResponse`; 400 `VALIDATION_FAILED` (`file`/`name`/`version`); 404 `WORKSPACE_NOT_FOUND`; 409 `SOURCE_NAME_TAKEN` (her durumda), `EMBEDDING_MODEL_MISMATCH`; 413 `FILE_TOO_LARGE`; 415 `UNSUPPORTED_FILE_TYPE`, `UNSUPPORTED_MEDIA_TYPE`; 422 `PDF_UNREADABLE`, `PDF_NO_TEXT`, `PDF_NO_BOOKMARKS`; 500.
- `POST .../sources/{sourceId}/ingestion-jobs` (`retryIngestion`, yeni): 202 + `Location`; 400 `BAD_REQUEST`; 404 `WORKSPACE_NOT_FOUND`, `SOURCE_NOT_FOUND`; 409 `SOURCE_ALREADY_INGESTED`, `INGESTION_IN_PROGRESS`, `EMBEDDING_MODEL_MISMATCH`; 500.
- `components.schemas`: yeni `SourceKind`, `SourceResponse`, `SourceResponsePage`, `CreateSourceRequest`.
- `IngestionErrorCode`: `PDF_NO_SECTIONS` → `PDF_NO_BOOKMARKS`, `PDF_NO_TEXT` eklendi; açıklama: dosya hataları yüklemede reddedilir.
- `IngestionJobResponse.version`: zorunlu `string` kaldı (açıklama güncellendi). `statusUrl` açıklaması.
- `ProblemDetail.errorCode` enum'u: `SOURCE_NAME_TAKEN`, `EMBEDDING_MODEL_MISMATCH`, `FILE_TOO_LARGE`, `UNSUPPORTED_FILE_TYPE`, `PDF_UNREADABLE`, `PDF_NO_TEXT`, `PDF_NO_BOOKMARKS`, `SOURCE_NOT_FOUND`, `SOURCE_ALREADY_INGESTED`, `INGESTION_IN_PROGRESS`.
- `sse.md`, `websocket.md`: yok.

## Backend planı
Ek skill'ler: `rag-pipeline/references/indexing.md`, `springboot-patterns`, `springboot-security` (dosya türü, boyut), `spring-transactions`, `flyway-migrations`, `postgres-patterns`, `jpa-patterns`.

### Dependency
Yok. Multipart `spring-boot-starter-webmvc`'de; PDFBox 3.0.6 zaten var.

### Migration: `V5__allow_user_uploaded_sources.sql`
- `ALTER TABLE sources DROP CONSTRAINT sources_workspace_id_name_key;` + `CREATE UNIQUE INDEX sources_workspace_id_lower_name_key ON sources (workspace_id, lower(name));`
- `ingestion_jobs` `error_code` CHECK'i düşürülüp yeniden kurulur: `PDF_UNREADABLE, PDF_NO_TEXT, PDF_NO_BOOKMARKS, EMBEDDING_UNAVAILABLE, EMBEDDING_DIMENSION_MISMATCH, EMBEDDING_MODEL_MISMATCH, INGESTION_INTERRUPTED, INTERNAL_ERROR`. Postgres'in verdiği ad (`ingestion_jobs_error_code_check`) backend-coder tarafından `\d ingestion_jobs` ile doğrulanır.
- `version` kolonları, `chapter_count > 0` CHECK'i ve V1-V4 değişmez.

### Properties
Kalkanlar: `ingestion.page-header-pattern`, `ingestion.page-footer-pattern`, `ingestion.bootstrap.*`. Eklenenler (dosya saf ASCII kalır):
```
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=21MB
server.tomcat.max-swallow-size=-1
ingestion.repeated-line-min-pages=3
ingestion.repeated-line-min-share=0.5
ingestion.repeated-line-max-per-edge=2
```

### Sınıflar (kök `com.rasitbank.retrieval`; yeni paketlerde `package-info.java` `@NullMarked`)
**exception/**
- `ValidationFailedException extends BaseException`: 400, `errorCode` sabit `VALIDATION_FAILED`, `List<Violation> getViolations()`; `Violation` record `(String field, String message)` aynı pakette.
- `UnsupportedMediaTypeException extends BaseException`: 415, `(errorCode, message)`.
- `GlobalExceptionHandler`: `handleBaseException` `ValidationFailedException`'da `violations` ekler (`title` "Validation Failed", mevcut `validationFailed` biçimiyle aynı). `handleMaxUploadSizeExceededException` override → 413, `errorCode` `FILE_TOO_LARGE`, `title` "Content Too Large". `parameterName` `@RequestPart` adını da okur.

**pagination/** (yeni)
- `PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)` + `static from(Page<T>)`; `PageParameters` (`MAX_SIZE = 100`, `PAGE_MESSAGE`, `SIZE_MESSAGE`) — `backend-conventions` bölüm 6.

**config/**
- `IngestionProperties`: `Bootstrap`, `pageHeaderPattern`, `pageFooterPattern` çıkar; `int repeatedLineMinPages`, `double repeatedLineMinShare`, `int repeatedLineMaxPerEdge` eklenir.
- `PathConversionConfig`: silinir.

**source/**
- `domain/dto/SourceResponse(long sourceId, long workspaceId, String name, SourceKind kind, Instant createdAt)` + `static from(Source)`.
- `domain/dto/SourceUpload(String name, String version, String fileName, byte[] content)`.
- `repository/SourceRepository`: `findByWorkspaceIdAndName` silinir. Eklenir: `@Query("select s from Source s where s.workspaceId = :workspaceId and lower(s.name) = lower(:name)") Optional<Source> findByLowerName(long workspaceId, String name)`; `Optional<Source> findByIdAndWorkspaceId(long id, long workspaceId)`; `Page<Source> findByWorkspaceIdOrderByIdAsc(long workspaceId, Pageable pageable)`.
- `service/SourceUploadValidator` (`@Component`, constructor `PdfDocumentParser`): `SourceUpload validate(@Nullable MultipartFile file, @Nullable String name, @Nullable String version)`. Kararlar #2, #4-#6. İhlal sırası `file`, `name`, `version`. Mesajlar: `must not be empty`, `file name must be at most 255 characters`, `size must be between 2 and 80`, `must not be blank`, `size must be between 1 and 40`. Baytlar bir kez okunur (`getBytes()`; `IOException` → `UncheckedIOException`, 500). Tür kontrolünden sonra `parser.parse(content)`; sonucu atılır (Kararlar #10).
- `service/SourceService` (`@Service`): `@Transactional(readOnly = true) Page<SourceResponse> listSources(long workspaceId, int page, int size)` (önce `ensureWorkspaceExists`).
- `controller/SourceController` (`@RequestMapping("/api/workspaces/{workspaceId}/sources")`, `@Validated`):
  - `@GetMapping PageResponse<SourceResponse> listSources(@PathVariable long workspaceId, page, size)` (`backend-conventions` bölüm 6 kalıbı).
  - `@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE) ResponseEntity<IngestionJobResponse> createSource(@PathVariable long workspaceId, @RequestPart(name = "file", required = false) @Nullable MultipartFile file, @RequestPart(name = "name", required = false) @Nullable String name, @RequestPart(name = "version", required = false) @Nullable String version)` → `validator.validate` → `ingestionService.createSource(workspaceId, upload)` → `accepted(job)`.
  - `@PostMapping("/{sourceId}/ingestion-jobs") ResponseEntity<IngestionJobResponse> retryIngestion(@PathVariable long workspaceId, @PathVariable long sourceId)` → `ingestionService.retryIngestion(workspaceId, sourceId)` → `accepted(job)`.
  - `accepted(job)` = `ResponseEntity.accepted().location(URI.create(job.statusUrl())).body(job)`.

**ingestion/**
- `service/IngestionService`:
  - `queueIngestion` silinir.
  - `@Transactional IngestionJobResponse createSource(long workspaceId, SourceUpload upload)`: (1) `workspaceService.ensureWorkspaceExists` (404); (2) `workspaceService.ensureEmbeddingModelMatches(workspaceId, embeddingProperties.modelLabel())` (409); (3) `findByLowerName` doluysa → `ConflictException("SOURCE_NAME_TAKEN")`; (4) `Source.create(workspaceId, upload.name(), PDF, now)` + `saveAndFlush`; `DataIntegrityViolationException` → `SOURCE_NAME_TAKEN`; (5) `IngestionJob.queued(...)` `saveAndFlush`, `IngestionJobFile` yazılır; (6) commit sonrası `ingestionJobRunner.submit(jobId, content)`.
  - `@Transactional IngestionJobResponse retryIngestion(long workspaceId, long sourceId)`: (1) `ensureWorkspaceExists`; (2) `sourceRepository.findByIdAndWorkspaceId` yoksa `NotFoundException("SOURCE_NOT_FOUND")`; (3) model kontrolü; (4) `source.hasActiveVersion()` → `ConflictException("SOURCE_ALREADY_INGESTED")`; (5) `existsBySourceIdAndStageIn(activeStages)` → `ConflictException("INGESTION_IN_PROGRESS")`; (6) `ingestionJobRepository.findFirstBySourceIdOrderByIdDesc(sourceId)` ve onun dosyası; yeni iş Kararlar #14 ile `saveAndFlush` (`DataIntegrityViolationException` → `INGESTION_IN_PROGRESS`); (7) commit sonrası `submit`.
  - `getIngestionJob`, `failInterruptedJobs` değişmez. Ortak `queueJob(...)` özel metodu iki yolda kullanılabilir.
- `repository/IngestionJobRepository`: `Optional<IngestionJob> findFirstBySourceIdOrderByIdDesc(long sourceId)` eklenir.
- `service/IngestionStartupListener`: constructor sadece `IngestionService`; `onApplicationReady()` sadece `failInterruptedJobs()` (try/catch + `ERROR` log kalır).
- `service/GuideBootstrap`, `service/GuideBootstrapResult`: silinir.
- `service/IngestionJobRunner`: model kontrolü `WorkspaceService.ensureEmbeddingModelMatches`'e taşınır; başka değişiklik yok.
- `parsing/PdfDocumentParser`: constructor `(PageMarginFilter pageMarginFilter)`; `ParsedDocument parse(byte[] content)`, kurallar Kararlar #9. `noSections()` yerine `noBookmarks()` (`PDF_NO_BOOKMARKS`), yeni `noText()` (`PDF_NO_TEXT`).
- `parsing/PageMarginFilter` (`@Component`, constructor `IngestionProperties`): `List<List<String>> removeRepeatedMargins(List<List<String>> pageLines)`; giriş/çıkış aynı sayfa sırası ve sayısı; algoritma Kararlar #11; `static String marginKey(String line)`.
- `chunking/TextChunker`, `ChunkDraft`: değişmez.

**workspace/**
- `service/WorkspaceService`: `void ensureEmbeddingModelMatches(long workspaceId, String configuredModelLabel)` → uyuşmazsa `ConflictException("EMBEDDING_MODEL_MISMATCH", ...)`.

### Loglar (İngilizce, `key=value`; dosya adı, kaynak adı, sürüm etiketi, başlık, parça metni ve dosya içeriği loglanmaz)
- `INFO Upload rejected errorCode={} fileSizeBytes={}` (422 ve 415'te, validator'da)
- `INFO Source created sourceId={} workspaceId={}`
- `INFO Ingestion job queued jobId={} sourceId={} fileSizeBytes={}` (mevcut satırdan `version` çıkar)
- `INFO Ingestion retry queued jobId={} sourceId={} previousJobId={}`
- `INFO Document parsed jobId={} pageCount={} chapterCount={} sectionCount={} removedMarginLines={}`
- Mevcut: `Document chunked … avgTokens`, `Ingestion job finished … durationMs`, `Ingestion job failed …`, `Interrupted ingestion jobs marked failed count={}`. `Guide bootstrap …` satırları silinir.

### `retrieval-api/CLAUDE.md` (backend-coder)
- Durum yorumu ve "Komutlar"daki açılış içe alması cümleleri kalkar; yerine yükleme örneği `curl -s -F file=@../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf -F name='Raşit Bankası Ürün ve Hizmet Rehberi' -F version=2026.10 localhost:8080/api/workspaces/42/sources` ve yeniden dene `curl -s -X POST localhost:8080/api/workspaces/42/sources/<id>/ingestion-jobs`.
- İçe alma ayarları: bootstrap ve kalıplar çıkar; `repeated-line-*` ve multipart ayarları girer. `PathConversionConfig` notu çıkar.
- PDF okuma paragrafı: genel okuyucu, yükleme anında ret (Kararlar #9-#11).
- Paket yapısı: `source/controller`, `source/service`, `pagination/`; silinen sınıflar.
- "Henüz olmayanlar"dan `PageResponse` ve yükleme ucu çıkar. Testte bootstrap cümleleri çıkar.

## Frontend planı
Yok (frontend-coder atlandı). Yeni `errorCode`'ların Türkçe mesajları ve "Yeniden dene" düğmesi TASK-007'de.

## Test senaryoları
Fixture'lar: `PdfFixtures` (PDFBox ile üretilir: numaralı/numarasız yer imli, yer imsiz, üst/alt bilgili, sadece görüntülü sayfa, şifreli), `SourceFixtures`. Sahte embedding `@MockitoBean EmbeddingClient` (TASK-005'teki deterministik vektör). Gerçek rehber: `../docs/rehber/pdf/Rasit-Bankasi-Urun-Hizmet-Rehberi-2026.10.pdf` (sadece testte okunur). Silinen testler Özet'te. Mevcut testler yeni imzalara göre güncellenir (`IngestionJobRunnerTest`, `IngestionServiceTest`, `IngestionStartupListenerTest`, `PdfDocumentParserTest`, `PdfDocumentParserGuideTest`).

| # | Senaryo | Katman | Test adı (given_when_then) | Beklenen sonuç (somut değerle) |
|---|---------|--------|----------------------------|--------------------------------|
| T1 | Ad strip | unit `SourceUploadValidatorTest` (gerçek `PdfDocumentParser`, yer imli fixture) | `givenNameWithSurroundingSpaces_whenValidate_thenNameIsStripped` | `"  Şirket El Kitabı  "` → `"Şirket El Kitabı"` |
| T2 | Boş ad → dosya adı | unit | `givenBlankName_whenValidate_thenNameIsFileNameWithoutExtension` | name `"   "`, dosya `Atlaskap-El-Kitabi.pdf` → `Atlaskap-El-Kitabi` |
| T3 | Türkçe dosya adı, büyük uzantı | unit | `givenTurkishFileNameWithUpperCaseExtension_whenValidate_thenAcceptedAndNameKeepsTurkishLetters` | `Şirket El Kitabı.PDF`, name yok → `Şirket El Kitabı`, `fileName` `Şirket El Kitabı.PDF` |
| T4 | Uzun dosya adı | unit | `givenFileNameOf90CharsAndNoName_whenValidate_thenNameIsFirst80Chars` | 90 `a` + `.pdf` → ad 80 `a` |
| T5 | Kısa türetilmiş ad | unit | `givenFileNameA_whenValidate_thenThrowsValidationFailedOnName` | `a.pdf` → `ValidationFailedException`, `VALIDATION_FAILED`, ihlal `name` / `size must be between 2 and 80` |
| T6 | Ad sınırları | unit | `givenNamesOf1_2_80_81Chars_whenValidate_thenOnly2And80Accepted` | 1 ve 81 → `name` ihlali; 2 ve 80 kabul (Türkçe karakterli 80: `ş` × 80) |
| T7 | Boş / eksik dosya | unit | `givenEmptyOrMissingFile_whenValidate_thenThrowsValidationFailedOnFile` | 0 bayt ve `null` → ihlal `file` / `must not be empty` |
| T8 | Uzantı | unit | `givenTxtFile_whenValidate_thenThrowsUnsupportedFileType` | `notlar.txt` (PDF baytları olsa bile) → `UnsupportedMediaTypeException`, `UNSUPPORTED_FILE_TYPE` |
| T9 | İmza | unit | `givenPdfExtensionWithoutPdfSignature_whenValidate_thenThrowsUnsupportedFileType` | `sahte.pdf`, içerik `merhaba` → `UNSUPPORTED_FILE_TYPE`. İmza 600. baytta → kabul |
| T10 | Sürüm | unit | `givenVersionMissingBlankOr41Chars_whenValidate_thenViolationOnVersion` | `null` ve `"  "` → `version` / `must not be blank`; 41 karakter → `size must be between 1 and 40`; `" 2026.10 "` → `2026.10`; 40 karakter kabul |
| T11 | Başarılı yükleme | controller `SourceControllerTest` (`@WebMvcTest`, `@MockitoBean IngestionService`, `@MockitoBean SourceUploadValidator`) | `givenValidUpload_whenCreateSource_thenReturns202WithLocationAndJob` | 202, `Location` `/api/workspaces/42/ingestion-jobs/7`, `stage` `QUEUED`, `statusUrl` aynı |
| T12 | Ad dolu | controller | `givenSourceNameTaken_whenCreateSource_thenReturns409SourceNameTaken` | 409, `application/problem+json`, `SOURCE_NAME_TAKEN` |
| T13 | Doğrulama gövdesi | controller (gerçek validator `@Import` edilen ayrı test sınıfı `SourceControllerValidationTest`) | `givenEmptyFileAnd81CharNameAndNoVersion_whenCreateSource_thenReturns400WithThreeViolations` | 400, `VALIDATION_FAILED`, `violations` = [`file`, `name`, `version`] sırayla; servis çağrılmadı |
| T14 | Multipart değil | controller | `givenJsonBody_whenCreateSource_thenReturns415UnsupportedMediaType` | 415, `UNSUPPORTED_MEDIA_TYPE` |
| T15 | Tür | controller `SourceControllerValidationTest` | `givenTxtFile_whenCreateSource_thenReturns415UnsupportedFileType` | 415, `UNSUPPORTED_FILE_TYPE` |
| T16 | Çalışma alanı | controller | `givenWorkspace43_whenCreateSource_thenReturns404WorkspaceNotFound` | 404, `WORKSPACE_NOT_FOUND` |
| T17 | Model | controller | `givenModelMismatch_whenCreateSource_thenReturns409EmbeddingModelMismatch` | 409, `EMBEDDING_MODEL_MISMATCH` |
| T18 | Liste | controller | `givenNoSources_whenListSources_thenReturnsEmptyPage` / `givenPageMinus1_whenListSources_thenReturns400OnPage` / `givenWorkspace43_whenListSources_thenReturns404` | `content` [], `totalElements` 0, `page` 0, `size` 20; 400 `violations[0].field` `page`; 404 `WORKSPACE_NOT_FOUND` |
| T19 | 422 gövdesi | controller `SourceControllerValidationTest` | `givenPdfWithoutBookmarks_whenCreateSource_thenReturns422PdfNoBookmarksAndServiceNotCalled` | 422, `application/problem+json`, `PDF_NO_BOOKMARKS`; `IngestionService` hiç çağrılmadı |
| T20 | Boyut sınırı | integration `SourceUploadHttpIntegrationTest` (`RANDOM_PORT`, Testcontainers, sahte embedding) | `givenFileOf20MiBPlusOneByte_whenPostSources_thenReturns413FileTooLarge` | 413, `application/problem+json`, `FILE_TOO_LARGE`; `sources` 0. 20 × 1024 × 1024 baytlık dosya 413 almaz (imza + dolgu → 422 `PDF_UNREADABLE`) |
| T21 | Rehber uçtan uca | integration `SourceUploadIntegrationTest` | `givenFreshDatabase_whenCreateSourceWithGuide_thenJobDoneSource10001With154Chunks` | iş `DONE`; `sources` 1, id 10001, ad verilen; `source_versions.version` `2026.10`, `page_count` 70, `chapter_count` 11, `chunk_count` 154; dosya baytları aynı; her parça 1024 boyut |
| T22 | Yer imli başka PDF | integration | `givenPdfWithUnnumberedBookmarks_whenCreateSource_thenJobDoneChunksCarryBookmarkHeadings` | `DONE`, `chapter_count` 2; parça başlıkları {`Giriş`, `Teslimat`, `Gecikmeler`} içinde; `page_number` 1..3 |
| T23 | Taranmış PDF | integration | `givenImageOnlyPdf_whenValidateAndCreate_thenRejectedPdfNoTextAndNothingWritten` | `UnprocessableContentException` `PDF_NO_TEXT`; `sources` 0, `ingestion_jobs` 0 |
| T23b | Yer imsiz PDF, ad boş kalır | integration | `givenPdfWithoutBookmarksRejected_whenFixedPdfUploadedWithSameName_thenAccepted` | İlk yükleme `PDF_NO_BOOKMARKS`, `sources` 0; aynı adla yer imli PDF → iş `DONE`, `sources` 1 |
| T23c | Bozuk PDF | integration | `givenPdfSignatureButCorruptBody_whenValidate_thenRejectedPdfUnreadableAndNothingWritten` | `PDF_UNREADABLE`; `sources` 0, `ingestion_jobs` 0 |
| T24 | Ad harf/boşluk farkı | integration | `givenDoneSourceRehber_whenCreateSourceNamedSpacedUpperRehber_thenSourceNameTakenAndNothingWritten` | `" REHBER "` → `ConflictException` `SOURCE_NAME_TAKEN`; `sources` 1, `ingestion_jobs` 1 |
| T26 | Başarısız kaynağın adı | integration | `givenSourceWhoseOnlyJobFailed_whenCreateSourceWithSameName_thenSourceNameTaken` | 409 `SOURCE_NAME_TAKEN`; `sources` 1, `ingestion_jobs` 1 |
| T27 | Yarış | unit `IngestionServiceTest` | `givenUniqueIndexRejectsSource_whenCreateSource_thenThrowsSourceNameTaken` | `saveAndFlush` `DataIntegrityViolationException` → `ConflictException`, `SOURCE_NAME_TAKEN`; runner çağrılmadı |
| T28 | Art arda iki yükleme | integration | `givenTwoUploadsBackToBack_whenJobsRun_thenBothDoneOneAfterAnother` | iki farklı ad; iki iş `DONE`; ikinci işin `started_at` ≥ ilkinin `finished_at` |
| T29 | Model farkı yazmaz | unit `IngestionServiceTest` | `givenConfiguredModelDiffers_whenCreateSource_thenThrowsEmbeddingModelMismatchAndSavesNothing` | `EMBEDDING_MODEL_MISMATCH`; `sourceRepository`, `ingestionJobRepository` hiç `save` görmedi |
| T30 | Çalışma alanı | integration (mevcut) | `givenFreshDatabase_whenMigrated_thenWorkspace42WithBgeM3At1024Exists` | Değişmeden geçer |
| T31 | Temiz açılış | integration `StartupIntegrationTest` | `givenFreshDatabase_whenApplicationStarted_thenNoSourceVersionChunkOrJobExists` | Context açıldı; `sources`, `source_versions`, `chunks`, `ingestion_jobs` 0; `workspaces` 1 |
| T32 | Yeniden başlatma | integration | `givenDoneSource_whenOnApplicationReadyAgain_thenCountsUnchangedAndNoNewJob` | Rehber yüklenmiş; `listener.onApplicationReady()` → `sources` 1, `ingestion_jobs` 1, `chunks` 154 |
| T33 | Açılış tek iş yapar | unit `IngestionStartupListenerTest` | `givenApplicationReady_whenOnApplicationReady_thenOnlyFailsInterruptedJobs` | `failInterruptedJobs` 1 kez; başka etkileşim yok. Hata fırlarsa log, exception dışarı çıkmaz (mevcut test kalır) |
| T34 | Index | integration (`@DataJpaTest` + Testcontainers) | `givenSourceRehber_whenSourceREHBERSaved_thenUniqueIndexRejects` | `DataIntegrityViolationException` |
| T35 | Numarasız yer imleri | unit `PdfDocumentParserTest` | `givenUnnumberedBookmarks_whenParse_thenSectionsFollowBookmarkTitles` | `Giriş` (s.1), `Teslimat` (s.2), altında `Gecikmeler` (s.2) → başlıklar `Giriş`, `Teslimat`, `Gecikmeler`; `chapterCount` 2 |
| T36 | Sayfada olmayan yer imi başlığı | unit | `givenBookmarkTitleNotPrintedOnItsPage_whenParse_thenSectionStartsAtTopOfThatPage` | `Bölüm B` yer imi s.2'de ama metinde yok → s.2'nin ilk satırı `Bölüm B` kısmında; s.1'in satırları önceki kısımda |
| T37 | Yer imi öncesi sayfalar | unit (mevcut T10 uyarlanır) | `givenCoverAndTocBeforeFirstBookmark_whenParse_thenThosePagesAreSkipped` | Kapak (s.1) ve `İçindekiler` (s.2) satırları yok; ilk kısım s.3 |
| T38 | Sayıyla başlayan satır | unit (mevcut) | `givenBodyLinesStartingWithNumbers_whenParse_thenOnlyBookmarkedLinesBecomeHeadings` | Değişmeden geçer |
| T39 | SSS | unit (mevcut) | `givenFaqLineAndChapterWithoutSubsections_whenParse_thenHeadingsAreChapterDotFaqAndChapterTitle` | Değişmeden geçer |
| T40 | Uzun başlık | unit | `givenBookmarkTitleOf250Chars_whenParse_thenHeadingCutAtWhitespaceBefore200WithEllipsis` | başlık ≤ 200 karakter, `…` ile biter |
| T41 | Üst/alt bilgi | unit `PageMarginFilterTest` | `givenSameFirstLineAndNumberedLastLineOnAllFourPages_whenRemove_thenBothDroppedEverywhere` | 4 sayfa, ilk satır `Atlaskap Lojistik · El Kitabı`, son satır `Sayfa N / 4` → hepsi atıldı; gövde satırları aynı |
| T41b | İki satırlı alt bilgi | unit | `givenTwoRepeatedLastLinesOnAllPages_whenRemove_thenBothDroppedButThirdRepeatedLineKept` | 4 sayfa, son üç satır: `Ortak not` (hepsinde aynı), `31.10.2026 18:05 RAG: Dil Modeline Kendi Verinle Cevap Verdirmek`, `file:///x/rag.html N/4` → son ikisi atılır, `Ortak not` kalır (en fazla 2). Üst kenar için aynısı. Rehberin sondan ikinci satırı sayfadan sayfaya farklı olduğu için atılmaz (T50 154 kalır) |
| T42 | Eşik %50 | unit | `givenFirstLineOnTwoOfFourPages_whenRemove_thenDropped` / `givenFirstLineOnOneOfFourPages_whenRemove_thenKept` | 2/4 atılır; 1/4 kalır |
| T43 | Kısa belge | unit | `givenTwoPagesWithSameFirstLine_whenRemove_thenNothingDropped` | 2 sayfa → değişmez |
| T44 | Anahtar | unit | `givenLinesDifferingOnlyInDigitsAndSpaces_whenMarginKey_thenKeysEqual` | `Yürürlük: 1 Ekim 2026 · Sayfa 7 / 70` ile `Yürürlük: 1 Ekim 2026 ·  Sayfa 12 / 70` aynı; `Sayfa 7` ile `Bölüm 7` farklı |
| T45 | Metin yok | unit | `givenPdfWithOnlyImagePages_whenParse_thenThrowsPdfNoText` | `UnprocessableContentException`, `PDF_NO_TEXT` (yer imi olsa da olmasa da) |
| T45b | Yer imi yok | unit | `givenPdfWithTextButNoBookmarks_whenParse_thenThrowsPdfNoBookmarks` | `PDF_NO_BOOKMARKS`. Büyük ve kalın yazılmış satırlar da başlık sayılmaz. Başlığı boş tek yer imi olan PDF → aynı kod |
| T46 | Yer imsiz rehber | unit `PdfDocumentParserGuideTest` | `givenGuideWithBookmarksRemoved_whenParse_thenThrowsPdfNoBookmarks` | Testte yer imleri silinmiş rehber → `PDF_NO_BOOKMARKS` |
| T47 | Bozuk / şifreli PDF | unit (mevcut + ek) | `givenBytesThatAreNotAPdf_whenParse_thenThrowsPdfUnreadable` / `givenEncryptedPdf_whenParse_thenThrowsPdfUnreadable` | `PDF_UNREADABLE` (`PDF_NO_SECTIONS` testi silinir) |
| T49 | Rehber kalitesi | unit `PdfDocumentParserGuideTest` (mevcut T15 uyarlanır) | `givenGuide202610_whenParse_thenSeventyPagesElevenChaptersAndEveryTocSubsectionIsASection` | TASK-005 T15'in bütün beklentileri, kalıp ayarı olmadan |
| T50 | TASK-005 ile aynı parçalar | unit | `givenGuide202610_whenParseAndChunk_then154ChunksWithAverage285Tokens` | 154 parça; `round(avg(tokenCount))` 285; ilk parçanın sayfası 5 |
| T51 | Log sınırı | integration (`OutputCaptureExtension`) | `givenUploadedAndRejectedPdfs_whenLogsCaptured_thenIdsAndCountsLoggedButNoNamesOrText` | Logda `Source created sourceId=`, `Upload rejected errorCode=PDF_NO_BOOKMARKS`, `Document parsed jobId=`; `Atlaskap` (kaynak ve dosya adı), sürüm etiketi ve fixture'daki gövde cümlesi yok |
| T52 | Yeniden dene uçtan uca | integration `RetryIngestionIntegrationTest` | `givenFirstJobFailedEmbeddingUnavailable_whenRetryIngestion_thenSameSourceNewJobDone` | Sahte istemci önce `EMBEDDING_UNAVAILABLE`; iş 1 `FAILED`. Sonra başarılı; `retryIngestion` → iş 2 `DONE`; `sources` 1 (aynı id); iş 2'nin `version`, `file_name`, dosya baytları iş 1'inkiyle aynı; `chunks` sadece iş 2'nin sürümüne ait |
| T53 | Kesilmiş işi yeniden dene | integration | `givenJobInterrupted_whenRetryIngestion_thenNewJobDone` | `INGESTION_INTERRUPTED` iş sonrası → iş 2 `DONE` |
| T54 | Tamamlanmış kaynak | integration | `givenDoneSource_whenRetryIngestion_thenSourceAlreadyIngestedAndNoNewJob` | `ConflictException` `SOURCE_ALREADY_INGESTED`; `ingestion_jobs` 1 |
| T55 | İş sürerken | unit `IngestionServiceTest` | `givenActiveJob_whenRetryIngestion_thenThrowsIngestionInProgress` | `INGESTION_IN_PROGRESS`; `save` yok |
| T56 | Yarış | unit | `givenActiveJobIndexRejectsNewJob_whenRetryIngestion_thenThrowsIngestionInProgress` | `saveAndFlush` `DataIntegrityViolationException` → `INGESTION_IN_PROGRESS` |
| T57 | Bilinmeyen kaynak | controller | `givenUnknownSource_whenRetryIngestion_thenReturns404SourceNotFound` / `givenWorkspace43_whenRetryIngestion_thenReturns404WorkspaceNotFound` | 404 `SOURCE_NOT_FOUND`; 404 `WORKSPACE_NOT_FOUND` |
| T58 | Yeniden dene 202 | controller | `givenRetryAccepted_whenRetryIngestion_thenReturns202WithLocation` / `givenSourceAlreadyIngested_whenRetryIngestion_thenReturns409` | 202, `Location` `/api/workspaces/42/ingestion-jobs/8`; 409 `SOURCE_ALREADY_INGESTED` |

**Elle çalıştırılanlar (gerçek PDF + gerçek `bge-m3`):**
- **Deneme belgeleri** (scratch klasöründe, repoya eklenmez): `kaynaklar/rag.html`'in Chrome ile basılmış iki hâli.
  - **Belge B (yer imli):** `"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless --generate-pdf-document-outline --print-to-pdf=<scratch>/rag-anlatim-yer-imli.pdf "file://$PWD/kaynaklar/rag.html"`.
  - **Belge C (yer imsiz):** aynı komut, `--generate-pdf-document-outline` olmadan.
  - Chrome tarayıcı üst/alt bilgisi (tarih, başlık, adres, `1/12`) basar; genel ayıklama bununla da denenir. Chrome yoksa ya da Belge B'de yer imi çıkmazsa (PDFBox ile kontrol), backend-coder lead'e bildirir; lead kullanıcıdan yer imli bir PDF ister.
- **M0 (backend-coder, lokal):** dev postgres boş (`down -v` + `up -d`), Ollama açık, servis açık. Açılışta `ingestion_jobs` 0. Rehber 2026.10 ve Belge B `curl -F … -F version=…` ile yüklenir; ikisi `DONE`. Belge C → 422 `PDF_NO_BOOKMARKS`, `GET .../sources`'ta yok. Her kabul edilen belge için: parça sayısı, `avg(token_count)`, süre (`finished_at - started_at`), `chapter_count`, `removedMarginLines` (log). Rehber için TASK-005'le fark (154 parça, ort. 285 token, 8,96 sn). Belge B'de tarayıcı üst ve alt bilgisinin parçalara girmediği iki sorguyla gösterilir: `content ~ 'file:///'` ve `content ~ '\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}'` olan parça sayısı 0 (sorgu Belge B'nin `source_id`'siyle sınırlanır).
- **M1 (devops, Docker):** `down -v` + `up --build -d`. `curl localhost:8080/api/workspaces/42/sources` → `content: []`, `totalElements: 0`; `curl localhost:8080/api/workspaces/42/ingestion-jobs/1` → 404 `INGESTION_JOB_NOT_FOUND`. Rehber 8080'den, Belge B 3000'den (nginx) yüklenir; ikisi `DONE`. Belge C → 422. M0 sayıları raporda.
- **M2 (devops):** `restart retrieval-api` → `sources` 2, `ingestion_jobs` 2, `chunks` aynı.
- **M3 (devops):** Ollama'ya ulaşılamayan override dosyasıyla (`deployment/CLAUDE.md`, TASK-005) üçüncü bir yer imli belge yüklenir → iş `FAILED` `EMBEDDING_UNAVAILABLE`. Override kaldırılıp servis yeniden açılınca `curl -X POST …/sources/<id>/ingestion-jobs` → yeni iş `DONE`, kaynak tek. Ayrıca 3000 üzerinden 20 MiB + 1 baytlık `.pdf` → 413 `application/problem+json` `FILE_TOO_LARGE` (nginx'in HTML'i değil).
- **M4 (qa):** rehberden ve Belge B'den `order by random() limit 5` parça; başlık, sayfa ve metin PDF görüntüleyicide elle karşılaştırılır.
- **M5 (qa):** iz araması `grep -rniE 'bootstrap|GuideBootstrap|INGESTION_BOOTSTRAP|/app/rehber|docs/rehber/pdf|page-header-pattern|page-footer-pattern|PDF_NO_SECTIONS|10001' retrieval-api/src/main deployment retrieval-api/Dockerfile` → 0 satır (V3'teki `START WITH 10001` hariç). Loglarda kaynak adı, dosya adı, sürüm etiketi ve parça metni yok.

## Deployment planı
- **`deployment/docker-compose.yml`, `retrieval-api`:** `INGESTION_BOOTSTRAP_FILE` ve `volumes: ../docs/rehber/pdf:/app/rehber:ro` kalkar. Diğerleri aynı.
- **`web/nginx.conf`:** `location /api/workspaces/` içine `client_max_body_size 21m;`.
- **`deployment/CLAUDE.md`:** "Gerçek yapı (TASK-005)"teki açılış içe alması maddeleri kalkar; "Gerçek yapı (TASK-027)": açılışta bilgi bankası boş; yükleme `curl -F` örneği (8080 ve 3000; `version` zorunlu); yeniden dene komutu; TASK-005'ten kalan rehber verisinin (kaynak 10001, parçaları) temizliği: tam sistem `docker compose -f deployment/docker-compose.yml down -v`, geliştirme `docker compose -f deployment/docker-compose.dev.yml down -v`; nginx yükleme sınırı.
- **`ollama.md` §8, kök `README.md`:** "ilk açılışta rehber kendiliğinden içe alınır" cümleleri kalkar; yerine yükleme komutu, durum sorgusu ve Ollama kapalıyken yüklenen belgenin yeniden denenmesi.
- **Lokal model:** yeni model yok (`bge-m3` TASK-003'ten).
- **Healthcheck:** değişmez.

## Açık sorular ve riskler
1. **Kart soruları:** Kullanıcı 5 soruyu cevapladı (Özet). Yeni soru yok.
2. **Üst/alt bilgi sezgisi (risk):** Sayfaların yarısında aynı olan gerçek bir ilk satır (ör. her sayfada tekrarlanan tablo başlığı) üst bilgi sanılıp atılır. Üç ve daha fazla satırlı üst/alt bilginin fazlası parçalarda kalır.
3. **Yer imi öncesi sayfalar (risk):** İlk yer iminden önceki sayfalardaki gerçek içerik atlanır.
4. **Yükleme süresi (risk):** PDF istek sırasında okunduğu için büyük bir belgede 202 birkaç saniye gecikebilir.
5. **Rehberin parça sayısı (risk):** T50 TASK-005'teki 154 parçayı bekliyor. Rehberde numarasız bir yer imi ya da ilk satırı üst bilgi olmayan bir içerik sayfası varsa sayı değişebilir. Değişirse backend-coder nedeni raporuna yazar ve planner'a bildirir; beklenti sessizce değiştirilmez.
6. **Türkçe büyük/küçük harf (risk):** `lower()` veritabanının kuralıyla çalışır; "İstanbul" ile "istanbul" farklı ad sayılabilir.
7. **Kapsam dışı not:** `.claude/skills/rag-pipeline/references/indexing.md` ve `rag.md` hâlâ rehberin açılışta girdiği varsayımını ya da lab sayılarını anıyor olabilir; bunlar kullanıcının dosyaları.
8. **Kapsam dışı not:** Kaynak silme olmadığı için deneme yüklemeleri ancak `down -v` ile temizlenir.

## Değişiklik 1 (M0 bulgusu, 2026-10-31)
backend-coder M0'da buldu: Chrome baskısı (Belge B) her sayfanın sonuna iki satır yazıyor; PDFBox bu sırayla veriyor: sondan ikinci "31.10.2026 18:05 RAG: …" (tarayıcının üst bilgisi), son "file:///…/rag.html 3/51". Sayfa başına tek satır kuralı sadece sonuncuyu attı (`removedMarginLines=51`); 66 parçanın 48'inde tarih + başlık satırı kaldı. Planın M0 sorgusu bunu göstermiyordu. Rehber etkilenmedi: 154 parça, ort. 285 token, 8,44 sn.
- **Kararlar #11:** kural her kenarda en fazla `ingestion.repeated-line-max-per-edge` (2) tur uygulanır. Kart kararı değiştiği için kart yeniden onaya açıldı (`ONAY: BEKLİYOR`).
- **Properties / `IngestionProperties`:** `repeated-line-max-per-edge=2`, `int repeatedLineMaxPerEdge`.
- **Test:** T41b eklendi. T50 (rehber 154 parça) değişikliğin rehberi bozmadığını korur.
- **M0:** Belge B kontrolü iki sorguya çevrildi (`file:///` ve tarih-saat satırı yok).
- **Risk 2:** "iki satırlı üst bilgi" riski "üç ve daha fazla satır" olarak daraldı.
- Sözleşme değişmez.

STATUS: DONE
