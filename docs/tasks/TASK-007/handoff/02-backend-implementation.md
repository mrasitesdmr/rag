TASK: TASK-007
# Backend uygulama raporu

## Kullanılan skill'ler
`backend-conventions`, `springboot-patterns`, `problem-details-rfc9457`, `jpa-patterns`, `postgres-patterns`.

## Yapılanlar
Önceki backend-coder oturumundan kalan commit'lenmemiş değişiklikleri plana göre inceledim. `ChunkRepository` (filtre koşulu, `findByIdAndWorkspaceId`, `averageTokenCountsByVersionIds`), `VersionTokenAverage`, `IngestionJobRepository` (`findBySourceIdOrderByIdDesc`, `findLatestBySourceIds`), `SourceResponse.of`, `SourceFile`, `WorkspaceResponse`, `WorkspaceService.getWorkspaceResponse`, `WorkspaceController` ve yeni `package-info.java` dosyaları plana uyuyordu, aynen korudum. Eksik kalanları tamamladım.

| Plan maddesi / Kural # | Dosya(lar) | Not |
|---|---|---|
| Kararlar #1, K1c, K5c: `GET /api/workspaces/{workspaceId}` | `workspace/controller/WorkspaceController`, `workspace/domain/dto/WorkspaceResponse`, `workspace/service/WorkspaceService` | Önceki oturumdan kaldı, kontrol edildi. `HNSW` / `COSINE` sabit, parçalama değerleri `IngestionProperties`'ten geliyor |
| Kararlar #2, #3, #11, K1a: `SourceResponse` genişledi | `source/domain/dto/SourceResponse` | `of(Source, SourceVersion?, Double?, IngestionJob)`. `from(Source)` silindi. Ortalama `Math.round` ile yuvarlanıyor (yarım yukarı) |
| `listSources`: toplu sorgular, N+1 yok | `source/service/SourceService` | Kaynak sayfası alınıyor, ardından üç toplu sorgu çalışıyor: güncel sürümler (`findAllById`), ortalamalar (`averageTokenCountsByVersionIds`), son işler (`findLatestBySourceIds`). Güncel sürümü olan kaynak yoksa ilk iki sorgu atlanıyor. Sonuç `PageImpl` ile sarılıyor |
| `getSource`, B6/B7 | `SourceService.getSource`, `SourceController.getSource` | `ensureWorkspaceExists` önce çalışıyor, sonra `findByIdAndWorkspaceId`. Kaynak yoksa 404 `SOURCE_NOT_FOUND` |
| Kararlar #7, K5b, K11: PDF ucu | `SourceService.getSourceFile`, `SourceController.getSourceFile` | Güncel sürüm varsa `ingestionJobId` üzerinden o işin dosyası, yoksa `findFirstBySourceIdOrderByIdDesc` dönüyor. Başlıklar: `application/pdf`, `ContentDisposition.inline().filename(name, UTF_8)` (ASCII olmayan adda `filename*` ekleniyor), `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`. Log: `Source file served sourceId= jobId= fileSizeBytes=` |
| K6, K7d: kaynağın işleri | `SourceController.listIngestionJobs`, `IngestionService.listIngestionJobs` | `id DESC`, sayfalı, `page`/`size` doğrulaması standart kalıpta. 404'ler önce. `SOURCE_NOT_FOUND` oluşturma kodu `sourceNotFound()` yardımcısına alındı, `retryIngestion` da onu kullanıyor |
| Kararlar #4, K7b, K7c: parça listesi + filtre | `chunk/controller/ChunkController`, `chunk/service/ChunkService`, `chunk/repository/ChunkRepository`, `chunk/domain/dto/ChunkSummaryResponse` | `query` için `@Size(max = 200)`, mesaj `size must be at most 200 characters`. Güncel sürüm yoksa sorgu atılmadan `Page.empty(...)` dönüyor. `normalizeQuery`: `strip` → boşsa `null` → `toLowerCase(tr)` → `\s+` → tek boşluk |
| Kararlar #5, #18, K8b, K8c, K8e: tek parça | `ChunkService.getChunk`, `ChunkController.getChunk`, `chunk/domain/dto/ChunkResponse` | Çalışma alanındaki her parça dönüyor, eski sürüm dahil. `embeddingPreview` = `Arrays.copyOf(embedding, min(8, length))`, `embeddingDimensions` = `embedding.length`, `embeddingModel` = `chunks.embedding_model` (`bge-m3@1024`). Parça yoksa 404 `CHUNK_NOT_FOUND` |
| `retrieval-api/CLAUDE.md` | `retrieval-api/CLAUDE.md` | Durum yorumu güncellendi. "Kapsam"a okuma uçları ve filtre kuralı eklendi. "Paket yapısı"na yeni sınıflar eklendi. "Henüz olmayanlar"dan okuma uçları çıkarıldı. "Komutlar"a okuma ucu `curl` örnekleri eklendi |

## Değişen dosyalar
Hepsi `retrieval-api/` altında.
- **Yeni:** `src/main/java/com/rasitbank/retrieval/chunk/controller/ChunkController.java`, `chunk/controller/package-info.java`, `chunk/service/ChunkService.java`, `chunk/service/package-info.java`, `chunk/domain/dto/ChunkSummaryResponse.java`, `chunk/domain/dto/ChunkResponse.java`, `chunk/domain/dto/package-info.java`, `chunk/repository/VersionTokenAverage.java`, `source/domain/dto/SourceFile.java`, `workspace/controller/WorkspaceController.java`, `workspace/controller/package-info.java`, `workspace/domain/dto/WorkspaceResponse.java`, `workspace/domain/dto/package-info.java`
- **Değişen:** `chunk/repository/ChunkRepository.java`, `ingestion/repository/IngestionJobRepository.java`, `ingestion/service/IngestionService.java`, `source/controller/SourceController.java`, `source/domain/dto/SourceResponse.java`, `source/service/SourceService.java`, `workspace/service/WorkspaceService.java`, `CLAUDE.md`
- **Silinen:** yok (sadece `SourceResponse.from(Source)` metodu).
- **Dependency / plugin / migration:** yok.

## Plandan sapmalar
İkisi de sözleşmeyi değiştirmiyor. Uygulamadan önce lead'e mesajla bildirdim.
1. **`ChunkController`'da `@Validated` yok.** Plan istiyordu. Servisteki hiçbir controller `@Validated` kullanmıyor. Sınıf düzeyinde `@Validated` olunca doğrulamayı AOP yapar ve `ConstraintViolationException` fırlatır. `GlobalExceptionHandler` bunu 500'e çevirirdi. Spring'in yerleşik yöntem doğrulaması (`HandlerMethodValidationException`) zaten 400 `VALIDATION_FAILED` + `violations[].field = query` dönüyor. Bunu gerçek HTTP isteğiyle doğruladım.
2. **`EMBEDDING_PREVIEW_SIZE` iki yerde aynı değeri taşıyor.** Asıl sabit `ChunkResponse.EMBEDDING_PREVIEW_SIZE = 8`, çünkü önizlemeyi DTO'nun `of(...)` metodu kesiyor. Plandaki ad `ChunkService.EMBEDDING_PREVIEW_SIZE` de duruyor ve aynı sabiti gösteriyor.

## Doğrulama
- `cd retrieval-api && ./mvnw -q compile` → başarılı (çıktı yok).
- `cd retrieval-api && ./mvnw clean verify -Djacoco.haltOnFailure=false` → **BUILD FAILURE, test derleme hatası.** `src/test/.../source/controller/SourceControllerTest.java:178` hâlâ eski 5 argümanlı `new SourceResponse(10002, 42, "Belge B", SourceKind.PDF, CREATED)` constructor'ını kullanıyor. Aynı test `containsOnlyKeys("sourceId","workspaceId","name","kind","createdAt")` ile eski alan kümesini doğruluyor. Plan `SourceResponse`'u sözleşmeye göre genişlettiği için bu kırılma bekleniyordu. Test dosyası tester'ın olduğundan dokunmadım.
- Kalan testleri görmek için servisi scratchpad'e kopyaladım, kopyada sadece `SourceControllerTest`'i çıkardım ve `./mvnw verify -Djacoco.haltOnFailure=false` çalıştırdım → **BUILD SUCCESS, Tests run: 241, Failures: 0, Errors: 0, Skipped: 0.** Repodaki hiçbir dosya değişmedi. JaCoCo eşik kontrolü kapalıydı, çünkü yeni kodun testleri henüz yok.
- Uçları ayrıca yine kopyada, geçici bir duman testiyle gerçek pgvector'e (Testcontainers) karşı denedim. Rehber PDF'i sahte embedding'le `Şirket El Kitabı.pdf` adıyla yükledim. Bu test repoya eklenmedi. Sonuçlar:
  - `GET /api/workspaces/42` → 200, beklenen değerler. `/43` → 404 `WORKSPACE_NOT_FOUND`.
  - `GET .../sources` ve `.../sources/10001` → 200, `pageCount` 70, `chapterCount` 11, `chunkCount` 154, `averageTokenCount` **283**, `latestJob.stage` `DONE`. `/99999` → 404 `SOURCE_NOT_FOUND`.
  - `.../ingestion-jobs` → 200, tek iş.
  - `.../chunks?page=1&size=12` → ilk `chunkNo` 12. `query="  ASGARİ   ödeme "` eşleşti. `query="   "` filtresiz döndü (null bağlama sorunsuz). 201 karakterlik `query` → 400 `VALIDATION_FAILED`, `field` `query`.
  - `GET .../chunks/{id}` → 200, 8 değerlik önizleme.
  - `.../file` → 200, gövde yüklenen baytlarla aynı. Başlıklar: `content-type: application/pdf`, `content-disposition: inline; filename="Sirket El Kitab_.pdf"; filename*=UTF-8''%C5%9Eirket%20El%20Kitab%C4%B1.pdf`, `x-content-type-options: nosniff`, `cache-control: no-store`.

## Tester için notlar
- **Önce `SourceControllerTest`'i düzelt:** satır 178'deki constructor 14 argüman alıyor. `containsOnlyKeys` listesi yeni alanları içermeli: `fileName`, `fileSizeBytes`, `version`, `pageCount`, `chapterCount`, `chunkCount`, `averageTokenCount`, `syncedAt`, `latestJob`. Bu düzelmeden repoda `./mvnw verify` derlenmez.
- **B2'deki beklenen değer:** Plan `averageTokenCount` için 285 bekliyor. Gerçek rehberle bu veritabanında `round(avg(token_count))` = **283** çıkıyor. `token_count` parçalayıcıdan geliyor (karakter / 4), embedding'den bağımsız. Beklenen değeri fixture'dan ya da SQL'den bağımsız hesapla.
- **Seam'ler:**
  - `ChunkService.normalizeQuery` statik ve saf; B18 için doğrudan unit test edilebilir.
  - `SourceResponse.of` statik; B3 ve B5 için entity kurmak yeterli. `Source`, `SourceVersion` ve `IngestionJob`'un id'si sadece persist ile dolar, unit testte Mockito ya da gerçek DB gerekir.
  - `ChunkResponse` `float[]` taşıdığı için record `equals` dizi içeriğini karşılaştırmaz; alanları tek tek doğrula.
  - Embedding testte `@MockitoBean EmbeddingClient` ile sahteleniyor (`AbstractIngestionIntegrationTest`).
- **Filtre SQL'i** `ChunkRepository.ACTIVE_VERSION_CHUNKS_CONDITION`'da: `translate(..., 'Iİ', 'ıi')` → `lower` → `regexp_replace('\s+', ' ')` → `strpos`. Noktalama korunuyor, bu yüzden B12'deki "Asgari-ödeme" eşleşmez. `lower()` veritabanının diline bağlı; Testcontainers'taki `pgvector/pgvector:0.8.1-pg18` imajı `ş, ğ, ö, ü, ç`'yi doğru çeviriyor (plan riski 2).
- **B15:** Güncel sürümü olmayan kaynakta `findActiveVersionChunks` hiç çağrılmıyor, `Page.empty(PageRequest.of(page, size))` dönüyor.
- **B9 (sürümsüz kaynağın PDF'i):** `findFirstBySourceIdOrderByIdDesc` kullanılıyor. **B4 / B10:** son iş `max(id)`, liste `id DESC`.
- **B19 (log):** Sadece `Source file served sourceId={} jobId={} fileSizeBytes={}` yazılıyor; dosya adı ve kaynak adı loglanmıyor. Liste ve okuma uçları log yazmıyor.
- **Zaman:** Okuma uçlarında `Clock` kullanılmıyor; `syncedAt` sürüm satırından geliyor (işin `finishedAt`'ı ile aynı an).
- **Göreli yol:** Mevcut entegrasyon testleri `../docs/rehber/...` yolunu okuyor; testler `retrieval-api/` içinden çalıştırılmalı.

STATUS: DONE
