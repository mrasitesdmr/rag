TASK: TASK-005
# QA raporu

## Kullanılan skill'ler
`code-review-and-quality`, `java-code-review`, `springboot-verification`, `rag-pipeline` (+ indexing). `backend-conventions` kuralları (yorum politikası, Lombok yasağı, loglama sınırı) grep ve elle okumayla kontrol edildi. Web değişmedi; web skill'leri ve Playwright uygulanamaz.

## Kural kontrolü
| # | Kural / Kabul kriteri | Kanıt (kod + test) | Sonuç ✅/❌ |
|---|---|---|---|
| 1 | Tek çalışma alanı 42, `bge-m3@1024` | `V2__create_workspaces_table.sql`; canlı DB: parçaların `embedding_model` tek değer `bge-m3@1024`; `WorkspaceMigrationIntegrationTest` | ✅ |
| 2 | Aşamalar `QUEUED→…→DONE`, `FAILED` + `errorCode`; GET ucu: aşama, ilerleme, parça sayısı, 3 zaman damgası | `IngestionJob`, `IngestionJobLifecycleService`, `IngestionJobController`. Canlı: `GET /api/workspaces/42/ingestion-jobs/1` → DONE, progress 1.0, 154/154, createdAt/startedAt/finishedAt. Testler: `IngestionJobTest`, `IngestionJobRunnerTest`, `IngestionJobControllerTest` | ✅ |
| 3 | İş arka planda; yarıda kalan iş unutulmaz (kart: `FAILED` + `INGESTION_INTERRUPTED`) | `IngestionJobRunner.submit` (afterCommit, tek thread'li executor), `IngestionService.failInterruptedJobs`. **Gerçek süreç kesintisi denendi** (aşağıda, Uçtan uca). Test: `InterruptedJobsIntegrationTest` | ✅ |
| 4 | Parçalama 1.600 / 2.000 / 200; başlık + sayfa + sıra + token; tablo satırı bölünmez | `TextChunker`; testler `TextChunkerTest` (sınır değerleri 1.600/1.601, 2.000/2.001, tam 200 örtüşme). Canlı: `char_length` min 205, maks 1.599; 154 parça; `chunk_no` 0'dan | ✅ |
| 5 | 96'lık gruplar, en fazla 2 eşzamanlı istek, 1024 boyut, farklı boyutta `FAILED` | `IngestionJobRunner.embedInBatches` (gruplar 96+58, logda görüldü), `EmbeddingClient` (Semaphore 2, 3 deneme). Testler `EmbeddingClientTest` (T17-T22), `IngestionJobRunnerTest#given150Chunks…`, `GuideBootstrapIntegrationTest#givenEmbeddingReturnsWrongDimension…`. Canlı: `vector_dims <> 1024` = 0 | ✅ |
| 6 | HNSW cosine | Canlı `pg_indexes`: `CREATE INDEX chunks_embedding_hnsw_idx … USING hnsw (embedding vector_cosine_ops)`; `WorkspaceMigrationIntegrationTest#…IndexesRead…` | ✅ |
| 7 | İlk giriş (kart: açılışta kendiliğinden); elle/seed parça yok | `GuideBootstrap` → `IngestionService.queueIngestion` → runner. Temiz DB'de açılışta `Guide bootstrap result=JOB_QUEUED`; tabloya elle yazan kod yok (V2-V4'te sadece çalışma alanı satırı) | ✅ |
| 8 | Yeniden açılışta mükerrer yok; kaynak 10001 | `sources_id_seq START 10001`, `UNIQUE(workspace_id,name)`, `GuideBootstrap` ada göre arar. Canlı: `restart retrieval-api` sonrası sources 1, ingestion_jobs 1, chunks 154. Testler `GuideBootstrapIntegrationTest#givenGuideAlreadyIngested…` | ✅ |
| 9 | PDF ve özellikleri saklanır | `ingestion_job_files.content` (bytea), `source_versions` (dosya adı, bayt, sayfa, bölüm sayısı). Canlı: 1.120.236 bayt, 70 sayfa, 11 bölüm. Test T31 bayt bayt karşılaştırır | ✅ |
| 10 | Sürüm etiketi; "Son senkron" = bitiş zamanı | `source_versions.synced_at` = işin `finished_at` (aynı `Instant`); T31 | ✅ |
| 11 | Parça içeriği loglanmaz | Canlı konteyner logu: parça/başlık metni araması (Kayıp, Vadesiz, kredi kart, Şifrenizi, limit emri, Eşiniz) = 0 eşleşme. Loglarda sadece jobId, sourceId, aşama, sayı, süre. Test `GuideBootstrapIntegrationTest#givenGuideIngested_whenLogsCaptured…` | ✅ |
| KK1 | Gerçek PDF + gerçek bge-m3 uçtan uca; parça sayısı, ort. token, süre | Benim koşum (temiz DB, Docker): 154 parça, ort. token 285,4 (min 60, maks 408), iş süresi ~7,5 sn (`startedAt` 12:45:16.26 → `finishedAt` 12:45:23.75), 2 embedding grubu (4,6 sn + 2,5 sn) | ✅ |
| KK2 | Rastgele parçaların sayfa/başlığı PDF'le elle karşılaştırıldı | Aşağıdaki tablo | ✅ |
| KK3 | Parçalama, durum geçişi, hata testleri | 104 test; PIT ile kuralların doğrulandığı görüldü (aşağıda) | ✅ |
| KK4 | `./mvnw verify` geçer | Kendim çalıştırdım, aşağıda | ✅ |

### Rastgele 5 parça (`order by random() limit 5`) ↔ PDF (`pdftotext -f N -l N`)
| Parça (chunk_no) | Veritabanı: sayfa / başlık | PDF'te bulunan | Sonuç |
|---|---|---|---|
| 7 | s. 8 / `2.3 Ek kart` | s. 8'de "2.3 Ek kart" başlığı ve "Eşiniz, anneniz, babanız…" metni; alt bilgi "Sayfa 8 / 70" | ✅ |
| 98 | s. 46 / `6.3 Şifre sıfırlama` | s. 46'da başlık ve "Şifrenizi unuttuysanız…" metni | ✅ |
| 115 | s. 52 / `7.4 Döviz alım satımı ve hafta sonu kuru` | s. 52'de başlık ve "Raşit Mobil, İnternet Şubesi…" metni | ✅ |
| 130 | s. 59 / `8.7 Müşteri hizmetleri ve iletişim kanalları` | s. 59'da başlık ve tablo satırı "Müşteri Hizmetleri 0850 724 72 74 7 gün 24 saat" | ✅ |
| 122 | s. 56 / `7 Yatırım · Sık sorulanlar` | Başlık (SSS kısmı) doğru. Sayfa: parça "Raşit Mobil'den limit emri verebilirsiniz…" (s. 55 sonu, örtüşme satırları) ile başlıyor; asıl yeni metin ("Döviz alırken komisyon…") s. 56'da. Kural "sayfa = ilk yeni satırın sayfası" ile tutarlı, ama parçanın görünen ilk metni s. 55'te (bkz. Optional-1) | ✅ (kuralla uyumlu, nota bak) |

Ek canlı kontroller (DB): üst/alt bilgi sızıntısı (`Sayfa N / 70`, `Rehberi · Sürüm`) = 0 parça; `page_number` 5-70 (kapak/içindekiler yok).

## Sözleşme uyumu
- `GET /api/workspaces/{workspaceId}/ingestion-jobs/{jobId}`: `IngestionJobResponse` 15 alanı (`jobId … statusUrl`) openapi şemasıyla birebir (canlı JSON'da 15 alan, null'lar `null`). Alan adları, tipler, nullable'lar (`failedStage`, `totalChunks`, `errorCode`, `startedAt`, `finishedAt`) aynı.
- Canlı hata denemeleri (retrieval-api 8080): `/42/ingestion-jobs/99` → 404 `INGESTION_JOB_NOT_FOUND`; `/43/ingestion-jobs/1` → 404 `WORKSPACE_NOT_FOUND`; `/42/ingestion-jobs/abc` → 400 `BAD_REQUEST`. Üçü de `application/problem+json`, `type` = `https://rag.example/errors/<kod>`, `instance` dolu. Nginx üzerinden (`localhost:3000/api/workspaces/42/ingestion-jobs/1`) aynı JSON.
- İşin `errorCode` değerleri (`PDF_UNREADABLE`, `PDF_NO_SECTIONS`, `EMBEDDING_UNAVAILABLE`, `EMBEDDING_DIMENSION_MISMATCH`, `EMBEDDING_MODEL_MISMATCH`, `INGESTION_INTERRUPTED`, `INTERNAL_ERROR`) kodda `BaseException` kategorilerinden ve sabitlerden geliyor; hepsi `IngestionErrorCode` enum'unda. Canlıda `INGESTION_INTERRUPTED` ve (devops) `EMBEDDING_UNAVAILABLE` görüldü.
- assistant-api'de bu task'ta uç yok; sözleşmede de yok.
- SSE/WebSocket bu task'ta yok.

## Komutlar (kendi çalıştırdıkların)
| Komut | Sonuç | Raporla tutarlı mı |
|---|---|---|
| `cd retrieval-api && ./mvnw verify` | BUILD SUCCESS; 104 test, 0 başarısız, 0 atlanan; "All coverage checks have been met" (JaCoCo ≥ %80) | Evet (tester: 104 test, %90,8) |
| `cd assistant-api && ./mvnw verify`, `npm run build/lint/test` | Uygulanamaz: bu task'ta assistant-api ve web değişmedi | — |
| `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | BUILD SUCCESS; 353 mutant, 272 öldü (%77), test gücü %88, satır coverage %91 | Evet |
| `docker compose ps` | postgres, assistant-api, retrieval-api, web: hepsi healthy | Evet |
| Statik analiz (SpotBugs/PMD/Checkstyle) | Atlandı (yapılandırılmamış) | — |
| Proje kuralı grep'i (`import lombok`, `@MockBean`, `System.out`, `printStackTrace`, `TODO`) | 0 bulgu | — |

## Mutation testi
PIT 1.30.1, hedef `com.rasitbank.retrieval.*`: **%77** (272/353), test gücü **%88**. Projede ayrı bir eşik tanımlı değil. `TextChunker`, `EmbeddingClient`, `IngestionJobRunner` ve `PdfDocumentParser` için hayatta kalan mutant'lara baktım:
- `TextChunker` (satır 45, 60, 102, 118, 141, 150, 160): `<=`/`<` sınır varyantları ve toplama/çıkarma; 1.600/2.000/200 sınırlarının kendisi testlerle ölüyor (T1-T8 + sınır testleri). Kalanlar eşdeğer ya da savunmacı dallar; task kuralını kaçıran bir mutant görmedim.
- `PdfDocumentParser` (66, 89, 101, 102, 144): `PDFTextStripper` ayarları (`setSortByPosition`, `setLineSeparator`) ve `closeQuietly`; tek sütunlu fixture'larda fark yaratmıyor, gerçek rehberde `PdfDocumentParserGuideTest` ve canlı sonuçla korunuyor.
- `EmbeddingClient` (65, 89, 95, 109, 118, 125, 128): `modelLabel()`, log ve semaphore bekleme dalları, `closeQuietly` benzeri; yeniden deneme sayısı, boyut kontrolü, 4xx'in denenmemesi ve en çok 2 eşzamanlı istek testle ölüyor.
- Geri kalanı JPA getter'ları, `@Bean` dönüşleri ve log metinleri (tester raporuyla aynı). Task kuralını doğrulamayan, önemli bir hayatta kalan yok.

## Uçtan uca
Playwright: uygulanamaz (ekran yok, `web/` değişmedi). Sistem Docker'da `localhost:3000`'de açık; retrieval-api doğrudan 8080'de.

| Senaryo | Sonuç |
|---|---|
| Temiz DB'de ilk açılış (gerçek PDF + gerçek bge-m3, Ollama lokal) | İş 1 DONE; kaynak 10001, sürüm 2026.10, 154 parça, hepsi 1024 boyut, ort. token 285,4, süre ~7,5 sn |
| Yeniden başlatma (`restart retrieval-api`) | `ALREADY_INGESTED`: kaynak 1, iş 1, parça 154 (değişmedi) |
| **Gerçek kesinti:** `truncate … restart identity` ile temiz DB, servis başlar, iş `EMBEDDING` olunca `docker kill` (SIGKILL), servis tekrar başlatılır | Öldürme anında `1 \| EMBEDDING`, parça 0. Yeniden açılışta log `Interrupted ingestion jobs marked failed count=1`; iş 1 `FAILED`, `failedStage=EMBEDDING`, `errorCode=INGESTION_INTERRUPTED`; rehber yeniden girdi: iş 2 `DONE`; sources 1 (10001), chunks 154, source_versions 1. Yani gerçek süreç kesintisi, devops'un SQL benzetiminden bağımsız doğrulandı |
| Ollama kapalı | Ben ayrıca denemedim (Ollama'yı kapatmak lead'in diğer işlerini etkileyebilir); devops'un (3) denemesini (`OLLAMA_BASE_URL` ulaşılamaz → `FAILED`, `failedStage=EMBEDDING`, `EMBEDDING_UNAVAILABLE`, chunks 0, ardından düzeltilip yeniden açılışta iş DONE, kaynak tek) ve test `GuideBootstrapIntegrationTest#givenEmbeddingUnavailable…` + `#givenPreviousBootstrapFailed…` kanıt olarak kabul ettim |
| Dosya yok / log sızıntısı | Log taraması 0 eşleşme; dosya yok durumu `…#givenBootstrapFileMissing…` ile test edildi |

**Sistemin son durumu:** Kesinti denemesi veriyi değiştirdiği için sistemi temiz hâle döndürdüm: retrieval tabloları sıfırlandı (`restart identity`), servis yeniden açıldı; şu an iş 1 DONE, kaynak 10001, 154 parça, restart sonrası sources 1 / jobs 1 / chunks 154. Sistem **açık bırakıldı** (localhost:3000, 8080, 8090).

Dil modeli içeren soru/cevap denemesi bu task'ta yok (sadece bge-m3 embedding).

## Kod review bulguları
| Severity | Dosya:satır | Bulgu | Sorumlu rol |
|---|---|---|---|
| Optional | `ingestion/chunking/TextChunker.java` (sayfa kuralı) | Parça örtüşme satırlarıyla başlıyorsa sayfa, ilk yeni satırın sayfasıdır. Örnek chunk 122: görünen ilk metin (s. 55'te) "Raşit Mobil'den limit emri verebilirsiniz…", ama sayfa 56. Ayrıca örtüşme cevabı alıp sorusunu ("Dolar belli bir kura gelince…") dışarıda bırakıyor. Onay kartında "sayfa yaklaşıktır" riski olarak kabul edilmişti ve kural plana uygun. TASK-006/007'de alıntı "s. 56" derken bilgi s. 55'te olabilir. Takip task'ında örtüşmeyi soru satırını da içerecek şekilde ya da sayfayı örtüşme satırının sayfası yapmak değerlendirilebilir | backend-coder (ayrı task) |
| Optional | `config/PathConversionConfig.java` | Plan dışı sınıf; ama gerekçeli: Spring'in varsayılan `PathEditor`'ü `../docs/...` yolunu classpath kaynağı sanıp servisi açtırmıyordu (backend raporunda ve CLAUDE.md'de yazılı). Küçük (20 satır), tek amaç, konvansiyona uygun. **Kabul edilebilir.** | — |
| FYI | `src/test/resources/fonts/LiberationSans-Regular.ttf` | Plan dışı test kaynağı (410 KB, SIL OFL); kullanıcı onaylı, repoda kalacak. Bulgu değildir. | — |
| Optional | `ingestion/service/IngestionJobRunner.java:submit`, `IngestionService.queueIngestion` | PDF baytları hem DB'ye hem executor kuyruğundaki lambda'ya (bellekte) gider. Rehber için (1,1 MB) sorun yok; TASK-015'te büyük yüklemede kuyruktaki işler bellekte baytları tutar. İşin kendi ihtiyacına göre `ingestion_job_files`'tan okuması düşünülebilir | backend-coder (TASK-015) |
| Nit | `ingestion/parsing/PdfDocumentParser.java:146` | Gövde içinde yorum satırı var; politika "koddan çıkarılamayan neden için tek satır" izin veriyor, uygun. | — |
| FYI | `ingestion/service/GuideBootstrap.java` | `bootstrapGuide` ile aynı anda iki servis kopyası çalışırsa kontrol yarışı olur; kısmi unique index (`ingestion_jobs_one_active_per_source`) ve `UNIQUE(workspace_id,name)` bunu veritabanında engelliyor, tasarım doğru. | — |
| FYI | `docker kill` denemesi | SIGKILL'de iş EMBEDDING'de kalıyor, açılışta `INGESTION_INTERRUPTED`; SIGTERM yolunda `shutdownNow` + "interrupted by shutdown" log'u (`failJob`) var. Beklenen davranış. | — |

Gözlenen iyi uygulamalar: parçalar tek transaction'da yazılıyor (yarım parça yok, canlıda `chunks=0` doğrulandı); her aşama kendi kısa transaction'ında (dış model çağrısı sırasında transaction açık değil); thread-safe parçalayıcı; `errorCode` sabitleri sözleşmeyle bire bir; loglar sınırlara uyuyor.

Dependency: sadece `pdfbox` 3.0.6 ve `hibernate-vector` (Boot yönetir); ikisi de kullanıcı onayıyla. Başka eklenen yok.

## Düzeltme
Yok. Critical ya da Required bulgu yok. Optional maddeler (özellikle sayfa/örtüşme davranışı) hafızadaki kural gereği ayrı takip task'ı olarak lead'e bırakıldı.

STATUS: DONE
